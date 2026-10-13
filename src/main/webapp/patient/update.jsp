
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<!DOCTYPE html>
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
</head>
<body>
<div class="panel admin-panel margin-top">
  <div class="panel-head" id="add"><strong><span class="icon-pencil-square-o"></span>修改分类</strong></div>
  <div class="body-content">
    <form method="post" class="form-x" action="/update">
      <div class="form-group">
        <div class="label">
          <label>患者id：${id}</label>
        </div>
        <div class="field">
          <input type="hidden" name="id" value="${id}" />
          <label></label>
          <div class="tips"></div>
        </div>
      </div>
      <div class="form-group">
        <div class="label">
          <label>id_card_number：</label>
        </div>
        <div class="field">
          <input type="text" class="input" name="name" value=""/>
        </div>
      </div>
      <div class="form-group">




        <div class="form-group">
























        </div>



      </div>
      <div class="form-group">
        <div class="label">
          <label>password：</label>
        </div>
        <div class="field">
          <input type="text" class="input w80" name="path" value=""/>
        </div>
      </div>
      <div class="form-group">
        <div class="label">
          <label>name：</label>
        </div>




        <div class="field">
          <input type="text" class="input" name="desc" value=""/>
        </div>
      </div>

      <div class="form-group">
        <div class="label">
          <label>avatar：</label>
        </div>
        <div class="field">
          <input type="text" class="input w80" name="path" value=""/>
        </div>
      </div>
      <div class="form-group">
        <div class="label">
          <label>phone：</label>
        </div>
        <div class="field">
          <input type="text" class="input w80" name="path" value=""/>
        </div>
      </div>
      <div class="form-group">
        <div class="label">
          <label>email：</label>
        </div>
        <div class="field">
          <input type="text" class="input w80" name="path" value=""/>
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
