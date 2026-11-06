<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<%@ page import="com.ustb.hospital.utils.SessionIdentity" %>
<%
    if (SessionIdentity.getCurrentUserId(request) == null) {
        response.sendRedirect(request.getContextPath() + "/hospro/login.html?roleCode=3");
        return;
    }
    if (SessionIdentity.getCurrentAdminId(request) == null) {
        response.sendRedirect(request.getContextPath() + "/hospro/index.html");
        return;
    }
%>
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>管理中心 - 医院挂号系统</title>
    <style>
        * { box-sizing:border-box; }
        body { margin:0; background:#f2f8f8; color:#183153; font:16px/1.6 "Microsoft YaHei",sans-serif; }
        header { background:#14365d; color:#fff; padding:24px max(20px,calc((100vw - 1100px)/2)); }
        header nav { display:flex; flex-wrap:wrap; gap:18px; }
        header a { color:#d9ffff; }
        h1 { margin:18px 0 4px; font-size:28px; }
        header p { margin:0; color:#d6e6ef; }
        main { max-width:1140px; margin:28px auto; padding:0 20px 40px; }
        .management-links { display:grid; grid-template-columns:repeat(auto-fit,minmax(min(100%,270px),1fr)); gap:18px; }
        .management-links article { min-width:0; border:1px solid #dce5ee; border-radius:10px; padding:22px; background:#fff; }
        h2 { margin:0 0 10px; font-size:20px; }
        a { color:#145a77; text-underline-offset:4px; }
        a:focus-visible { outline:3px solid #21b7ad; outline-offset:5px; }
        article p { margin:0; color:#52677e; }
        #notificationCenterNavigation { flex-wrap:wrap; }
        @media(max-width:480px) { main { margin-top:18px; padding:0 14px 28px; } header { padding:20px 16px; } }
    </style>
</head>
<body>
<header>
    <nav aria-label="常用导航"><a href="index.html">返回首页</a><a href="portal-profile.html">个人资料</a></nav>
    <h1>管理中心</h1>
    <p>维护医院基础信息，处理停诊申请，查看业务报表。</p>
</header>
<main>
    <nav class="management-links" aria-label="管理功能">
        <article><h2><a href="admin-master-data.html">基础数据维护</a></h2><p>维护医生、患者、科室和职称资料。</p></article>
        <article><h2><a href="../clinic-workflow.html">停诊审批</a></h2><p>查看医生停诊申请并进行审批。</p></article>
        <article><h2><a href="admin-announcements.html">公告管理</a></h2><p>发布和维护医院公告。</p></article>
        <article><h2><a href="account-security.html">账号安全管理</a></h2><p>管理医生和患者的账号状态、密码及登录锁定。</p></article>
        <article><h2><a href="sum.html">收入报表</a></h2><p>按日期查看实际挂号扣费和收入明细。</p></article>
        <article><h2><a href="audit-logs.html">审计日志</a></h2><p>查看系统操作记录。</p></article>
    </nav>
</main>
<script src="../js/notification-center.js"></script>
</body>
</html>
