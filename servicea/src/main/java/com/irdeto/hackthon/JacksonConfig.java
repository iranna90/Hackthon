package com.irdeto.hackthon;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.Json;

import java.time.ZoneOffset;
import java.util.TimeZone;

public class JacksonConfig {
    public static ObjectMapper registerModulesToJackson() {
        return Json.mapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
    }
}
