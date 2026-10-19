package com.ustb.hospital.security;
import org.junit.Test;
import javax.servlet.annotation.WebFilter;
import java.util.Arrays;
import static org.junit.Assert.*;

public class LegacyWritesDisabledTest {
    @Test public void unsafeLegacyMutationsCannotBypassAuthenticationAndAudit() throws Exception {
        Class<?> filter = Class.forName("com.ustb.hospital.filter.LegacyUnsafeWritesDisabledFilter");
        String[] paths = filter.getAnnotation(WebFilter.class).urlPatterns();
        for (String path : new String[]{"/accounts/register", "/appointments/delete", "/portal/doctors/create", "/portal/doctors/profile", "/departments/delete", "/departments/parent-check", "/patients/profile", "/news/add"})
            assertTrue("missing unsafe path " + path, Arrays.asList(paths).contains(path));
    }
}
