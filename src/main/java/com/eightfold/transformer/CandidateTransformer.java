package com.eightfold.transformer;

import com.eightfold.model.Candidate;
import com.eightfold.model.Config;
import com.eightfold.model.Education;
import com.eightfold.model.Experience;
import com.eightfold.model.Provenance;
import com.eightfold.model.Skill;
import com.eightfold.normalizer.DateNormalizer;
import com.eightfold.normalizer.NameNormalizer;
import com.eightfold.normalizer.PhoneNormalizer;
import com.eightfold.normalizer.SkillNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class CandidateTransformer {
    private static final Logger logger = LoggerFactory.getLogger(CandidateTransformer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Normalizes a Candidate's fields in-place (Name, Phone, Dates, Skills).
     *
     * @param c The candidate profile to normalize.
     * @return The normalized candidate profile.
     */
    public static Candidate normalizeCandidate(Candidate c) {
        if (c == null) return null;

        logger.debug("Normalizing candidate: {}", c.getFullName());

        // 1. Name normalization
        if (c.getFullName() != null) {
            c.setFullName(NameNormalizer.normalize(c.getFullName()));
        }

        // 2. Email normalization: remove duplicates & lowercase
        if (c.getEmails() != null && !c.getEmails().isEmpty()) {
            List<String> cleanEmails = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (String email : c.getEmails()) {
                String normalized = email.strip().toLowerCase();
                if (!seen.contains(normalized)) {
                    seen.add(normalized);
                    cleanEmails.add(email.strip());
                }
            }
            c.setEmails(cleanEmails);
        }

        // 3. Phone normalization: remove duplicates & format E.164
        if (c.getPhones() != null && !c.getPhones().isEmpty()) {
            List<String> normalizedPhones = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (String rawPhone : c.getPhones()) {
                String normalized = PhoneNormalizer.normalize(rawPhone);
                if (normalized != null && !seen.contains(normalized)) {
                    seen.add(normalized);
                    normalizedPhones.add(normalized);
                }
            }
            c.setPhones(normalizedPhones);
        }

        // 4. Skills normalization: map to canonical names
        if (c.getSkills() != null && !c.getSkills().isEmpty()) {
            List<Skill> normalizedSkills = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (Skill skill : c.getSkills()) {
                if (skill.getName() != null) {
                    String canonicalName = SkillNormalizer.normalize(skill.getName());
                    String key = canonicalName.toLowerCase();
                    if (!seen.contains(key)) {
                        seen.add(key);
                        skill.setName(canonicalName);
                        normalizedSkills.add(skill);
                    }
                }
            }
            c.setSkills(normalizedSkills);
        }

        // 5. Experience dates normalization
        if (c.getExperience() != null) {
            for (Experience exp : c.getExperience()) {
                if (exp.getStartDate() != null) {
                    exp.setStartDate(DateNormalizer.normalize(exp.getStartDate()));
                }
                if (exp.getEndDate() != null) {
                    exp.setEndDate(DateNormalizer.normalize(exp.getEndDate()));
                }
                if (exp.getCompany() != null) {
                    exp.setCompany(exp.getCompany().strip());
                }
                if (exp.getRole() != null) {
                    exp.setRole(exp.getRole().strip());
                }
            }
        }

        // 6. Education dates normalization
        if (c.getEducation() != null) {
            for (Education edu : c.getEducation()) {
                if (edu.getStartDate() != null) {
                    edu.setStartDate(DateNormalizer.normalize(edu.getStartDate()));
                }
                if (edu.getEndDate() != null) {
                    edu.setEndDate(DateNormalizer.normalize(edu.getEndDate()));
                }
                if (edu.getInstitution() != null) {
                    edu.setInstitution(edu.getInstitution().strip());
                }
            }
        }

        return c;
    }

    /**
     * Transforms a Candidate profile to a schema-valid JSON string according to Config settings.
     *
     * @param candidate The candidate profile.
     * @param config    The configuration.
     * @return Schema-valid JSON string.
     * @throws IllegalArgumentException If required fields are missing and missingValueHandling is "error".
     * @throws IOException              If JSON generation fails.
     */
    public static String transformToJson(Candidate candidate, Config config) throws IOException {
        logger.info("Transforming candidate profile to JSON based on config specifications");
        Map<String, Object> outputMap = transformToMap(candidate, config);
        return objectMapper.writeValueAsString(outputMap);
    }

    private static Map<String, Object> transformToMap(Candidate candidate, Config config) {
        Map<String, Object> output = new LinkedHashMap<>();

        // Process configured fields
        for (String fieldName : config.getSelectedFields()) {
            if (fieldName.equals("provenance") || fieldName.equals("overallConfidence")) {
                continue; // Handled separately based on boolean flags
            }

            Object rawValue = getFieldValue(candidate, fieldName);
            boolean isMissing = isValueMissing(rawValue);

            if (isMissing) {
                String handling = config.getMissingValueHandling();
                if ("error".equalsIgnoreCase(handling)) {
                    throw new IllegalArgumentException("Configured required field is missing in Candidate profile: " + fieldName);
                } else if ("null".equalsIgnoreCase(handling)) {
                    String outputKey = getOutputKey(fieldName, config);
                    output.put(outputKey, null);
                }
                // If "omit", we do not include it in the map
            } else {
                String outputKey = getOutputKey(fieldName, config);
                Object formattedValue = formatValue(rawValue, config);
                output.put(outputKey, formattedValue);
            }
        }

        // Add overallConfidence if included by config
        if (config.isIncludeConfidence() && config.getSelectedFields().contains("overallConfidence")) {
            String outputKey = getOutputKey("overallConfidence", config);
            output.put(outputKey, candidate.getOverallConfidence());
        }

        // Add provenance if included by config
        if (config.isIncludeProvenance() && config.getSelectedFields().contains("provenance")) {
            String outputKey = getOutputKey("provenance", config);
            output.put(outputKey, formatProvenance(candidate.getProvenance()));
        }

        return output;
    }

    private static Object getFieldValue(Candidate candidate, String fieldName) {
        switch (fieldName) {
            case "candidateId": return candidate.getCandidateId();
            case "fullName": return candidate.getFullName();
            case "emails": return candidate.getEmails();
            case "phones": return candidate.getPhones();
            case "location": return candidate.getLocation();
            case "links": return candidate.getLinks();
            case "headline": return candidate.getHeadline();
            case "yearsExperience": return candidate.getYearsExperience();
            case "skills": return candidate.getSkills();
            case "experience": return candidate.getExperience();
            case "education": return candidate.getEducation();
            default:
                logger.warn("Unknown field name configured: {}", fieldName);
                return null;
        }
    }

    private static boolean isValueMissing(Object value) {
        if (value == null) return true;
        if (value instanceof String) return ((String) value).strip().isEmpty();
        if (value instanceof Collection) return ((Collection<?>) value).isEmpty();
        if (value instanceof Map) return ((Map<?, ?>) value).isEmpty();
        return false;
    }

    private static String getOutputKey(String fieldName, Config config) {
        if (config.getFieldRenames() != null && config.getFieldRenames().containsKey(fieldName)) {
            return config.getFieldRenames().get(fieldName);
        }
        return fieldName;
    }

    private static Object formatValue(Object value, Config config) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) return list;

            Object firstItem = list.get(0);
            if (firstItem instanceof Skill) {
                List<Map<String, Object>> skillsList = new ArrayList<>();
                for (Object item : list) {
                    Skill s = (Skill) item;
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", s.getName());
                    if (s.getLevel() != null && !s.getLevel().equals("unknown")) {
                        map.put("level", s.getLevel());
                    }
                    if (config.isIncludeProvenance() && s.getProvenance() != null) {
                        map.put("provenance", formatProvenanceItem(s.getProvenance()));
                    }
                    skillsList.add(map);
                }
                return skillsList;
            } else if (firstItem instanceof Experience) {
                List<Map<String, Object>> expList = new ArrayList<>();
                for (Object item : list) {
                    Experience exp = (Experience) item;
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("company", exp.getCompany());
                    map.put("role", exp.getRole());
                    map.put("startDate", exp.getStartDate());
                    map.put("endDate", exp.getEndDate());
                    if (exp.getDescription() != null) {
                        map.put("description", exp.getDescription());
                    }
                    if (config.isIncludeProvenance() && exp.getProvenance() != null) {
                        map.put("provenance", formatProvenanceItem(exp.getProvenance()));
                    }
                    expList.add(map);
                }
                return expList;
            } else if (firstItem instanceof Education) {
                List<Map<String, Object>> eduList = new ArrayList<>();
                for (Object item : list) {
                    Education edu = (Education) item;
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("institution", edu.getInstitution());
                    map.put("degree", edu.getDegree());
                    map.put("fieldOfStudy", edu.getFieldOfStudy());
                    map.put("startDate", edu.getStartDate());
                    map.put("endDate", edu.getEndDate());
                    if (config.isIncludeProvenance() && edu.getProvenance() != null) {
                        map.put("provenance", formatProvenanceItem(edu.getProvenance()));
                    }
                    eduList.add(map);
                }
                return eduList;
            }
        }
        return value;
    }

    private static Map<String, Object> formatProvenance(Map<String, Provenance> provenanceMap) {
        Map<String, Object> formatted = new LinkedHashMap<>();
        for (Map.Entry<String, Provenance> entry : provenanceMap.entrySet()) {
            if (entry.getValue() != null) {
                formatted.put(entry.getKey(), formatProvenanceItem(entry.getValue()));
            }
        }
        return formatted;
    }

    private static Map<String, Object> formatProvenanceItem(Provenance prov) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("source", prov.getSource());
        map.put("confidence", prov.getConfidence());
        return map;
    }
}
