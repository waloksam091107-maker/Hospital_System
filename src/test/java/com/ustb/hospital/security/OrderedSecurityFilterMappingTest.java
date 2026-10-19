package com.ustb.hospital.security;

import org.junit.Test;
import org.w3c.dom.*;
import javax.servlet.annotation.WebFilter;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import static org.junit.Assert.*;

public class OrderedSecurityFilterMappingTest {
    @Test public void descriptorOrdersFiltersAndCoversSecurityRoutesExactlyOnce() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document document = factory.newDocumentBuilder().parse(new File("src/main/webapp/WEB-INF/web.xml"));
        Map<String, String> classes = new HashMap<>();
        NodeList declarations = document.getElementsByTagName("filter");
        for (int i = 0; i < declarations.getLength(); i++) {
            Element element = (Element) declarations.item(i);
            classes.put(value(element, "filter-name"), value(element, "filter-class"));
        }
        List<String> order = new ArrayList<>();
        Map<String, List<String>> routes = new HashMap<>();
        NodeList mappings = document.getElementsByTagName("filter-mapping");
        for (int i = 0; i < mappings.getLength(); i++) {
            Element element = (Element) mappings.item(i);
            String name = value(element, "filter-name");
            if (!routes.containsKey(name)) { routes.put(name, new ArrayList<>()); order.add(name); }
            routes.get(name).add(value(element, "url-pattern"));
        }
        assertEquals(Arrays.asList("encoding", "audit", "authentication", "csrf"), order);
        for (String name : order) {
            Class<?> type = Class.forName(classes.get(name));
            assertNull("descriptor filters must not also be registered by annotation", type.getAnnotation(WebFilter.class));
            assertEquals(new HashSet<>(routes.get(name)).size(), routes.get(name).size());
        }
        assertTrue(routes.get("authentication").containsAll(Arrays.asList("/appointments/*", "/medical-records/*", "/schedules/*", "/password/reset", "/audit-logs/*")));
        assertTrue(routes.get("csrf").containsAll(Arrays.asList("/appointments/book", "/medical-records/draft", "/medical-records/finalize", "/schedules/*", "/password/reset")));
        assertTrue(routes.get("audit").containsAll(Arrays.asList("/appointments/*", "/medical-records/*", "/schedules/*", "/password/reset", "/audit-logs/*")));
    }
    private static String value(Element parent, String tag) { return parent.getElementsByTagName(tag).item(0).getTextContent(); }
}
