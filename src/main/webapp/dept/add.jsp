<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core_1_1" %>

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
    <div class="panel-head" id="add"><strong><span class="icon-pencil-square-o"></span>添加内容</strong></div>
    <div class="body-content">
        <form method="post" class="form-x" action="/save">




















            <if condition="$iscid eq 1">
                <div class="form-group">
                    <div class="label">
                        <label>上级科室：</label>
                    </div>
                    <div class="field">
                        <select name="deptPid" class="input w50">
                            <option value="-1">无</option>
                            <c:forEach items="${list}" var="dept">
                                <option value="${dept.departmentId }">${dept.departmentName}</option>
                            </c:forEach>




                        </select>
                        <div class="tips"></div>
                    </div>
                </div>

                <div class="label">
                    <label>不选择上级分类默认为一级科室</label>
                </div>

                <div class="form-group">
                    <div class="label">
                        <label>科室名称：</label>
                    </div>
                    <div class="field">
                        <input type="text" class="input" name="departmentName" value="" />
                    </div>
                </div>

            </if>


            <div class="clear"></div>

            <div class="form-group">
                <div class="label">
                    <label>科室描述：</label>
                </div>
                <div class="field">
                    <textarea type="text" class="input" name="departmentDescription" style="height:80px;"></textarea>
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
