
<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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
<div class="panel admin-panel">
  <div class="panel-head" id="add"><strong><span class="icon-pencil-square-o"></span>增加内容</strong></div>
  <div class="body-content">

    <form method="post" class="form-x" action="/menu/update" id="menuForm">
      <input type="hidden" name="id" value="${id}" />
      <input type="hidden" name="name" value="${name}" />


      <div class="form-group">
        <div class="label">
          <label>菜品编号：${id}</label>


        </div>
        <div class="label">
          <label>菜品名称：${name}</label>

        </div>

      <div class="form-group">
        <div class="label">
          <label>菜品价格：</label>
        </div>
        <div class="field">
          <input type="text" class="input w50" value="${price}" name="price" data-validate="required:请输入价格" />
          <div class="tips"></div>
        </div>


      <if condition="$iscid eq 1">
        <div class="form-group">
          <div class="label">
            <label>菜品分类：（原为${fenlei}）</label>
          </div>
          <div class="field">

            <select name="fenlei" class="input" style="width:200px; line-height:17px;" >
              <option value="-1">请选择分类</option>
              <option value="招牌">招牌</option>
              <option value="凉菜">凉菜</option>
              <option value="热菜">热菜</option>
              <option value="汤类">汤类</option>

            </select>
            <div class="tips"></div>
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

</body>

</html>

<script>
  $(function(){
    $.ajax({
      url: '/second/all',
      type: 'GET',
      dataType: "json",
      success: function (res) {

        console.log("res",res)
        $.each(res, function (i, v) {
          var optionObj = $("<option value='"+v.departmentId+"'>"+v.departmentName+"</option>");

          optionObj.appendTo($("select[name='deptId']"));


        });


      }
    });
  })

  // 修改：添加表单提交时的验证逻辑
  $("#menuForm").submit(function(e) {
    var fenlei = $("select[name='fenlei']").val();
    var price = $("input[name='price']").val().trim();

    if (fenlei == "-1") {
      alert("请选择菜品分类");
      e.preventDefault();  // 阻止表单提交
    } else if (price == "") {
      alert("请输入价格");
      e.preventDefault();  // 阻止表单提交
    }else{
      alert("修改成功！");
    }
  });



</script>
