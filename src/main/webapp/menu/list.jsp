
<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<form>
    <div id="input-content">

        <input id="inputView" type="text" name="keyword" placeholder="请输入科室名称">

        <button type="button" id="searchButton">搜索</button>
        <!-- 搜索结果 -->
        <div id="result"></div>

    </div>
</form>

<script>
    document.getElementById("searchButton").onclick = function() {
        var name = document.getElementById("inputView").value.trim(); // 使用 trim() 方法去除输入框内容两端的空白字符

        if (name === "") {
            alert("搜索内容不能为空");
        } else {
            window.location.href = '/dept/showfind?name=' + encodeURIComponent(name);
        }
    };
</script>



<!-- 监听输入框 -->
<script type="text/javascript">
    $(document).ready(function() {
        $("#inputView").on("input", function(event) {
            var keyword = $.trim(this.value);
            // ajax搜索
            $.ajax({
                type: "GET",
                url: "/find?keyword=" + keyword,
                //datatype: "json", //////
                success: function(data) {
                    // 将返回的 JSON 字符串解析为 JavaScript 对象数组
                    var dataArray = JSON.parse(data);

                    if (keyword === '') {
                        $("#result").html("<p>关键词不得为空</p>");
                    } else if (dataArray.length === 0) {
                        $("#result").html("<p>暂无搜索结果</p>");
                    } else {
                        // 清空结果区域
                        $("#result").empty();
                        // 循环遍历列表
                        for (var i = 0; i < dataArray.length; i++) {
                            var deptname = dataArray[i].departmentName;

                            $("#result").append('<p class="suggestion">'+deptname+"</p>");
                        }
                    }
                },
                error: function() {
                    console.log("服务器错误");
                }
            });
        });

        // 监听点击事件
        $("#result").on("click", ".suggestion", function() {
            var selectedDeptName = $(this).text();
            $("#inputView").val(selectedDeptName);
        });
    });





</script>





<div class="panel admin-panel">
    <div class="panel-head"><strong class="icon-reorder"> 内容列表</strong></div>
    <div class="padding border-bottom">
        <button type="button" class="button border-yellow" onclick="window.location.href='/first'"><span class="icon-plus-square-o"></span> 添加科室</button>
    </div>
    <table class="table table-hover text-center">
        <tr>
            <th>展开/折叠</th>
            <th>编号</th>
            <th>科室名称</th>
            <th>上级科室</th>
            <th>科室等级</th>
            <th>科室路径</th>
            <th>科室介绍</th>
            <th>操作</th>
        </tr>
        <c:forEach items="${pageInfo.list}" var="dept" varStatus="vs">
            <tr>
                <td><button deptPid="${dept.departmentId}" onclick="toggleSubMenu(${vs.index+1})">+</button></td>
                <td>${vs.index+1}</td>
                <td>${dept.departmentName}</td>
                <td>${dept.departmentPid}</td>
                <td>${dept.departmentLevel}</td>
                <td>${dept.departmentPath}</td>
                <td>${dept.departmentDescription}</td>
                <td>
                    <div class="button-group">

                        <a class="button border-main" href="#" onclick="checkAndModify('${dept.departmentId}')"><span class="icon-edit"></span> 修改</a>
                        <a class="button border-red" onclick="f1('${dept.departmentId}')"><span class="icon-trash-o"></span> 删除</a>
                    </div>
                </td>
            </tr>


            <tr style="display: none" id="subMenu${vs.index+1}">
                <td colspan="8">
                    <table class="table table-hover text-center">
                        <thead>
                        <tr>
                            <th>科室名称</th>
                            <th>科室介绍</th>
                            <th>操作</th>
                        </tr>
                        </thead>
                        <tbody>

                        </tbody>


                    </table>
                </td>
            </tr>


        </c:forEach>


        <script>
            var xhr;

            function createXHR() {
                if (window.ActiveXObject) {
                    xhr = new ActiveXObject("Microsoft.XMLHTTP");
                } else if (window.XMLHttpRequest) {
                    xhr = new XMLHttpRequest();
                } else {
                    alert("can't create xhr object!");
                }
            }
            function f1(departmentId) {

                createXHR();
                xhr.onreadystatechange=callback;
                xhr.open("get", "/departments/delete?id="+ encodeURIComponent(departmentId));
                if (confirm("确定要删除吗？")) {
                    // 用户点击“确定”，发送请求
                    xhr.send();
                }
            }

            function callback() {
                if (xhr.readyState == 4) {
                    if (xhr.status == 200) {
                        if (xhr.responseText== "1") {
                            alert("删除成功");
                            // 刷新页面或更新页面内容
                            //bom对象  js
                            location.reload(); // 刷新页面
                        } else if (xhr.responseText == "0") {
                            alert("删除失败,下面有子科室");
                        } else {
                            alert("未知错误");
                        }
                    } else {
                        alert("请求失败: " + xhr.status);
                    }
                }
            }


        </script>

        <script>
            function checkAndModify(deptId) {
                $.ajax({
                    url: '/departments/parent-check',
                    type: 'GET',
                    data: { id: deptId },
                    success: function(response) {
                        if(response == 1) {
                            window.location.href = '/departments/edit-page?id=' + deptId;
                        } else {
                            alert('该科室下面有子科室，不能修改。');
                        }
                    },
                    error: function() {
                        alert('请求出错，请稍后再试。');
                    }
                });
            }
        </script>


        <tr>
            <td colspan="8">
                <div class="pagelist">
                    <a href="/list?pageNum=${pageInfo.prePage}">上一页</a>
                    <c:forEach items="${pageInfo.navigatepageNums}" var="i">
                        <a href="/list?pageNum=${i}" class="current">${i}</a>
                    </c:forEach>


                    <c:if test="${pageInfo.isLastPage}">
                        <a href="javascript:alert('已经是最后一页了')">下一页</a>
                    </c:if>
                    <c:if test="${not pageInfo.isLastPage}">
                        <a href="/list?pageNum=${pageInfo.nextPage}">下一页</a>
                    </c:if>



                    <a href="/list?pageNum=${pageInfo.pages}">尾页</a>
                </div>
            </td>
        </tr>

    </table>































































































</body>
</html>
