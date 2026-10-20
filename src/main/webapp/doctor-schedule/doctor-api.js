(function (window) {
  'use strict';

  function responseMessage(xhr) {
    if (!xhr) return '';
    if (xhr.responseJSON && xhr.responseJSON.message) return xhr.responseJSON.message;
    if (!xhr.responseText) return '';
    try {
      var body = JSON.parse(xhr.responseText);
      return body && body.message ? body.message : '';
    } catch (ignore) {
      return '';
    }
  }

  /**
   * Turns protected-API failures into messages suitable for doctors.  The server
   * message is preferred where available because it contains business rules
   * such as an appointment's current state; HTTP fallbacks keep malformed and
   * reverse-proxy responses understandable without exposing raw payloads.
   */
  function doctorApiErrorMessage(xhr, fallback) {
    var message = responseMessage(xhr);
    if (message) return message;
    if (xhr && xhr.status === 401) return '请先登录医生账号。';
    if (xhr && xhr.status === 403) return '当前账号无权执行此操作。';
    if (xhr && xhr.status === 404) return '未找到对应预约或资源。';
    if (xhr && xhr.status === 409) return '当前数据状态已变化，请刷新页面后重试。';
    return fallback || '请求失败，请稍后重试。';
  }

  window.doctorApiErrorMessage = doctorApiErrorMessage;
}(window));
