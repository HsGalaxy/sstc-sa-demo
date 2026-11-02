package com.sstc.platform.common.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Health and meta endpoints. They are public so the frontend can render the sign-in page and
 * the system status without a session.
 */
@RestController
@RequestMapping("/api")
public class MetaController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("status", "UP");
        map.put("application", "STC Platform");
        map.put("time", LocalDateTime.now().toString());
        return map;
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("name", "STC Platform");
        map.put("subtitle", "Small Team Collaboration - Smart Working Management App");
        map.put("version", "1.0.0");
        map.put("caseStudy", "Smart Working Management App (small teams)");
        map.put("actors", java.util.Arrays.asList("Team Member", "Senior Manager", "Overall Lead"));

        List<Map<String, Object>> modules = new ArrayList<Map<String, Object>>();
        modules.add(module("S01", "Identity, Role & Access Management",
                "Accounts, roles, permission policies, access audit",
                java.util.Arrays.asList("US01 accounts", "US02 roles", "US03 policies", "US04 access audit")));
        modules.add(module("S02", "Task Dashboard",
                "Kanban tasks, file tracker, OKR and performance indicators",
                java.util.Arrays.asList("US01 file query", "US02 upload", "US03 task CRUD",
                        "US04 operation log", "US05 display panel", "US06 performance score")));
        modules.add(module("S03", "Team Collaboration",
                "Group chat, @mentions, meetings with AI summary, smart digest",
                java.util.Arrays.asList("US01 meetings", "US02 group messages", "US03 mentions",
                        "US04 notifications", "US05 AI summary", "US06 smart digest")));
        modules.add(module("S04", "Toolbox",
                "Word/PPT editing, Git, containers and project file management",
                java.util.Arrays.asList("US01 documents", "US02 presentations", "US03 version control",
                        "US04 project files", "US05 container runtime")));
        map.put("modules", modules);
        map.put("stack", java.util.Arrays.asList("Java 8", "Spring Boot 2.7", "Spring Data JPA",
                "H2 database", "Vite + React frontend"));
        return map;
    }

    private Map<String, Object> module(String id, String name, String description, List<String> stories) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("name", name);
        map.put("description", description);
        map.put("userStories", stories);
        return map;
    }
}
