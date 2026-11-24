package ru.vtb.api.common.assertions.conditions;

import io.restassured.response.ValidatableResponse;
import lombok.RequiredArgsConstructor;
import org.testng.Assert;
import ru.vtb.api.common.assertions.Condition;

@RequiredArgsConstructor
public class StatusCodeCondition implements Condition {

    private final Integer statusCode;

    @Override
    public void check(ValidatableResponse response) {
        Integer actualStatusCode = response.extract().statusCode();
        Assert.assertEquals(statusCode, actualStatusCode, "У респонса неверный статус код.");
    }
}
