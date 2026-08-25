package dev.vality.orgmanager.util;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class JsonCodec {

    private final JsonMapper jsonMapper;

    @SneakyThrows(JacksonException.class)
    public String toJson(Object data) {
        return jsonMapper.writeValueAsString(data);
    }

    @SuppressWarnings("rawtypes")
    @SneakyThrows(JacksonException.class)
    public Map toMap(String json) {
        return jsonMapper.readValue(json, Map.class);
    }
}
