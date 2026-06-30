package com.eightfold.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private List<String> selectedFields = new ArrayList<>();
    private Map<String, String> fieldRenames = new HashMap<>();
    private boolean includeConfidence = true;
    private boolean includeProvenance = true;
    private String missingValueHandling = "omit"; // "null", "omit", "error"

    public Config() {}

    public List<String> getSelectedFields() {
        return selectedFields;
    }

    public void setSelectedFields(List<String> selectedFields) {
        this.selectedFields = selectedFields;
    }

    public Map<String, String> getFieldRenames() {
        return fieldRenames;
    }

    public void setFieldRenames(Map<String, String> fieldRenames) {
        this.fieldRenames = fieldRenames;
    }

    public boolean isIncludeConfidence() {
        return includeConfidence;
    }

    public void setIncludeConfidence(boolean includeConfidence) {
        this.includeConfidence = includeConfidence;
    }

    public boolean isIncludeProvenance() {
        return includeProvenance;
    }

    public void setIncludeProvenance(boolean includeProvenance) {
        this.includeProvenance = includeProvenance;
    }

    public String getMissingValueHandling() {
        return missingValueHandling;
    }

    public void setMissingValueHandling(String missingValueHandling) {
        this.missingValueHandling = missingValueHandling;
    }

    @Override
    public String toString() {
        return "Config{" +
                "selectedFields=" + selectedFields +
                ", fieldRenames=" + fieldRenames +
                ", includeConfidence=" + includeConfidence +
                ", includeProvenance=" + includeProvenance +
                ", missingValueHandling='" + missingValueHandling + '\'' +
                '}';
    }
}
