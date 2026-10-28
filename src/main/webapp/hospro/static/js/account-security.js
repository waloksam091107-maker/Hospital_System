/* Account administration never inserts server content as HTML or retains temporary passwords. */
(function () {
  'use strict';
  var root = new URL('../', window.location.href), page = 1, resetId = null, resetRole = null, busy = false;
  function el(id) { return document.getElementById(id); }
  function message(text) { el('message').textContent = text; }
  async function api(path, options) {
    var response = await fetch(new URL(path, root), Object.assign({ credentials: 'same-origin' }, options));
    var result = await response.json();
    if (response.status === 401) { window.location.href = 'login.html'; throw new Error('请先登录。'); }
    if (!response.ok || result.success === false) throw new Error(result.message || '请求失败。');
    return result;
  }
  async function load() {
    try {
      var data = await api('portal/admin/account-security/list?role=' + el('role').value + '&page=' + page + '&pageSize=20');
      var tbody = el('accounts'); tbody.replaceChildren();
      data.items.forEach(function (account) {
        var tr = document.createElement('tr');
        [account.id, account.name, account.enabled ? '启用' : '停用', account.mustChangePassword ? '须修改临时密码' : '正常', account.lockedUntil ? new Date(account.lockedUntil).toLocaleString() : '未锁定'].forEach(function (value) { var td = document.createElement('td'); td.textContent = String(value); tr.appendChild(td); });
        var actions = document.createElement('td');
        [[account.enabled ? '停用' : '启用', account.enabled ? 'disable' : 'enable'], ['解除锁定', 'unlock'], ['重置密码', 'reset']].forEach(function (entry) {
          var button = document.createElement('button'); button.type = 'button'; button.textContent = entry[0];
          button.addEventListener('click', function () {
            if (entry[1] === 'reset') { resetId = account.id; resetRole = el('role').value; el('reset-target').textContent = account.name + '（编号 ' + account.id + '）'; el('reset-form').hidden = false; el('temporary-password').value = ''; el('temporary-password').focus(); }
            else mutate(entry[1], account.id, el('role').value);
          }); actions.appendChild(button);
        }); tr.appendChild(actions); tbody.appendChild(tr);
      });
      if (!data.items.length) { var tr = document.createElement('tr'), td = document.createElement('td'); td.colSpan = 6; td.textContent = '本页暂无账号'; tr.appendChild(td); tbody.appendChild(tr); }
      el('page').textContent = '第 ' + page + ' 页'; el('previous').disabled = page <= 1; el('next').disabled = data.items.length < 20;
    } catch (error) { message(error.message); }
  }
  async function mutate(action, id, role, password) {
    if (busy) return; busy = true;
    try {
      var token = await api('security/csrf');
      var body = new URLSearchParams({ accountId: id, role: role });
      if (password !== undefined) body.set('temporaryPassword', password);
      var result = await api('portal/admin/account-security/' + action, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': token.csrfToken || token.token }, body: body.toString() });
      message(result.message); closeReset(); await load();
    } catch (error) { message(error.message); } finally { busy = false; el('temporary-password').value = ''; }
  }
  function closeReset() { el('temporary-password').value = ''; el('reset-form').hidden = true; resetId = null; resetRole = null; }
  el('role').addEventListener('change', function () { page = 1; closeReset(); load(); });
  el('refresh').addEventListener('click', load);
  el('previous').addEventListener('click', function () { if (page > 1) { page--; load(); } });
  el('next').addEventListener('click', function () { page++; load(); });
  el('cancel-reset').addEventListener('click', closeReset);
  el('reset-form').addEventListener('submit', function (event) { event.preventDefault(); var password = el('temporary-password').value; if (password.length < 8 || new TextEncoder().encode(password).length > 72) { message('临时密码至少 8 个字符，UTF-8 编码不超过 72 字节。'); return; } el('temporary-password').value = ''; mutate('reset', resetId, resetRole, password); });
  load();
}());
