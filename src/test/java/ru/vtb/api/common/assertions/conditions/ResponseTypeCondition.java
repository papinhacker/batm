package ru.vtb.api.common.assertions.conditions;

import io.restassured.response.ValidatableResponse;
import lombok.RequiredArgsConstructor;
import org.testng.Assert;
import ru.vtb.api.common.assertions.Condition;
import ru.vtb.api.models.batm.body.Response;

@RequiredArgsConstructor
public class ResponseTypeCondition implements Condition {

    private final String expectedMessage;

    @Override
    public void check(ValidatableResponse response) {
        Response resp = response.extract().jsonPath().getObject("body.response", Response.class);
        Assert.assertEquals(expectedMessage, resp.getType(), "Пришёл неверный респонс.");
    }
}
