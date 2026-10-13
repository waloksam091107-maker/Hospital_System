package com.ustb.hospital.servlet;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/** Prevents a deployment-breaking collision between two servlet URL mappings. */
public class ServletMappingUniquenessTest {
    private static final Pattern SINGLE_MAPPING = Pattern.compile("@WebServlet\\s*\\(\\s*\"([^\"]+)\"\\s*\\)");
    private static final Pattern ARRAY_MAPPING = Pattern.compile("@WebServlet\\s*\\(\\s*\\{([^}]*)\\}\\s*\\)");
    private static final Pattern QUOTED_VALUE = Pattern.compile("\"([^\"]+)\"");

    @Test
    public void servletUrlPatternsAreUnique() throws IOException {
        Map<String, List<String>> ownersByUrl = new LinkedHashMap<String, List<String>>();
        Path sourceRoot = Paths.get("src", "main", "java");

        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> addMappings(path, ownersByUrl));
        }

        List<String> collisions = new ArrayList<String>();
        for (Map.Entry<String, List<String>> entry : ownersByUrl.entrySet()) {
            if (entry.getValue().size() > 1) {
                collisions.add(entry.getKey() + " -> " + entry.getValue());
            }
        }

        assertTrue("每个 Servlet URL 映射必须唯一，当前冲突：" + collisions, collisions.isEmpty());
    }

    private void addMappings(Path sourceFile, Map<String, List<String>> ownersByUrl) {
        try {
            String source = new String(Files.readAllBytes(sourceFile), StandardCharsets.UTF_8);
            String owner = sourceFile.getFileName().toString();
            addSingleMappings(source, owner, ownersByUrl);
            addArrayMappings(source, owner, ownersByUrl);
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取 Servlet 源文件：" + sourceFile, exception);
        }
    }

    private void addSingleMappings(String source, String owner, Map<String, List<String>> ownersByUrl) {
        Matcher matcher = SINGLE_MAPPING.matcher(source);
        while (matcher.find()) {
            addOwner(matcher.group(1), owner, ownersByUrl);
        }
    }

    private void addArrayMappings(String source, String owner, Map<String, List<String>> ownersByUrl) {
        Matcher arrayMatcher = ARRAY_MAPPING.matcher(source);
        while (arrayMatcher.find()) {
            Matcher valueMatcher = QUOTED_VALUE.matcher(arrayMatcher.group(1));
            while (valueMatcher.find()) {
                addOwner(valueMatcher.group(1), owner, ownersByUrl);
            }
        }
    }

    private void addOwner(String url, String owner, Map<String, List<String>> ownersByUrl) {
        String normalizedUrl = url.toLowerCase(Locale.ROOT);
        List<String> owners = ownersByUrl.get(normalizedUrl);
        if (owners == null) {
            owners = new ArrayList<String>();
            ownersByUrl.put(normalizedUrl, owners);
        }
        owners.add(owner);
    }
}
