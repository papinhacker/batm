package ru.vtb.api.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import org.testng.Assert;
import ru.vtb.core.common.Log;

import java.util.Set;

public class JsonSchemaValidator {

    public static void validate(String expectedJson, String actualJson) {
        JsonNode schemaNode = JsonSchemaGenerator.generateSchemaFromRawJson(expectedJson);

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode);
        Set<ValidationMessage> errors = schema.validate(actualJson, InputFormat.JSON);
        if (!errors.isEmpty()) {
            Log.error("Json schema validation errors:");
            errors.forEach(Log::error);
            Assert.fail("Unable to validate JSON%n %s%n======= with schema ======%n%s.".formatted(actualJson, expectedJson));
        }
    }
}
