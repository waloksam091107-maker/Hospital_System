package com.ustb.hospital.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/** Standalone path contract runnable with only the JDK when Maven is unavailable. */
public class EnglishPagePathContractTest {
    private static final String[] EXPECTED_PAGE_PATHS = {
            "src/main/webapp/appointment-records.html",
            "src/main/webapp/change-password.html",
            "src/main/webapp/menu/edit.html",
            "src/main/webapp/menu/edit.jsp",
            "src/main/webapp/hospro/appointment-records.html",
            "src/main/webapp/hospro/change-password.html",
            "src/main/webapp/hospro/order-edit.html",
            "src/main/webapp/hospro/forgot-password.html",
            "src/main/webapp/hospro/doctor-profile.html",
            "src/main/webapp/hospro/admin/profile-edit.jsp"
    };

    public static void main(String[] args) {
        verifiesEnglishPagesExist();
        rejectsLegacyPinyinHttpPaths();
        rejectsOpaqueRuntimeIdentifiers();
        rejectsUnusedNumberedCompatibilityPages();
        rejectsUnusedStandaloneSources();
    }

    private static void verifiesEnglishPagesExist() {
        for (String expectedPagePath : EXPECTED_PAGE_PATHS) {
            Path page = Paths.get(expectedPagePath);
            if (!Files.isRegularFile(page)) {
                throw new AssertionError("Expected English page path is missing: " + expectedPagePath);
            }
        }
        if (!Files.isDirectory(Paths.get("src/main/webapp/hospro/archive"))) {
            throw new AssertionError("Expected English archive directory is missing.");
        }
    }

    private static void rejectsLegacyPinyinHttpPaths() {
        String[] legacyHttpPaths = {"/xiugaiOrder", "/xiugaipassword", "/xiugaipasswordzhen"};
        try (Stream<Path> files = Files.walk(Paths.get("src/main"))) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java")
                            || path.toString().endsWith(".html")
                            || path.toString().endsWith(".jsp"))
                    .forEach(path -> assertFileHasNoLegacyHttpPath(path, legacyHttpPaths));
        } catch (java.io.IOException exception) {
            throw new AssertionError("Could not inspect application paths.", exception);
        }
    }

    private static void rejectsOpaqueRuntimeIdentifiers() {
        String[] opaqueIdentifiers = {
                "selectAll111", "getadminmes", "get111adminmes", "checkPid111", "doctoEdit111",
                "docEdit111", "save111", "xiugai111", "update111", "doc/list111"
        };
        assertApplicationSourcesDoNotContain(opaqueIdentifiers);
    }

    private static void rejectsUnusedNumberedCompatibilityPages() {
        String[] obsoletePages = {
                "src/main/webapp/dept/list1.jsp",
                "src/main/webapp/doc/list2.html",
                "src/main/webapp/hospro/edit1.jsp",
                "src/main/webapp/hospro/patient/edit1.jsp",
                "src/main/webapp/hospro/patient/edit2.jsp",
                "src/main/webapp/hospro/doc/edit1.jsp",
                "src/main/webapp/hospro/doc/edit11.jsp",
                "src/main/webapp/hospro/doctors-1.html",
                "src/main/webapp/hospro/doctors-1.jsp",
                "src/main/webapp/hospro/archive/register-page(1).html"
        };
        for (String obsoletePage : obsoletePages) {
            if (Files.exists(Paths.get(obsoletePage))) {
                throw new AssertionError("Unused numbered compatibility page remains: " + obsoletePage);
            }
        }
    }

    private static void rejectsUnusedStandaloneSources() {
        String[] obsoleteSources = {
                "src/main/java/com/ustb/hospital/entity/Main.java"
        };
        for (String obsoleteSource : obsoleteSources) {
            if (Files.exists(Paths.get(obsoleteSource))) {
                throw new AssertionError("Unused standalone source remains: " + obsoleteSource);
            }
        }
    }

    private static void assertApplicationSourcesDoNotContain(String[] prohibitedTokens) {
        try (Stream<Path> files = Files.walk(Paths.get("src/main"))) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java")
                            || path.toString().endsWith(".xml")
                            || path.toString().endsWith(".html")
                            || path.toString().endsWith(".jsp"))
                    .forEach(path -> assertFileDoesNotContain(path, prohibitedTokens));
        } catch (java.io.IOException exception) {
            throw new AssertionError("Could not inspect application sources.", exception);
        }
    }

    private static void assertFileHasNoLegacyHttpPath(Path path, String[] legacyHttpPaths) {
        assertFileDoesNotContain(path, legacyHttpPaths);
    }

    private static void assertFileDoesNotContain(Path path, String[] prohibitedTokens) {
        try {
            String source = new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
            for (String prohibitedToken : prohibitedTokens) {
                if (source.contains(prohibitedToken)) {
                    throw new AssertionError("Prohibited legacy token remains in " + path + ": " + prohibitedToken);
                }
            }
        } catch (java.io.IOException exception) {
            throw new AssertionError("Could not read " + path, exception);
        }
    }
}
