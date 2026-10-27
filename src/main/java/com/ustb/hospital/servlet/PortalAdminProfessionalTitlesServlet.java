package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.ProfessionalTitles;
import com.ustb.hospital.service.AdminMasterDataService;
import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.SessionIdentity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/portal/admin/professional-titles")
public class PortalAdminProfessionalTitlesServlet extends HttpServlet {
    private final AdminMasterDataService service;
    public PortalAdminProfessionalTitlesServlet() { this(new AdminMasterDataService()); }
    PortalAdminProfessionalTitlesServlet(AdminMasterDataService service) { this.service = service; }
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws IOException{if(PortalServletSupport.requireRole(request,response,SessionIdentity.ROLE_ADMIN)==null)return;PortalServletSupport.writeMasterDataPage(request,response,service,"professional-titles");}
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException {
        if(PortalServletSupport.requireRole(request,response,SessionIdentity.ROLE_ADMIN)==null)return;
        String action=request.getParameter("action");
        Integer id=PortalServletSupport.parsePositiveInt(request.getParameter("id"));
        AppointmentOperationResult result;
        if("delete".equals(action)) {
            result=id==null?AppointmentOperationResult.failure("INVALID_REQUEST","职称编号无效。"):service.deleteProfessionalTitle(id,AuditContext.fromRequest(request));
        } else if("update".equals(action)&&id==null) {
            result=AppointmentOperationResult.failure("INVALID_REQUEST","职称编号无效，未进行保存。");
        } else if("create".equals(action)||"update".equals(action)) {
            ProfessionalTitles title=new ProfessionalTitles();title.setId("create".equals(action)?0:id);
            title.setTitleName(request.getParameter("titleName"));title.setDescription(request.getParameter("description"));
            result=service.saveProfessionalTitle(title,AuditContext.fromRequest(request));
        } else result=AppointmentOperationResult.failure("INVALID_REQUEST","不支持的操作。");
        PortalServletSupport.writeOperationResult(response,result);
    }
}
