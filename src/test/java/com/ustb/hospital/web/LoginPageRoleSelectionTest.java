package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Prevents the direct login URL from submitting an empty role code. */
public class LoginPageRoleSelectionTest {
    @Test
    public void loginPageLetsUserSubmitDoctorRoleWithoutQueryParameters() throws Exception {
        String page = new String(Files.readAllBytes(Paths.get("src", "main", "webapp", "hospro", "login.html")), StandardCharsets.UTF_8);

        assertTrue("登录页应提交角色字段", page.contains("name=\"roleCode\""));
        assertTrue("直接访问登录页时，医生应为默认角色", page.contains("<option value=\"1\" selected>医生</option>"));
        assertTrue("登录页应允许选择患者角色", page.contains("<option value=\"2\">患者</option>"));
        assertTrue("登录页应允许选择管理员角色", page.contains("<option value=\"3\">管理员</option>"));
    }
}
