package ru.vtb.api.common.assertions;

import ru.vtb.api.common.assertions.conditions.ResponseCodeCondition;
import ru.vtb.api.common.assertions.conditions.ResponseTypeCondition;
import ru.vtb.api.common.assertions.conditions.SchemaValidationCondition;
import ru.vtb.api.common.assertions.conditions.StatusCodeCondition;
import ru.vtb.core.data.static_data.ResponseTypes;

public class Conditions {

    public static ResponseTypeCondition hasResponseType(String expectedMessage) {
        return new ResponseTypeCondition(expectedMessage);
    }

    public static ResponseTypeCondition responseTypeIsSuccess() {
        return new ResponseTypeCondition(ResponseTypes.SUCCESS);
    }

    public static ResponseCodeCondition responseTypeIsSLR0001() {
        return new ResponseCodeCondition("SLR-0001");
    }

    public static StatusCodeCondition hasStatusCode(Integer expectedStatus) {
        return new StatusCodeCondition(expectedStatus);
    }

    public static SchemaValidationCondition hasJsonSchema(String jsonPath) {
        return new SchemaValidationCondition(jsonPath);
    }
}
