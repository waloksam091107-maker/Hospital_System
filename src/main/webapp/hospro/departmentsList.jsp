<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0">
  <title>Docpro - HTML 5 Template Preview</title><!-- Fav Icon --><!-- Google Fonts -->
  <link href="static/css/css2.css" rel="stylesheet">
  <link href="static/css/css21.css" rel="stylesheet"><!-- Stylesheets -->
  <link href="static/css/font-awesome-all.css" rel="stylesheet">
  <link href="static/css/flaticon.css" rel="stylesheet">
  <link href="static/css/owl.css" rel="stylesheet">
  <link href="static/css/bootstrap.css" rel="stylesheet">
  <link href="static/css/jquery.fancybox.min.css" rel="stylesheet">
  <link href="static/css/animate.css" rel="stylesheet">
  <link href="static/css/color.css" rel="stylesheet">
  <link href="static/css/nice-select.css" rel="stylesheet">
  <link href="static/css/style.css" rel="stylesheet">
  <link href="static/css/responsive.css" rel="stylesheet">
  <script>
    function toggleSubMenu(index) {
      var $button = $('button[onclick="toggleSubMenu(' + index + ')"');
      $button.html(($button.html() === '+') ? '-' : '+');
      var $subMenu = $('#subMenu' + index);
      $subMenu.css('display', ($subMenu.css('display') === 'none') ? 'table-row' : 'none');
      $.ajax({
        url: '/second',
        type: 'GET',
        data: {"deptPid": $button.attr("deptPid")},
        dataType: 'json',
        success: function (data) {
          $("#subMenu" + index + " tbody").empty();
          $.each(data, function (i, v) {
            var trObj = $("<tr></tr>");
            trObj.append($("<td>" + v.departmentName + "</td>"));
            trObj.append($("<td>" + v.departmentDiscription + "</td>"));
            trObj.append($("<td><div class='button-group'> <a class='button border-main' href='/portal/departments/edit-page?id="+ v.departmentId + "'><span class='icon-edit'></span> 修改</a > " +
                    "<a class='button border-red' href='javascript:void(0)' onclick='return confirmDeletion(" + v.departmentId + ")'><span class='icon-trash-o'></span> 删除</a > </div></td>"));
            trObj.appendTo($("#subMenu" + index + " tbody"));
          });
        }
      });
    }
    function confirmDeletion(departmentId) {
      var confirmation = confirm("确定要删除这个部门吗？");
      if (confirmation) {
        $.ajax({
          url: '/departments/has-children',
          type: 'GET',
          data: {deptId: departmentId},
          success: function (response) {
            if (response === 1 ) {
              // 如果有子科室，提示用户不能修改
              alert('该科室下有二级科室，不能删除');
            } else {
              // 如果没有子科室，则继续更新操作
              $.ajax({
                url: '/delete', // 服务器端点URL
                type: 'POST', // 或者使用'DELETE'，但注意跨浏览器兼容性和服务器配置
                data: {deptId: departmentId}, // 发送的数据
                success: function () {
                  alert('删除成功');
                  window.location.href = '/departments/list';
                },
                error: function () {
                  alert('删除失败');
                },
              });
            }
          },
          error: function () {
            alert('检查失败，请稍后再试');
          }
        });
      } else {
        // 用户取消删除，不执行任何操作（或执行其他逻辑）
        alert('删除已取消');
      }
    }
  </script>
</head><!-- page wrapper -->
<body>
<div class="boxed_wrapper"><!-- preloader -->
  <div class="preloader"></div><!-- preloader --><!-- main header -->
  <header class="main-header style-three"><!-- header-lower -->
    <div class="header-lower">
      <div class="outer-box clearfix">
        <div class="left-column pull-left">
          <div class="logo-box">
            <figure class="logo"><a href="index.html"><img src="static/picture/logo-3.png" alt=""></a>
            </figure>
          </div>
          <div class="menu-area"><!--Mobile Navigation Toggler-->
            <div class="mobile-nav-toggler"><i class="icon-bar"></i><i class="icon-bar"></i><i
                    class="icon-bar"></i></div>
            <nav class="main-menu navbar-expand-md navbar-light">
              <div class="collapse navbar-collapse show clearfix" id="navbarSupportedContent">
                <ul class="navigation clearfix">
                  <li class="dropdown"><a href="index.html">Home</a>
                    <ul>
                      <li><a href="index.html">Home Page 01</a></li>
                      <li><a href="index-2.html">Home Page 02</a></li>
                      <li><a href="index-3.html">Home Page 03</a></li>
                      <li><a href="index-4.html">Home Page 04</a></li>
                      <li><a href="index-5.html">Home Page 05</a></li>
                      <li><a href="index-onepage.html">OnePage Home</a></li>
                      <li><a href="index-rtl.html">RTL Home</a></li>
                      <li class="dropdown"><a href="index.html">Header Style</a>
                        <ul>
                          <li><a href="index.html">Header Style 01</a></li>
                          <li><a href="index-2.html">Header Style 02</a></li>
                          <li><a href="index-3.html">Header Style 03</a></li>
                          <li><a href="index-5.html">Header Style 04</a></li>
                        </ul>
                      </li>
                    </ul>
                  </li>
                  <li class="current dropdown"><a href="index.html">Doctors</a>
                    <ul>
                      <li class="dropdown"><a href="doctors-dashboard.html">Doctors Dashboard</a>
                        <ul>
                          <li><a href="">Dashboard</a></li>
                          <li><a href="departments.html">科室管理</a></li><!---->
                          <li><a href="my-patients.html">患者管理</a></li><!---->
                          <li><a href="doctors.html">医生管理</a></li><!---->

                          <li><a href="adminProfile.html">个人资料</a></li><!---->

                          <li><a href="login.html">退出登录</a></li><!---->
                        </ul>
                    </ul>
                  </li>
                  <li class="dropdown"><a href="index.html">Patients</a>
                    <ul>
                      <li class="dropdown"><a href="patient-dashboard.html">Patient Dashboard</a>
                        <ul>
                          <li><a href="">Dashboard</a></li>
                          <li><a href="departments.html">科室管理</a></li><!---->
                          <li><a href="my-patients.html">患者管理</a></li><!---->
                          <li><a href="doctors.html">医生管理</a></li><!---->

                          <li><a href="my-profile.html">个人资料</a></li><!---->

                          <li><a href="login.html">退出登录</a></li><!---->
                        </ul>
                      </li>
                      <li><a href="submit.html">Submit Review</a></li>
                      <li><a href="book-appointment.html">Book Appointment</a></li>
                      <li><a href="register-page.html">Register Page</a></li>
                    </ul>
                  </li>
                  <li class="dropdown"><a href="index.html">Pages</a>
                    <div class="megamenu">
                      <div class="row clearfix">
                        <div class="col-lg-6 column">
                          <ul>
                            <li><h4>Page List 01</h4></li>
                            <li><a href="about.html">About Us</a></li>
                            <li><a href="service-1.html">Service 01</a></li>
                            <li><a href="service-2.html">Service 02</a></li>
                            <li><a href="faq.html">Faq's</a></li>
                            <li><a href="pricing.html">Our Pricing</a></li>
                            <li><a href="contact.html">Contact Us</a>
                            <li><a href="error.html">404</a></li>
                          </ul>
                        </div>
                        <div class="col-lg-6 column">
                          <ul>
                            <li><h4>Page List 02</h4></li>
                            <li><a href="clinic-1.html">Clinic Page 01</a></li>
                            <li><a href="clinic-2.html">Clinic Page 02</a></li>
                            <li><a href="clinic-3.html">Clinic Page 03</a></li>
                            <li><a href="clinic-4.html">Clinic Page 04</a></li>
                            <li><a href="clinic-details.html">Clinic Details</a></li>
                          </ul>
                        </div>
                      </div>
                    </div>
                  </li>
                  <li class="dropdown"><a href="index.html">Elements</a>
                    <div class="megamenu">
                      <div class="row clearfix">
                        <div class="col-lg-6 col-md-12 col-sm-12 column">
                          <ul>
                            <li><h4>Elements 1</h4></li>
                            <li><a href="about-element-1.html">About Block 01</a></li>
                            <li><a href="about-element-2.html">About Block 02</a></li>
                            <li><a href="about-element-3.html">About Block 03</a></li>
                            <li><a href="feature-element-1.html">Feature Block 01</a></li>
                            <li><a href="feature-element-2.html">Feature Block 02</a></li>
                            <li><a href="process-element-1.html">Process Block 01</a></li>
                            <li><a href="process-element-2.html">Process Block 02</a></li>
                            <li><a href="team-element-1.html">Team Block 01</a></li>
                          </ul>
                        </div>
                        <div class="col-lg-6 col-md-12 col-sm-12 column">
                          <ul>
                            <li><h4>Elements 2</h4></li>
                            <li><a href="team-element-2.html">Team Block 02</a></li>
                            <li><a href="news-element-1.html">News Block 01</a></li>
                            <li><a href="news-element-2.html">News Block 02</a></li>
                            <li><a href="category-element-1.html">Category Block 01</a></li>
                            <li><a href="category-element-2.html">Category Block 02</a></li>
                            <li><a href="faq-element.html">Faq Block</a></li>
                            <li><a href="cta-element.html">CTA Block</a></li>
                            <li><a href="clients-element.html">Clients Block</a></li>
                          </ul>
                        </div>
                      </div>
                    </div>
                  </li>
                  <li class="dropdown"><a href="index.html">News</a>
                    <ul>
                      <li><a href="blog.html">Blog Grid 01</a></li>
                      <li><a href="blog-2.html">Blog Grid 02</a></li>
                      <li><a href="blog-3.html">Blog Standard</a></li>
                      <li><a href="blog-details.html">Blog Details</a></li>
                    </ul>
                  </li>
                </ul>
              </div>
            </nav>
          </div>
        </div>
        <div class="right-column pull-right">
          <div class="author-box">
            <div class="icon-box"><a href="index.html"><i class="icon-Bell"></i></a></div>
            <div class="author">
              <figure class="author-image"><img src="static/picture/author-1.png" alt=""></figure>
              <div class="select-box"><select class="wide">
                <option data-display="Dr. Rex Allen">Dr. Rex Allen</option>
                <option value="1">Cardiology</option>
                <option value="2">Nurology</option>
                <option value="4">Pragnency</option>
              </select></div>
            </div>
          </div>
        </div>
      </div>
    </div><!--sticky Header-->
    <div class="sticky-header">
      <div class="auto-container">
        <div class="outer-box">
          <div class="logo-box">
            <figure class="logo"><a href="index.html"><img src="static/picture/small-logo.png" alt=""></a>
            </figure>
          </div>
          <div class="menu-area">
            <nav class="main-menu clearfix"><!--Keep This Empty / Menu will come through Javascript--></nav>
          </div>
          <div class="btn-box"><a href="register-page.html" class="theme-btn-one"><i class="icon-image"></i>Join
            Now</a></div>
        </div>
      </div>
    </div>
  </header><!-- main-header end --><!-- Mobile Menu -->
  <div class="mobile-menu">
    <div class="menu-backdrop"></div>
    <div class="close-btn"><i class="fas fa-times"></i></div>
    <nav class="menu-box">
      <div class="nav-logo"><a href="index.html"><img src="static/picture/logo-2.png" alt="" title=""></a></div>
      <div class="menu-outer">
        <!--Here Menu Will Come Automatically Via Javascript / Same Menu as in Header--></div>
      <div class="contact-info"><h4>Contact Info</h4>
        <ul>
          <li>Chicago 12,Melborne City,USA</li>
          <li><a href="">+88 01682648101</a></li>
          <li><a href="mailto:info@example.com">info@example.com</a></li>
        </ul>
      </div>
      <div class="social-links">
        <ul class="clearfix">
          <li><a href="index.html"><span class="fab fa-twitter"></span></a></li>
          <li><a href="index.html"><span class="fab fa-facebook-square"></span></a></li>
          <li><a href="index.html"><span class="fab fa-pinterest-p"></span></a></li>
          <li><a href="index.html"><span class="fab fa-instagram"></span></a></li>
          <li><a href="index.html"><span class="fab fa-youtube"></span></a></li>
        </ul>
      </div>
    </nav>
  </div><!-- End Mobile Menu --><!--page-title-two-->
  <section class="page-title-two">
    <div class="title-box centred bg-color-2">
      <div class="pattern-layer">
        <div class="pattern-1" style="background-image: url(static/picture/shape-70.png);"></div>
        <div class="pattern-2" style="background-image: url(static/picture/shape-71.png);"></div>
      </div>
      <div class="auto-container">
        <div class="title"><h1>科室管理</h1></div>
      </div>
    </div>
    <div class="lower-content">
      <ul class="bread-crumb clearfix">
        <li><a href="index.html">Home</a></li>
        <li>科室管理</li>
      </ul>
    </div>
  </section><!--page-title-two end--><!-- doctors-dashboard -->
  <section class="doctors-dashboard bg-color-3">
    <div class="left-panel">
      <div class="profile-box">
        <div class="upper-box">
          <figure class="profile-image"><img src="" alt=""></figure>
          <div class="title-box centred">

          </div>
        </div>
        <div class="profile-info">
          <ul class="list clearfix">
            <li><a href="/departments/list" class="current"><i class="fas fa-calendar-alt"></i>科室管理</a></li>
            <li><a href="doctors.jsp"><i class="fas fa-plus-circle"></i>医生管理</a></li>
            <li><a href="my-patients.jsp"><i class="fas fa-wheelchair"></i>患者管理</a></li>
            <li><a href="newsAdmins.html"><i class="fas fa-clock"></i>公告管理</a></li>
            <li><a href="sum.html"><i class="fas fa-star"></i>统计报表</a></li>

            <li><a href=""><i class="fas fa-user"></i>个人资料</a></li>

            <li><a href="../login.html"><i class="fas fa-sign-out-alt"></i>退出登录</a></li>
          </ul>
        </div>
      </div>
    </div>
    <div class="right-panel">
      <div class="content-container">
        <div class="outer-container">
          <div class="doctors-appointment my-patients">
            <div class="title-box clearfix">
              <div class="text pull-left"><h3>Departments List</h3></div>
              <div class="btn-box pull-right">
                <form action="/departments/search" method="post" class="search-form">
                  <div class="form-group">
                    <input type="search" name="deptName" placeholder="请输入搜索关键字" required="">
                    <button type="submit"><i class="far fa-search"></i></button>
                  </div>
                </form>
                <a href="/portal/departments/create-page" class="theme-btn-one">添加科室<i class="icon-image"></i></a>
              </div>
            </div>
            <div class="doctors-list">
              <div class="table-outer">
                <table class="doctors-table">
                  <thead class="table-header">
                  <tr>
                    <th>展开/折叠</th>
                    <th>编号</th>
                    <th>科室名称</th>
                    <th>科室路径</th>
                    <th>科室介绍</th>
                    <th>操作</th>
                    <th>&nbsp;</th>
                  </tr>
                  </thead>
                  <c:forEach items="${pageInfo.list}" var="dept" varStatus="vs">
                    <tr>
                      <td><p><button deptPid="${dept.departmentId}" onclick="toggleSubMenu(${vs.index+1})">+</button></p></td>
                      <td><p>${vs.index+1}</p></td>
                      <td><p>${dept.departmentName}</p></td>
                        <!-- main-footer -->
  <footer class="main-footer">
    <div class="footer-top pt-100">
      <div class="pattern-layer">
        <div class="pattern-1" style="background-image: url(static/picture/shape-30.png);"></div>
        <div class="pattern-2" style="background-image: url(static/picture/shape-31.png);"></div>
      </div>
      <div class="auto-container">
        <div class="widget-section">
          <div class="row clearfix">
            <div class="col-lg-4 col-md-6 col-sm-12 footer-column">
              <div class="footer-widget logo-widget">
                <figure class="footer-logo"><a href="index.html"><img
                        src="static/picture/footer-logo.png" alt=""></a></figure>
                <div class="text"><p>Lorem ipsum is placeholder text commonly used in the
                  graphic,print,and publishing .</p></div>
              </div>
            </div>
            <div class="col-lg-2 col-md-6 col-sm-12 footer-column">
              <div class="footer-widget links-widget">
                <div class="widget-title"><h3>About</h3></div>
                <div class="widget-content">
                  <ul class="links clearfix">
                    <li><a href="index.html">About Us</a></li>
                    <li><a href="index.html">Listing</a></li>
                    <li><a href="index.html">How It Works</a></li>
                    <li><a href="index.html">Our Services</a></li>
                    <li><a href="index.html">Our Blog</a></li>
                    <li><a href="index.html">Contact Us</a></li>
                  </ul>
                </div>
              </div>
            </div>
            <div class="col-lg-3 col-md-6 col-sm-12 footer-column">
              <div class="footer-widget links-widget">
                <div class="widget-title"><h3>Useful Links</h3></div>
                <div class="widget-content">
                  <ul class="links clearfix">
                    <li><a href="index.html">Specialist</a></li>
                    <li><a href="index.html">Clinic</a></li>
                    <li><a href="index.html">Hospital</a></li>
                    <li><a href="index.html">Download App</a></li>
                    <li><a href="index.html">Join as a Doctor</a></li>
                    <li><a href="index.html">Privacy Policy</a></li>
                  </ul>
                </div>
              </div>
            </div>
            <div class="col-lg-3 col-md-6 col-sm-12 footer-column">
              <div class="footer-widget contact-widget">
                <div class="widget-title"><h3>Contacts</h3></div>
                <div class="widget-content">
                  <ul class="info-list clearfix">
                    <li><i class="fas fa-map-marker-alt"></i>Flat 20,Reynolds Neck,North
                      Helenaville,FV77 8WS
                    </li>
                    <li><i class="fas fa-microphone"></i><a href="">+2(305) 587-3407</a></li>
                    <li><i class="fas fa-envelope"></i><a href="mailto:info@example.com">info@example.com</a>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="footer-bottom">
      <div class="auto-container">
        <div class="inner-box clearfix">
          <div class="copyright pull-left"><p><a href="/">Docpro</a>&copy;2020 All Right Reserved</p></div>
          <ul class="footer-nav pull-right clearfix">
            <li><a href="index.html">Terms of Service</a></li>
            <li><a href="index.html">Privacy Policy</a></li>
          </ul>
        </div>
      </div>
    </div>
  </footer><!-- main-footer end --><!--Scroll to top-->
  <button class="scroll-top scroll-to-target" data-target="html"><span class="fa fa-arrow-up"></span></button>
</div><!-- jequery plugins -->
<script src="static/js/jquery.js"></script>
<script src="static/js/popper.min.js"></script>
<script src="static/js/bootstrap.min.js"></script>
<script src="static/js/owl.js"></script>
<script src="static/js/wow.js"></script>
<script src="static/js/validation.js"></script>
<script src="static/js/jquery.fancybox.js"></script>
<script src="static/js/appear.js"></script>
<script src="static/js/scrollbar.js"></script>
<script src="static/js/tilt.jquery.js"></script>
<script src="static/js/jquery.paroller.min.js"></script>
<script src="static/js/jquery.nice-select.min.js"></script>
<script src="static/js/product-filter.js"></script><!-- map script -->
<script src="static/js/gmaps.js"></script>
<script src="static/js/map-helper.js"></script><!-- main-js -->
<script src="static/js/script.js"></script>
</body><!-- End of .page_wrapper -->
</html>
