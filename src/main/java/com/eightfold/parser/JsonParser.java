package com.eightfold.parser;

import com.eightfold.model.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class JsonParser {
    private static final Logger logger = LoggerFactory.getLogger(JsonParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses the config.json configuration file.
     *
     * @param filePath Path to the config file.
     * @return Config object parsed from JSON.
     * @throws IOException If file reading or parsing fails.
     */
    public static Config parseConfig(String filePath) throws IOException {
        logger.info("Parsing configuration file from path: {}", filePath);
        try {
            return objectMapper.readValue(new File(filePath), Config.class);
        } catch (IOException e) {
            logger.error("Failed to parse config file: {}. Error: {}", filePath, e.getMessage());
            throw e;
        }
    }
}
