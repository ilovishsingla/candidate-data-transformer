package com.eightfold.normalizer;

import java.util.HashMap;
import java.util.Map;

public class SkillNormalizer {
    private static final Map<String, String> SKILL_MAP = new HashMap<>();

    static {
        // Programming Languages
        SKILL_MAP.put("js", "JavaScript");
        SKILL_MAP.put("javascript", "JavaScript");
        SKILL_MAP.put("java", "Java");
        SKILL_MAP.put("py", "Python");
        SKILL_MAP.put("python", "Python");
        SKILL_MAP.put("cpp", "C++");
        SKILL_MAP.put("c++", "C++");
        SKILL_MAP.put("cs", "C#");
        SKILL_MAP.put("c#", "C#");
        SKILL_MAP.put("ts", "TypeScript");
        SKILL_MAP.put("typescript", "TypeScript");
        SKILL_MAP.put("rb", "Ruby");
        SKILL_MAP.put("ruby", "Ruby");
        SKILL_MAP.put("golang", "Go");
        SKILL_MAP.put("go", "Go");

        // Web Technologies
        SKILL_MAP.put("html5", "HTML");
        SKILL_MAP.put("html", "HTML");
        SKILL_MAP.put("css3", "CSS");
        SKILL_MAP.put("css", "CSS");
        SKILL_MAP.put("reactjs", "React");
        SKILL_MAP.put("react.js", "React");
        SKILL_MAP.put("react", "React");
        SKILL_MAP.put("vue", "Vue.js");
        SKILL_MAP.put("vuejs", "Vue.js");
        SKILL_MAP.put("angular", "Angular");

        // Cloud & DevOps
        SKILL_MAP.put("aws", "Amazon Web Services");
        SKILL_MAP.put("amazon web services", "Amazon Web Services");
        SKILL_MAP.put("gcp", "Google Cloud Platform");
        SKILL_MAP.put("google cloud", "Google Cloud Platform");
        SKILL_MAP.put("k8s", "Kubernetes");
        SKILL_MAP.put("kubernetes", "Kubernetes");
        SKILL_MAP.put("docker", "Docker");
        SKILL_MAP.put("jenkins", "Jenkins");

        // Databases & Others
        SKILL_MAP.put("postgres", "PostgreSQL");
        SKILL_MAP.put("postgresql", "PostgreSQL");
        SKILL_MAP.put("mongo", "MongoDB");
        SKILL_MAP.put("mongodb", "MongoDB");
        SKILL_MAP.put("mysql", "MySQL");
        SKILL_MAP.put("git", "Git");
    }

    /**
     * Normalizes a skill name to its canonical version.
     *
     * @param rawSkill The raw skill name.
     * @return Canonical skill name, or the cleaned raw input if not found in mapping.
     */
    public static String normalize(String rawSkill) {
        if (rawSkill == null || rawSkill.strip().isEmpty()) {
            return null;
        }

        String cleaned = rawSkill.strip();
        String lookupKey = cleaned.toLowerCase();

        if (SKILL_MAP.containsKey(lookupKey)) {
            return SKILL_MAP.get(lookupKey);
        }

        // Default: If it's a known skill but not in map, just capitalize words nicely or return as-is
        return capitalizeSkill(cleaned);
    }

    private static String capitalizeSkill(String skill) {
        if (skill.length() <= 3) {
            // Keep short acronyms uppercase (e.g. SQL, XML, PHP)
            return skill.toUpperCase();
        }
        
        // Capitalize first letter of each word
        String[] words = skill.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) continue;
            
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                sb.append(word.substring(1).toLowerCase());
            }
            if (i < words.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
