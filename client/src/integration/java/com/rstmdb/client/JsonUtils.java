package com.rstmdb.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;

@UtilityClass
public class JsonUtils {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    public static <T> T readFile(String file, Class<T> type) throws IOException {
        try (InputStream is = JsonUtils.class.getClassLoader().getResourceAsStream(file)) {
            if (is == null) {
                throw new IOException("Resource not found: " + file);
            }
            return MAPPER.readValue(is, type);
        }
    }
}
