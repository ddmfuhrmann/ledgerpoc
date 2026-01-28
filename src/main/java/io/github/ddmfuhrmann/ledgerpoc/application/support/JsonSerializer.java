package io.github.ddmfuhrmann.ledgerpoc.application.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonSerializer {

    private final ObjectMapper objectMapper;

    public JsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize outbox event payload: " + payload.getClass().getSimpleName(),
                    e
            );
        }
    }
}