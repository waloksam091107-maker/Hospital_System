package com.ustb.hospital.deploy;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Ensures the container topology starts this application against OceanBase rather than MySQL. */
public class OceanBaseComposeContractTest {
    @Test
    public void definesAnOceanBaseBackedTomcatDeploymentWithoutCommittedSecrets() throws Exception {
        Path composePath = Paths.get("docker-compose.oceanbase.yml");
        Path dockerfilePath = Paths.get("Dockerfile");
        Path environmentExamplePath = Paths.get(".env.oceanbase.example");
        assertTrue("OceanBase Compose file is required.", Files.isRegularFile(composePath));
        assertTrue("Application Dockerfile is required.", Files.isRegularFile(dockerfilePath));
        assertTrue("Secret-free environment example is required.", Files.isRegularFile(environmentExamplePath));

        String compose = new String(Files.readAllBytes(composePath), StandardCharsets.UTF_8).toLowerCase();
        String dockerfile = new String(Files.readAllBytes(dockerfilePath), StandardCharsets.UTF_8).toLowerCase();
        String environmentExample = new String(Files.readAllBytes(environmentExamplePath), StandardCharsets.UTF_8).toLowerCase();

        assertTrue(compose.contains("oceanbase:"));
        assertTrue(compose.contains("app:"));
        assertTrue(compose.contains("oceanbase/oceanbase-ce"));
        assertTrue(compose.contains("oceanbase-data:"));
        assertTrue(compose.contains("jdbc:oceanbase://oceanbase:2881/hospt"));
        assertTrue(compose.contains("com.oceanbase.jdbc.driver"));
        assertTrue("The OceanBase image exposes obclient rather than a mysql binary, so its health check must use obclient.",
                compose.contains("obclient -h127.0.0.1 -p2881 -uroot@test"));
        assertFalse(compose.contains("mysql:"));
        assertFalse(compose.contains("mysql:8"));

        assertTrue(dockerfile.contains("tomcat:9"));
        assertTrue(dockerfile.contains("ustbhospital.war"));
        assertTrue(dockerfile.contains("webapps/ustbhospital.war"));
        assertTrue(environmentExample.contains("ob_root_password=<replace-locally>"));
        assertTrue(environmentExample.contains("hospital_app_password=<replace-locally>"));
        assertFalse(environmentExample.contains("password=123"));
    }
}
