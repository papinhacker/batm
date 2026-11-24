package ru.vtb.api.services;

import io.restassured.http.ContentType;
import ru.vtb.api.common.assertions.AssertableResponse;
import ru.vtb.api.models.batm.Body;
import ru.vtb.api.models.batm.Header;
import ru.vtb.api.models.batm.RootPOJO;
import ru.vtb.api.models.batm.body.*;
import ru.vtb.api.models.batm.generators.HeaderGenerator;
import ru.vtb.api.models.batm.generators.SaveAgreementGenerator;
import ru.vtb.core.common.Log;
import ru.vtb.core.utils.CommonHelper;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static ru.vtb.core.common.EnvConstants.BO_JSON_API_ENDPOINT;

public class AgreementService {

    private static final String ENDPOINT = BO_JSON_API_ENDPOINT + "/saveAgreement";

    public AssertableResponse post(Map<String, Object> fields, String step) {
        RootPOJO model = SaveAgreementGenerator.generateSaveAgreementModel(fields);
        return post(model, step);
    }

    public AssertableResponse post(Map<String, Object> fields) {
        RootPOJO model = SaveAgreementGenerator.generateSaveAgreementModel(fields);
        return post(model);
    }

    public AssertableResponse post(RootPOJO model, String... step) {
        if (step.length > 0)
            Log.step(step);
        return new AssertableResponse(given().contentType(ContentType.JSON)
                .body(CommonHelper.serializeToJsonStringIgnoringNullableFields(model))
                .post(ENDPOINT)
                .then());
    }

    public AssertableResponse close(String agreementNumber) {
        Header header = HeaderGenerator.generateHeaderModel(Map.of());
        Body body = Body.builder()
                .agreement(Agreement.builder()
                        .agreementRef(AgreementRef.builder()
                                .parameters(Parameters.builder()
                                        .number(agreementNumber)
                                        .build())
                                .build())
                        .classifiers(Classifiers.builder()
                                .classifier(List.of(ClassifierItem.builder()
                                        .typeRef(TypeRef.builder()
                                                .parameters(Parameters.builder()
                                                        .code("AGREEMENT_STATUS")
                                                        .build())
                                                .build())
                                        .valueRef(ValueRef.builder()
                                                .parameters(Parameters.builder()
                                                        .code("TO_CLOSING")
                                                        .build())
                                                .build())
                                        .validFrom(CommonHelper.getCurrentDateInFormat("yyyy-MM-dd"))
                                        .build()))
                                .build())
                        .build())
                .build();

        return new AssertableResponse(given().contentType(ContentType.JSON)
                .body(CommonHelper.serializeToJsonStringIgnoringNullableFields(new RootPOJO(header, body)))
                .post(ENDPOINT)
                .then());
    }
}
