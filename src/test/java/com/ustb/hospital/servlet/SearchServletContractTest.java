package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SearchServletContractTest {
    @Test
    public void missingKeywordReturnsUtf8BadRequestInsteadOfQueryingDepartments() throws Exception {
        Response response = new Response();

        new SearchServlet().doGet(request(null), response.proxy());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
        assertEquals("application/json;charset=UTF-8", response.contentType);
        JsonNode body = new ObjectMapper().readTree(response.body.toString());
        assertFalse(body.get("success").asBoolean());
        assertEquals("INVALID_REQUEST", body.get("code").asText());
    }

    @Test
    public void blankKeywordReturnsUtf8BadRequestInsteadOfQueryingDepartments() throws Exception {
        Response response = new Response();

        new SearchServlet().doGet(request("   "), response.proxy());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
        assertEquals("application/json;charset=UTF-8", response.contentType);
    }

    private static HttpServletRequest request(final String keyword) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                SearchServletContractTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, arguments) -> {
                    if ("getParameter".equals(method.getName()) && "keyword".equals(arguments[0])) return keyword;
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return 0;
    }

    private static final class Response {
        private int status = HttpServletResponse.SC_OK;
        private String contentType;
        private final StringWriter body = new StringWriter();

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    SearchServletContractTest.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, method, arguments) -> {
                        if ("setStatus".equals(method.getName())) status = ((Integer) arguments[0]).intValue();
                        if ("setContentType".equals(method.getName())) contentType = (String) arguments[0];
                        if ("getWriter".equals(method.getName())) return new PrintWriter(body, true);
                        return defaultValue(method.getReturnType());
                    });
        }
    }
}
