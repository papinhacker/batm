package ru.vtb.api.services;

import io.restassured.http.ContentType;
import ru.vtb.api.common.assertions.AssertableResponse;
import ru.vtb.api.models.batm.Body;
import ru.vtb.api.models.batm.Header;
import ru.vtb.api.models.batm.RootPOJO;
import ru.vtb.api.models.batm.body.AccessorRef;
import ru.vtb.api.models.batm.body.AccessorTypeRef;
import ru.vtb.api.models.batm.body.Parameters;
import ru.vtb.api.models.batm.generators.HeaderGenerator;
import ru.vtb.core.utils.CommonHelper;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static ru.vtb.core.common.EnvConstants.BO_JSON_API_ENDPOINT;

public class DeactivateAccessorService {

    public AssertableResponse deactivate(RootPOJO model) {
        return new AssertableResponse(given().contentType(ContentType.JSON)
                .body(CommonHelper.serializeToJsonStringIgnoringNullableFields(model))
                .post(BO_JSON_API_ENDPOINT + "/deactivateAccessor")
                .then());
    }

    public AssertableResponse deactivate(String contractNumber) {
        Header header = HeaderGenerator.generateHeaderModel(Map.of());
        Body body = Body.builder()
                .accessorRef(AccessorRef.builder()
                        .parameters(Parameters.builder()
                                .number(contractNumber)
                                .accessorTypeRef(AccessorTypeRef.builder()
                                        .parameters(Parameters.builder()
                                                .code("TERM")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        return new AssertableResponse(given().contentType(ContentType.JSON)
                .body(CommonHelper.serializeToJsonStringIgnoringNullableFields(new RootPOJO(header, body)))
                .post(BO_JSON_API_ENDPOINT + "/deactivateAccessor")
                .then());
    }
}
