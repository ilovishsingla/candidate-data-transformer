package com.eightfold.parser;

import com.eightfold.model.Candidate;
import com.eightfold.model.Education;
import com.eightfold.model.Experience;
import com.eightfold.model.Provenance;
import com.eightfold.model.Skill;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvParser {
    private static final Logger logger = LoggerFactory.getLogger(CsvParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final double CONFIDENCE_SCORE = 0.9;
    private static final String SOURCE_NAME = "CSV";

    /**
     * Parses candidate profiles from a CSV file.
     *
     * @param filePath Path to the CSV file.
     * @return List of candidates parsed from the file.
     * @throws IOException If file reading fails.
     */
    public static List<Candidate> parse(String filePath) throws IOException {
        List<Candidate> candidates = new ArrayList<>();
        
        try (Reader reader = new FileReader(filePath, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            if (headerMap == null) {
                logger.warn("CSV file has no headers or is empty: {}", filePath);
                return candidates;
            }

            for (CSVRecord record : csvParser) {
                try {
                    Candidate candidate = new Candidate();
                    
                    // candidateId
                    String candidateId = getRecordValue(record, "candidateId", null);
                    if (candidateId != null) {
                        candidate.setCandidateId(candidateId);
                        candidate.getProvenance().put("candidateId", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                    }

                    // fullName
                    String fullName = getRecordValue(record, "fullName", "name");
                    if (fullName != null) {
                        candidate.setFullName(fullName);
                        candidate.getProvenance().put("fullName", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                    }

                    // emails
                    String emailVal = getRecordValue(record, "emails", "email");
                    if (emailVal != null) {
                        List<String> emails = splitValue(emailVal);
                        candidate.setEmails(emails);
                        candidate.getProvenance().put("emails", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                    }

                    // phones
                    String phoneVal = getRecordValue(record, "phones", "phone");
                    if (phoneVal != null) {
                        List<String> phones = splitValue(phoneVal);
                        candidate.setPhones(phones);
                        candidate.getProvenance().put("phones", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                    }

                    // location
                    String location = getRecordValue(record, "location", "address");
                    if (location != null) {
                        candidate.setLocation(location);
                        candidate.getProvenance().put("location", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                    }

                    // links
                    Map<String, String> links = new HashMap<>();
                    String github = getRecordValue(record, "github", null);
                    if (github != null) links.put("GitHub", github);
                    String linkedin = getRecordValue(record, "linkedin", null);
                    if (linkedin != null) links.put("LinkedIn", linkedin);
                    String portfolio = getRecordValue(record, "portfolio", null);
                    if (portfolio != null) links.put("Portfolio", portfolio);
                    
                    if (!links.isEmpty()) {
                        candidate.setLinks(links);
                        candidate.getProvenance().put("links", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                    }

                    // headline
                    String headline = getRecordValue(record, "headline", "title");
                    if (headline != null) {
                        candidate.setHeadline(headline);
                        candidate.getProvenance().put("headline", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                    }

                    // yearsExperience
                    String yearsExpVal = getRecordValue(record, "yearsExperience", "experienceYears");
                    if (yearsExpVal != null) {
                        try {
                            candidate.setYearsExperience(Double.parseDouble(yearsExpVal));
                            candidate.getProvenance().put("yearsExperience", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid yearsExperience value in CSV: {}", yearsExpVal);
                        }
                    }

                    // skills
                    String skillsVal = getRecordValue(record, "skills", "skillList");
                    if (skillsVal != null) {
                        List<String> skillNames = splitValue(skillsVal);
                        List<Skill> skills = new ArrayList<>();
                        for (String sName : skillNames) {
                            skills.add(new Skill(sName, "unknown", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE)));
                        }
                        candidate.setSkills(skills);
                        candidate.getProvenance().put("skills", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                    }

                    // experience (can be JSON array string)
                    String expVal = getRecordValue(record, "experience", "workExperience");
                    if (expVal != null) {
                        try {
                            List<Experience> expList = objectMapper.readValue(expVal, new TypeReference<List<Experience>>() {});
                            for (Experience exp : expList) {
                                exp.setProvenance(new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                            }
                            candidate.setExperience(expList);
                            candidate.getProvenance().put("experience", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                        } catch (Exception e) {
                            logger.warn("Could not parse experience JSON in CSV column: {}", expVal, e);
                        }
                    }

                    // education (can be JSON array string)
                    String eduVal = getRecordValue(record, "education", "academicRecord");
                    if (eduVal != null) {
                        try {
                            List<Education> eduList = objectMapper.readValue(eduVal, new TypeReference<List<Education>>() {});
                            for (Education edu : eduList) {
                                edu.setProvenance(new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                            }
                            candidate.setEducation(eduList);
                            candidate.getProvenance().put("education", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                        } catch (Exception e) {
                            logger.warn("Could not parse education JSON in CSV column: {}", eduVal, e);
                        }
                    }

                    candidates.add(candidate);
                } catch (Exception e) {
                    logger.error("Error parsing CSV record number {}: {}", record.getRecordNumber(), e.getMessage(), e);
                }
            }
        }

        return candidates;
    }

    private static String getRecordValue(CSVRecord record, String primaryHeader, String secondaryHeader) {
        if (record.isMapped(primaryHeader)) {
            String val = record.get(primaryHeader);
            if (val != null && !val.strip().isEmpty()) {
                return val.strip();
            }
        }
        if (secondaryHeader != null && record.isMapped(secondaryHeader)) {
            String val = record.get(secondaryHeader);
            if (val != null && !val.strip().isEmpty()) {
                return val.strip();
            }
        }
        return null;
    }

    private static List<String> splitValue(String value) {
        List<String> list = new ArrayList<>();
        if (value == null) return list;
        // Split by comma, semicolon, or vertical bar (pipe)
        String[] parts = value.split("[,;|]");
        for (String p : parts) {
            String trimmed = p.strip();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }
}
