package com.ustb.hospital.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;


public class SMSUtils {
    public static String send(String tel, String setsignName,String templatecode,String content) throws IOException, ServerException, ClientException {
        //设置超时时间-可自行调整
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");
        //初始化ascClient需要的几个参数
        final String product = "Dysmsapi";//短信API产品名称（短信产品名固定，无需修改）
        final String domain = "dysmsapi.aliyuncs.com";//短信API产品域名（接口地址固定，无需修改）
        //替换成你的AK
        String[] credentials = loadCredentials(credentialsPath());
        final String accessKeyId = credentials[0];
        final String accessKeySecret = credentials[1];
        //初始化ascClient,暂时不支持多region（请勿修改）
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId,
                accessKeySecret);
        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", product, domain);
        IAcsClient acsClient = new DefaultAcsClient(profile);
        //组装请求对象
        SendSmsRequest request = new SendSmsRequest();
        //使用post提交
        request.setMethod(MethodType.POST);
        //必填:待发送手机号。支持以逗号分隔的形式进行批量调用，批量上限为1000个手机号码,批量调用相对于单条调用及时性稍有延迟,验证码类型的短信推荐使用单条调用的方式；发送国际/港澳台消息时，接收号码格式为国际区号+号码，如“85200000000”
        request.setPhoneNumbers(tel);
        //必填:短信签名-可在短信控制台中找到
        request.setSignName(setsignName);
        //必填:短信模板-可在短信控制台中找到，发送国际/港澳台消息时，请使用国际/港澳台短信模版
        request.setTemplateCode(templatecode);
        request.setTemplateParam(content);
        request.setOutId("yourOutId");
        //请求失败这里会抛ClientException异常
        SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);
        System.out.print(request.getTemplateParam());
        System.out.print(sendSmsResponse.getCode()); //返回码，可得知发送状态
        return sendSmsResponse.getCode();
    }
    private static Path credentialsPath() {
        String path = System.getProperty("hospital.sms.config");
        if (path == null || path.trim().isEmpty()) path = System.getenv("HOSPITAL_SMS_CONFIG");
        return path == null || path.trim().isEmpty()
                ? Paths.get(".hospital-local", "sms.properties")
                : Paths.get(path.trim());
    }

    static String[] loadCredentials(Path path) {
        Properties values = new Properties();
        try (InputStream stream = Files.newInputStream(path)) {
            values.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException("无法读取短信本地配置，请设置 hospital.sms.config 或 HOSPITAL_SMS_CONFIG。", e);
        }
        String id = values.getProperty("accessKeyId", "").trim();
        String secret = values.getProperty("accessKeySecret", "").trim();
        if (id.isEmpty() || secret.isEmpty()) {
            throw new IllegalStateException("短信本地配置缺少 AccessKey ID 或 AccessKey Secret。");
        }
        return new String[] {id, secret};
    }

    public static void main(String[] args) throws Exception {

    String randomCode = String.valueOf((int) (Math.random() * 9000 + 1000)); //4位数验证码
    String content = "{\"code\":\""+randomCode+"\"}";
    SMSUtils.send("17866668888","医通","SMS_469075527",content); //发送短信
    }

}
