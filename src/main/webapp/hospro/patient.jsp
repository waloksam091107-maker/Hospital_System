
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0">
    <title>患者中心</title><!-- Fav Icon --><!-- Google Fonts -->
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
    <script src="static/js/jquery.js"></script>
<script>
const appContext = window.location.pathname.split('/hospro/')[0];
function appUrl(path) { return appContext + path; }
</script>
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
</head><!-- page wrapper -->
<body>
<script>

    function handlePersonalPageClick(event) {

        event.preventDefault();
        const url = new URL(window.location.href);
        const searchParams = new URLSearchParams(url.search);
        const patientId = searchParams.get('patientId');
        const redirectUrl = '/hospro/patientpr.html?patientId=' + encodeURIComponent(patientId);
        window.location.href = redirectUrl;
    }

    // 在页面加载完成后，为按钮添加点击事件监听器
    document.addEventListener('DOMContentLoaded', function() {
        const btn = document.getElementById('hzgeren');
        btn.addEventListener('click', handlePersonalPageClick);
    });
</script>
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
                                                    <li><a href="doctors-dashboard.html">Dashboard</a></li>
                                                    <li><a href="appointment.html">Appointments</a></li>
                                                    <li><a href="">My Patients</a></li>
                                                    <li><a href="add-listing.html">Add Listing</a></li>
                                                    <li><a href="schedule-timing.html">Schedule Timing</a></li>
                                                    <li><a href="review.html">Reviews</a></li>
                                                    <li><a href="message.html">Messages</a></li>
                                                    <li><a href="my-profile.html">My Profile</a></li>
                                                    <li><a href="change-password.html">Change Password</a></li>
                                                    <li><a href="login.html">Logout</a></li>
                                                </ul>
                                            </li>
                                            <li><a href="doctor-directory.html">Doctors Page 01</a></li>
                                            <li><a href="doctors-2.html">Doctors Page 02</a></li>
                                            <li><a href="doctors-3.html">Doctors Page 03</a></li>
                                            <li><a href="doctors-4.html">Doctors Page 04</a></li>
                                            <li><a href="doctors-details.html">Doctors Details</a></li>
                                        </ul>
                                    </li>
                                    <li class="dropdown"><a href="index.html">Patients</a>
                                        <ul>
                                            <li class="dropdown"><a href="patient-dashboard.html">Patient Dashboard</a>
                                                <ul>
                                                    <li><a href="patient-dashboard.html">Dashboard</a></li>
                                                    <li><a href="favourite-doctors.html">Favourite Doctors</a></li>
                                                    <li><a href="schedule-timing-2.html">Schedule Timing</a></li>
                                                    <li><a href="message-2.html">Messages</a></li>
                                                    <li><a href="patient-profile.html">My Profile</a></li>
                                                    <li><a href="change-password-2.html">Change Password</a></li>
                                                    <li><a href="login-2.html">Logout</a></li>
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
                <div class="title"><h1>就诊记录</h1></div>
            </div>
        </div>
        <div class="lower-content">
            <ul class="bread-crumb clearfix">
                <li><a href="index.html">主页</a></li>
                <li>就诊记录</li>
            </ul>
        </div>
    </section><!--page-title-two end--><!-- doctors-dashboard -->
    <section class="doctors-dashboard bg-color-3">
        <div class="left-panel">
            <div class="profile-box">
                <div class="upper-box">
                    <figure class="profile-image"><img id="patientAvatarImg" src="/hos_images" alt=""></figure>
                    <div class="title-box centred">
                        <div class="inner"><h3 id="patientNameLabel">name</h3></div>
                    </div>
                </div>
                <div class="profile-info">
                    <ul class="list clearfix">
                        <li><a id="hzgeren"><i class="fas fa-user"></i>个人主页</a></li>

                        <li><a id="hzyuyue" class="current"><i class="fas fa-wheelchair"></i>预约记录</a></li>
                        <li><a href="exit.jsp"><i class="fas fa-plus-circle"></i>修改信息</a></li>

                        <li><a href="review.html"><i class="fas fa-star"></i>医嘱详情</a></li>

                        <li><a href="change-password.html"><i class="fas fa-unlock-alt"></i>修改密码</a></li>
                        <li><a href="index.html"><i class="fas fa-sign-out-alt"></i>返回主页</a></li>
                    </ul>
                </div>
            </div>
        </div>
        <style>

            table {
                width: 100%;
                border-collapse: collapse;
            }

            table td, table th {
                text-align: center;
                padding: 8px;
                border: 1px solid #ddd;
            }

            th, td {
                padding: 10px;
                text-align: left;
                border: 1px solid #ddd;
            }

            th {
                background-color: #f2f2f2;
            }


            .button {
                display: inline-block;
                padding: 8px 16px;
                margin-right: 8px;
                text-decoration: none;
                color: #fff;
                cursor: pointer;
                border-radius: 4px;
                transition: background-color 0.3s ease;
            }

            .border-main {
                background-color: #3498db;
                border: 1px solid #3498db;
            }

            .border-red {
                background-color: #e74c3c;
                border: 1px solid #e74c3c;
            }

            .border-orange {
                background-color: #f39c12;
                border: 1px solid #f39c12;
                color: #fff;
            }

            .border-orange:hover {
                background-color: #e67e22;
                border-color: #e67e22;
            }

            .icon-edit {
                margin-right: 5px;
            }

            .icon-trash-o {
                margin-right: 5px;
            }

            .button:hover {
                background-color: #2980b9;
                border-color: #2980b9;
            }


            #updateModal {
                display: none;
                position: fixed;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                background-color: white;
                padding: 20px;
                border: 1px solid #ccc;
                z-index: 1000;
                box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
                border-radius: 10px;
                max-width: 400px;
                width: 100%;
            }

            .form-group {
                margin-bottom: 15px;
            }

            .form-group label {
                display: block;
                margin-bottom: 5px;
                font-weight: bold;
            }

            .input {
                width: calc(100% - 22px);
                padding: 10px;
                border: 1px solid #ccc;
                border-radius: 5px;
                font-size: 16px;
            }

            .input:focus {
                border-color: #007bff;
                outline: none;
            }

            .columns-container {
                display: flex;
                flex-wrap: wrap;
            }

            .column {
                flex: 1;
                min-width: 50%;
            }

            .column option {
                padding: 5px;
            }

            h3, p {
                margin-top: 0;
            }
        </style>
        <style>
            .profile-image img {
                width: 450px;
                height: 300px;
                object-fit: cover;
            }
        </style>
<script type="text/javascript">
    function getPatientIdFromUrl() {
        // 获取当前页面的 URL 对象
        const url = new URL(window.location.href);
        // 获取 URL 中的 search 参数
        const searchParams = new URLSearchParams(url.search);
        // 获取 patientId 参数的值
        const patientId = searchParams.get('patientId');
        // 输出获取到的 patientId 值
        console.log('从地址栏中获取到的 patientId:', patientId);
        // 返回获取到的 patientId 值（如果需要在函数外部使用）
        return patientId;
    }

    $(function () {
        const patientId = getPatientIdFromUrl();
        $.ajax({
            url: appUrl('/patients/current'),
            type: 'GET',
            data: { "patientId": patientId },
            dataType: "json",
            success: function (patient) {
                $("#patientNameLabel").text(patient.name);
                var patientAvatarPath = '/hos_images/' + patient.avatar;
                $("#patientAvatarImg").attr("src", patientAvatarPath);
            },
            error: function (xhr, status, error) {
                console.error('AJAX Error: ' + status, error);
            }
        });
    });
    function loadAppointmentsPage(p) {
        $.ajax({
            url: appUrl('/appointments/patient'),
            type: 'GET',
            data: { "pageNum": p },
            dataType: "json",
            success: function (res) {
                console.log("Appointment data:", res);
                $("#appBody").empty();
                $.each(res.list, function (i, v) {
                    var trObj = $("<tr></tr>");
                    trObj.append($("<td>" + (i + 1) + "</td>"));
                    trObj.append($("<td>" + v.doctors.name + "</td>"));
                    trObj.append($("<td>" + v.departments.departmentName + "</td>"));
                    trObj.append($("<td>" + v.appointmentDate + "</td>"));
                    trObj.append($("<td>" + v.appointmentTime + "</td>"));
                    trObj.append($("<td>" + v.status + "</td>"));
                    var buttonGroup = $("<td><div class='button-group'></div></td>");
                    // 修改按钮
                    if (v.status === "booked") {
                        buttonGroup.find('.button-group').append($("<a class='button border-main' href='javascript:void(0)' onclick='showUpdateModal(" + v.appointmentId + ", \"" + v.appointmentDate + "\", \"" + v.appointmentTime + "\")'><span class='icon-edit'></span> 修改</a>"));
                    } else {
                        buttonGroup.find('.button-group').append($("<a class='button border-main' href='javascript:void(0)' disabled><span class='icon-edit'></span> 修改</a>"));
                    }
                    // 取消按钮
                    if (v.status === "booked" ) {
                        buttonGroup.find('.button-group').append($("<a class='button border-red' href='javascript:void(0)' onclick='return confirmCancellation(" + v.appointmentId + ")'><span class='icon-trash-o'></span> 取消</a>"));
                    } else {
                        buttonGroup.find('.button-group').append($("<a class='button border-red' href='javascript:void(0)' disabled><span class='icon-trash-o'></span> 取消</a>"));
                    }
                    trObj.append(buttonGroup);
                    trObj.appendTo($("#appBody"));
                });

                $(".pagination li").remove();
                for (var i = 0; i < res.pages; i++) {
                    var $li = $("<li><a href='javascript:void(0)' onclick='changePage(" + (i + 1) + ")'>" + (i + 1) + "</a></li>");
                    if (i + 1 === p) {
                        $li.find('a').addClass('current');
                    }
                    $(".pagination").append($li);
                }
            },
            error: function () {
                alert("加载预约记录失败！");
            }
        });
    }


    $(document).ready(function() {
        loadAppointmentsPage(1, "", "");
    });
    function changePage(p){
        loadAppointmentsPage(p)
    }
    function nextPage(){
        var currentPageNumber =$("span[class='current']").text()
        // alert(currentPageNumber)
        loadAppointmentsPage(Number(currentPageNumber)+1)
    }
    function showUpdateModal(appointmentId, appointmentDate, appointmentTime) {
        $("#updateAppointmentId").val(appointmentId);
        $("#updateAppointmentDate").val(appointmentDate);
        $("#updateAppointmentTime").val(appointmentTime);
        $("#updateModal").show();
    }

    function hideUpdateModal() {
        $('#updateModal').hide();
    }

    function updateAppointment() {
        var appointmentId = $('#updateAppointmentId').val();
        var appointmentDate = $('#updateAppointmentDate').val();
        var appointmentTime = $('#updateAppointmentTime').val();
        if (appointmentDate.trim() === "") {
            alert("请选择日期！");
            return;
        }
        $.ajax({
            url: appUrl('/appointments/reschedule'),
            type: 'POST',
            data: {appointmentId: appointmentId, appointmentDate: appointmentDate, appointmentTime: appointmentTime },
            dataType: 'json',
            success: function(response) {
                if (response.success) {
                    alert(response.message);
                    location.reload();
                } else {
                    alert(response.message || '更新失败');
                }
            },
            error: function() {
                alert('更新失败');
            }
        });
    }

    function confirmCancellation(appointmentId) {
        if (confirm("确定取消该预约？")) {
            $.ajax({
                url: appUrl('/appointments/cancel'),
                type: 'POST',
                data: {
                    "appointmentId": appointmentId
                },
                dataType: 'json',
                success: function (response) {
                    if (response.success) {
                        alert(response.message);
                        location.reload();
                        loadAppointmentsPage(1, "", "");
                    } else {
                        alert(response.message || "取消预约失败！");

                    }
                },
                error: function () {
                    alert("取消预约失败！");
                }
            });
        }
    }
</script>
        <div id="updateModal">
            <h3><p>修改就诊日期</p ></h3>
            <input type="hidden" id="updateAppointmentId">
            <div class="form-group">
                <label for="updateAppointmentDate">新日期:</label>
                <input type="date" id="updateAppointmentDate" class="input">
            </div>
            <h3><p>修改就诊时间</p ></h3>
            <div class="form-group">
                <label for="updateAppointmentTime">请选择时间段:</label>
                <div class="select-wrapper">
                    <select id="updateAppointmentTime" class="input select-scroll">
                        <optgroup label="上午">
                            <option value="09:30">09:30 AM</option>
                            <option value="10:00">10:00 AM</option>
                            <option value="10:30">10:30 AM</option>
                            <option value="11:00">11:00 AM</option>
                            <option value="11:30">11:30 AM</option>
                        </optgroup>
                        <optgroup label="下午">
                            <option value="13:00">01:00 PM</option>
                            <option value="13:30">01:30 PM</option>
                            <option value="14:00">02:00 PM</option>
                            <option value="14:30">02:30 PM</option>
                            <option value="15:00">03:00 PM</option>
                            <option value="15:30">03:30 PM</option>
                            <option value="16:00">04:00 PM</option>
                            <option value="16:30">04:30 PM</option>
                        </optgroup>
                    </select>
                </div>
            </div>
            <button type="button" class="button border-main" onclick="updateAppointment()">提交</button>
            <button type="button" class="button border-gray" onclick="hideUpdateModal()">取消</button>
        </div>
        <style>
            .select-wrapper {
                position: relative;
                display: inline-block;
                width: 100%;
            }

            .select-scroll {
                appearance: none;
                -webkit-appearance: none;
                -moz-appearance: none;
                background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8' viewBox='0 0 8 8'%3E%3Cpath d='M0 2l4 4 4-4z' fill='%23000000'/%3E%3C/svg%3E") no-repeat right 0.75rem center/8px 10px;
                padding-right: 2rem;
            }

            .select-scroll::-ms-expand {
                display: none;
            }

            .select-scroll option {
                padding: 0.5rem;
            }

            .select-scroll optgroup {
                font-weight: bold;
                color: #333;
            }

            @media screen and (min-width: 768px) {
                .select-scroll {
                    max-height: 200px;
                    overflow-y: auto;
                }
            }
        </style>
        <style> table {
            width: 100%;
            border-collapse: collapse;
        }
        </style>
<div class="right-panel">
    <div class="content-container">
        <div class="outer-container">
            <div class="doctors-appointment my-patients">
                <div class="title-box clearfix">
                    <div class="text pull-left"><h3>预约记录</h3></div>
                    <div class="btn-box pull-right">
                        <a href="index.html" class="theme-btn-one">新增预约<i class="icon-image"></i></a>
                    </div>
                </div>
                <div class="doctors-list">
                    <div class="table-outer">
                        <table class="doctors-table">
                            <thead class="table-header">
                            <tr>
                                <th>编号</th>
                                <th>就诊医生</th>
                                <th>就诊科室</th>
                                <th>就诊日期</th>
                                <th>就诊时间</th>
                                <th>订单详情</th>
                                <th>操作</th>
                            </tr>
                            </thead>
                            <tbody id="appBody"></tbody>

                            <ul class="pagination">
                                <li><a href="" class="current">1</a></li>
                                <li><a href="">2</a></li>
                                <li><a href="">3</a></li>
                                <li><a href=""><i class="icon-Arrow-Right"></i></a></li>
                            </ul>
                        </table>
                    </div>
                </div>
            </div>

        </div>
    </div>
</div>
</section><!-- doctors-dashboard --><!-- main-footer -->
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

</body><!-- End of .page_wrapper --></html>
