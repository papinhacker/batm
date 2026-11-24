package ru.vtb.api.services;

import io.restassured.http.ContentType;
import ru.vtb.api.common.assertions.AssertableResponse;
import ru.vtb.api.models.batm.Body;
import ru.vtb.api.models.batm.Header;
import ru.vtb.api.models.batm.RootPOJO;
import ru.vtb.api.models.batm.body.AgreementRef;
import ru.vtb.api.models.batm.body.Parameters;
import ru.vtb.api.models.batm.generators.HeaderGenerator;
import ru.vtb.api.models.batm.header.Originator;
import ru.vtb.core.utils.CommonHelper;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static ru.vtb.core.data.static_data.CodesNamesValues.EXTERNAL;
import static ru.vtb.core.data.static_data.MapsKeysConstants.ORIGINATOR;

public class GetAccountListService {

    public AssertableResponse post(String agreementNumber) {
        Header header = HeaderGenerator.generateHeaderModel(Map.of(ORIGINATOR, new Originator(EXTERNAL)));
        Body body = Body.builder()
                .agreementRef(AgreementRef.builder().parameters(Parameters.builder().number(agreementNumber).build()).build())
                .build();

        return new AssertableResponse(given().contentType(ContentType.JSON)
                .body(CommonHelper.serializeToJsonStringIgnoringNullableFields(new RootPOJO(header, body)))
                .post("/acc-json-api/getAccountList")
                .then());
    }
}
