package com.eightfold.merger;

import com.eightfold.model.Candidate;
import com.eightfold.model.Education;
import com.eightfold.model.Experience;
import com.eightfold.model.Provenance;
import com.eightfold.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CandidateMerger {
    private static final Logger logger = LoggerFactory.getLogger(CandidateMerger.class);

    /**
     * Merges two candidate profiles (typically one structured and one unstructured)
     * using the specified conflict resolution strategy.
     *
     * @param primary   The primary (usually structured) candidate profile.
     * @param secondary The secondary (usually unstructured) candidate profile.
     * @return The merged canonical candidate profile.
     */
    public static Candidate merge(Candidate primary, Candidate secondary) {
        if (primary == null) return secondary;
        if (secondary == null) return primary;

        logger.info("Merging candidate profiles: '{}' and '{}'", primary.getFullName(), secondary.getFullName());

        Candidate merged = new Candidate();

        // 1. candidateId: Prefer primary, ignore empty
        mergeFieldString(primary, secondary, merged, "candidateId", primary.getCandidateId(), secondary.getCandidateId(), true);

        // 2. fullName: Prefer primary structured, fallback to secondary
        mergeFieldString(primary, secondary, merged, "fullName", primary.getFullName(), secondary.getFullName(), true);

        // 3. emails: Prefer structured (primary) over resume (secondary)
        mergeEmailsAndPhones(primary, secondary, merged, "emails", primary.getEmails(), secondary.getEmails());

        // 4. phones: Prefer structured (primary) over resume (secondary)
        mergeEmailsAndPhones(primary, secondary, merged, "phones", primary.getPhones(), secondary.getPhones());

        // 5. location: Prefer primary, ignore empty
        mergeFieldString(primary, secondary, merged, "location", primary.getLocation(), secondary.getLocation(), false);

        // 6. headline: Prefer primary, ignore empty
        mergeFieldString(primary, secondary, merged, "headline", primary.getHeadline(), secondary.getHeadline(), false);

        // 7. yearsExperience: Prefer primary (structured), fallback to secondary
        mergeYearsExperience(primary, secondary, merged);

        // 8. links: Merge unique links, preferring primary on key conflicts
        mergeLinks(primary, secondary, merged);

        // 9. skills: Merge unique skills, resolving duplicate skills by keeping higher confidence
        mergeSkills(primary, secondary, merged);

        // 10. experience: Merge similar experiences, keep complete company name
        mergeExperiences(primary, secondary, merged);

        // 11. education: Merge similar educations
        mergeEducations(primary, secondary, merged);

        // Calculate overall confidence score
        calculateOverallConfidence(merged);

        return merged;
    }

    private static void mergeFieldString(Candidate p, Candidate s, Candidate m, String fieldName, String valP, String valS, boolean preferPrimary) {
        String chosenVal = null;
        Provenance chosenProv = null;

        boolean pExists = valP != null && !valP.strip().isEmpty();
        boolean sExists = valS != null && !valS.strip().isEmpty();

        if (pExists && sExists) {
            if (preferPrimary) {
                chosenVal = valP.strip();
                chosenProv = p.getProvenance().get(fieldName);
            } else {
                // Keep the longer value if not strictly preferring primary
                if (valP.strip().length() >= valS.strip().length()) {
                    chosenVal = valP.strip();
                    chosenProv = p.getProvenance().get(fieldName);
                } else {
                    chosenVal = valS.strip();
                    chosenProv = s.getProvenance().get(fieldName);
                }
            }
        } else if (pExists) {
            chosenVal = valP.strip();
            chosenProv = p.getProvenance().get(fieldName);
        } else if (sExists) {
            chosenVal = valS.strip();
            chosenProv = s.getProvenance().get(fieldName);
        }

        if (chosenVal != null) {
            switch (fieldName) {
                case "candidateId": m.setCandidateId(chosenVal); break;
                case "fullName": m.setFullName(chosenVal); break;
                case "location": m.setLocation(chosenVal); break;
                case "headline": m.setHeadline(chosenVal); break;
            }
            m.getProvenance().put(fieldName, chosenProv != null ? chosenProv : new Provenance("Unknown", 0.5));
        }
    }

    private static void mergeEmailsAndPhones(Candidate p, Candidate s, Candidate m, String fieldName, List<String> listP, List<String> listS) {
        boolean pExists = listP != null && !listP.isEmpty();
        boolean sExists = listS != null && !listS.isEmpty();

        List<String> resultList = new ArrayList<>();
        Provenance chosenProv = null;

        // Prefer structured (primary)
        if (pExists) {
            resultList.addAll(listP);
            chosenProv = p.getProvenance().get(fieldName);
        } else if (sExists) {
            resultList.addAll(listS);
            chosenProv = s.getProvenance().get(fieldName);
        }

        // Remove duplicates case-insensitively/trimmed but preserve order
        List<String> cleanList = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String item : resultList) {
            String norm = item.strip().toLowerCase();
            if (!seen.contains(norm)) {
                seen.add(norm);
                cleanList.add(item.strip());
            }
        }

        if (!cleanList.isEmpty()) {
            if (fieldName.equals("emails")) {
                m.setEmails(cleanList);
            } else {
                m.setPhones(cleanList);
            }
            m.getProvenance().put(fieldName, chosenProv != null ? chosenProv : new Provenance("Unknown", 0.5));
        }
    }

    private static void mergeYearsExperience(Candidate p, Candidate s, Candidate m) {
        Double valP = p.getYearsExperience();
        Double valS = s.getYearsExperience();
        Double chosenVal = null;
        Provenance chosenProv = null;

        if (valP != null && valS != null) {
            // Prefer structured (primary)
            chosenVal = valP;
            chosenProv = p.getProvenance().get("yearsExperience");
        } else if (valP != null) {
            chosenVal = valP;
            chosenProv = p.getProvenance().get("yearsExperience");
        } else if (valS != null) {
            chosenVal = valS;
            chosenProv = s.getProvenance().get("yearsExperience");
        }

        if (chosenVal != null) {
            m.setYearsExperience(chosenVal);
            m.getProvenance().put("yearsExperience", chosenProv != null ? chosenProv : new Provenance("Unknown", 0.5));
        }
    }

    private static void mergeLinks(Candidate p, Candidate s, Candidate m) {
        Map<String, String> mergedLinks = new HashMap<>();
        
        // Populate with secondary (unstructured)
        if (s.getLinks() != null) {
            mergedLinks.putAll(s.getLinks());
        }
        
        // Overwrite with primary (structured) on key conflicts
        if (p.getLinks() != null) {
            mergedLinks.putAll(p.getLinks());
        }

        if (!mergedLinks.isEmpty()) {
            m.setLinks(mergedLinks);
            
            // Choose provenance: if primary provided links, we mark primary, otherwise secondary
            Provenance pProv = p.getProvenance().get("links");
            Provenance sProv = s.getProvenance().get("links");
            if (pProv != null && p.getLinks() != null && !p.getLinks().isEmpty()) {
                m.getProvenance().put("links", pProv);
            } else {
                m.getProvenance().put("links", sProv != null ? sProv : new Provenance("Unknown", 0.5));
            }
        }
    }

    private static void mergeSkills(Candidate p, Candidate s, Candidate m) {
        // Use a map to merge skills by normalized lower-case name
        Map<String, Skill> skillMap = new LinkedHashMap<>();

        if (s.getSkills() != null) {
            for (Skill skill : s.getSkills()) {
                if (skill.getName() != null) {
                    skillMap.put(skill.getName().toLowerCase(), skill);
                }
            }
        }

        if (p.getSkills() != null) {
            for (Skill skill : p.getSkills()) {
                if (skill.getName() != null) {
                    // Overwrite or update with primary structured skills
                    skillMap.put(skill.getName().toLowerCase(), skill);
                }
            }
        }

        if (!skillMap.isEmpty()) {
            m.setSkills(new ArrayList<>(skillMap.values()));
            
            // For skills field provenance, set to primary if primary provided skills, otherwise secondary
            Provenance pProv = p.getProvenance().get("skills");
            Provenance sProv = s.getProvenance().get("skills");
            if (pProv != null && p.getSkills() != null && !p.getSkills().isEmpty()) {
                m.getProvenance().put("skills", pProv);
            } else {
                m.getProvenance().put("skills", sProv != null ? sProv : new Provenance("Unknown", 0.5));
            }
        }
    }

    private static void mergeExperiences(Candidate p, Candidate s, Candidate m) {
        List<Experience> pList = p.getExperience() != null ? p.getExperience() : new ArrayList<>();
        List<Experience> sList = s.getExperience() != null ? s.getExperience() : new ArrayList<>();
        List<Experience> mergedList = new ArrayList<>();

        // Keep track of matched secondary experiences
        Set<Experience> matchedSecondary = new HashSet<>();

        for (Experience expP : pList) {
            Experience match = null;
            for (Experience expS : sList) {
                if (matchedSecondary.contains(expS)) continue;
                
                if (areExperiencesSimilar(expP, expS)) {
                    match = expS;
                    matchedSecondary.add(expS);
                    break;
                }
            }

            if (match != null) {
                // Merge expP and match
                Experience mergedExp = new Experience();
                
                // Keep the most complete company name
                mergedExp.setCompany(getMostCompleteName(expP.getCompany(), match.getCompany()));
                
                // Prefer primary role
                mergedExp.setRole(expP.getRole() != null ? expP.getRole() : match.getRole());
                
                // Dates: Prefer primary
                mergedExp.setStartDate(expP.getStartDate() != null ? expP.getStartDate() : match.getStartDate());
                mergedExp.setEndDate(expP.getEndDate() != null ? expP.getEndDate() : match.getEndDate());
                
                // Description: Keep the longer one
                String descP = expP.getDescription() != null ? expP.getDescription() : "";
                String descS = match.getDescription() != null ? match.getDescription() : "";
                mergedExp.setDescription(descP.length() >= descS.length() ? expP.getDescription() : match.getDescription());
                
                // Provenance: Prefer higher confidence
                double confP = expP.getProvenance() != null ? expP.getProvenance().getConfidence() : 0.0;
                double confS = match.getProvenance() != null ? match.getProvenance().getConfidence() : 0.0;
                mergedExp.setProvenance(confP >= confS ? expP.getProvenance() : match.getProvenance());

                mergedList.add(mergedExp);
            } else {
                mergedList.add(expP);
            }
        }

        // Add remaining secondary experiences
        for (Experience expS : sList) {
            if (!matchedSecondary.contains(expS)) {
                mergedList.add(expS);
            }
        }

        if (!mergedList.isEmpty()) {
            m.setExperience(mergedList);
            // Track experience provenance field: overall if we got any structured, mark CSV, else Resume
            Provenance pProv = p.getProvenance().get("experience");
            Provenance sProv = s.getProvenance().get("experience");
            m.getProvenance().put("experience", pProv != null ? pProv : (sProv != null ? sProv : new Provenance("Unknown", 0.5)));
        }
    }

    private static boolean areExperiencesSimilar(Experience e1, Experience e2) {
        if (e1.getCompany() == null || e2.getCompany() == null) return false;
        
        String c1 = e1.getCompany().toLowerCase().replaceAll("[^a-z0-9]", "");
        String c2 = e2.getCompany().toLowerCase().replaceAll("[^a-z0-9]", "");
        
        // Simple company overlap check
        boolean companyOverlap = c1.contains(c2) || c2.contains(c1);
        
        // If roles or dates also match/overlap, they are similar
        boolean roleOverlap = false;
        if (e1.getRole() != null && e2.getRole() != null) {
            String r1 = e1.getRole().toLowerCase();
            String r2 = e2.getRole().toLowerCase();
            roleOverlap = r1.contains(r2) || r2.contains(r1);
        }

        boolean dateOverlap = false;
        if (e1.getStartDate() != null && e2.getStartDate() != null) {
            dateOverlap = e1.getStartDate().strip().equals(e2.getStartDate().strip());
        }

        return companyOverlap && (roleOverlap || dateOverlap);
    }

    private static void mergeEducations(Candidate p, Candidate s, Candidate m) {
        List<Education> pList = p.getEducation() != null ? p.getEducation() : new ArrayList<>();
        List<Education> sList = s.getEducation() != null ? s.getEducation() : new ArrayList<>();
        List<Education> mergedList = new ArrayList<>();

        Set<Education> matchedSecondary = new HashSet<>();

        for (Education eduP : pList) {
            Education match = null;
            for (Education eduS : sList) {
                if (matchedSecondary.contains(eduS)) continue;
                
                if (areEducationsSimilar(eduP, eduS)) {
                    match = eduS;
                    matchedSecondary.add(eduS);
                    break;
                }
            }

            if (match != null) {
                Education mergedEdu = new Education();
                
                // Keep the most complete school name
                mergedEdu.setInstitution(getMostCompleteName(eduP.getInstitution(), match.getInstitution()));
                
                // Degree/Field
                mergedEdu.setDegree(eduP.getDegree() != null ? eduP.getDegree() : match.getDegree());
                mergedEdu.setFieldOfStudy(eduP.getFieldOfStudy() != null ? eduP.getFieldOfStudy() : match.getFieldOfStudy());
                
                // Dates
                mergedEdu.setStartDate(eduP.getStartDate() != null ? eduP.getStartDate() : match.getStartDate());
                mergedEdu.setEndDate(eduP.getEndDate() != null ? eduP.getEndDate() : match.getEndDate());
                
                // Provenance
                double confP = eduP.getProvenance() != null ? eduP.getProvenance().getConfidence() : 0.0;
                double confS = match.getProvenance() != null ? match.getProvenance().getConfidence() : 0.0;
                mergedEdu.setProvenance(confP >= confS ? eduP.getProvenance() : match.getProvenance());

                mergedList.add(mergedEdu);
            } else {
                mergedList.add(eduP);
            }
        }

        for (Education eduS : sList) {
            if (!matchedSecondary.contains(eduS)) {
                mergedList.add(eduS);
            }
        }

        if (!mergedList.isEmpty()) {
            m.setEducation(mergedList);
            Provenance pProv = p.getProvenance().get("education");
            Provenance sProv = s.getProvenance().get("education");
            m.getProvenance().put("education", pProv != null ? pProv : (sProv != null ? sProv : new Provenance("Unknown", 0.5)));
        }
    }

    private static boolean areEducationsSimilar(Education e1, Education e2) {
        if (e1.getInstitution() == null || e2.getInstitution() == null) return false;
        
        String inst1 = e1.getInstitution().toLowerCase().replaceAll("[^a-z0-9]", "");
        String inst2 = e2.getInstitution().toLowerCase().replaceAll("[^a-z0-9]", "");
        
        boolean instOverlap = inst1.contains(inst2) || inst2.contains(inst1);
        
        boolean degreeOverlap = false;
        if (e1.getDegree() != null && e2.getDegree() != null) {
            String d1 = e1.getDegree().toLowerCase().replaceAll("[^a-z]", "");
            String d2 = e2.getDegree().toLowerCase().replaceAll("[^a-z]", "");
            degreeOverlap = d1.contains(d2) || d2.contains(d1);
        }

        return instOverlap && degreeOverlap;
    }

    private static String getMostCompleteName(String name1, String name2) {
        if (name1 == null) return name2;
        if (name2 == null) return name1;
        String n1 = name1.toLowerCase().strip();
        String n2 = name2.toLowerCase().strip();
        if (n1.contains(n2)) return name1;
        if (n2.contains(n1)) return name2;
        return name1.length() >= name2.length() ? name1 : name2;
    }

    private static void calculateOverallConfidence(Candidate merged) {
        Map<String, Provenance> provMap = merged.getProvenance();
        if (provMap.isEmpty()) {
            merged.setOverallConfidence(0.5);
            return;
        }

        double totalConfidence = 0.0;
        int count = 0;

        // Sum the confidence of all fields tracked in the provenance map
        for (Provenance prov : provMap.values()) {
            if (prov != null) {
                totalConfidence += prov.getConfidence();
                count++;
            }
        }

        double avgConfidence = count > 0 ? (totalConfidence / count) : 0.5;
        
        // Round to 2 decimal places
        avgConfidence = Math.round(avgConfidence * 100.0) / 100.0;
        merged.setOverallConfidence(avgConfidence);
    }
}
