package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression checks for navigation links used by the registration minimum loop. */
public class CoreRegistrationNavigationTest {
    @Test
    public void homePageUsesTheLoginRoleAndCanonicalDoctorWorkPages() throws Exception {
        String page = readWebFile("hospro", "index.html");

        assertTrue("登录页传递的 roleCode 必须能驱动首页角色菜单", page.contains(
                "const typeId = urlParams.get('roleCode') || urlParams.get('typeId');"));
        assertTrue("医生预约记录入口必须进入当前的预约页", page.contains(
                "`../doctor-schedule/appointments.html`"));
        assertTrue("医生排班入口必须进入当前的排班页", page.contains(
                "`../doctor-schedule/date.html?doctorId=${encodeURIComponent(doctorId)}`"));
    }

    @Test
    public void everyDoctorCardEntryCarriesTheSelectedDoctorId() throws Exception {
        String page = readWebFile("hospro", "doctor-directory.html");

        assertTrue("医生卡片应集中生成带医生编号的详情地址", page.contains(
                "var doctorDetailsUrl = 'doctors-details.html?doctorId=' + encodeURIComponent(v.doctorId);"));
        assertTrue("医生姓名入口必须使用带医生编号的详情地址", page.contains("href=\"' + doctorDetailsUrl + '\""));
        assertTrue("医生可预约入口必须使用带医生编号的详情地址", page.contains(
                "<div class=\"link\"><a href=\"' + doctorDetailsUrl + '\">24/7 Available</a>"));
    }

    @Test
    public void patientRecordPageKeepsThePatientIdentityWhenReturningToBooking() throws Exception {
        String page = readWebFile("hospro", "patient.jsp");

        assertTrue("患者新增预约入口必须由当前患者编号生成", page.contains("id=\"newAppointmentLink\""));
        assertTrue("患者新增预约入口必须打开科室选择并保留患者页面参数", page.contains(
                "appointment-departments.html?patientId=' + encodeURIComponent(patientId)"));
        assertTrue("患者返回主页入口必须由当前患者编号生成", page.contains("id=\"patientHomeLink\""));
    }

    @Test
    public void doctorScheduleSidebarDoesNotPointToMissingLocalPages() throws Exception {
        String page = readWebFile("doctor-schedule", "date.html");

        assertTrue("医生个人主页入口必须指向实际存在的页面", page.contains("id=\"doctorProfileLink\""));
        assertTrue("医生返回主页入口必须保留医生身份", page.contains("id=\"doctorHomeLink\""));
        assertTrue("排班页必须生成医生个人主页的正确相对路径", page.contains(
                "../hospro/doctor-profile.html?doctorId="));
        assertTrue("排班页必须生成医生首页的正确相对路径", page.contains(
                "../hospro/index.html?roleCode=1&doctorId="));
    }

    @Test
    public void directoryDoesNotExposeTemplateDoctorsBeforeTheRealListLoads() throws Exception {
        String page = readWebFile("hospro", "doctor-directory.html");

        assertTrue("加载真实医生列表前必须清空无编号的模板医生卡片", page.contains(
                "clinicListContent.empty().append('<p class=\"doctor-list-loading\">正在加载医生列表…</p>');"));
    }

    @Test
    public void departmentCardsNavigateToTheDoctorDirectory() throws Exception {
        String page = readWebFile("hospro", "index.html");

        assertTrue("一级科室卡片必须生成医生目录地址", page.contains(
                "var departmentBookingUrl = 'doctor-directory.html?departmentId=' + encodeURIComponent(v.departmentId)"));
        assertTrue("科室箭头入口必须使用医生目录地址", page.contains("href=\"${departmentBookingUrl}\""));
        assertTrue("科室立即预约入口必须使用医生目录地址", page.contains("class=\"theme-btn-one\" href=\"${departmentBookingUrl}\""));
    }

    @Test
    public void departmentSearchBindsARealDepartmentIdBeforeSubmitting() throws Exception {
        String page = readWebFile("hospro", "index.html");

        assertTrue("搜索请求应通过参数对象传递关键词", page.contains("url: appUrl('/find'),"));
        assertTrue("搜索结果必须使用表单外的可点击项，而不是无效的 option 容器", page.contains(
                "var suggestion = $('<button type=\"button\" class=\"department-suggestion\"></button>');"));
        assertTrue("选择搜索结果必须写入科室编号", page.contains("$(this).data('department-id')"));
        assertTrue("未选择有效科室时不应跳转至空的医生目录", page.contains("请选择搜索结果中的科室后再预约。"));
    }

    @Test
    public void departmentSearchFormIsClosedBeforeTheNewsletterForm() throws Exception {
        String page = readWebFile("hospro", "index.html");

        int departmentFormStart = page.indexOf("<form id=\"departmentForm\"");
        int departmentFormEnd = page.indexOf("</form>", departmentFormStart);
        int newsletterFormStart = page.indexOf("<form action=\"\" method=\"post\" class=\"subscribe-form\"");

        assertTrue("科室搜索表单必须存在", departmentFormStart >= 0);
        assertTrue("科室搜索表单必须在订阅表单前正确闭合，避免提交时校验邮箱", departmentFormEnd > departmentFormStart
                && departmentFormEnd < newsletterFormStart);
    }

    @Test
    public void departmentSuggestionsAreLaidOutAsASeparateDropdown() throws Exception {
        String page = readWebFile("hospro", "index.html");

        int departmentFormEnd = page.indexOf("</form>", page.indexOf("<form id=\"departmentForm\""));
        int resultStart = page.indexOf("<div id=\"departmentSearchResults\"");

        assertTrue("搜索结果容器必须位于表单外，避免与圆形提交按钮重叠", resultStart > departmentFormEnd);
        assertFalse("搜索建议不能继续放在圆形提交按钮所在的表单组中", page.contains("#departmentForm .form-group #result"));
    }

    @Test
    public void departmentSuggestionDoesNotUseTheSubmitButtonElement() throws Exception {
        String page = readWebFile("hospro", "index.html");

        assertTrue("搜索建议必须位于独立结果区域", page.contains("$(\"#departmentSearchResults\").append(suggestion);"));
        assertTrue("提交前必须保留已选择的科室编号", page.contains("var departmentId = $('#departmentId').val();"));
    }

    @Test
    public void departmentSearchUsesDedicatedResultsOutsideTheSubmitButtonLayout() throws Exception {
        String page = readWebFile("hospro", "index.html");

        assertTrue("页面必须提供独立的科室搜索结果区域", page.contains(
                "<div id=\"departmentSearchResults\""));
        assertTrue("搜索结果区域必须在没有结果时隐藏", page.contains("$(\"#departmentSearchResults\").empty().hide();"));
        assertTrue("选择建议后必须同步对应的科室编号", page.contains(
                "$('#departmentId').val($(this).data('department-id'));"));
        assertFalse("不应再使用浏览器原生候选列表，避免浏览器将其绘制到提交按钮上", page.contains("<datalist id=\"departmentOptions\">"));
    }

    @Test
    public void departmentSearchDisablesBrowserAutocompleteToAvoidOverlappingHistorySuggestions() throws Exception {
        String page = readWebFile("hospro", "index.html");

        assertTrue("科室搜索表单必须关闭浏览器自动补全", page.contains(
                "<form id=\"departmentForm\" action=\"\" method=\"post\" autocomplete=\"off\">"));
        assertTrue("科室输入框必须关闭浏览器自动补全", page.contains(
                "<input id=\"inputView\" type=\"text\" name=\"name\" autocomplete=\"off\""));
    }

    private String readWebFile(String directory, String fileName) throws Exception {
        Path page = Paths.get("src", "main", "webapp", directory, fileName);
        return new String(Files.readAllBytes(page), StandardCharsets.UTF_8);
    }
}
