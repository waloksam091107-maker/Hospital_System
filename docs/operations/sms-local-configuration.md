# 短信服务本地配置

短信密钥不应写入源码或提交至 Git。请在本机的项目根目录创建被忽略的 `.hospital-local/sms.properties`：

```properties
accessKeyId=填写本机的阿里云 AccessKey ID
accessKeySecret=填写本机的阿里云 AccessKey Secret
```

程序默认从当前工作目录的 `.hospital-local/sms.properties` 读取。Tomcat 的工作目录可能不是项目目录，部署时请设置 JVM 参数 `-Dhospital.sms.config=配置文件的绝对路径`，或为启动 Tomcat 的进程设置环境变量 `HOSPITAL_SMS_CONFIG` 为配置文件的绝对路径。JVM 参数优先于环境变量；没有设置时使用默认路径。

配置缺失或内容为空时，短信发送会失败并提示配置不完整，不会使用内置密钥。请为文件设置适当的操作系统访问权限，不要上传它、截图展示内容，或将其打进 WAR 包。此前出现在 Git 历史中的密钥应及时在阿里云轮换或停用。
