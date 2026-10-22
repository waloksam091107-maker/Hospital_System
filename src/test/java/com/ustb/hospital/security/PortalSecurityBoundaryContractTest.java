package com.ustb.hospital.security;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Contract for new portal endpoints: they must use the shared session-security boundary. */
public class PortalSecurityBoundaryContractTest {
    @Test
    public void portalWriteRoutesAreCoveredByAuthenticationAuditAndCsrfFilters() throws Exception {
        String descriptor = read("src/main/webapp/WEB-INF/web.xml");

        for (String route : new String[]{"/portal/profile/*", "/portal/balance/*", "/portal/admin/*"}) {
            assertTrue("认证过滤器必须保护 " + route, descriptor.contains(
                    "<filter-name>authentication</filter-name><url-pattern>" + route + "</url-pattern>"));
            assertTrue("审计过滤器必须覆盖 " + route, descriptor.contains(
                    "<filter-name>audit</filter-name><url-pattern>" + route + "</url-pattern>"));
            assertTrue("CSRF 过滤器必须保护 " + route, descriptor.contains(
                    "<filter-name>csrf</filter-name><url-pattern>" + route + "</url-pattern>"));
        }
    }

    @Test
    public void portalServletSupportCentralizesRoleAndInputValidation() throws Exception {
        String support = read("src/main/java/com/ustb/hospital/servlet/PortalServletSupport.java");

        assertTrue("门户 Servlet 必须从 SessionIdentity 获取当前用户", support.contains("SessionIdentity"));
        assertTrue("门户 Servlet 必须集中角色校验", support.contains("requireRole"));
        assertTrue("门户 Servlet 必须集中输出安全 JSON 错误", support.contains("writeJsonError"));
        assertTrue("门户 Servlet 必须集中金额校验", support.contains("parsePositiveAmount"));
    }

    private String read(String relativePath) throws Exception {
        Path path = Paths.get(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
