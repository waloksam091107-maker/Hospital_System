
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>

<!DOCTYPE html><html lang="en">
<head><meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0">
  <title>Docpro - HTML 5 Template Preview</title>
  <!-- Fav Icon --><!-- Google Fonts -->
  <link href="static/css/css2.css" rel="stylesheet">
  <link href="static/css/css21.css" rel="stylesheet">
  <!-- Stylesheets -->
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
  <script src="/js/jquery-3.7.1.min.js"></script>
</head>


<script>
  $(function(){
    findDoc(10);
  })
  function findDoc(departmentId) {//index=1

    $.ajax({
      type: "GET",
      url: "/doc/findByDeptId",
      data: {"departmentId": departmentId},
      // datatype: "json",
      dataType: "json",
      success: function (res) {
        console.log("res", res)
        // $("#subMenu" + index + " tbody").empty()
        var clinicListContent = $("#clinicListContent"); // 假设你的容器ID是clinicListContent
        clinicListContent.empty(); // 清空当前内容
        $.each(res.list, function (i, v) {
          var clinicBlock = $('<div class="clinic-block-one"></div>');
          var innerBox = $('<div class="inner-box"></div>');
          var pattern = $('<div class="pattern"></div>');
          var pattern1 = $('<div class="pattern-1" style="background-image: url(static/picture/shape-24.png);"></div>');
          var pattern2 = $('<div class="pattern-2" style="background-image: url(static/picture/shape-25.png);"></div>');

          // 假设医生的头像图片路径是存储在v.avatar中
          var imageBox = $('<figure class="image-box"><img src="/img/' + v.avatar + '" alt=""></figure>');
          var contentBox = $('<div class="content-box"></div>');
          var likeBox = $('<div class="like-box"><a href="doctors-details.html?doctorId=' + v.doctorId + '"><i class="far fa-heart"></i></a></div>');
          var nameBox = $('<ul class="name-box clearfix"></ul>');
          var nameItem = $('<li class="name"><h3><a href="doctors-details.html?doctorId=' + v.doctorId + '">' + v.name + '</a></h3></li>');
          // 其他医生信息的添加方式与此类似，根据你的HTML结构添加相应的内容

          // 组合元素
          pattern.append(pattern1);
          pattern.append(pattern2);
          innerBox.append(pattern);
          innerBox.append(imageBox);
          innerBox.append(contentBox);
          contentBox.append(likeBox);
          contentBox.append(nameBox);
          nameBox.append(nameItem);
          // ... 添加其他医生信息
          clinicBlock.append(innerBox);
          clinicBlock.appendTo(clinicListContent); // 将生成的医生信息块添加到容器中

        });

      }
    });

  }
</script>


<!-- page wrapper -->
<body><div class="boxed_wrapper">
  <!-- preloader -->
  <div class="preloader"></div>
  <!-- preloader --><!-- main header -->
  <header class="main-header style-two">
    <!-- header-lower -->
    <div class="header-lower">
      <div class="auto-container">
        <div class="outer-box"><div class="logo-box">
          <figure class="logo">
            <a href="archive/index.html">
              <img src="static/picture/logo-3.png" alt="">
            </a>
          </figure></div><div class="menu-area">
          <!--Mobile Navigation Toggler-->
          <div class="mobile-nav-toggler">
            <i class="icon-bar"></i>
            <i class="icon-bar"></i>
            <i class="icon-bar"></i>
          </div><nav class="main-menu navbar-expand-md navbar-light">
          <div class="collapse navbar-collapse show clearfix" id="navbarSupportedContent">
            <ul class="navigation clearfix">
              <li class="dropdown">
                <a href="archive/index.html">Home</a><ul><li><a href="archive/index.html">Home Page 01</a></li><li><a href="index-2.html">Home Page 02</a></li><li><a href="index-3.html">Home Page 03</a></li><li><a href="index-4.html">Home Page 04</a></li><li><a href="index-5.html">Home Page 05</a></li><li><a href="index-onepage.html">OnePage Home</a></li><li><a href="index-rtl.html">RTL Home</a></li><li class="dropdown"><a href="archive/index.html">Header Style</a><ul><li><a href="archive/index.html">Header Style 01</a></li><li><a href="index-2.html">Header Style 02</a></li><li><a href="index-3.html">Header Style 03</a></li><li><a href="index-5.html">Header Style 04</a></li></ul></li></ul></li><li class="current dropdown"><a href="archive/index.html">Doctors</a><ul><li class="dropdown"><a href="doctors-dashboard.html">Doctors Dashboard</a><ul><li><a href="doctors-dashboard.html">Dashboard</a></li><li><a href="appointment.html">Appointments</a></li><li><a href="my-patients.html">My Patients</a></li><li><a href="add-listing.html">Add Listing</a></li><li><a href="schedule-timing.html">Schedule Timing</a></li><li><a href="review.html">Reviews</a></li><li><a href="message.html">Messages</a></li><li><a href="my-profile.html">My Profile</a></li><li><a href="change-password.html">Change Password</a></li><li><a href="login.html">Logout</a></li></ul></li><li><a href="">Doctors Page 01</a></li><li><a href="doctors-2.html">Doctors Page 02</a></li><li><a href="doctors-3.html">Doctors Page 03</a></li><li><a href="doctors-4.html">Doctors Page 04</a></li><li><a href="doctors-details.html">Doctors Details</a></li></ul></li><li class="dropdown"><a href="archive/index.html">Patients</a><ul><li class="dropdown"><a href="patient-dashboard.html">Patient Dashboard</a><ul><li><a href="patient-dashboard.html">Dashboard</a></li><li><a href="favourite-doctors.html">Favourite Doctors</a></li><li><a href="schedule-timing-2.html">Schedule Timing</a></li><li><a href="message-2.html">Messages</a></li><li><a href="patient-profile.html">My Profile</a></li><li><a href="change-password-2.html">Change Password</a></li><li><a href="login-2.html">Logout</a></li></ul></li><li><a href="submit.html">Submit Review</a></li><li><a href="book-appointment.html">Book Appointment</a></li><li><a href="register-page.html">Register Page</a></li></ul></li><li class="dropdown"><a href="archive/index.html">Pages</a><div class="megamenu"><div class="row clearfix"><div class="col-lg-6 column"><ul><li><h4>Page List 01</h4></li><li><a href="about.html">About Us</a></li><li><a href="service-1.html">Service 01</a></li><li><a href="service-2.html">Service 02</a></li><li><a href="faq.html">Faq's</a></li>
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
              <li class="dropdown"><a href="archive/index.html">Elements</a>
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
              <li class="dropdown"><a href="archive/index.html">News</a>
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
          <div class="btn-box"><a href="register-page.html" class="theme-btn-one"><i class="icon-image"></i>Join Now</a></div>
        </div>
      </div>
    </div>

    <!--sticky Header-->
    <div class="sticky-header">
      <div class="auto-container">
        <div class="outer-box">
          <div class="logo-box">
            <figure class="logo"><a href="archive/index.html"><img src="static/picture/small-logo.png" alt=""></a></figure>
          </div>
          <div class="menu-area">
            <nav class="main-menu clearfix">
              <!--Keep This Empty / Menu will come through Javascript-->
            </nav>
          </div>
          <div class="btn-box"><a href="register-page.html" class="theme-btn-one"><i class="icon-image"></i>Join Now</a></div>
        </div>
      </div>
    </div>
  </header>
  <!-- main-header end -->

  <!-- Mobile Menu  -->
  <div class="mobile-menu">
    <div class="menu-backdrop"></div>
    <div class="close-btn"><i class="fas fa-times"></i></div>

    <nav class="menu-box">
      <div class="nav-logo"><a href="archive/index.html"><img src="static/picture/logo-2.png" alt="" title=""></a></div>
      <div class="menu-outer"><!--Here Menu Will Come Automatically Via Javascript / Same Menu as in Header--></div>
      <div class="contact-info">
        <h4>Contact Info</h4>
        <ul>
          <li>Chicago 12, Melborne City, USA</li>
          <li><a href="">+88 01682648101</a></li>
          <li><a href="mailto:info@example.com">info@example.com</a></li>
        </ul>
      </div>
      <div class="social-links">
        <ul class="clearfix">
          <li><a href="archive/index.html"><span class="fab fa-twitter"></span></a></li>
          <li><a href="archive/index.html"><span class="fab fa-facebook-square"></span></a></li>
          <li><a href="archive/index.html"><span class="fab fa-pinterest-p"></span></a></li>
          <li><a href="archive/index.html"><span class="fab fa-instagram"></span></a></li>
          <li><a href="archive/index.html"><span class="fab fa-youtube"></span></a></li>
        </ul>
      </div>
    </nav>
  </div><!-- End Mobile Menu -->


  <!--page-title-two-->
  <section class="page-title-two">
    <div class="title-box centred bg-color-2">
      <div class="pattern-layer">
        <div class="pattern-1" style="background-image: url(static/picture/shape-70.png);"></div>
        <div class="pattern-2" style="background-image: url(static/picture/shape-71.png);"></div>
      </div>
      <div class="auto-container">
        <div class="title">
          <h1>Doctors List</h1>
        </div>
      </div>
    </div>
    <div class="lower-content">
      <div class="auto-container">
        <ul class="bread-crumb clearfix">
          <li><a href="archive/index.html">Home</a></li>
          <li>Doctors List</li>
        </ul>
      </div>
    </div>
  </section>
  <!--page-title-two end-->


  <div class="select-field bg-color-3">
    <div class="auto-container">
      <div class="content-box">
        <div class="form-inner clearfix">
          <form action="archive/index.html" method="post">
            <div class="form-group clearfix">
              <select class="wide">
                <option data-display="Select Location">Select Location</option>
                <option value="1">California</option>
                <option value="2">New York</option>
                <option value="3">Sun Francis</option>
                <option value="4">Shicago</option>
              </select>
              <input type="text" name="name" placeholder="Ex. Name, Specialization..." required="">
              <button type="submit"><i class="icon-Arrow-Right"></i></button>
            </div>
          </form>
          <ul class="select-box clearfix">
            <li>
              <div class="single-checkbox">
                <input type="radio" name="check-box" id="check1">
                <label for="check1"><span></span>All</label>
              </div>
            </li>
            <li>
              <div class="single-checkbox">
                <input type="radio" name="check-box" id="check2" checked="">
                <label for="check2"><span></span>Doctor</label>
              </div>
            </li>
            <li>
              <div class="single-checkbox">
                <input type="radio" name="check-box" id="check3">
                <label for="check3"><span></span>Clinic</label>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>


  <!-- doctors-page-section -->
  <section class="clinic-section doctors-page-section">
    <div class="auto-container">
      <div class="row clearfix">
        <div class="col-lg-8 col-md-12 col-sm-12 content-side">
          <div class="item-shorting clearfix">
            <div class="left-column pull-left">
              <h3>Showing 1-6 of 20 Results</h3>
            </div>
            <div class="right-column pull-right clearfix">
              <div class="short-box clearfix">
                <div class="select-box">
                  <select class="wide">
                    <option data-display="Sort by">Sort by</option>
                    <option value="1">Cardiology</option>
                    <option value="2">Nurology</option>
                    <option value="4">Pragnency</option>
                  </select>
                </div>
              </div>
              <div class="menu-box">
                <button class="list-view on"><i class="icon-List"></i></button>
                <button class="grid-view"><i class="icon-Grid"></i></button>
              </div>
            </div>
          </div>
          <div class="wrapper list">
            <div id="clinicListContent" class="clinic-list-content list-item">
              <div class="clinic-block-one">
                <div class="inner-box">
                  <div class="pattern">
                    <div class="pattern-1" style="background-image: url(static/picture/shape-24.png);"></div>
                    <div class="pattern-2" style="background-image: url(static/picture/shape-25.png);"></div>
                  </div>
                  <figure class="image-box"><img src="/img/1.jpg" alt=""></figure>
                  <div class="content-box">
                    <div class="like-box"><a href="doctors-details.html"><i class="far fa-heart"></i></a></div>
                    <ul class="name-box clearfix">
                      <li class="name"><h3><a href="doctors-details.html">李华</a></h3></li>
                      <li><i class="icon-Trust-1"></i></li>
                      <li><i class="icon-Trust-2"></i></li>
                    </ul>
                    <span class="designation">工号:10001</span>
                    <div class="text">
                      <p>电话：17866668888</p>
                      <p>职称：副教授</p>
                      <p>简介：李华医生毕业于。。。。，在。。领域是颇有建树。</p>
                    </div>
                    <div class="rating-box clearfix">
                      <ul class="rating clearfix">
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><a href="doctors-details.html">(17)</a></li>
                      </ul>
                      <div class="link"><a href="doctors-details.html">24/7 Available</a></div>
                    </div>



                    <div class="btn-box"><a href="doctors-details.html">Visit Now</a></div>
                  </div>
                </div>
              </div>




              <div class="clinic-block-one">
                <div class="inner-box">
                  <div class="pattern">
                    <div class="pattern-1" style="background-image: url(static/picture/shape-24.png);"></div>
                    <div class="pattern-2" style="background-image: url(static/picture/shape-25.png);"></div>
                  </div>
                  <figure class="image-box"><img src="static/picture/team-2.jpg" alt=""></figure>
                  <div class="content-box">
                    <div class="like-box"><a href="doctors-details.html"><i class="far fa-heart"></i></a></div>
                    <ul class="name-box clearfix">
                      <li class="name"><h3><a href="doctors-details.html">Dr. Mary Astor</a></h3></li>
                      <li><i class="icon-Trust-1"></i></li>
                      <li><i class="icon-Trust-2"></i></li>
                    </ul>
                    <span class="designation">MDS - Periodontology and BDS</span>
                    <div class="text">
                      <p>Lorem ipsum dolor sit amet consectur adipisc elit sed eiusmod tempor incididunt labore dolore magna.</p>
                    </div>
                    <div class="rating-box clearfix">
                      <ul class="rating clearfix">
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><a href="doctors-details.html">(21)</a></li>
                      </ul>
                      <div class="link not-available"><a href="doctors-details.html">Not Available</a></div>
                    </div>
                    <div class="location-box">
                      <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                    </div>
                    <div class="btn-box"><a href="doctors-details.html">Visit Now</a></div>
                  </div>
                </div>
              </div>
              <div class="clinic-block-one">
                <div class="inner-box">
                  <div class="pattern">
                    <div class="pattern-1" style="background-image: url(static/picture/shape-24.png);"></div>
                    <div class="pattern-2" style="background-image: url(static/picture/shape-25.png);"></div>
                  </div>
                  <figure class="image-box"><img src="static/picture/team-3.jpg" alt=""></figure>
                  <div class="content-box">
                    <div class="like-box"><a href="doctors-details.html"><i class="far fa-heart"></i></a></div>
                    <ul class="name-box clearfix">
                      <li class="name"><h3><a href="doctors-details.html">Dr. Rex Allen</a></h3></li>
                      <li><i class="icon-Trust-1"></i></li>
                      <li></li>
                    </ul>
                    <span class="designation">BDS, MDS - Oral & Maxillofacial Surgery</span>
                    <div class="text">
                      <p>Lorem ipsum dolor sit amet consectur adipisc elit sed eiusmod tempor incididunt labore dolore magna.</p>
                    </div>
                    <div class="rating-box clearfix">
                      <ul class="rating clearfix">
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><a href="doctors-details.html">(32)</a></li>
                      </ul>
                      <div class="link"><a href="doctors-details.html">24/7 Available</a></div>
                    </div>
                    <div class="location-box">
                      <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                    </div>
                    <div class="btn-box"><a href="doctors-details.html">Visit Now</a></div>
                  </div>
                </div>
              </div>
              <div class="clinic-block-one">
                <div class="inner-box">
                  <div class="pattern">
                    <div class="pattern-1" style="background-image: url(static/picture/shape-24.png);"></div>
                    <div class="pattern-2" style="background-image: url(static/picture/shape-25.png);"></div>
                  </div>
                  <figure class="image-box"><img src="static/picture/team-4.jpg" alt=""></figure>
                  <div class="content-box">
                    <div class="like-box"><a href="doctors-details.html"><i class="far fa-heart"></i></a></div>
                    <ul class="name-box clearfix">
                      <li class="name"><h3><a href="doctors-details.html">Dr. Stella Adler</a></h3></li>
                      <li></li>
                      <li><i class="icon-Trust-2"></i></li>
                    </ul>
                    <span class="designation">MDS - Periodontology and BDS</span>
                    <div class="text">
                      <p>Lorem ipsum dolor sit amet consectur adipisc elit sed eiusmod tempor incididunt labore dolore magna.</p>
                    </div>
                    <div class="rating-box clearfix">
                      <ul class="rating clearfix">
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><a href="doctors-details.html">(20)</a></li>
                      </ul>
                      <div class="link"><a href="doctors-details.html">24/7 Available</a></div>
                    </div>
                    <div class="location-box">
                      <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                    </div>
                    <div class="btn-box"><a href="doctors-details.html">Visit Now</a></div>
                  </div>
                </div>
              </div>
              <div class="clinic-block-one">
                <div class="inner-box">
                  <div class="pattern">
                    <div class="pattern-1" style="background-image: url(static/picture/shape-24.png);"></div>
                    <div class="pattern-2" style="background-image: url(static/picture/shape-25.png);"></div>
                  </div>
                  <figure class="image-box"><img src="static/picture/team-19.jpg" alt=""></figure>
                  <div class="content-box">
                    <div class="like-box"><a href="doctors-details.html"><i class="far fa-heart"></i></a></div>
                    <ul class="name-box clearfix">
                      <li class="name"><h3><a href="doctors-details.html">Dr. Leroy Anderson</a></h3></li>
                      <li><i class="icon-Trust-1"></i></li>
                      <li><i class="icon-Trust-2"></i></li>
                    </ul>
                    <span class="designation">BDS, MDS - Oral & Maxillofacial Surgery</span>
                    <div class="text">
                      <p>Lorem ipsum dolor sit amet consectur adipisc elit sed eiusmod tempor incididunt labore dolore magna.</p>
                    </div>
                    <div class="rating-box clearfix">
                      <ul class="rating clearfix">
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><a href="doctors-details.html">(35)</a></li>
                      </ul>
                      <div class="link not-available"><a href="doctors-details.html">Not Available</a></div>
                    </div>
                    <div class="location-box">
                      <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                    </div>
                    <div class="btn-box"><a href="doctors-details.html">Visit Now</a></div>
                  </div>
                </div>
              </div>
              <div class="clinic-block-one">
                <div class="inner-box">
                  <div class="pattern">
                    <div class="pattern-1" style="background-image: url(static/picture/shape-24.png);"></div>
                    <div class="pattern-2" style="background-image: url(static/picture/shape-25.png);"></div>
                  </div>
                  <figure class="image-box"><img src="static/picture/team-20.jpg" alt=""></figure>
                  <div class="content-box">
                    <div class="like-box"><a href="doctors-details.html"><i class="far fa-heart"></i></a></div>
                    <ul class="name-box clearfix">
                      <li class="name"><h3><a href="doctors-details.html">Dr. Agnes Ayres</a></h3></li>
                      <li><i class="icon-Trust-1"></i></li>
                      <li></li>
                    </ul>
                    <span class="designation">BDS, MDS - Oral & Maxillofacial Surgery</span>
                    <div class="text">
                      <p>Lorem ipsum dolor sit amet consectur adipisc elit sed eiusmod tempor incididunt labore dolore magna.</p>
                    </div>
                    <div class="rating-box clearfix">
                      <ul class="rating clearfix">
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><i class="icon-Star"></i></li>
                        <li><a href="doctors-details.html">(35)</a></li>
                      </ul>
                      <div class="link"><a href="doctors-details.html">24/7 Available</a></div>
                    </div>
                    <div class="location-box">
                      <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                    </div>
                    <div class="btn-box"><a href="doctors-details.html">Visit Now</a></div>
                  </div>
                </div>
              </div>
            </div>
            <div class="clinic-grid-content">
              <div class="row clearfix">
                <div class="col-lg-6 col-md-6 col-sm-12 team-block">
                  <div class="team-block-three">
                    <div class="inner-box">
                      <figure class="image-box">
                        <img src="static/picture/team-5.jpg" alt="">
                        <a href="doctors-details.html"><i class="far fa-heart"></i></a>
                      </figure>
                      <div class="lower-content">
                        <ul class="name-box clearfix">
                          <li class="name"><h3><a href="doctors-details.html">Dr. Mary Astor</a></h3></li>
                          <li><i class="icon-Trust-1"></i></li>
                          <li><i class="icon-Trust-2"></i></li>
                        </ul>
                        <span class="designation">MDS - Periodontology and BDS</span>
                        <div class="rating-box clearfix">
                          <ul class="rating clearfix">
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><a href="doctors-details.html">(32)</a></li>
                          </ul>
                        </div>
                        <div class="location-box">
                          <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                        </div>
                        <div class="lower-box clearfix">
                          <span class="text">24/7 Available</span>
                          <a href="doctors-details.html">Book Now</a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="col-lg-6 col-md-6 col-sm-12 team-block">
                  <div class="team-block-three">
                    <div class="inner-box">
                      <figure class="image-box">
                        <img src="static/picture/team-6.jpg" alt="">
                        <a href="doctors-details.html"><i class="far fa-heart"></i></a>
                      </figure>
                      <div class="lower-content">
                        <ul class="name-box clearfix">
                          <li class="name"><h3><a href="doctors-details.html">Dr. Rex Allen</a></h3></li>
                          <li><i class="icon-Trust-1"></i></li>
                          <li><i class="icon-Trust-2"></i></li>
                        </ul>
                        <span class="designation">BDS, MDS - Oral & Maxillofacial Surgery</span>
                        <div class="rating-box clearfix">
                          <ul class="rating clearfix">
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><a href="doctors-details.html">(17)</a></li>
                          </ul>
                        </div>
                        <div class="location-box">
                          <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                        </div>
                        <div class="lower-box clearfix">
                          <span class="text not-available">Not Available</span>
                          <a href="doctors-details.html">Book Now</a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="col-lg-6 col-md-6 col-sm-12 team-block">
                  <div class="team-block-three">
                    <div class="inner-box">
                      <figure class="image-box">
                        <img src="static/picture/team-7.jpg" alt="">
                        <a href="doctors-details.html"><i class="far fa-heart"></i></a>
                      </figure>
                      <div class="lower-content">
                        <ul class="name-box clearfix">
                          <li class="name"><h3><a href="doctors-details.html">Dr. Leroy Anderson</a></h3></li>
                          <li><i class="icon-Trust-1"></i></li>
                          <li><i class="icon-Trust-2"></i></li>
                        </ul>
                        <span class="designation">MDS - Periodontology and BDS</span>
                        <div class="rating-box clearfix">
                          <ul class="rating clearfix">
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><a href="doctors-details.html">(25)</a></li>
                          </ul>
                        </div>
                        <div class="location-box">
                          <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                        </div>
                        <div class="lower-box clearfix">
                          <span class="text">24/7 Available</span>
                          <a href="doctors-details.html">Book Now</a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="col-lg-6 col-md-6 col-sm-12 team-block">
                  <div class="team-block-three">
                    <div class="inner-box">
                      <figure class="image-box">
                        <img src="static/picture/team-8.jpg" alt="">
                        <a href="doctors-details.html"><i class="far fa-heart"></i></a>
                      </figure>
                      <div class="lower-content">
                        <ul class="name-box clearfix">
                          <li class="name"><h3><a href="doctors-details.html">Dr. Agnes Ayres</a></h3></li>
                          <li><i class="icon-Trust-1"></i></li>
                          <li><i class="icon-Trust-2"></i></li>
                        </ul>
                        <span class="designation">BDS, MDS - Oral & Maxillofacial Surgery</span>
                        <div class="rating-box clearfix">
                          <ul class="rating clearfix">
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><a href="doctors-details.html">(10)</a></li>
                          </ul>
                        </div>
                        <div class="location-box">
                          <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                        </div>
                        <div class="lower-box clearfix">
                          <span class="text">24/7 Available</span>
                          <a href="doctors-details.html">Book Now</a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="col-lg-6 col-md-6 col-sm-12 team-block">
                  <div class="team-block-three">
                    <div class="inner-box">
                      <figure class="image-box">
                        <img src="static/picture/team-9.jpg" alt="">
                        <a href="doctors-details.html"><i class="far fa-heart"></i></a>
                      </figure>
                      <div class="lower-content">
                        <ul class="name-box clearfix">
                          <li class="name"><h3><a href="doctors-details.html">Dr. Julia Jhones</a></h3></li>
                          <li><i class="icon-Trust-1"></i></li>
                          <li><i class="icon-Trust-2"></i></li>
                        </ul>
                        <span class="designation">MBBS, MS - General Surgery, MCh</span>
                        <div class="rating-box clearfix">
                          <ul class="rating clearfix">
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><a href="doctors-details.html">(17)</a></li>
                          </ul>
                        </div>
                        <div class="location-box">
                          <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                        </div>
                        <div class="lower-box clearfix">
                          <span class="text not-available">Not Available</span>
                          <a href="doctors-details.html">Book Now</a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="col-lg-6 col-md-6 col-sm-12 team-block">
                  <div class="team-block-three">
                    <div class="inner-box">
                      <figure class="image-box">
                        <img src="static/picture/team-9.jpg" alt="">
                        <a href="doctors-details.html"><i class="far fa-heart"></i></a>
                      </figure>
                      <div class="lower-content">
                        <ul class="name-box clearfix">
                          <li class="name"><h3><a href="doctors-details.html">Dr. Terry Bradshaw</a></h3></li>
                          <li><i class="icon-Trust-1"></i></li>
                          <li><i class="icon-Trust-2"></i></li>
                        </ul>
                        <span class="designation">MBBS, MS - General Surgery, MCh</span>
                        <div class="rating-box clearfix">
                          <ul class="rating clearfix">
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><a href="doctors-details.html">(30)</a></li>
                          </ul>
                        </div>
                        <div class="location-box">
                          <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                        </div>
                        <div class="lower-box clearfix">
                          <span class="text">24/7 Available</span>
                          <a href="doctors-details.html">Book Now</a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="col-lg-6 col-md-6 col-sm-12 team-block">
                  <div class="team-block-three">
                    <div class="inner-box">
                      <figure class="image-box">
                        <img src="static/picture/team-10.jpg" alt="">
                        <a href="doctors-details.html"><i class="far fa-heart"></i></a>
                      </figure>
                      <div class="lower-content">
                        <ul class="name-box clearfix">
                          <li class="name"><h3><a href="doctors-details.html">Dr. Richard Branson</a></h3></li>
                          <li><i class="icon-Trust-1"></i></li>
                          <li><i class="icon-Trust-2"></i></li>
                        </ul>
                        <span class="designation">MBBS, MS - General Surgery, MCh</span>
                        <div class="rating-box clearfix">
                          <ul class="rating clearfix">
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><a href="doctors-details.html">(12)</a></li>
                          </ul>
                        </div>
                        <div class="location-box">
                          <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                        </div>
                        <div class="lower-box clearfix">
                          <span class="text">24/7 Available</span>
                          <a href="doctors-details.html">Book Now</a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="col-lg-6 col-md-6 col-sm-12 team-block">
                  <div class="team-block-three">
                    <div class="inner-box">
                      <figure class="image-box">
                        <img src="static/picture/team-11.jpg" alt="">
                        <a href="doctors-details.html"><i class="far fa-heart"></i></a>
                      </figure>
                      <div class="lower-content">
                        <ul class="name-box clearfix">
                          <li class="name"><h3><a href="doctors-details.html">Dr. Stella Adler</a></h3></li>
                          <li><i class="icon-Trust-1"></i></li>
                          <li><i class="icon-Trust-2"></i></li>
                        </ul>
                        <span class="designation">MBBS, MS - General Surgery, MCh</span>
                        <div class="rating-box clearfix">
                          <ul class="rating clearfix">
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><i class="icon-Star"></i></li>
                            <li><a href="doctors-details.html">(05)</a></li>
                          </ul>
                        </div>
                        <div class="location-box">
                          <p><i class="fas fa-map-marker-alt"></i>G87P, Birmingham, UK</p>
                        </div>
                        <div class="lower-box clearfix">
                          <span class="text not-available">Not Available</span>
                          <a href="doctors-details.html">Book Now</a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="pagination-wrapper">
            <ul class="pagination">
              <li><a href="clinic-1.html" class="current">1</a></li>
              <li><a href="clinic-1.html">2</a></li>
              <li><a href="clinic-1.html">3</a></li>
              <li><a href="clinic-1.html"><i class="icon-Arrow-Right"></i></a></li>
            </ul>
          </div>
        </div>
        <div class="col-lg-4 col-md-12 col-sm-12 sidebar-side">
          <div class="map-inner ml-10">
            <div class="google-map" id="contact-google-map" data-map-lat="40.712776" data-map-lng="-74.005974" data-icon-path="assets/images/icons/map-marker.png" data-map-title="Brooklyn, New York, United Kingdom" data-map-zoom="12" data-markers='{"marker-1":[40.712776,-74.005974,"<h4>Branch Office</h4><p>77/99 New York</p>","assets/images/icons/map-marker.png"]}'></div></div></div></div></div></section><!-- doctors-page-section end --><!-- agent-section --><section class="agent-section"><div class="auto-container"><div class="inner-container bg-color-2"><div class="row clearfix"><div class="col-lg-6 col-md-12 col-sm-12 left-column"><div class="content_block_3"><div class="content-box"><h3>Emergency call</h3><div class="support-box"><div class="icon-box"><i class="fas fa-phone"></i></div><span>Telephone</span><h3><a href="">+(111) 65_458_856</a></h3></div></div></div></div><div class="col-lg-6 col-md-12 col-sm-12 right-column"><div class="content_block_4"><div class="content-box"><h3>Sign up for Email</h3><form action="archive/index.html" method="post" class="subscribe-form"><div class="form-group"><input type="email" name="email" placeholder="Your Email" required=""><button type="submit" class="theme-btn-one">Submit now<i class="icon-Arrow-Right"></i></button></div></form></div></div></div></div></div></div></section><!-- agent-section --><!-- main-footer --><footer class="main-footer"><div class="footer-top"><div class="pattern-layer"><div class="pattern-1" style="background-image: url(static/picture/shape-30.png);"></div><div class="pattern-2" style="background-image: url(static/picture/shape-31.png);"></div></div><div class="auto-container"><div class="widget-section"><div class="row clearfix"><div class="col-lg-4 col-md-6 col-sm-12 footer-column"><div class="footer-widget logo-widget"><figure class="footer-logo"><a href="archive/index.html"><img src="static/picture/footer-logo.png" alt=""></a></figure><div class="text"><p>Lorem ipsum is placeholder text commonly used in the graphic,print,and publishing .</p></div></div></div><div class="col-lg-2 col-md-6 col-sm-12 footer-column"><div class="footer-widget links-widget"><div class="widget-title"><h3>About</h3></div><div class="widget-content"><ul class="links clearfix"><li><a href="archive/index.html">About Us</a></li><li><a href="archive/index.html">Listing</a></li><li><a href="archive/index.html">How It Works</a></li><li><a href="archive/index.html">Our Services</a></li><li><a href="archive/index.html">Our Blog</a></li><li><a href="archive/index.html">Contact Us</a></li></ul></div></div></div><div class="col-lg-3 col-md-6 col-sm-12 footer-column"><div class="footer-widget links-widget"><div class="widget-title"><h3>Useful Links</h3></div><div class="widget-content"><ul class="links clearfix"><li><a href="archive/index.html">Specialist</a></li><li><a href="archive/index.html">Clinic</a></li><li><a href="archive/index.html">Hospital</a></li><li><a href="archive/index.html">Download App</a></li><li><a href="archive/index.html">Join as a Doctor</a></li><li><a href="archive/index.html">Privacy Policy</a></li></ul></div></div></div><div class="col-lg-3 col-md-6 col-sm-12 footer-column"><div class="footer-widget contact-widget"><div class="widget-title"><h3>Contacts</h3></div><div class="widget-content"><ul class="info-list clearfix"><li><i class="fas fa-map-marker-alt"></i>Flat 20,Reynolds Neck,North Helenaville,FV77 8WS </li><li><i class="fas fa-microphone"></i><a href="">+2(305) 587-3407</a></li><li><i class="fas fa-envelope"></i><a href="mailto:info@example.com">info@example.com</a></li></ul></div></div></div></div></div></div></div><div class="footer-bottom"><div class="auto-container"><div class="inner-box clearfix"><div class="copyright pull-left"><p><a href="/">Docpro</a>&copy;2020 All Right Reserved</p></div><ul class="footer-nav pull-right clearfix"><li><a href="archive/index.html">Terms of Service</a></li><li><a href="archive/index.html">Privacy Policy</a></li></ul></div></div></div></footer><!-- main-footer end --><!--Scroll to top--><button class="scroll-top scroll-to-target" data-target="html"><span class="fa fa-arrow-up"></span></button></div><!-- jequery plugins --><script src="static/js/jquery.js"></script><script src="static/js/popper.min.js"></script><script src="static/js/bootstrap.min.js"></script><script src="static/js/owl.js"></script><script src="static/js/wow.js"></script><script src="static/js/validation.js"></script><script src="static/js/jquery.fancybox.js"></script><script src="static/js/appear.js"></script><script src="static/js/scrollbar.js"></script><script src="static/js/tilt.jquery.js"></script><script src="static/js/jquery.paroller.min.js"></script><script src="static/js/jquery.nice-select.min.js"></script><script src="static/js/product-filter.js"></script><!-- map script --><script src="static/js/gmaps.js"></script><script src="static/js/map-helper.js"></script><!-- main-js --><script src="static/js/script.js"></script></body><!-- End of .page_wrapper --></html>
