(function () {
  'use strict';
  if (window.hospitalNotificationCenterLoaded) return;
  window.hospitalNotificationCenterLoaded = true;
  var script = document.currentScript;
  var scriptUrl = new URL(script.src, window.location.href);
  var context = scriptUrl.pathname.substring(0, scriptUrl.pathname.lastIndexOf('/js/notification-center.js'));
  var badge, navigation;

  function element(tag, text, className) {
    var node = document.createElement(tag);
    if (text != null) node.textContent = text;
    if (className) node.className = className;
    return node;
  }
  async function json(path, options) {
    var response = await fetch(context + path, Object.assign({credentials: 'same-origin', cache: 'no-store'}, options || {}));
    var body;
    try { body = await response.json(); } catch (failure) { body = {}; }
    if (!response.ok || body.success === false) {
      var error = new Error(body.message || (response.status === 401 ? '登录已失效，请重新登录。' : '操作失败，请稍后重试。'));
      error.status = response.status; throw error;
    }
    return body;
  }
  async function post(path, values) {
    var csrf = await json('/security/csrf');
    return json(path, {method: 'POST', headers: {'X-CSRF-Token': csrf.token, 'Content-Type': 'application/x-www-form-urlencoded'}, body: new URLSearchParams(values || {}).toString()});
  }
  async function refreshBadge() {
    try {
      var result = await json('/notifications/unread-count');
      if (!Number.isSafeInteger(result.unreadCount) || result.unreadCount < 0) throw new Error('未读数量无效。');
      navigation.hidden = false; badge.hidden = result.unreadCount === 0;
      badge.textContent = result.unreadCount > 99 ? '99+' : String(result.unreadCount);
      badge.setAttribute('aria-label', result.unreadCount + ' 条未读通知');
    } catch (failure) {
      if (failure.status === 401) navigation.hidden = true;
      else { badge.hidden = true; navigation.title = '暂时无法读取未读数量，可打开通知页面重试。'; }
    }
  }
  function addNavigation() {
    var styles = element('style', '#notificationCenterNavigation[hidden],#notificationCenterBadge[hidden]{display:none!important}');
    document.head.appendChild(styles);
    navigation = element('nav'); navigation.id = 'notificationCenterNavigation'; navigation.setAttribute('aria-label', '账号与通知');
    navigation.style.cssText = 'display:flex;justify-content:flex-end;align-items:center;gap:18px;padding:10px 20px;background:#eff8f7;color:#183354;font:15px Arial,"Microsoft YaHei",sans-serif;position:relative;z-index:5;';
    var link = element('a', '站内通知 '); link.href = context + '/notifications.html';
    badge = element('span'); badge.id = 'notificationCenterBadge'; badge.hidden = true;
    badge.style.cssText = 'display:inline-block;background:#b43b32;color:white;padding:1px 6px;border-radius:12px;font-size:12px;';
    link.appendChild(badge); navigation.appendChild(link);
    var logout = element('button', '退出登录'); logout.type = 'button';
    logout.style.cssText = 'font:inherit;cursor:pointer;padding:5px 10px;border:1px solid #99b6b4;border-radius:4px;background:white;color:#18534f;';
    logout.addEventListener('click', async function () {
      logout.disabled = true;
      try { await post('/auth/logout'); window.location.assign(context + '/login.html'); }
      catch (failure) { logout.disabled = false; navigation.title = failure.message; feedback(failure.message, true); }
    });
    navigation.appendChild(logout); document.body.insertBefore(navigation, document.body.firstChild);
    refreshBadge(); window.addEventListener('focus', refreshBadge);
    document.addEventListener('hospital:notifications-changed', refreshBadge);
  }
  function feedback(text, error) {
    var target = document.getElementById('notificationFeedback');
    if (!target) {
      target = element('span'); target.setAttribute('role', 'status'); navigation.appendChild(target);
    }
    target.textContent = text || ''; target.className = error ? 'error' : '';
  }
  function targetUrl(item, role) {
    if (!/^\d+$/.test(String(item.targetId || ''))) return null;
    var id = encodeURIComponent(item.targetId);
    if (item.targetType === 'APPOINTMENT') {
      if (role === 'PATIENT') return context + '/hospro/patient.jsp?appointmentId=' + id;
      if (role === 'DOCTOR') return context + '/doctor-schedule/appointments.html?appointmentId=' + id;
    }
    if (item.targetType === 'MEDICAL_RECORD' && role === 'PATIENT') return context + '/hospro/medical-records.html?appointmentId=' + id;
    if (item.targetType === 'LEAVE' || item.targetType === 'CLINIC_LEAVE') return context + '/clinic-workflow.html?leaveId=' + id;
    return null;
  }
  function startPage() {
    if (!document.getElementById('notificationPage')) return;
    var list = document.getElementById('notificationList'), previous = document.getElementById('previousPage'), next = document.getElementById('nextPage');
    var all = document.getElementById('markAllRead'), reload = document.getElementById('reloadNotifications'), size = document.getElementById('pageSize');
    var page = 1, total = 0, busy = false, failed = false;
    function controls() {
      previous.disabled = busy || failed || page <= 1; next.disabled = busy || failed || page * Number(size.value) >= total;
      all.disabled = busy; reload.disabled = busy; size.disabled = busy; list.setAttribute('aria-busy', String(busy));
    }
    function draw(item, role) {
      var article = element('article', null, 'notification' + (item.readAt ? ' read' : ''));
      article.appendChild(element('h2', item.title)); article.appendChild(element('p', item.body));
      var time = element('time', item.createdAt || ''); article.appendChild(time);
      var actions = element('p', null, 'actions'), status = element('span', item.readAt ? '已读' : '未读', 'read-status'); actions.appendChild(status);
      if (!item.readAt) {
        var read = element('button', '标为已读'); read.type = 'button';
        read.addEventListener('click', async function () {
          read.disabled = true;
          try {
            await post('/notifications/read', {id: item.id}); article.classList.add('read'); status.textContent = '已读'; read.remove();
            feedback('已标为已读。'); refreshBadge();
          } catch (failure) { read.disabled = false; feedback(failure.message, true); }
        }); actions.appendChild(read);
      }
      var url = targetUrl(item, role);
      if (url) { var link = element('a', '查看相关业务'); link.href = url; actions.appendChild(link); }
      article.appendChild(actions); return article;
    }
    async function load(target) {
      if (busy) return;
      busy = true; failed = false; page = target; controls(); list.className = ''; list.textContent = '正在加载…'; feedback('');
      try {
        var result = await json('/notifications/list?page=' + page + '&pageSize=' + Number(size.value));
        if (!Array.isArray(result.items) || !Number.isSafeInteger(result.total) || result.total < 0) throw new Error('通知数据读取失败，请刷新重试。');
        total = result.total; list.textContent = '';
        result.items.forEach(function (item) { list.appendChild(draw(item, result.role)); });
        if (!result.items.length) list.appendChild(element('p', total ? '本页暂无通知，可返回上一页。' : '暂无站内通知。'));
        document.getElementById('pageInfo').textContent = '共 ' + total + ' 条，第 ' + page + ' / ' + Math.max(1, Math.ceil(total / Number(size.value))) + ' 页';
      } catch (failure) {
        failed = true; list.textContent = failure.message; list.className = 'error'; document.getElementById('pageInfo').textContent = '加载未完成';
        if (failure.status === 401) { var login = element('a', '前往登录'); login.href = context + '/login.html'; list.appendChild(login); }
      } finally { busy = false; controls(); }
    }
    previous.addEventListener('click', function () { load(page - 1); }); next.addEventListener('click', function () { load(page + 1); });
    reload.addEventListener('click', function () { load(page); refreshBadge(); }); size.addEventListener('change', function () { load(1); });
    all.addEventListener('click', async function () {
      if (busy) return;
      busy = true; controls();
      try { await post('/notifications/read-all'); busy = false; await load(page); feedback('全部通知已标为已读。'); refreshBadge(); }
      catch (failure) { busy = false; feedback(failure.message, true); controls(); }
    });
    load(1);
  }
  function start() { addNavigation(); startPage(); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start); else start();
}());
