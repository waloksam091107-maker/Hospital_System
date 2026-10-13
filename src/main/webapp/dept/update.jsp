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
        <form method="post" class="form-x" action="/update" onsubmit="return validateForm()">
            <div class="form-group">
                <div class="label">
                    <label>department_id：${id}</label>
                </div>
                <div class="field">
                    <input type="hidden" name="id" value="${id}" />
                    <label></label>
                    <div class="tips"></div>
                </div>
            </div>
            <div class="form-group">
                <div class="label">
                    <label>部门名称：</label>
                </div>
                <div class="field">
                    <input type="text" class="input" name="name" value=""/>
                </div>
            </div>
            <div class="form-group">
























            </div>











            <div class="form-group">
                <div class="label">
                    <label>部门介绍：</label>
                </div>




                <div class="field">
                    <input type="text" class="input" name="desc" value=""/>
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

<script>
    function validateForm() {
        var name = document.getElementsByName('name')[0].value;
        var desc = document.getElementsByName('desc')[0].value;
        if (name === "" || desc === "") {
            alert("部门名称与部门介绍不能为空！");
            return false;
        }
        return true;
    }
</script>

</body>
</html>
