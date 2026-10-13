package com.ustb.hospital.servlet;

import com.ustb.hospital.service.DoctorsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.UUID;

@MultipartConfig
@WebServlet("/updateAvatar")
public class DoctorUpdateAvatarServlet extends HttpServlet {
    private static final String UPLOAD_DIRECTORY = "D:\\hos_images";
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置请求和响应的字符编码
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");
// 获取请求参数
        String doctorIdStr = req.getParameter("doctorId");
Part avatarPart = req.getPart("avatar");
if (doctorIdStr == null || doctorIdStr.isEmpty() || avatarPart == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("缺少参数：patientId 或 patientAvatar");
            return;
        }

        // 将文件保存到指定目录，并生成唯一的文件名
        String avatarFileName = generateUniqueFileName(avatarPart);
        String uploadPath = UPLOAD_DIRECTORY + "\\" + avatarFileName;
avatarPart.write(uploadPath);

        // 调用服务层更新患者头像信息
        DoctorsServiceImpl service = new DoctorsServiceImpl();
        try {
            int patientId = Integer.parseInt(doctorIdStr);
            service.updateAvatar(patientId, avatarFileName);

            // 更新成功后重定向到个人主页
            resp.sendRedirect("/hospro/doctor-profile.html");
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid patientId format");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("更新头像失败：" + e.getMessage());
        }
    }

    // 生成唯一的文件名，防止文件名冲突
    private String generateUniqueFileName(Part part) {
        String submittedFileName = part.getSubmittedFileName();
        String extension = submittedFileName.substring(submittedFileName.lastIndexOf('.'));
        String uniqueFileName = UUID.randomUUID().toString().replaceAll("-", "") + extension;
        return uniqueFileName;
    }
}
