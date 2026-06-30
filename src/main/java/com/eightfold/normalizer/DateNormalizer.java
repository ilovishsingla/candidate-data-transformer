package com.eightfold.normalizer;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateNormalizer {
    private static final Logger logger = LoggerFactory.getLogger(DateNormalizer.class);

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    };

    private static final DateTimeFormatter[] YEAR_MONTH_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM"),
            DateTimeFormatter.ofPattern("MM/yyyy"),
            DateTimeFormatter.ofPattern("M/yyyy"),
            DateTimeFormatter.ofPattern("MM-yyyy"),
            DateTimeFormatter.ofPattern("M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM"),
            
            // Month names
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM-yyyy", Locale.ENGLISH)
    };

    /**
     * Normalizes date input to "YYYY-MM" format.
     * Keeps "Present" as "Present".
     *
     * @param rawDate The raw date string.
     * @return Normalized date string in "YYYY-MM" format, or "Present", or the original string if parsing fails.
     */
    public static String normalize(String rawDate) {
        if (rawDate == null || rawDate.strip().isEmpty()) {
            return null;
        }

        String cleaned = rawDate.strip();
        String lowercase = cleaned.toLowerCase();

        // Check for present/current markers
        if (lowercase.equals("present") || lowercase.equals("current") || lowercase.equals("now") || lowercase.equals("till date")) {
            return "Present";
        }

        // 1. Try parsing as full LocalDate formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(cleaned, formatter);
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            } catch (DateTimeParseException e) {
                // Keep trying
            }
        }

        // 2. Try parsing as YearMonth formats
        for (DateTimeFormatter formatter : YEAR_MONTH_FORMATTERS) {
            try {
                YearMonth ym = YearMonth.parse(cleaned, formatter);
                return ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            } catch (DateTimeParseException e) {
                // Keep trying
            }
        }

        // 3. Try parsing as a 4-digit Year
        if (cleaned.matches("^\\d{4}$")) {
            try {
                Year year = Year.parse(cleaned);
                return year.atMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            } catch (DateTimeParseException e) {
                // Keep trying
            }
        }

        logger.warn("Could not normalize date: {}", rawDate);
        return cleaned; // Fallback to raw string if unrecognized
    }
}
