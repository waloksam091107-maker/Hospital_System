
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
<div class="panel admin-panel">
    <div class="panel-head"><strong class="icon-reorder"> 内容列表</strong></div>
    <div class="padding border-bottom">
        <button type="button" class="button border-yellow" onclick="window.location.href='/first'"><span class="icon-plus-square-o"></span> 添加患者</button>
    </div>
    <table class="table table-hover text-center">
        <tr>

            <th>编号</th>
            <th>id卡号</th>
            <th>密码</th>
            <th>姓名</th>
            <th>图片</th>
            <th>电话</th>
            <th>邮箱</th>
            <th>操作</th>
        </tr>
        <c:forEach items="${pageInfo.list}" var="dept" varStatus="vs">
            <tr>
                <td>${vs.index+1}</td>
                <td>${dept.idCardNumber}</td>
                <td>${dept.password}</td>
                <td>${dept.name}</td>
                <td>${dept.avatar}</td>
                <td>${dept.phone}</td>
                <td>${dept.email}</td>
                <td>


                    <div class="button-group"> <a class="button border-main" href="/patients/edit-page?id=${dept.patientId}"><span class="icon-edit"></span> 修改</a> <a class="button border-red" href="javascript:void(0)" onclick="return confirmDeletion(${dept.patientId})"><span class="icon-trash-o"></span> 删除</a> </div>
                </td>
            </tr>

        </c:forEach>













        <script>
            function confirmDeletion(departmentId) {
                var confirmation = confirm("确定要删除这个患者记录吗？");
                if (confirmation) {
                    var form = document.createElement('form');
                    form.method = 'POST';
                    form.action = '/departments/delete';
                    var input = document.createElement('input');
                    input.type = 'hidden';
                    input.name = 'id';
                    input.value = departmentId;
                    form.appendChild(input);
                    document.body.appendChild(form);
                    form.submit();
                }
                return false; // 阻止默认的<a>标签行为
            }
        </script>












        <tr>
            <td colspan="8">
                <div class="pagelist">
                    <a href="/list_patient?pageNum=${pageInfo.prePage}">上一页</a>
                    <c:forEach items="${pageInfo.navigatepageNums}" var="i">
                        <a href="/list_patient?pageNum=${i}" class="current">${i}</a>
                    </c:forEach>


                    <c:if test="${pageInfo.isLastPage}">
                        <a href="javascript:alert('已经是最后一页了')">下一页</a>
                    </c:if>
                    <c:if test="${not pageInfo.isLastPage}">
                        <a href="/list_patient?pageNum=${pageInfo.nextPage}">下一页</a>
                    </c:if>



                    <a href="/list_patient?pageNum=${pageInfo.pages}">尾页</a>
                </div>
            </td>
        </tr>

    </table>





























































































</body>
</html>
