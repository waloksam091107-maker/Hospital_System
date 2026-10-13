
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Title</title>
</head>
<body>



<form action="/doc/add" method="post" enctype="multipart/form-data">
    医生姓名：<input type="text" name="docname"><br>
    医生头像：<input type="file" name="docavatar"><br>
    <input type="submit" value="保存">


</form>

</body>
</html>
