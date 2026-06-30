package com.eightfold.model;

import java.util.Objects;

public class Skill {
    private String name;
    private String level;
    private Provenance provenance;

    public Skill() {}

    public Skill(String name) {
        this.name = name;
    }

    public Skill(String name, String level, Provenance provenance) {
        this.name = name;
        this.level = level;
        this.provenance = provenance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skill skill = (Skill) o;
        return Objects.equals(name != null ? name.toLowerCase() : null, 
                              skill.name != null ? skill.name.toLowerCase() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name != null ? name.toLowerCase() : null);
    }

    @Override
    public String toString() {
        return "Skill{" +
                "name='" + name + '\'' +
                ", level='" + level + '\'' +
                ", provenance=" + provenance +
                '}';
    }
}
