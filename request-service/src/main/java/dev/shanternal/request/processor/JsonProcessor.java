package dev.shanternal.request.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonProcessor {

    private final JsonFactory jsonFactory = new JsonFactory();

    public int countKeys(String json) {
        if (json == null || json.isBlank()) {
            return 0;
        }

        int count = 0;

        try (JsonParser parser = jsonFactory.createParser(json)) {
            JsonToken token;

            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.FIELD_NAME) {
                    count++;
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON format", e);
        }

        return count;
    }
}