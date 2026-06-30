package com.eightfold.normalizer;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhoneNormalizer {
    private static final Logger logger = LoggerFactory.getLogger(PhoneNormalizer.class);
    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private static final String DEFAULT_REGION = "US";

    /**
     * Normalizes a phone number to E.164 format.
     *
     * @param rawPhone The raw phone number string.
     * @return Normalized phone number in E.164 format, or the original string if parsing fails.
     */
    public static String normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.strip().isEmpty()) {
            return null;
        }

        String cleaned = rawPhone.strip();
        try {
            // Parse with default region US (fallback if no country code prefix like + is present)
            PhoneNumber numberProto = phoneUtil.parse(cleaned, DEFAULT_REGION);
            if (phoneUtil.isValidNumber(numberProto)) {
                return phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
            } else {
                logger.warn("Phone number is parsed but marked as invalid: {}", cleaned);
                // Return digits only with '+' if it started with it, otherwise just return cleaned
                return cleaned;
            }
        } catch (NumberParseException e) {
            logger.warn("Failed to parse phone number: {}. Error: {}", cleaned, e.toString());
            return cleaned;
        }
    }
}
