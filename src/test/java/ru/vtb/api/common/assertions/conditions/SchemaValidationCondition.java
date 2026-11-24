package ru.vtb.api.common.assertions.conditions;

import io.restassured.response.ValidatableResponse;
import lombok.RequiredArgsConstructor;
import ru.vtb.api.common.assertions.Condition;
import ru.vtb.api.common.utils.JsonSchemaValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static ru.vtb.core.common.EnvConstants.FILE_SEPARATOR;
import static ru.vtb.core.common.EnvConstants.SCHEMAS_FOLDER;

@RequiredArgsConstructor
public class SchemaValidationCondition implements Condition {

    private final String jsonPath;

    @Override
    public void check(ValidatableResponse response) {
        String expectedJson;
        try {
            expectedJson = Files.readString(Path.of(SCHEMAS_FOLDER + FILE_SEPARATOR + jsonPath));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read response schema file. Reason is: " + e.getMessage());
        }
        JsonSchemaValidator.validate(expectedJson, response.extract().response().asString());
    }
}
