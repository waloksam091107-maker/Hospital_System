package com.ustb.hospital.deploy;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Prevents migration tooling from gaining a write path to the MySQL source database. */
public class OceanBaseMigrationScriptContractTest {
    @Test
    public void migrationToolsParseInWindowsPowerShellFive() throws Exception {
        assertWindowsPowerShellCanParse(Paths.get("scripts", "oceanbase", "migrate-from-mysql.ps1"));
        assertWindowsPowerShellCanParse(Paths.get("scripts", "oceanbase", "verify-migration.ps1"));
    }

    @Test
    public void exportsMySqlReadOnlyAndVerifiesCountsAndClinicalRelationships() throws Exception {
        Path migrationPath = Paths.get("scripts", "oceanbase", "migrate-from-mysql.ps1");
        Path verificationPath = Paths.get("scripts", "oceanbase", "verify-migration.ps1");
        assertTrue("Read-only migration script is required.", Files.isRegularFile(migrationPath));
        assertTrue("Migration verification script is required.", Files.isRegularFile(verificationPath));

        String migration = new String(Files.readAllBytes(migrationPath), StandardCharsets.UTF_8).toLowerCase();
        String verification = new String(Files.readAllBytes(verificationPath), StandardCharsets.UTF_8).toLowerCase();

        assertTrue(migration.contains("read-host -assecurestring"));
        assertTrue(migration.contains("--single-transaction"));
        assertTrue(migration.contains("--no-create-info"));
        assertTrue(migration.contains("show tables"));
        assertTrue("Windows PowerShell 5.1 defaults native-command pipeline output to US-ASCII. "
                        + "The migration script must force UTF-8 before streaming SQL dumps into obclient.",
                migration.contains("$outputencoding = new-object system.text.utf8encoding($false)"));
        assertFalse("Windows PowerShell 5.1 strips the embedded quotes passed to docker exec; OceanBase SQL must be sent through standard input instead of obclient -e.",
                migration.contains("obclient -h127.0.0.1 -p2881 -uroot@test -n -b -e"));
        assertFalse("Verification queries must use the same PowerShell-safe standard-input path.",
                verification.contains("obclient -h127.0.0.1 -p2881 -uroot@test -n -b -e"));
        assertTrue("A one-row MySQL aggregate must remain a one-element row array, rather than a String whose [0] is only its first character.",
                migration.contains("$summary = @(invoke-mysqlread"));
        assertTrue("Source aggregate results must preserve row cardinality during verification.",
                verification.contains("$source = (@(invoke-mysqlread"));
        assertTrue("Target aggregate results must preserve row cardinality during verification.",
                verification.contains("$target = (@(invoke-oceanbaseread"));
        assertFalse(migration.contains("drop database"));
        assertFalse(migration.contains("truncate"));
        assertFalse(migration.contains("delete from"));
        assertFalse(migration.contains("alter table"));

        assertTrue(verification.contains("count(*)"));
        assertTrue(verification.contains("appointments"));
        assertTrue(verification.contains("schedule_slots"));
        assertTrue(verification.contains("medical_records"));
        assertTrue(verification.contains("medical_orders"));
        assertTrue(verification.contains("audit_logs"));
        assertTrue(verification.contains("min("));
        assertTrue(verification.contains("max("));
    }

    private void assertWindowsPowerShellCanParse(Path scriptPath) throws Exception {
        String parserCommand = "$tokens = $null; $errors = $null; "
                + "[System.Management.Automation.Language.Parser]::ParseFile((Resolve-Path '" + scriptPath.toString().replace("\\", "\\\\") + "'), [ref]$tokens, [ref]$errors) | Out-Null; "
                + "if ($errors.Count -gt 0) { $errors | ForEach-Object { $_.Message }; exit 1 }";
        Process process = new ProcessBuilder(
                System.getenv("WINDIR") + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe",
                "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", parserCommand)
                .redirectErrorStream(true)
                .start();
        String output = readFully(process.getInputStream());
        int exitCode = process.waitFor();
        assertTrue("Windows PowerShell 5.1 must parse " + scriptPath + ": " + output, exitCode == 0);
    }

    private String readFully(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
