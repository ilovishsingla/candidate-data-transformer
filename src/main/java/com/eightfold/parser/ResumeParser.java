package com.eightfold.parser;

import com.eightfold.model.Candidate;
import com.eightfold.model.Education;
import com.eightfold.model.Experience;
import com.eightfold.model.Provenance;
import com.eightfold.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Year;

public class ResumeParser {
    private static final Logger logger = LoggerFactory.getLogger(ResumeParser.class);
    private static final double CONFIDENCE_SCORE = 0.6;
    private static final String SOURCE_NAME = "Resume TXT";

    // Regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\+?\\d{1,4}[-.\\s]?\\(?\\d{2,3}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{4}"
    );
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?linkedin\\.com/in/[a-zA-Z0-9_-]+"
    );
    private static final Pattern GITHUB_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?github\\.com/[a-zA-Z0-9_-]+"
    );
    private static final Pattern GENERAL_URL_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(?:/[a-zA-Z0-9_.-]*)*"
    );
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}|\\d{4}|Present|Current)\\s*-\\s*((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}|\\d{4}|Present|Current)",
            Pattern.CASE_INSENSITIVE
    );

    private enum ParserSection {
        HEADER,
        SUMMARY,
        SKILLS,
        EXPERIENCE,
        EDUCATION,
        NONE
    }

    /**
     * Parses a plain text resume file into a Candidate profile.
     *
     * @param filePath Path to the resume text file.
     * @return Candidate profile parsed from the resume.
     * @throws IOException If file reading fails.
     */
    public static Candidate parse(String filePath) throws IOException {
        Candidate candidate = new Candidate();
        List<String> lines = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }

        if (lines.isEmpty()) {
            logger.warn("Resume text file is empty: {}", filePath);
            return candidate;
        }

        // Set default candidateId from filename or extract a name
        String fileName = new java.io.File(filePath).getName();
        candidate.setCandidateId(fileName.replaceFirst("[.][^.]+$", "")); // filename without extension
        candidate.getProvenance().put("candidateId", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));

        ParserSection currentSection = ParserSection.HEADER;
        List<String> skillTokens = new ArrayList<>();
        
        // Multi-line builder helpers
        Experience currentExp = null;
        Education currentEdu = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty()) {
                continue;
            }

            // Detect section transitions
            ParserSection nextSection = detectSection(line);
            if (nextSection != ParserSection.NONE) {
                currentSection = nextSection;
                // Finalize previous sections if any
                if (currentExp != null && currentExp.getCompany() != null) {
                    candidate.getExperience().add(currentExp);
                    currentExp = null;
                }
                if (currentEdu != null && currentEdu.getInstitution() != null) {
                    candidate.getEducation().add(currentEdu);
                    currentEdu = null;
                }
                continue;
            }

            // Global Regex Scans (Emails, Phones, Links)
            scanGlobalFields(line, candidate);

            // Section Specific Parsing
            switch (currentSection) {
                case HEADER:
                    // Usually, the first line that is not empty is the candidate's name
                    if (candidate.getFullName() == null) {
                        candidate.setFullName(line);
                        candidate.getProvenance().put("fullName", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                    } else if (candidate.getHeadline() == null && !line.contains("@") && !line.matches(".*\\d{4}.*")) {
                        // A line under the name, which has no email and no dates, can be treated as a headline
                        candidate.setHeadline(line);
                        candidate.getProvenance().put("headline", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                    }
                    break;

                case SUMMARY:
                    if (candidate.getHeadline() == null) {
                        candidate.setHeadline(line);
                        candidate.getProvenance().put("headline", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                    }
                    break;

                case SKILLS:
                    // Extract skills (split by comma, semicolon, or bullet points)
                    String[] tokens = line.split("[,;•|]");
                    for (String token : tokens) {
                        String skillName = token.replaceFirst("^[\\-\\s\\*]+", "").strip();
                        if (!skillName.isEmpty() && skillName.length() < 50) {
                            skillTokens.add(skillName);
                        }
                    }
                    break;

                case EXPERIENCE:
                    // Parse experience
                    // Handle key-value formatting first
                    if (line.toLowerCase().startsWith("company:") || line.toLowerCase().startsWith("employer:")) {
                        if (currentExp != null && currentExp.getCompany() != null) {
                            candidate.getExperience().add(currentExp);
                        }
                        currentExp = new Experience();
                        currentExp.setProvenance(new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                        currentExp.setCompany(line.substring(line.indexOf(":") + 1).strip());
                    } else if (line.toLowerCase().startsWith("role:") || line.toLowerCase().startsWith("title:") || line.toLowerCase().startsWith("position:")) {
                        if (currentExp == null) currentExp = new Experience();
                        currentExp.setRole(line.substring(line.indexOf(":") + 1).strip());
                    } else if (line.toLowerCase().startsWith("dates:") || line.toLowerCase().startsWith("duration:")) {
                        if (currentExp == null) currentExp = new Experience();
                        parseExperienceDates(line.substring(line.indexOf(":") + 1).strip(), currentExp);
                    } else if (line.toLowerCase().startsWith("description:") || line.toLowerCase().startsWith("summary:")) {
                        if (currentExp == null) currentExp = new Experience();
                        currentExp.setDescription(line.substring(line.indexOf(":") + 1).strip());
                    }
                    // Handle pipe-separated line: Company | Role | Dates | Description
                    else if (line.contains("|")) {
                        if (currentExp != null && currentExp.getCompany() != null) {
                            candidate.getExperience().add(currentExp);
                        }
                        currentExp = parsePipeExperience(line);
                    }
                    // Handle bullet points/descriptions for current experience
                    else if (currentExp != null) {
                        // Check if this line is just a date range (new experience entry fallback)
                        Matcher dateMatcher = DATE_RANGE_PATTERN.matcher(line);
                        if (dateMatcher.find() && (line.length() < 100)) {
                            candidate.getExperience().add(currentExp);
                            currentExp = new Experience();
                            currentExp.setProvenance(new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                            // Try to guess company/role from the rest of the line
                            String rest = line.replaceAll(DATE_RANGE_PATTERN.pattern(), "").replace("at", "").replace("in", "").strip();
                            currentExp.setCompany(rest);
                            parseExperienceDates(line, currentExp);
                        } else {
                            // Append to description
                            String existingDesc = currentExp.getDescription();
                            if (existingDesc == null) {
                                currentExp.setDescription(line);
                            } else {
                                currentExp.setDescription(existingDesc + " " + line);
                            }
                        }
                    }
                    break;

                case EDUCATION:
                    // Parse education
                    if (line.toLowerCase().startsWith("institution:") || line.toLowerCase().startsWith("school:") || line.toLowerCase().startsWith("university:")) {
                        if (currentEdu != null && currentEdu.getInstitution() != null) {
                            candidate.getEducation().add(currentEdu);
                        }
                        currentEdu = new Education();
                        currentEdu.setProvenance(new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                        currentEdu.setInstitution(line.substring(line.indexOf(":") + 1).strip());
                    } else if (line.toLowerCase().startsWith("degree:")) {
                        if (currentEdu == null) currentEdu = new Education();
                        currentEdu.setDegree(line.substring(line.indexOf(":") + 1).strip());
                    } else if (line.toLowerCase().startsWith("field:") || line.toLowerCase().startsWith("major:") || line.toLowerCase().startsWith("field of study:")) {
                        if (currentEdu == null) currentEdu = new Education();
                        currentEdu.setFieldOfStudy(line.substring(line.indexOf(":") + 1).strip());
                    } else if (line.toLowerCase().startsWith("dates:") || line.toLowerCase().startsWith("duration:")) {
                        if (currentEdu == null) currentEdu = new Education();
                        parseEducationDates(line.substring(line.indexOf(":") + 1).strip(), currentEdu);
                    } else if (line.contains("|")) {
                        if (currentEdu != null && currentEdu.getInstitution() != null) {
                            candidate.getEducation().add(currentEdu);
                        }
                        currentEdu = parsePipeEducation(line);
                    }
                    break;

                default:
                    break;
            }
        }

        // Finalize remaining items
        if (currentExp != null && currentExp.getCompany() != null) {
            candidate.getExperience().add(currentExp);
        }
        if (currentEdu != null && currentEdu.getInstitution() != null) {
            candidate.getEducation().add(currentEdu);
        }

        // Add collected skills
        if (!skillTokens.isEmpty()) {
            List<Skill> skills = new ArrayList<>();
            for (String sName : skillTokens) {
                skills.add(new Skill(sName, "unknown", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE)));
            }
            candidate.setSkills(skills);
            candidate.getProvenance().put("skills", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
        }

        // Add overall location guess if we found one
        // (Typically look for line starting with "Location:" in the header)
        for (String line : lines) {
            if (line.toLowerCase().startsWith("location:") || line.toLowerCase().startsWith("address:")) {
                String loc = line.substring(line.indexOf(":") + 1).strip();
                candidate.setLocation(loc);
                candidate.getProvenance().put("location", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                break;
            }
        }

        // Estimate years of experience from experience dates
        double totalYears = calculateYearsExperience(candidate.getExperience());
        if (totalYears > 0.0) {
            candidate.setYearsExperience(totalYears);
            candidate.getProvenance().put("yearsExperience", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
        }

        return candidate;
    }

    private static ParserSection detectSection(String line) {
        String cleaned = line.toLowerCase().strip();
        if (cleaned.matches("^(?:work\\s+)?experience(?:s)?$") || cleaned.matches("^professional\\s+experience$") || cleaned.matches("^employment\\s+history$")) {
            return ParserSection.EXPERIENCE;
        } else if (cleaned.matches("^education$") || cleaned.matches("^academic\\s+background$") || cleaned.matches("^studies$")) {
            return ParserSection.EDUCATION;
        } else if (cleaned.matches("^skills$") || cleaned.matches("^technical\\s+skills$") || cleaned.matches("^core\\s+competencies$")) {
            return ParserSection.SKILLS;
        } else if (cleaned.matches("^summary$") || cleaned.matches("^professional\\s+summary$") || cleaned.matches("^about\\s+me$") || cleaned.matches("^headline$")) {
            return ParserSection.SUMMARY;
        }
        return ParserSection.NONE;
    }

    private static void scanGlobalFields(String line, Candidate candidate) {
        // Email scan
        Matcher emailMatcher = EMAIL_PATTERN.matcher(line);
        while (emailMatcher.find()) {
            String email = emailMatcher.group().strip();
            if (!candidate.getEmails().contains(email)) {
                candidate.getEmails().add(email);
                candidate.getProvenance().put("emails", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
            }
        }

        // Phone scan
        Matcher phoneMatcher = PHONE_PATTERN.matcher(line);
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group().strip();
            // Basic length check to avoid capturing zip codes or years
            if (phone.replaceAll("[^\\d]", "").length() >= 7 && !candidate.getPhones().contains(phone)) {
                candidate.getPhones().add(phone);
                candidate.getProvenance().put("phones", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
            }
        }

        // LinkedIn scan
        Matcher liMatcher = LINKEDIN_PATTERN.matcher(line);
        if (liMatcher.find()) {
            candidate.getLinks().put("LinkedIn", liMatcher.group().strip());
            candidate.getProvenance().put("links", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
        }

        // GitHub scan
        Matcher ghMatcher = GITHUB_PATTERN.matcher(line);
        if (ghMatcher.find()) {
            candidate.getLinks().put("GitHub", ghMatcher.group().strip());
            candidate.getProvenance().put("links", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
        }

        // General URL scan (Portfolio)
        Matcher urlMatcher = GENERAL_URL_PATTERN.matcher(line);
        while (urlMatcher.find()) {
            String url = urlMatcher.group().strip();
            if (!url.contains("linkedin.com") && !url.contains("github.com")) {
                candidate.getLinks().put("Portfolio", url);
                candidate.getProvenance().put("links", new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
                break;
            }
        }
    }

    private static Experience parsePipeExperience(String line) {
        String[] parts = line.split("\\|");
        Experience exp = new Experience();
        exp.setProvenance(new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
        
        if (parts.length > 0) exp.setCompany(parts[0].strip());
        if (parts.length > 1) exp.setRole(parts[1].strip());
        if (parts.length > 2) parseExperienceDates(parts[2].strip(), exp);
        if (parts.length > 3) exp.setDescription(parts[3].strip());
        
        return exp;
    }

    private static void parseExperienceDates(String dateStr, Experience exp) {
        Matcher matcher = DATE_RANGE_PATTERN.matcher(dateStr);
        if (matcher.find()) {
            exp.setStartDate(matcher.group(1).strip());
            exp.setEndDate(matcher.group(2).strip());
        } else {
            exp.setStartDate(dateStr.strip());
        }
    }

    private static Education parsePipeEducation(String line) {
        String[] parts = line.split("\\|");
        Education edu = new Education();
        edu.setProvenance(new Provenance(SOURCE_NAME, CONFIDENCE_SCORE));
        
        if (parts.length > 0) edu.setInstitution(parts[0].strip());
        if (parts.length > 1) edu.setDegree(parts[1].strip());
        if (parts.length > 2) edu.setFieldOfStudy(parts[2].strip());
        if (parts.length > 3) parseEducationDates(parts[3].strip(), edu);
        
        return edu;
    }

    private static void parseEducationDates(String dateStr, Education edu) {
        Matcher matcher = DATE_RANGE_PATTERN.matcher(dateStr);
        if (matcher.find()) {
            edu.setStartDate(matcher.group(1).strip());
            edu.setEndDate(matcher.group(2).strip());
        } else {
            edu.setStartDate(dateStr.strip());
        }
    }

    private static double calculateYearsExperience(List<Experience> experiences) {
        double totalYears = 0.0;
        for (Experience exp : experiences) {
            String start = exp.getStartDate();
            String end = exp.getEndDate();
            if (start != null) {
                try {
                    int startYear = extractYear(start);
                    int endYear = Year.now().getValue(); // Default to current year if Present
                    if (end != null && !end.toLowerCase().matches("present|current|now|till date")) {
                        endYear = extractYear(end);
                    }
                    int diff = endYear - startYear;
                    if (diff > 0) {
                        totalYears += diff;
                    }
                } catch (Exception e) {
                    // Ignore date parsing error for years calculations
                }
            }
        }
        return totalYears;
    }

    private static int extractYear(String dateStr) {
        Pattern yearPattern = Pattern.compile("\\b(\\d{4})\\b");
        Matcher matcher = yearPattern.matcher(dateStr);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException("No 4-digit year found in string: " + dateStr);
    }
}
