package com.eightfold.normalizer;

public class NameNormalizer {

    /**
     * Normalizes capitalization of a full name.
     * E.g., "john doe" -> "John Doe", "SARAH CONNOR" -> "Sarah Connor", "jean-luc picard" -> "Jean-Luc Picard"
     *
     * @param rawName The raw name.
     * @return Properly capitalized name, or null if input is null/empty.
     */
    public static String normalize(String rawName) {
        if (rawName == null || rawName.strip().isEmpty()) {
            return null;
        }

        String[] words = rawName.strip().split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            sb.append(capitalizeWord(word));
            if (i < words.length - 1) {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    private static String capitalizeWord(String word) {
        if (word.isEmpty()) {
            return word;
        }

        if (word.contains("-")) {
            String[] parts = word.split("-", -1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                sb.append(capitalizeSingleWord(parts[i]));
                if (i < parts.length - 1) {
                    sb.append("-");
                }
            }
            return sb.toString();
        }

        return capitalizeSingleWord(word);
    }

    private static String capitalizeSingleWord(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
