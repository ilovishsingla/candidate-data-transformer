# Multi-Source Candidate Data Transformer

A Java-based Command Line Interface (CLI) application built using clean architecture, OOP principles, and SOLID design. This tool extracts candidate details from multiple sources (both structured and unstructured), normalizes fields, resolves merging conflicts, and serializes the final profile to schema-valid JSON based on a customizable configuration file.

---

## Architecture & Directory Structure

The project is structured according to **Clean Architecture** guidelines to ensure separation of concerns, easy maintainability, and testing:

```
candidate-transformer/
│
├── src/main/java/com/eightfold/
│   ├── model/                  # Domain objects and configurations
│   │   ├── Candidate.java
│   │   ├── Skill.java
│   │   ├── Experience.java
│   │   ├── Education.java
│   │   ├── Provenance.java
│   │   └── Config.java
│   │
│   ├── parser/                 # Parsers for CSV, Resume TXT, and JSON configs
│   │   ├── CsvParser.java
│   │   ├── ResumeParser.java
│   │   └── JsonParser.java
│   │
│   ├── normalizer/             # Data cleaning and formatting components
│   │   ├── PhoneNormalizer.java
│   │   ├── SkillNormalizer.java
│   │   ├── DateNormalizer.java
│   │   └── NameNormalizer.java
│   │
│   ├── merger/                 # Merging logic and conflict resolution rules
│   │   └── CandidateMerger.java
│   │
│   ├── transformer/            # Coordinator for ETL and config-driven serialization
│   │   └── CandidateTransformer.java
│   │
│   └── Main.java               # CLI entrypoint
│
├── src/main/resources/
│   ├── config.json             # Default serialization configuration
│   └── logback.xml             # Logback configuration routing logs to stderr
│
├── src/test/java/com/eightfold/
│   └── TransformerTest.java    # JUnit 5 integration and unit tests
│
├── sample-data/                # Sample input files
│   ├── candidates.csv          # Structured candidate source
│   └── resume.txt              # Unstructured resume source
│
├── pom.xml                     # Maven project specification
└── README.md                   # This file
```

---

## Features

### 1. Multi-Source Parsers
*   **Structured Parser (CSV)**: Uses Apache Commons CSV to read structured candidate sheets. Supports parsing nested lists (emails, phones, skills) and structured JSON strings for experiences and educations.
*   **Unstructured Parser (Resume TXT)**: Reads plain text resumes using a stateful block-parsing algorithm. Scans for section headers (Experience, Education, Skills, Summary), extracts contact details using regex, parses job roles, companies, dates, and institutions.

### 2. Normalization Engine
*   **Phone Numbers**: Normalizes valid national/international phone numbers to standard E.164 format (e.g., `(415) 555-2671` $\rightarrow$ `+14155552671`) using Google's `libphonenumber`.
*   **Dates**: Formats input dates of various patterns (e.g. `YYYY-MM-DD`, `MM/YYYY`, `Month YYYY`, `YYYY`) to `YYYY-MM` using Java `java.time` APIs. Handles relative markers like "Present" or "Current".
*   **Skills**: Normalizes skill variations and abbreviations (e.g. `JS` $\rightarrow$ `JavaScript`, `py` $\rightarrow$ `Python`, `aws` $\rightarrow$ `Amazon Web Services`) using an internal dictionary mapping.
*   **Names**: Formats word casing correctly (e.g., `john doe` $\rightarrow$ `John Doe`, `anne-marie` $\rightarrow$ `Anne-Marie`).
*   **Deduplication**: Removes duplicate email addresses and phone numbers.

### 3. Smart Candidate Merger & Conflict Resolution
When merging structured (CSV) and unstructured (Resume) profiles of matching candidates, the system applies these rules:
1.  **Prefer Structured Data**: Prefers email and phone numbers from the CSV source over the Resume.
2.  **Merge Unique Skills**: Integrates unique skills from both sources.
3.  **Complete Company/School Names**: Keeps the most complete/detailed name if there is an overlap (e.g., `Google LLC` over `Google`, `University of California, Berkeley` over `UC Berkeley`).
4.  **Field-Level Provenance**: Maps the source (`CSV` or `Resume TXT`) and confidence score (e.g., `0.9` for CSV and `0.6` for Resume) for every field.
5.  **Aggregate Confidence**: Calculates the candidate's `overallConfidence` as the mathematical average of populated fields.

### 4. Configurable output via `config.json`
Output is customizable via a config JSON file supporting:
*   **`selectedFields`**: List of fields to include in the output.
*   **`fieldRenames`**: Dictionary mapping original field names to custom keys (e.g., `fullName` $\rightarrow$ `name`).
*   **`includeConfidence`**: Boolean flag to enable or disable the overall confidence score.
*   **`includeProvenance`**: Boolean flag to toggle metadata tracking at the root and nested item levels.
*   **`missingValueHandling`**: Defines behavior for missing fields:
    *   `omit`: Remove the field key entirely from the JSON object.
    *   `null`: Write the key with a `null` value.
    *   `error`: Halt execution and throw an exception.

---

## Build & Usage Instructions

### Requirements
*   Java 17 or higher
*   Maven 3.6+

### Build the JAR
Compile and package the project into a runnable fat JAR containing all dependencies:
```bash
mvn clean package
```
This produces `candidate-transformer-1.0-SNAPSHOT.jar` inside the `target/` directory.

### Run the Application
Execute the transformer by feeding it the structured CSV, unstructured resume, and config file:
```bash
java -jar target/candidate-transformer-1.0-SNAPSHOT.jar \
  --csv sample-data/candidates.csv \
  --resume sample-data/resume.txt \
  --config src/main/resources/config.json
```

To redirect the schema-valid JSON output to a file:
```bash
java -jar target/candidate-transformer-1.0-SNAPSHOT.jar \
  --csv sample-data/candidates.csv \
  --resume sample-data/resume.txt \
  --config src/main/resources/config.json > output.json
```
*(Note: Because Logback routing is configured to send all logs to `stderr`, the stdout output redirected to `output.json` will be completely clean JSON).*

---

## Configuration Settings Example (`config.json`)

Here is an example structure of `config.json`:
```json
{
  "selectedFields": [
    "candidateId",
    "fullName",
    "emails",
    "phones",
    "location",
    "links",
    "headline",
    "yearsExperience",
    "skills",
    "experience",
    "education",
    "provenance",
    "overallConfidence"
  ],
  "fieldRenames": {
    "fullName": "name",
    "emails": "emailAddresses",
    "phones": "phoneNumbers"
  },
  "includeConfidence": true,
  "includeProvenance": true,
  "missingValueHandling": "omit"
}
```
