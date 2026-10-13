
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="zh-cn">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <meta name="renderer" content="webkit">
  <title></title>
  <link rel="stylesheet" href="/css/pintuer.css">
  <link rel="stylesheet" href="/css/admin.css">
  <script src="/js/jquery.js"></script>
  <script src="/js/pintuer.js"></script>
--%>








      <div class="form-group">
        <div class="label">
          <label>管理员账号：</label>
        </div>
        <div class="field">
          <input type="text" class="input w50" id="mpass" name="mpass" size="50" placeholder="请输入管理员账号" data-validate="required:请输入管理员账号" />
        </div>
      </div>
      <div class="form-group">
        <div class="label">
          <label>邮箱地址：</label>
        </div>
        <div class="field">
          <input type="text" class="input w50" id="email" name="" size="50" placeholder="请输入验证邮箱地址" data-validate="required:请输入验证邮箱地址" />
          <button class="button button-small bg-main" type="button" id="getVerificationCode">获取验证码</button>
        </div>
      </div>
      <div class="form-group">
        <div class="label">
          <label>验证码：</label>
        </div>
        <div class="field">
          <input type="text" class="input w50" id="" name="" size="50" placeholder="请输入验证码" data-validate="required:请输入验证码" />
        </div>
      </div>

      <div class="form-group">
        <div class="label">
          <label for="">新密码：</label>
        </div>
        <div class="field">
          <input type="password" class="input w50" name="newpass" size="50" placeholder="请输入新密码" data-validate="required:请输入新密码,length#>=5:新密码不能小于5位" />
        </div>
      </div>
      <div class="form-group">
        <div class="label">
          <label for="">确认新密码：</label>
        </div>
        <div class="field">
          <input type="password" class="input w50" name="renewpass" size="50" placeholder="请再次输入新密码" data-validate="required:请再次输入新密码,repeat#newpass:两次输入的密码不一致" />
        </div>
      </div>

      <div class="form-group">
        <div class="label">
          <label></label>
        </div>
        <div class="field">
          <button class="button bg-main icon-check-square-o" type="submit"> 提交</button>
        </div>
      </div>
    </form>
  </div>
</div>
</body></html>
