package com.ustb.hospital.servlet;

import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.function.Supplier;

/** Readiness is public and deliberately reveals no connection details or business data. */
@WebServlet(value = "/health/ready", loadOnStartup = 1)
public class ReadinessServlet extends HttpServlet {
    private final Supplier<SqlSession> sessions;
    public ReadinessServlet() { this(MybatisUtil::getSqlSession); }
    ReadinessServlet(Supplier<SqlSession> sessions) { this.sessions = sessions; }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean ready = false;
        try (SqlSession session = sessions.get(); Statement query = session.getConnection().createStatement()) {
            query.setQueryTimeout(3);
            try (ResultSet result = query.executeQuery("SELECT 1")) { ready = result.next() && result.getInt(1) == 1; }
        } catch (Exception unavailable) { /* Never serialize credentials or SQL exceptions into a health response. */ }
        response.setStatus(ready ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(ready ? "{\"ready\":true}" : "{\"ready\":false}");
    }
}
