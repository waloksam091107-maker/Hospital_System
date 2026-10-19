package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.MedicalOrder;
import com.ustb.hospital.entity.MedicalRecord;
import com.ustb.hospital.service.MedicalRecordOperationResult;
import com.ustb.hospital.service.MedicalRecordsServiceImpl;
import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.utils.SessionIdentity;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Role-scoped read and finalization API for clinical records. */
@WebServlet("/medical-records/*")
public class MedicalRecordsServlet extends HttpServlet {
    private final MedicalRecordsServiceImpl service;
    public MedicalRecordsServlet() { this(new MedicalRecordsServiceImpl()); }
    MedicalRecordsServlet(MedicalRecordsServiceImpl service) { this.service = service; }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();
        if ("/doctor".equals(path)) {
            Integer doctorId = AppointmentServletSupport.requireDoctor(request, response);
            Integer appointmentId = AppointmentServletSupport.parsePositiveInt(request.getParameter("appointmentId"));
            if (doctorId == null) return;
            if (appointmentId == null) { AppointmentServletSupport.writeBadRequest(response, "预约编号无效。"); return; }
            writeRecord(response, service.findForDoctor(doctorId.intValue(), appointmentId.intValue()));
            return;
        }
        if ("/patient".equals(path)) {
            Integer patientId = AppointmentServletSupport.requirePatient(request, response);
            if (patientId == null) return;
            Integer appointmentId = AppointmentServletSupport.parsePositiveInt(request.getParameter("appointmentId"));
            if (request.getParameter("appointmentId") != null && appointmentId == null) {
                AppointmentServletSupport.writeBadRequest(response, "预约编号无效。");
                return;
            }
            if (appointmentId == null) {
                AppointmentServletSupport.writeJson(response, service.findAllForPatient(patientId.intValue()));
            } else {
                writeRecord(response, service.findForPatient(patientId.intValue(), appointmentId.intValue()));
            }
            return;
        }
        writeNotFound(response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean draft = "/draft".equals(request.getPathInfo());
        if (!draft && !"/finalize".equals(request.getPathInfo())) {
            AppointmentServletSupport.writeMethodNotAllowed(response);
            return;
        }
        Integer doctorId = AppointmentServletSupport.requireDoctor(request, response);
        Integer appointmentId = AppointmentServletSupport.parsePositiveInt(request.getParameter("appointmentId"));
        if (doctorId == null) return;
        if (appointmentId == null) { AppointmentServletSupport.writeBadRequest(response, "预约编号无效。"); return; }
        Integer version = null;
        String versionText = request.getParameter("version");
        if (versionText != null && !versionText.isEmpty()) {
            try {
                if (!versionText.matches("[0-9]{1,10}")) throw new NumberFormatException();
                version = Integer.valueOf(versionText);
            } catch (NumberFormatException exception) { AppointmentServletSupport.writeBadRequest(response, "草稿版本无效。"); return; }
        }
        AuditContext audit = AuditContext.fromRequest(request);
        MedicalRecordOperationResult result = draft ? service.saveDraft(doctorId.intValue(), appointmentId.intValue(),
                request.getParameter("diagnosis"), request.getParameter("treatmentPlan"),
                request.getParameter("medicalAdvice"), readOrders(request), version, audit)
                : service.finalizeEncounter(doctorId.intValue(), appointmentId.intValue(),
                request.getParameter("diagnosis"), request.getParameter("treatmentPlan"),
                request.getParameter("medicalAdvice"), readOrders(request), version, audit);
        writeResult(response, result);
    }

    private static List<MedicalOrder> readOrders(HttpServletRequest request) {
        String[] items = request.getParameterValues("orderItem");
        String[] dosages = request.getParameterValues("orderDosage");
        String[] frequencies = request.getParameterValues("orderFrequency");
        String[] quantities = request.getParameterValues("orderQuantity");
        String[] instructions = request.getParameterValues("orderInstruction");
        int count = items == null ? 0 : items.length;
        List<MedicalOrder> orders = new ArrayList<MedicalOrder>();
        for (int index = 0; index < count; index++) {
            MedicalOrder order = new MedicalOrder();
            order.setItemName(valueAt(items, index));
            order.setDosage(valueAt(dosages, index));
            order.setFrequency(valueAt(frequencies, index));
            order.setInstructionText(valueAt(instructions, index));
            String quantity = valueAt(quantities, index);
            if (quantity != null && quantity.trim().length() > 0) {
                try { order.setQuantity(Integer.valueOf(quantity.trim())); }
                catch (NumberFormatException exception) { order.setQuantity(Integer.valueOf(0)); }
            }
            orders.add(order);
        }
        return orders;
    }

    private static String valueAt(String[] values, int index) { return values != null && index < values.length ? values[index] : null; }
    private static void writeRecord(HttpServletResponse response, MedicalRecord record) throws IOException {
        if (record == null) { writeNotFound(response); return; }
        AppointmentServletSupport.writeJson(response, record);
    }
    private static void writeNotFound(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("success", Boolean.FALSE);
        body.put("code", "NOT_FOUND");
        body.put("message", "未找到可访问的就诊记录。");
        AppointmentServletSupport.writeJson(response, body);
    }
    private static void writeResult(HttpServletResponse response, MedicalRecordOperationResult result) throws IOException {
        if (!result.isSuccessful()) {
            String code = result.getCode();
            response.setStatus("FORBIDDEN".equals(code) ? HttpServletResponse.SC_FORBIDDEN
                    : ("INVALID_STATE".equals(code) || "DUPLICATE_RECORD".equals(code) || "VERSION_CONFLICT".equals(code) ? HttpServletResponse.SC_CONFLICT
                    : ("FINALIZE_FAILED".equals(code) || "RECORD_SAVE_FAILED".equals(code) || "ORDER_SAVE_FAILED".equals(code) ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_BAD_REQUEST)));
        }
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("success", Boolean.valueOf(result.isSuccessful()));
        body.put("code", result.getCode());
        body.put("message", result.getMessage());
        if (result.isSuccessful()) {
            body.put("medicalRecordId", result.getMedicalRecordId());
            body.put("recordStatus", result.getRecordStatus());
            body.put("version", result.getVersion());
        }
        AppointmentServletSupport.writeJson(response, body);
    }
}
