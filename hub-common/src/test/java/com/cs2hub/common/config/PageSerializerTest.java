package com.cs2hub.common.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageSerializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(new PageSerializer());
        objectMapper.registerModule(module);
    }

    @Test
    @DisplayName("Should serialize PageImpl to flat JSON without sort field when unsorted")
    void shouldSerializePageImplUnsorted() throws Exception {
        List<String> items = List.of("item1", "item2", "item3");
        Page<String> page = new PageImpl<>(items, PageRequest.of(0, 10), 25);

        String json = objectMapper.writeValueAsString(page);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("content")).isTrue();
        assertThat(root.get("content").isArray()).isTrue();
        assertThat(root.get("content").size()).isEqualTo(3);
        assertThat(root.get("content").get(0).asText()).isEqualTo("item1");

        assertThat(root.get("totalPages").asInt()).isEqualTo(3);
        assertThat(root.get("totalElements").asLong()).isEqualTo(25L);
        assertThat(root.get("size").asInt()).isEqualTo(10);
        assertThat(root.get("number").asInt()).isEqualTo(0);
        assertThat(root.get("numberOfElements").asInt()).isEqualTo(3);
        assertThat(root.get("first").asBoolean()).isTrue();
        assertThat(root.get("last").asBoolean()).isFalse();
        assertThat(root.get("empty").asBoolean()).isFalse();
        assertThat(root.has("sort")).isFalse();
    }

    @Test
    @DisplayName("Should serialize PageImpl with sort field when sorted")
    void shouldSerializePageImplWithSortWhenSorted() throws Exception {
        List<String> items = List.of("item1");
        PageRequest pageRequest = PageRequest.of(1, 10, Sort.by("title").descending());
        Page<String> page = new PageImpl<>(items, pageRequest, 11);

        String json = objectMapper.writeValueAsString(page);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("number").asInt()).isEqualTo(1);
        assertThat(root.get("last").asBoolean()).isTrue();
        assertThat(root.get("first").asBoolean()).isFalse();
        assertThat(root.has("sort")).isTrue();
        assertThat(root.get("sort").get("sorted").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Should serialize empty PageImpl correctly")
    void shouldSerializeEmptyPage() throws Exception {
        Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        String json = objectMapper.writeValueAsString(page);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("content").size()).isEqualTo(0);
        assertThat(root.get("totalPages").asInt()).isEqualTo(0);
        assertThat(root.get("totalElements").asLong()).isEqualTo(0L);
        assertThat(root.get("empty").asBoolean()).isTrue();
        assertThat(root.get("first").asBoolean()).isTrue();
        assertThat(root.get("last").asBoolean()).isTrue();
        assertThat(root.has("sort")).isFalse();
    }
}
