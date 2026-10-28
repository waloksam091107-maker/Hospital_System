package com.ustb.hospital.servlet;

import com.ustb.hospital.service.*;
import com.ustb.hospital.utils.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

/** Session-scoped clinic APIs. Authentication/CSRF filters also protect the route prefix. */
@WebServlet(urlPatterns={"/clinic/slots","/clinic/leave","/clinic/queue"})
public class ClinicWorkflowServlet extends HttpServlet {
    private final ClinicWorkflowService service;
    public ClinicWorkflowServlet(){this(new ClinicWorkflowService());}
    public ClinicWorkflowServlet(ClinicWorkflowService service){this.service=service;}
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        String role=SessionIdentity.getCurrentRole(req);Integer actor=SessionIdentity.getCurrentUserId(req);if(actor==null){unauthorized(resp);return;}
        String path=req.getServletPath();
        try{
            Object list;
            if("/clinic/slots".equals(path)){
                Integer doctor=AppointmentServletSupport.parsePositiveInt(req.getParameter("doctorId"));
                if(doctor==null&&"DOCTOR".equals(role))doctor=actor;if(doctor==null)throw new IllegalArgumentException("请选择医生。");list=service.slots(doctor,req.getParameter("date"));
            }else if("/clinic/leave".equals(path)){
                if(!"DOCTOR".equals(role)&&!"ADMIN".equals(role)){forbidden(resp);return;}list=service.leaves(role,actor);
            }else if("/clinic/queue".equals(path)){
                if(!"DOCTOR".equals(role)&&!"PATIENT".equals(role)){forbidden(resp);return;}list=service.queue(role,actor,req.getParameter("date"),req.getParameter("period"));
            }else{resp.setStatus(404);return;}
            Map<String,Object> result=new LinkedHashMap<>();result.put("success",true);result.put("list",list);AppointmentServletSupport.writeJson(resp,result);
        }catch(IllegalArgumentException e){AppointmentServletSupport.writeBadRequest(resp,e.getMessage());}
        catch(RuntimeException e){resp.setStatus(503);AppointmentServletSupport.writeJson(resp,Collections.singletonMap("message","门诊服务暂时不可用，请确认数据库已完成升级。"));}
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        String role=SessionIdentity.getCurrentRole(req);Integer actor=SessionIdentity.getCurrentUserId(req);if(actor==null){unauthorized(resp);return;}
        try{
            String path=req.getServletPath(),action=req.getParameter("action");AppointmentOperationResult result;
            if("/clinic/slots".equals(path)){
                if(!"DOCTOR".equals(role)){forbidden(resp);return;}if(!"capacity".equals(action))throw new IllegalArgumentException("操作无效。");
                result=service.capacity(actor,positive(req,"slotId"),positive(req,"capacity"),AuditContext.fromRequest(req));
            }else if("/clinic/leave".equals(path)){
                result=service.leaveAction(action,role,actor,"apply".equals(action)?0:positive(req,"id"),req.getParameter("startDate"),req.getParameter("endDate"),req.getParameter("periods"),req.getParameter("reason"),AuditContext.fromRequest(req));
            }else if("/clinic/queue".equals(path)){
                result=service.queueAction(action,role,actor,"call-next".equals(action)?0:positive(req,"appointmentId"),req.getParameter("date"),req.getParameter("period"),AuditContext.fromRequest(req));
            }else{resp.setStatus(404);return;}
            AppointmentServletSupport.writeResult(resp,result);
        }catch(IllegalArgumentException e){AppointmentServletSupport.writeBadRequest(resp,e.getMessage());}
    }
    private static int positive(HttpServletRequest r,String key){Integer n=AppointmentServletSupport.parsePositiveInt(r.getParameter(key));if(n==null)throw new IllegalArgumentException("编号或容量无效。");return n;}
    private static void unauthorized(HttpServletResponse r)throws IOException{r.setStatus(401);Map<String,Object>b=new HashMap<>();b.put("success",false);b.put("code","LOGIN_REQUIRED");b.put("message","请先登录。");AppointmentServletSupport.writeJson(r,b);}
    private static void forbidden(HttpServletResponse r)throws IOException{AppointmentServletSupport.writeResult(r,AppointmentOperationResult.failure("FORBIDDEN","当前账号无权执行此操作。"));}
}
