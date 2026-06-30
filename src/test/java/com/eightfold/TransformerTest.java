package com.eightfold;

import com.eightfold.model.Candidate;
import com.eightfold.model.Config;
import com.eightfold.model.Experience;
import com.eightfold.model.Provenance;
import com.eightfold.model.Skill;
import com.eightfold.normalizer.DateNormalizer;
import com.eightfold.normalizer.NameNormalizer;
import com.eightfold.normalizer.PhoneNormalizer;
import com.eightfold.normalizer.SkillNormalizer;
import com.eightfold.merger.CandidateMerger;
import com.eightfold.transformer.CandidateTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TransformerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testPhoneNormalizer() {
        // Valid US format
        assertEquals("+14155552671", PhoneNormalizer.normalize("415-555-2671"));
        assertEquals("+14155552671", PhoneNormalizer.normalize("+1 (415) 555-2671"));
        // Already E.164
        assertEquals("+919876543210", PhoneNormalizer.normalize("+919876543210"));
        // Invalid/unparseable - fallback to raw
        assertEquals("123", PhoneNormalizer.normalize("123"));
        assertNull(PhoneNormalizer.normalize(null));
    }

    @Test
    public void testNameNormalizer() {
        assertEquals("John Doe", NameNormalizer.normalize("john doe"));
        assertEquals("Sarah Connor", NameNormalizer.normalize("SARAH CONNOR"));
        assertEquals("Anne-Marie", NameNormalizer.normalize("anne-marie"));
        assertEquals("Jean-Luc Picard", NameNormalizer.normalize("jean-luc picard"));
        assertNull(NameNormalizer.normalize("   "));
    }

    @Test
    public void testDateNormalizer() {
        // Full dates
        assertEquals("2021-05", DateNormalizer.normalize("2021-05-12"));
        assertEquals("2021-05", DateNormalizer.normalize("12/05/2021"));
        // Year Month
        assertEquals("2021-05", DateNormalizer.normalize("05/2021"));
        assertEquals("2021-05", DateNormalizer.normalize("May 2021"));
        assertEquals("2021-05", DateNormalizer.normalize("May, 2021"));
        // Years
        assertEquals("2021-01", DateNormalizer.normalize("2021"));
        // Present
        assertEquals("Present", DateNormalizer.normalize("Present"));
        assertEquals("Present", DateNormalizer.normalize("current"));
    }

    @Test
    public void testSkillNormalizer() {
        assertEquals("JavaScript", SkillNormalizer.normalize("JS"));
        assertEquals("JavaScript", SkillNormalizer.normalize("javascript"));
        assertEquals("Python", SkillNormalizer.normalize("py"));
        assertEquals("Amazon Web Services", SkillNormalizer.normalize("aws"));
        assertEquals("Kubernetes", SkillNormalizer.normalize("k8s"));
        assertEquals("Kotlin", SkillNormalizer.normalize("kotlin")); // word capitalization fallback
    }

    @Test
    public void testCandidateMerger() {
        Candidate primary = new Candidate();
        primary.setFullName("John Doe");
        primary.setEmails(List.of("john.doe@gmail.com"));
        primary.setPhones(List.of("+14155552671"));
        primary.setSkills(List.of(new Skill("JavaScript", "unknown", new Provenance("CSV", 0.9))));
        
        Experience expP = new Experience();
        expP.setCompany("Google");
        expP.setRole("Developer");
        expP.setStartDate("2021-05");
        expP.setEndDate("Present");
        expP.setProvenance(new Provenance("CSV", 0.9));
        primary.setExperience(List.of(expP));
        primary.getProvenance().put("fullName", new Provenance("CSV", 0.9));
        primary.getProvenance().put("emails", new Provenance("CSV", 0.9));
        primary.getProvenance().put("phones", new Provenance("CSV", 0.9));
        primary.getProvenance().put("skills", new Provenance("CSV", 0.9));

        Candidate secondary = new Candidate();
        secondary.setFullName("john doe");
        secondary.setEmails(List.of("john.work@gmail.com", "john.doe@gmail.com"));
        secondary.setPhones(List.of("+15551234567"));
        secondary.setSkills(List.of(
                new Skill("Python", "unknown", new Provenance("TXT", 0.6)),
                new Skill("JavaScript", "unknown", new Provenance("TXT", 0.6))
        ));

        Experience expS = new Experience();
        expS.setCompany("Google LLC"); // More complete company name
        expS.setRole("Developer");
        expS.setStartDate("2021-05");
        expS.setEndDate("Present");
        expS.setDescription("Developed apps");
        expS.setProvenance(new Provenance("TXT", 0.6));
        secondary.setExperience(List.of(expS));
        secondary.getProvenance().put("fullName", new Provenance("TXT", 0.6));
        secondary.getProvenance().put("emails", new Provenance("TXT", 0.6));
        secondary.getProvenance().put("phones", new Provenance("TXT", 0.6));
        secondary.getProvenance().put("skills", new Provenance("TXT", 0.6));

        // Merge candidates
        Candidate merged = CandidateMerger.merge(primary, secondary);

        // Verification of conflict resolutions
        // 1. Prefer structured (primary) emails/phones
        assertEquals(List.of("john.doe@gmail.com"), merged.getEmails());
        assertEquals(List.of("+14155552671"), merged.getPhones());

        // 2. Merge unique skills
        assertEquals(2, merged.getSkills().size());
        assertTrue(merged.getSkills().contains(new Skill("JavaScript")));
        assertTrue(merged.getSkills().contains(new Skill("Python")));

        // 3. Keep most complete company name
        assertEquals(1, merged.getExperience().size());
        assertEquals("Google LLC", merged.getExperience().get(0).getCompany());
        assertEquals("Developed apps", merged.getExperience().get(0).getDescription()); // took description from secondary since primary was empty

        // 4. Overall confidence calculation check
        assertNotNull(merged.getOverallConfidence());
        assertTrue(merged.getOverallConfidence() > 0.6 && merged.getOverallConfidence() < 0.95);
    }

    @Test
    public void testCandidateTransformerSerialization() throws Exception {
        Candidate c = new Candidate();
        c.setCandidateId("123");
        c.setFullName("John Doe");
        c.setEmails(List.of("john.doe@gmail.com"));
        c.setLocation("San Francisco, CA");
        c.setOverallConfidence(0.85);
        c.getProvenance().put("fullName", new Provenance("CSV", 0.9));

        Config config = new Config();
        config.setSelectedFields(List.of("candidateId", "fullName", "emails", "overallConfidence"));
        config.setFieldRenames(Map.of("fullName", "name"));
        config.setIncludeConfidence(true);
        config.setIncludeProvenance(false); // Exclude provenance
        config.setMissingValueHandling("omit");

        String json = CandidateTransformer.transformToJson(c, config);
        
        // Parse back to inspect
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        
        assertEquals("123", map.get("candidateId"));
        assertEquals("John Doe", map.get("name")); // renamed
        assertEquals(List.of("john.doe@gmail.com"), map.get("emails"));
        assertEquals(0.85, map.get("overallConfidence"));
        assertFalse(map.containsKey("location")); // not in selected fields
        assertFalse(map.containsKey("provenance")); // excluded
    }
}
