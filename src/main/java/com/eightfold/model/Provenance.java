package com.eightfold.model;

public class Provenance {
    private String source;
    private double confidence;

    public Provenance() {}

    public Provenance(String source, double confidence) {
        this.source = source;
        this.confidence = confidence;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "Provenance{" +
                "source='" + source + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
