(function () {
  'use strict';
  const context = location.pathname.split('/hospro/')[0], params = new URLSearchParams(location.search);
  const doctorId = params.get('doctor_id') || params.get('doctorId');
  const dateInput = document.getElementById('datepicker');
  const anchor = document.getElementById('005') || document.getElementById('book-btn');
  if (!anchor || !doctorId) return;
  const output = document.createElement('div'); output.id = 'bookingCapacity'; output.setAttribute('role', 'status'); output.style.cssText = 'padding:12px;margin:12px 0;background:#eef8f7;color:#16365c;line-height:1.8';
  anchor.parentNode.insertBefore(output, anchor);
  let generation = 0;
  async function load() {
    const date = dateInput ? dateInput.value : params.get('date'), current = ++generation;
    if (!date) { output.textContent = '选择日期后显示各时段容量和余号。'; return; }
    output.textContent = '正在查询余号…';
    try {
      const response = await fetch(context + '/clinic/slots?' + new URLSearchParams({ doctorId, date }), { credentials: 'same-origin' });
      const data = await response.json();
      if (current !== generation) return;
      if (!response.ok || data.success === false) throw new Error(data.message || '余号查询失败，请刷新后重试。');
      const list = (data.list || []).filter(s => !params.get('time') || s.slotPeriod === params.get('time'));
      output.replaceChildren();
      if (!list.length) { output.textContent = '所选日期 / 时段尚未排班。'; return; }
      list.forEach(s => { const p = document.createElement('p'); const state = s.leaveBlocked ? '（停诊）' : s.doctorEnabled === false ? '（医生停用）' : ['CLOSED', '0'].includes(s.slotStatus) ? '（已关闭）' : s.remaining <= 0 ? '（已满）' : ''; p.textContent = ({ AM: '上午', PM: '下午', EVENING: '晚间' }[s.slotPeriod] || s.slotPeriod) + '：容量 ' + s.capacity + ' / 余号 ' + s.remaining + state; output.appendChild(p); });
    } catch (error) { if (current === generation) output.textContent = error.message || '余号查询失败，请刷新后重试。'; }
  }
  if (dateInput) { dateInput.addEventListener('change', load); dateInput.addEventListener('input', load); }
  load();
}());
