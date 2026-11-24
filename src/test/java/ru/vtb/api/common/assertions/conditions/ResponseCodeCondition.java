package ru.vtb.api.common.assertions.conditions;

import io.restassured.response.ValidatableResponse;
import lombok.RequiredArgsConstructor;
import org.testng.Assert;
import ru.vtb.api.common.assertions.Condition;
import ru.vtb.api.models.batm.body.Response;

@RequiredArgsConstructor
public class ResponseCodeCondition implements Condition {

    private final String expectedResponseCode;

    @Override
    public void check(ValidatableResponse response) {
        String errMsg = "Пришёл неверный респонс.";
        Response resp = response.extract().jsonPath().getObject("body.response", Response.class);
        Assert.assertEquals(expectedResponseCode, resp.getCode(), errMsg);
    }
}
