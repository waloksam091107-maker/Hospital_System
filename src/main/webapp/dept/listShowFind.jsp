

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
    <script>
        function toggleSubMenu(index) {//index=1
            //获取按钮对象
            var $button = $('button[onclick="toggleSubMenu(' + index + ')"');
            //切换+ -  三元运算符
            $button.html(($button.html() === '+')? '-' : '+');
            //获取index对应的子菜单
            var $subMenu = $('#subMenu' + index);
            //修改子菜单的display 样式
            $subMenu.css('display', ($subMenu.css('display') === 'none')? 'table-row' : 'none');

            $.ajax({
                type: "GET",
                url: "/second",
                data: {"deptPid": $button.attr("deptPid")},
                datatype: "json",
                success: function (res) {
                    console.log("res", res)
                    $("#subMenu" + index + " tbody").empty()
                    $.each(res, function (i, v) {
                        var trObj = $("<tr></tr>");
                        trObj.append($("<td>" + v.departmentName + "</td>"));
                        trObj.append($("<td>" + v.departmentDiscription + "</td>"));

                        var actionTd = $("<td></td>");
                        var buttonGroup = $('<div class="button-group"></div>');

                        var editButton = $('<a class="button border-main" href="/departments/edit-page?id=' + v.departmentId + '"><span class="icon-edit"></span> 修改</a>');
                        var deleteButton = $('<a class="button border-red" onclick="f1(\'' + v.departmentId + '\')"><span class="icon-trash-o"></span> 删除</a>');

                        buttonGroup.append(editButton);
                        buttonGroup.append(deleteButton);
                        actionTd.append(buttonGroup);

                        trObj.append(actionTd);

                        trObj.appendTo($("#subMenu" + index + " tbody"));
                    });
                }
            });

        }
    </script>


    <style type="text/css">
        #input-content{
            width: 600px;
            height: 200px;
            margin:10px auto 20px;
            display: flex;
            align-items: center;
        }

        #input-content input{
            flex: 1;
            width: calc(100% - 60px);

            height: 50px;
            text-indent: 15px;
            font-size: 18px;
            outline: none;
            border:2px solid #333;
            border-right: none;
            border-radius: 100px 0 0 100px;

        }
        #input-content button {
            width: 60px;
            height: 50px;
            font-size: 18px;
            outline: none;
            border: 2px solid #333;
            border-left: none;
            border-left: none;
            border-radius: 0 100px 100px 0;
            background-color: #333;
            color: white;
            cursor: pointer;
        }
        #input-content button:hover {
            background-color: #555;
        }




        input:-webkit-autofill,
        textarea:-webkit-autofill,
        select:-webkit-autofill {
            -webkit-box-shadow: 0 0 0 1000px white inset;
        }
        input[type=text]:focus, input[type=password]:focus, textarea:focus {
            -webkit-box-shadow: 0 0 0 1000px white inset;
        }
    </style>



<form>
    <div id="input-content">

        <input id="inputView" type="text" name="keyword" placeholder="请输入科室名称">

        <button type="button" id="searchButton">搜索</button>
        <!-- 搜索结果 -->
        <div id="result"></div>

    </div>
</form>

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
      <thead>
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
      </thead>

      <tbody>
      <c:choose>

--%>




              </c:forEach>
          </c:otherwise>
      </c:choose>
      </tbody>




















  </table>


















































































































































</body>
</html>
