package com.cs2hub.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.io.IOException;

/**
 * Custom Jackson serializer for Spring Data {@link Page} instances.
 * <p>
 * Silences Spring Boot 3 PageImpl serialization warning and provides
 * a clean flat JSON structure compatible with frontend consumers.
 */
@JsonComponent
public class PageSerializer extends JsonSerializer<Page<?>> {

    @Override
    public void serialize(Page<?> page, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("content", page.getContent());
        gen.writeNumberField("totalPages", page.getTotalPages());
        gen.writeNumberField("totalElements", page.getTotalElements());
        gen.writeNumberField("size", page.getSize());
        gen.writeNumberField("number", page.getNumber());
        gen.writeNumberField("numberOfElements", page.getNumberOfElements());
        gen.writeBooleanField("first", page.isFirst());
        gen.writeBooleanField("last", page.isLast());
        gen.writeBooleanField("empty", page.isEmpty());

        Sort sort = page.getSort();
        if (sort != null && sort.isSorted()) {
            gen.writeObjectField("sort", sort);
        }

        gen.writeEndObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Page<?>> handledType() {
        return (Class<Page<?>>) (Class<?>) Page.class;
    }
}
