package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.service.BalanceService;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.SessionIdentity;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Patient-only simulated recharge endpoint; no real-payment credential is accepted. */
@WebServlet("/portal/balance/recharge")
public class PortalBalanceRechargeServlet extends HttpServlet {
    private final BalanceService balance;
    public PortalBalanceRechargeServlet() { this(new BalanceService()); }
    PortalBalanceRechargeServlet(BalanceService balance) { this.balance = balance; }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer patientId = PortalServletSupport.requireRole(request, response, SessionIdentity.ROLE_PATIENT);
        if (patientId == null) return;
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("transactions", balance.recentTransactions(patientId.intValue(), 30));
        PortalServletSupport.writeJson(response, result);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer patientId = PortalServletSupport.requireRole(request, response, SessionIdentity.ROLE_PATIENT);
        if (patientId == null) return;
        BigDecimal amount = PortalServletSupport.parsePositiveAmount(request.getParameter("amount"), BalanceService.MAX_RECHARGE_AMOUNT);
        AppointmentOperationResult result = balance.recharge(patientId.intValue(), amount, AuditContext.fromRequest(request));
        PortalServletSupport.writeOperationResult(response, result);
    }
}
