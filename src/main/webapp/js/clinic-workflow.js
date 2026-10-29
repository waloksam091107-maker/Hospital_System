(function () {
  'use strict';
  const context = location.pathname.replace(/\/[^/]+$/, '');
  const byId = id => document.getElementById(id);
  const periodText = value => ({ AM: '上午', PM: '下午', EVENING: '晚间' }[value] || value || '');
  const queueText = value => ({ NOT_CHECKED_IN: '未签到', UNCHECKED: '未签到', WAITING: '候诊中', CHECKED_IN: '候诊中', CALLED: '已叫号', SKIPPED: '已过号', IN_PROGRESS: '接诊中', COMPLETED: '已完成', DONE: '已完成', REMOVED: '已移出队列', NO_SHOW: '爽约', CANCELLED: '已取消', RESCHEDULED: '已改期' }[value] || value || '未签到');
  const leaveText = value => ({ PENDING: '待审核', APPROVED: '已批准停诊', REJECTED: '已拒绝', WITHDRAWN: '已撤回', REVOKED: '已撤销 / 恢复' }[value] || value);
  let role = '', csrf = '', busy = false;
  function chinaNow() { return new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Shanghai' })); }
  function today() { const d = chinaNow(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); }
  function message(text, error) { byId('message').textContent = text || ''; byId('message').className = error ? 'error' : 'success'; }
  async function json(path, options) {
    const response = await fetch(context + path, Object.assign({ credentials: 'same-origin' }, options));
    let data; try { data = await response.json(); } catch (_) { throw new Error('服务器未返回有效数据，请刷新重试。'); }
    if (!response.ok || data.success === false) throw new Error(data.message || '操作失败，请刷新后重试。');
    return data;
  }
  function cell(row, text) { const td = document.createElement('td'); td.textContent = text == null ? '' : text; row.appendChild(td); return td; }
  function rows(id, list, render, count) {
    const body = byId(id); body.replaceChildren();
    if (!list.length) { const row = body.insertRow(); cell(row, '暂无记录').colSpan = count; return; }
    list.forEach(item => render(body.insertRow(), item));
  }
  function button(parent, label, action) { const b = document.createElement('button'); b.type = 'button'; b.textContent = label; b.addEventListener('click', action); parent.appendChild(b); return b; }
  async function mutate(path, data, confirmation, after) {
    if (busy || (confirmation && !window.confirm(confirmation))) return;
    busy = true; const buttons = Array.from(document.querySelectorAll('button')); buttons.forEach(b => { b.dataset.wasDisabled = String(b.disabled); b.disabled = true; });
    try {
      const result = await json(path, { method: 'POST', headers: { 'X-CSRF-Token': csrf, 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' }, body: new URLSearchParams(data) });
      message(result.message || '操作成功。');
      if (after) await after();
    } catch (error) { message(error.message, true); }
    finally { busy = false; buttons.forEach(b => { if (b.isConnected) b.disabled = b.dataset.wasDisabled === 'true'; }); }
  }
  function ended(item) { return item.appointmentDate < today() || (item.appointmentDate === today() && chinaNow().getHours() >= ({ AM: 12, PM: 17, EVENING: 21 }[item.appointmentTime] || 24)); }
  async function loadQueue() {
    const params = new URLSearchParams({ date: byId('queueDate').value, period: byId('queuePeriod').value });
    const data = await json('/clinic/queue?' + params);
    rows('queueRows', data.list || [], (row, item) => {
      cell(row, '预约号 ' + (item.appointmentSequence == null ? '—' : item.appointmentSequence));
      cell(row, (item.patientName || '本人预约') + ' / ' + item.appointmentId);
      cell(row, item.appointmentDate + ' ' + periodText(item.appointmentTime));
      const active = item.status === 'BOOKED', q = item.queueStatus || 'NOT_CHECKED_IN';
      cell(row, active && item.leaveBlocked ? '停诊：请改期或取消' : queueText(active ? q : item.status));
      const actions = cell(row, '');
      const queueAction = (action, label, confirmText) => button(actions, label, () => mutate('/clinic/queue', { action, appointmentId: item.appointmentId }, confirmText, loadQueue));
      if (role === 'PATIENT' && active && !item.leaveBlocked && ['NOT_CHECKED_IN', 'UNCHECKED'].includes(q) && item.appointmentDate === today() && !ended(item)) queueAction('check-in', '签到', '确认本人已到院，进行签到？');
      if (role === 'DOCTOR' && active && !item.leaveBlocked) {
        if (q === 'CALLED') {
          button(actions, '开始接诊', () => mutate('/appointments/start', { appointmentId: item.appointmentId }, '确认开始本次接诊？', () => { location.href = context + '/doctor-schedule/encounter.html?appointmentId=' + encodeURIComponent(item.appointmentId); }));
          queueAction('skip', '过号', '确认患者未到场，标记过号？');
        }
        if (q === 'SKIPPED' && item.appointmentDate === today() && !ended(item)) queueAction('requeue', '恢复候诊', '确认将患者恢复到候诊队尾？');
        if (ended(item) && ['NOT_CHECKED_IN', 'UNCHECKED', 'SKIPPED'].includes(q)) queueAction('no-show', '确认爽约', '确认患者未到诊？爽约占用本次额度且不退款。');
      }
      if (role === 'DOCTOR' && item.status === 'IN_PROGRESS') { const a = document.createElement('a'); a.href = context + '/doctor-schedule/encounter.html?appointmentId=' + encodeURIComponent(item.appointmentId); a.textContent = '填写就诊记录'; actions.appendChild(a); }
      if (!actions.childNodes.length) actions.textContent = '—';
    }, 5);
  }
  async function loadSlots() {
    const data = await json('/clinic/slots?' + new URLSearchParams({ date: byId('slotDate').value }));
    rows('slotRows', data.list || [], (row, slot) => {
      cell(row, slot.scheduleDate + ' ' + periodText(slot.slotPeriod)); cell(row, '容量 ' + slot.capacity + ' / 已占用 ' + slot.occupiedCount + ' / 余号 ' + slot.remaining);
      cell(row, slot.leaveBlocked ? '停诊冻结' : slot.doctorEnabled === false ? '医生账号停用' : (slot.slotStatus === 'CLOSED' || slot.slotStatus === '0') ? '手工关闭' : slot.remaining <= 0 ? '已满' : '开放');
      const actions = cell(row, ''), form = document.createElement('form'), input = document.createElement('input');
      input.type = 'number'; input.name = 'capacity'; input.min = String(Math.max(1, slot.occupiedCount || 0)); input.max = '1000'; input.required = true; input.value = slot.capacity; input.setAttribute('aria-label', slot.scheduleDate + periodText(slot.slotPeriod) + '容量');
      form.appendChild(input); const save = button(form, '保存容量', () => {}); save.type = 'submit';
      form.addEventListener('submit', e => { e.preventDefault(); mutate('/clinic/slots', { action: 'capacity', slotId: slot.scheduleSlotId, capacity: input.value }, '确认调整该时段容量？', loadSlots); }); actions.appendChild(form);
    }, 4);
  }
  async function loadLeave() {
    const data = await json('/clinic/leave');
    rows('leaveRows', data.list || [], (row, item) => {
      cell(row, '#' + item.leaveId + ' / 医生 ' + item.doctorId); cell(row, item.startDate + ' 至 ' + item.endDate + ' / ' + String(item.periods || '').split(',').map(periodText).join('、'));
      cell(row, (item.reason || '') + '\n影响排班：' + (item.affectedSlots || 0) + ' / 影响预约：' + (item.affectedAppointments || 0)); cell(row, leaveText(item.status) + (item.reviewNote ? '：' + item.reviewNote : ''));
      const actions = cell(row, '');
      function action(code, label, needsReason) {
        button(actions, label, () => {
          let reason = ''; if (needsReason) { reason = window.prompt('请填写' + label + '意见 / 原因：'); if (reason == null) return; if (!reason.trim()) { message('请填写审核意见或撤销原因。', true); return; } }
          mutate('/clinic/leave', { action: code, id: item.leaveId, reason }, '确认' + label + '申请 #' + item.leaveId + '？', async () => { await loadLeave(); if (role === 'DOCTOR') await loadSlots(); });
        });
      }
      if (role === 'DOCTOR' && item.status === 'PENDING') action('withdraw', '撤回', false);
      if (role === 'ADMIN' && item.status === 'PENDING') { action('approve', '批准', true); action('reject', '拒绝', true); }
      if (role === 'ADMIN' && item.status === 'APPROVED') action('revoke', '撤销停诊', true);
    }, 5);
  }
  const safe = task => () => task().catch(error => message(error.message, true));
  byId('refreshQueue').addEventListener('click', safe(loadQueue)); byId('refreshSlots').addEventListener('click', safe(loadSlots)); byId('refreshLeave').addEventListener('click', safe(loadLeave));
  byId('callNext').addEventListener('click', () => mutate('/clinic/queue', { action: 'call-next', date: byId('queueDate').value, period: byId('queuePeriod').value }, '确认叫下一位已签到患者？', loadQueue));
  byId('leaveForm').addEventListener('submit', e => {
    e.preventDefault(); const start = byId('leaveStart').value, end = byId('leaveEnd').value, periods = Array.from(document.querySelectorAll('[name=period]:checked')).map(i => i.value).join(',');
    if (!periods || start < today() || end < start || (Date.parse(end) - Date.parse(start)) / 86400000 >= 31) { message('请选择未来或当日的日期、至少一个时段，范围不得超过 31 天。', true); return; }
    mutate('/clinic/leave', { action: 'apply', startDate: start, endDate: end, periods, reason: byId('leaveReason').value.trim() }, '确认提交停诊申请？审批通过后将通知受影响患者。', loadLeave);
  });
  (async () => {
    const profile = await json('/portal/profile'); role = profile.role;
    if (!['PATIENT', 'DOCTOR', 'ADMIN'].includes(role)) throw new Error('请先登录后使用门诊服务。');
    csrf = (await json('/security/csrf')).token;
    if (!csrf) throw new Error('安全校验初始化失败，请刷新页面。');
    byId('identity').textContent = ({ PATIENT: '患者', DOCTOR: '医生', ADMIN: '管理员' }[role]) + ' · ' + (profile.name || '');
    if (role === 'PATIENT') {
      const allPeriods = document.createElement('option'); allPeriods.value = ''; allPeriods.textContent = '全部时段';
      byId('queuePeriod').prepend(allPeriods); byId('queuePeriod').value = '';
    }
    ['queueDate', 'slotDate', 'leaveStart', 'leaveEnd'].forEach(id => { byId(id).value = today(); });
    ['leaveStart', 'leaveEnd'].forEach(id => { byId(id).min = today(); });
    byId('queuePanel').hidden = role === 'ADMIN'; byId('capacityPanel').hidden = role !== 'DOCTOR'; byId('leaveApplyPanel').hidden = role !== 'DOCTOR'; byId('leavePanel').hidden = role === 'PATIENT'; byId('callNext').hidden = role !== 'DOCTOR'; byId('patientQueueHelp').hidden = role !== 'PATIENT';
    if (role === 'DOCTOR') { byId('backLink').href = context + '/doctor-schedule/appointments.html'; await Promise.all([loadQueue(), loadSlots(), loadLeave()]); }
    else if (role === 'ADMIN') { byId('backLink').href = context + '/hospro/adm_main.jsp'; byId('backLink').textContent = '管理中心'; byId('leaveTitle').textContent = '停诊审批'; await loadLeave(); }
    else await loadQueue();
  })().catch(error => message(error.message, true));
}());
