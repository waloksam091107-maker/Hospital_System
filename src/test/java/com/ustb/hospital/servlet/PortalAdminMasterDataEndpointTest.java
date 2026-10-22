package com.ustb.hospital.servlet;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.assertTrue;

/** The administrative CRUD surface must be separate from all legacy write endpoints. */
public class PortalAdminMasterDataEndpointTest {
    @Test public void eachMasterDataRouteUsesTheNewAdminPortalNamespace() throws Exception {
        assertRoute("PortalAdminDoctorsServlet", "/portal/admin/doctors");
        assertRoute("PortalAdminPatientsServlet", "/portal/admin/patients");
        assertRoute("PortalAdminDepartmentsServlet", "/portal/admin/departments");
        assertRoute("PortalAdminProfessionalTitlesServlet", "/portal/admin/professional-titles");
    }

    @Test public void patientMaintenanceSupportsUpdateAndReferenceConflictsUse409() throws Exception {
        String patients = source("PortalAdminPatientsServlet");
        String support = source("PortalServletSupport");
        assertTrue(patients.contains("\"update\".equals(action)"));
        assertTrue(patients.contains("service.updatePatient"));
        assertTrue(support.contains("\"HAS_REFERENCES\".equals(code)"));
        assertTrue(support.contains("SC_CONFLICT"));
    }
    private void assertRoute(String className, String route) throws Exception {
        String source = source(className);
        assertTrue(source.contains("@WebServlet(\"" + route + "\")"));
        assertTrue(source.contains("SessionIdentity.ROLE_ADMIN"));
    }
    private String source(String className) throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/ustb/hospital/servlet/" + className + ".java")), StandardCharsets.UTF_8);
    }
}
