
<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<!DOCTYPE html>

<html lang="zh-cn">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>登录</title>
    <link rel="stylesheet" href="/css/style.css">
    <link rel="shortcut icon" href="favicon.ico">
    <style>
        .signup-forgot-container {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .forgot-password a {
            color: #6c757d;
            font-size: 0.8em;
            text-decoration: underline;
            margin-left: 3px;
        }
    </style>
</head>
<body>
<section>

    <!-- 登录 -->

    <div class="container">
        <div class="user singinBx">
            <div class="imgBx"><img src="/images/1.jpg" alt=""></div>
            <div class="formBx">
                <form action="/login">
                    <h2>登录</h2>
                    <input type="text" name="username" placeholder="用户名">
                    <input type="password" name="password" placeholder="密码">
                    <input type="submit" name="" value="登录">
                    <div class="signup-forgot-container">
                        <p class="signup">没有账号？<a href="#" onclick="topggleForm();">注册</a></p>
                        <!-- 修改部分开始 -->
                        <p class="forgot-password"><a href="#">忘记密码？</a></p>
                        <!-- 修改部分结束 -->
                    </div>
                </form>
            </div>
        </div>

        <!-- 注册 -->

        <div class="user singupBx">
            <div class="formBx">
                <form action="">
                    <h2>注册</h2>
                    <input type="text" name="" placeholder="用户名">
                    <input type="email" name="" placeholder="邮箱地址">
                    <input type="password" name="" placeholder="密码">
                    <input type="password" name="" placeholder="再次输入密码">
                    <input type="submit" name="" value="注册">
                    <p class="signup">已有账号？<a href="#" onclick="topggleForm();">登录</a></p>
                </form>
            </div>
            <div class="imgBx"><img src="/images/2.jpg" alt=""></div>
        </div>
    </div>
</section>
<script type="text/javascript">
    function topggleForm(){
        var container = document.querySelector('.container');
        container.classList.toggle('active');
    }
</script>
</body>
</html>
