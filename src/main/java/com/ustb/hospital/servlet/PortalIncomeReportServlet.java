package com.ustb.hospital.servlet;

import com.ustb.hospital.service.IncomeReportService;
import com.ustb.hospital.utils.SessionIdentity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/portal/admin/income")
public class PortalIncomeReportServlet extends HttpServlet {
    private final IncomeReportService reports;
    public PortalIncomeReportServlet(){this(new IncomeReportService());}
    public PortalIncomeReportServlet(IncomeReportService reports){this.reports=reports;}
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws IOException{
        if(PortalServletSupport.requireRole(request,response,SessionIdentity.ROLE_ADMIN)==null)return;
        try{PortalServletSupport.writeJson(response,reports.report(request.getParameter("date")));}
        catch(IllegalArgumentException invalid){PortalServletSupport.writeJsonError(response,400,"INVALID_DATE","请选择有效的日期（YYYY-MM-DD）。");}
        catch(RuntimeException unavailable){PortalServletSupport.writeJsonError(response,503,"REPORT_UNAVAILABLE","收入报表暂时不可用，请稍后重试。");}
    }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{response.setStatus(405);response.setHeader("Allow","GET");}
}
