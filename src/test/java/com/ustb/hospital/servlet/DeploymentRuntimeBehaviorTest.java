package com.ustb.hospital.servlet;

import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.function.Supplier;
import static org.junit.Assert.*;

/** Exercises persistent bytes and HTTP status/body, rather than searching source text. */
public class DeploymentRuntimeBehaviorTest {
    @Test public void avatarBytesSurviveNewStorageInstanceAndRejectTraversal() throws Exception {
        Path root = Files.createTempDirectory("hospital-uploads-");
        Object first = storage(root);
        Path avatar = (Path) first.getClass().getMethod("resolve", String.class).invoke(first, "patient-1-abc.png");
        Files.write(avatar, new byte[]{1, 2, 3});
        Object second = storage(root);
        Path same = (Path) second.getClass().getMethod("resolve", String.class).invoke(second, "patient-1-abc.png");
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(same));
        assertEquals(root.resolve("avatars").resolve("patient-1-abc.png"), same);
        for (String bad : new String[]{"../secret.png", "..\\secret.png", "/secret.png", "%2e%2e.png", "x.jsp", "x.png/extra"}) {
            try { first.getClass().getMethod("resolve", String.class).invoke(first, bad); fail("Accepted unsafe name: " + bad); }
            catch (InvocationTargetException expected) { assertTrue(expected.getCause() instanceof IllegalArgumentException); }
        }
        Files.delete(same); Files.delete(same.getParent()); Files.delete(root);
    }

    @Test public void avatarReadRouteReturnsBytesAndRejectsEscapingNames() throws Exception {
        Path root = Files.createTempDirectory("hospital-avatars-");
        Object store = storage(root);
        Path file = (Path) store.getClass().getMethod("resolve", String.class).invoke(store, "patient-1-abc.png");
        byte[] bytes = new byte[]{(byte)0x89, 80, 78, 71}; Files.write(file, bytes);
        Class<?> type = required("com.ustb.hospital.servlet.AvatarFileServlet");
        Constructor<?> constructor = type.getDeclaredConstructor(store.getClass()); constructor.setAccessible(true);
        Object servlet = constructor.newInstance(store);
        Reply good = invokeGet(type, servlet, "/patient-1-abc.png");
        assertEquals(200, good.status); assertArrayEquals(bytes, good.bytes.toByteArray()); assertEquals("image/png", good.contentType);
        assertEquals(404, invokeGet(type, servlet, "/../secret.png").status);
        assertEquals(404, invokeGet(type, servlet, "/missing.png").status);
        Files.delete(file); Files.delete(file.getParent()); Files.delete(root);
    }

    @Test public void readinessQueriesDatabaseAndReturnsOnlySanitizedStatus() throws Exception {
        Class<?> type = required("com.ustb.hospital.servlet.ReadinessServlet");
        Constructor<?> constructor = type.getDeclaredConstructor(Supplier.class); constructor.setAccessible(true);
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:readiness;DB_CLOSE_DELAY=-1")) {
            Supplier<SqlSession> ready = () -> (SqlSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{SqlSession.class},
                    (p, m, a) -> "getConnection".equals(m.getName()) ? connection : null);
            Reply good = invokeGet(type, constructor.newInstance(ready), null);
            assertEquals(200, good.status); assertEquals("{\"ready\":true}", good.body());
        }
        Supplier<SqlSession> down = () -> { throw new IllegalStateException("secret jdbc:oceanbase://internal password=private"); };
        Reply bad = invokeGet(type, constructor.newInstance(down), null);
        assertEquals(503, bad.status); assertEquals("{\"ready\":false}", bad.body());
        assertFalse(bad.body().contains("secret"));
    }

    private Object storage(Path root) throws Exception { return required("com.ustb.hospital.utils.AvatarStorage").getConstructor(Path.class).newInstance(root); }
    private Class<?> required(String name) throws Exception {
        try { return Class.forName(name); } catch (ClassNotFoundException missing) { fail("Missing deployment behavior: " + name); return null; }
    }
    private Reply invokeGet(Class<?> type, Object servlet, String path) throws Exception {
        Reply result = new Reply();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletRequest.class},
                (p,m,a) -> "getPathInfo".equals(m.getName()) ? path : null);
        Method get = type.getDeclaredMethod("doGet", HttpServletRequest.class, HttpServletResponse.class); get.setAccessible(true);
        get.invoke(servlet, request, result.response()); result.writer.flush(); return result;
    }
    private static class Reply {
        int status = 200; String contentType; ByteArrayOutputStream bytes = new ByteArrayOutputStream(); PrintWriter writer = new PrintWriter(bytes);
        String body() throws Exception { writer.flush(); return bytes.toString("UTF-8"); }
        HttpServletResponse response() {
            return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletResponse.class}, (p,m,a) -> {
                if ("setStatus".equals(m.getName()) || "sendError".equals(m.getName())) { status = (Integer)a[0]; return null; }
                if ("setContentType".equals(m.getName())) { contentType = (String)a[0]; return null; }
                if ("getWriter".equals(m.getName())) return writer;
                if ("getOutputStream".equals(m.getName())) return new ServletOutputStream() {
                    public boolean isReady() { return true; } public void setWriteListener(WriteListener listener) {}
                    public void write(int b) { bytes.write(b); }
                };
                return null;
            });
        }
    }
}
