package com.ustb.hospital.servlet;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class PortalBalanceRechargeEndpointTest {
    @Test public void patientRechargeUsesTheSafePortalRouteAndPage() throws Exception {
        String servlet = read("src/main/java/com/ustb/hospital/servlet/PortalBalanceRechargeServlet.java");
        String page = read("src/main/webapp/hospro/patients-pay.html");
        assertTrue(servlet.contains("@WebServlet(\"/portal/balance/recharge\")"));
        assertTrue(servlet.contains("requireRole(request, response, SessionIdentity.ROLE_PATIENT)"));
        assertTrue(servlet.contains("parsePositiveAmount"));
        assertTrue(page.contains("/portal/balance/recharge"));
        assertTrue(page.contains("/security/csrf"));
        assertTrue(page.contains("模拟充值"));
    }
    private String read(String relativePath) throws Exception { return new String(Files.readAllBytes(Paths.get(relativePath)), StandardCharsets.UTF_8); }
}
