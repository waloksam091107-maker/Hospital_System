package com.ustb.hospital.servlet;

import com.ustb.hospital.service.*;
import com.ustb.hospital.utils.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/portal/admin/account-security/*")
public class AccountSecurityServlet extends HttpServlet {
    private final AccountSecurityService service;
    public AccountSecurityServlet(){this(new AccountSecurityService());}
    public AccountSecurityServlet(AccountSecurityService service){this.service=service;}
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        if(PortalServletSupport.requireRole(req,resp,SessionIdentity.ROLE_ADMIN)==null)return;
        try{
            int page=req.getParameter("page")==null?1:Integer.parseInt(req.getParameter("page"));
            int size=req.getParameter("pageSize")==null?20:Integer.parseInt(req.getParameter("pageSize"));
            Map<String,Object> body=new LinkedHashMap<>();body.put("success",true);body.put("items",service.list(req.getParameter("role"),page,size));body.put("page",page);body.put("pageSize",size);PortalServletSupport.writeJson(resp,body);
        }catch(IllegalArgumentException e){PortalServletSupport.writeJsonError(resp,400,"INVALID_REQUEST","角色或分页参数无效。");}
        catch(RuntimeException e){PortalServletSupport.writeJsonError(resp,503,"SECURITY_UNAVAILABLE","账号列表暂时不可用。");}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        if(PortalServletSupport.requireRole(req,resp,SessionIdentity.ROLE_ADMIN)==null)return;
        Integer id=PortalServletSupport.parsePositiveInt(req.getParameter("accountId"));
        if(id==null){PortalServletSupport.writeJsonError(resp,400,"INVALID_REQUEST","账号编号无效。");return;}
        String path=req.getPathInfo(),role=req.getParameter("role");AuditContext context=AuditContext.fromRequest(req);AppointmentOperationResult result;
        if("/enable".equals(path))result=service.setEnabled(role,id,true,context);
        else if("/disable".equals(path))result=service.setEnabled(role,id,false,context);
        else if("/unlock".equals(path))result=service.unlock(role,id,context);
        else if("/reset".equals(path))result=service.resetPassword(role,id,req.getParameter("temporaryPassword"),context);
        else{PortalServletSupport.writeJsonError(resp,400,"INVALID_REQUEST","账号操作无效。");return;}
        PortalServletSupport.writeOperationResult(resp,result);
    }
}
