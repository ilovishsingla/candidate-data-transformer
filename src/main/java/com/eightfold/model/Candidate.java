package com.eightfold.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Candidate {
    private String candidateId;
    private String fullName;
    private List<String> emails = new ArrayList<>();
    private List<String> phones = new ArrayList<>();
    private String location;
    private Map<String, String> links = new HashMap<>(); // key: "GitHub", "LinkedIn", "Portfolio", "Other" etc.
    private String headline;
    private Double yearsExperience;
    private List<Skill> skills = new ArrayList<>();
    private List<Experience> experience = new ArrayList<>();
    private List<Education> education = new ArrayList<>();
    private Map<String, Provenance> provenance = new HashMap<>(); // fieldName -> Provenance
    private Double overallConfidence;

    public Candidate() {}

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public List<String> getPhones() {
        return phones;
    }

    public void setPhones(List<String> phones) {
        this.phones = phones;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public Double getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Double yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public List<Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<Skill> skills) {
        this.skills = skills;
    }

    public List<Experience> getExperience() {
        return experience;
    }

    public void setExperience(List<Experience> experience) {
        this.experience = experience;
    }

    public List<Education> getEducation() {
        return education;
    }

    public void setEducation(List<Education> education) {
        this.education = education;
    }

    public Map<String, Provenance> getProvenance() {
        return provenance;
    }

    public void setProvenance(Map<String, Provenance> provenance) {
        this.provenance = provenance;
    }

    public Double getOverallConfidence() {
        return overallConfidence;
    }

    public void setOverallConfidence(Double overallConfidence) {
        this.overallConfidence = overallConfidence;
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "candidateId='" + candidateId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", emails=" + emails +
                ", phones=" + phones +
                ", location='" + location + '\'' +
                ", links=" + links +
                ", headline='" + headline + '\'' +
                ", yearsExperience=" + yearsExperience +
                ", skills=" + skills +
                ", experience=" + experience +
                ", education=" + education +
                ", provenance=" + provenance +
                ", overallConfidence=" + overallConfidence +
                '}';
    }
}
