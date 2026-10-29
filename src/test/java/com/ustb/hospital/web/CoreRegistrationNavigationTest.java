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
    public void homePageUsesTheSessionRoleAndCanonicalDoctorWorkPages() throws Exception {
        String page = readWebFile("hospro", "index.html");

        assertTrue("首页必须读取经过认证的会话资料", page.contains("fetch(appUrl('/portal/profile')"));
        assertFalse("URL 角色参数不能决定账号身份", page.contains("urlParams.get('roleCode')"));
        assertFalse("首页不能再调用已停用的患者资料接口", page.contains("appUrl('/patients/select')"));
        assertTrue("医生预约记录入口必须进入当前的预约页", page.contains(
                "href=\"../doctor-schedule/appointments.html\""));
        assertTrue("医生排班入口必须进入当前的排班页", page.contains(
                "href=\"../doctor-schedule/date.html\""));
    }

    @Test
    public void doctorScheduleMenuUsesARealLinkBeforeTheLegacyGuardRuns() throws Exception {
        String page = readWebFile("hospro", "index.html");

        assertTrue("排班菜单必须具备非占位 href，避免被旧模板导航保护脚本拦截", page.contains(
                "<a id=\"paiban\" href=\"../doctor-schedule/date.html\""));
        assertTrue("医生角色菜单必须由会话资料驱动", page.contains("this.dataset.roleMenu === profile.role"));
        assertFalse("排班入口不能再信任 URL 的医生编号", page.contains("searchParams.get('doctorId')"));
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
        assertTrue("排班页必须进入会话所有的个人资料页", page.contains("../hospro/portal-profile.html"));
        assertTrue("排班页返回首页不能依赖 URL 身份", page.contains("../hospro/index.html"));
        assertTrue("排班页必须读取当前会话资料", page.contains("appUrl('/portal/profile')"));
        assertFalse("排班页不能查询 URL 指定的医生资料", page.contains("appUrl('/doctors/details')"));
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
    public void departmentSearchFormIsClosedBeforeNotificationEntry() throws Exception {
        String page = readWebFile("hospro", "index.html");

        int departmentFormStart = page.indexOf("<form id=\"departmentForm\"");
        int departmentFormEnd = page.indexOf("</form>", departmentFormStart);
        int notificationEntry = page.indexOf("<h3>预约与就诊消息提醒</h3>");

        assertTrue("科室搜索表单必须存在", departmentFormStart >= 0);
        assertTrue("科室搜索表单应独立闭合，消息提醒进入现有通知页面", departmentFormEnd > departmentFormStart
                && departmentFormEnd < notificationEntry);
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

    @Test
    public void patientFacingTemplatePagesLoadTheLegacyNavigationGuard() throws Exception {
        String[] pages = {"index.html", "doctor-directory.html", "book-appointment.html", "appointment-records.html"};
        for (String file : pages) {
            assertTrue(file + " 必须加载统一的旧模板导航保护脚本", readWebFile("hospro", file)
                    .contains("static/js/legacy-navigation-guard.js"));
        }
    }

    private String readWebFile(String directory, String fileName) throws Exception {
        Path page = Paths.get("src", "main", "webapp", directory, fileName);
        return new String(Files.readAllBytes(page), StandardCharsets.UTF_8);
    }
}
