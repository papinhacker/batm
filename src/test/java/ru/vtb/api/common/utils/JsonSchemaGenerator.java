package ru.vtb.api.common.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

public class JsonSchemaGenerator {

    public static JsonNode generateSchemaFromRawJson(String rawJson) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode inputJson;
        try {
            inputJson = mapper.readTree(rawJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse json in '%s' Reason is: ".formatted(JsonSchemaGenerator.class.getName()) + e.getMessage());
        }

        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("$schema", "http://json-schema.org/draft-07/schema#");
        schemaNode.setAll(generateSchemaForNode(inputJson, mapper));
        schemaNode.put("additionalProperties", false);

        return schemaNode;
    }

    private static ObjectNode generateSchemaForNode(JsonNode node, ObjectMapper mapper) {
        ObjectNode result = mapper.createObjectNode();

        if (node.isObject()) {
            result.put("type", "object");
            ObjectNode props = mapper.createObjectNode();
            ArrayNode required = mapper.createArrayNode();

            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                JsonNode child = node.get(field);
                props.set(field, generateSchemaForNode(child, mapper));
                required.add(field);
                result.set("properties", props);
                result.set("required", required);
                result.put("additionalProperties", false);
            }
        } else if (node.isArray()) {
            result.put("type", "array");
            if (node.size() > 0) {
                for (int i = 0; i < node.size(); i++) {
                    result.set("items", generateSchemaForNode(node.get(i), mapper));
                }
            } else {
                result.set("items", mapper.createObjectNode());
            }
        } else if (node.isTextual()) {
            result.put("type", "string");
        } else if (node.isInt() || node.isLong()) {
            result.put("type", "integer");
        } else if (node.isBoolean()) {
            result.put("type", "boolean");
        } else if (node.isDouble() || node.isFloat() || node.isBigDecimal()) {
            result.put("type", "number");
        } else {
            result.put("type", "null");
        }

        return result;
    }
}
