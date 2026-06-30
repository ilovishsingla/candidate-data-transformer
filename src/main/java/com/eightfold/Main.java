package com.eightfold;

import com.eightfold.merger.CandidateMerger;
import com.eightfold.model.Candidate;
import com.eightfold.model.Config;
import com.eightfold.parser.CsvParser;
import com.eightfold.parser.JsonParser;
import com.eightfold.parser.ResumeParser;
import com.eightfold.transformer.CandidateTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        logger.info("Starting Multi-Source Candidate Data Transformer...");

        String csvPath = null;
        String resumePath = null;
        String configPath = null;

        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if ("--csv".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                csvPath = args[++i];
            } else if ("--resume".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                resumePath = args[++i];
            } else if ("--config".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                configPath = args[++i];
            }
        }

        // Validate CLI options
        if (csvPath == null && resumePath == null) {
            printUsage();
            System.exit(1);
        }

        try {
            // 1. Load config
            Config config = loadConfig(configPath);
            logger.info("Loaded configuration: {}", config);

            // 2. Parse CSV candidates if provided
            List<Candidate> csvCandidates = new ArrayList<>();
            if (csvPath != null) {
                logger.info("Parsing CSV from: {}", csvPath);
                File file = new File(csvPath);
                if (!file.exists()) {
                    System.err.println("Error: CSV file not found: " + csvPath);
                    System.exit(1);
                }
                csvCandidates = CsvParser.parse(csvPath);
                for (Candidate c : csvCandidates) {
                    CandidateTransformer.normalizeCandidate(c);
                }
            }

            // 3. Parse Resume candidate if provided
            Candidate resumeCandidate = null;
            if (resumePath != null) {
                logger.info("Parsing resume from: {}", resumePath);
                File file = new File(resumePath);
                if (!file.exists()) {
                    System.err.println("Error: Resume file not found: " + resumePath);
                    System.exit(1);
                }
                resumeCandidate = ResumeParser.parse(resumePath);
                CandidateTransformer.normalizeCandidate(resumeCandidate);
            }

            // 4. Merge candidates
            List<Candidate> finalCandidates = new ArrayList<>();

            if (resumeCandidate != null && !csvCandidates.isEmpty()) {
                boolean merged = false;
                for (Candidate csvC : csvCandidates) {
                    if (shouldMerge(csvC, resumeCandidate)) {
                        logger.info("Matching candidates found. Merging CSV candidate '{}' and Resume candidate '{}'", csvC.getFullName(), resumeCandidate.getFullName());
                        Candidate mergedCandidate = CandidateMerger.merge(csvC, resumeCandidate);
                        finalCandidates.add(mergedCandidate);
                        merged = true;
                    } else {
                        finalCandidates.add(csvC);
                    }
                }
                if (!merged) {
                    // Fallback: If CSV contains exactly 1 candidate, merge them anyway
                    if (csvCandidates.size() == 1) {
                        logger.info("Single CSV candidate and Resume found, merging directly.");
                        Candidate mergedCandidate = CandidateMerger.merge(csvCandidates.get(0), resumeCandidate);
                        finalCandidates.add(mergedCandidate);
                    } else {
                        logger.info("No matching candidate found in CSV list for Resume. Outputting separately.");
                        finalCandidates.add(resumeCandidate);
                    }
                }
            } else if (resumeCandidate != null) {
                finalCandidates.add(resumeCandidate);
            } else {
                finalCandidates.addAll(csvCandidates);
            }

            // 5. Transform and output JSON
            if (finalCandidates.isEmpty()) {
                System.out.println("[]");
            } else if (finalCandidates.size() == 1) {
                String jsonOutput = CandidateTransformer.transformToJson(finalCandidates.get(0), config);
                System.out.println(jsonOutput);
            } else {
                // Serialize list of candidates
                List<Object> transformedList = new ArrayList<>();
                for (Candidate c : finalCandidates) {
                    // Use transformer map generator helper
                    String json = CandidateTransformer.transformToJson(c, config);
                    transformedList.add(objectMapper.readValue(json, Object.class));
                }
                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(transformedList));
            }

        } catch (Exception e) {
            logger.error("An error occurred during candidate transformation: {}", e.getMessage(), e);
            System.err.println("Transformation Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Config loadConfig(String configPath) {
        if (configPath != null) {
            try {
                return JsonParser.parseConfig(configPath);
            } catch (Exception e) {
                logger.warn("Could not parse config file from {}, falling back to default.", configPath);
            }
        }

        // Try to load default from resources
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("config.json")) {
            if (is != null) {
                logger.info("Loading default config.json from resources...");
                return objectMapper.readValue(is, Config.class);
            }
        } catch (Exception e) {
            logger.warn("Could not load default config.json from resources: {}", e.getMessage());
        }

        // Programmatic fallback
        logger.info("Creating default configuration programmatically...");
        Config defaultConfig = new Config();
        defaultConfig.setSelectedFields(List.of(
                "candidateId", "fullName", "emails", "phones", "location", "links",
                "headline", "yearsExperience", "skills", "experience", "education",
                "provenance", "overallConfidence"
        ));
        defaultConfig.setIncludeConfidence(true);
        defaultConfig.setIncludeProvenance(true);
        defaultConfig.setMissingValueHandling("omit");
        return defaultConfig;
    }

    private static boolean shouldMerge(Candidate c1, Candidate c2) {
        // 1. Check email overlap
        if (c1.getEmails() != null && c2.getEmails() != null) {
            for (String e1 : c1.getEmails()) {
                for (String e2 : c2.getEmails()) {
                    if (e1.strip().equalsIgnoreCase(e2.strip())) {
                        return true;
                    }
                }
            }
        }
        // 2. Check name match (case-insensitive, alpha-only)
        if (c1.getFullName() != null && c2.getFullName() != null) {
            String n1 = c1.getFullName().toLowerCase().replaceAll("[^a-z]", "");
            String n2 = c2.getFullName().toLowerCase().replaceAll("[^a-z]", "");
            if (!n1.isEmpty() && n1.equals(n2)) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage() {
        System.out.println("Multi-Source Candidate Data Transformer");
        System.out.println("Usage:");
        System.out.println("  java -jar candidate-transformer.jar --csv <csv_file> --resume <resume_txt_file> [--config <config_json>]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --csv      Path to candidate CSV file");
        System.out.println("  --resume   Path to candidate resume TXT file");
        System.out.println("  --config   Path to output configuration JSON file (optional)");
    }
}
