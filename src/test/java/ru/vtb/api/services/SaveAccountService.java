package ru.vtb.api.services;

import io.restassured.http.ContentType;
import ru.vtb.api.common.assertions.AssertableResponse;
import ru.vtb.api.models.batm.Body;
import ru.vtb.api.models.batm.Header;
import ru.vtb.api.models.batm.RootPOJO;
import ru.vtb.api.models.batm.body.*;
import ru.vtb.api.models.batm.generators.HeaderGenerator;
import ru.vtb.api.models.batm.header.Originator;
import ru.vtb.core.utils.CommonHelper;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static ru.vtb.core.data.static_data.CodesNamesValues.EXTERNAL;
import static ru.vtb.core.data.static_data.MapsKeysConstants.ORIGINATOR;

public class SaveAccountService {

    public AssertableResponse closeAccount(String accountId, String accountTypeId, String agreementId, String accountNumber) {
        Header header = HeaderGenerator.generateHeaderModel(Map.of(ORIGINATOR, new Originator(EXTERNAL)));
        Body body = Body.builder()
                .account(AccountItem.builder()
                        .accountRef(AccountRef.builder().id(accountId).build())
                        .agreementRef(AgreementRef.builder().id(agreementId).build())
                        .accountTypeRef(AccountTypeRef.builder()
                                .id(accountTypeId)
                                .parameters(Parameters.builder().code("20208").build())
                                .build())
                        .accountCategory("ANALYTIC")
                        .currency("810")
                        .accountNumber(accountNumber)
                        .accountStatus("CLOSED")
                        .build())
                .build();

        return new AssertableResponse(given().contentType(ContentType.JSON)
                .body(CommonHelper.serializeToJsonStringIgnoringNullableFields(new RootPOJO(header, body)))
                .post("/acc-json-api/saveAccount")
                .then());
    }

    public AssertableResponse createNewAccount(String agreementId, String accountNumber) {
        Header header = HeaderGenerator.generateHeaderModel(Map.of(ORIGINATOR, new Originator(EXTERNAL)));
        Body body = Body.builder()
                .account(AccountItem.builder()
                        .agreementRef(AgreementRef.builder().id(agreementId).build())
                        .accountTypeRef(AccountTypeRef.builder()
                                .parameters(Parameters.builder().code("20208").build())
                                .build())
                        .accountCategory("ANALYTIC")
                        .currency("810")
                        .accountNumber(accountNumber)
                        .accountStatus("ACTIVE")
                        .build())
                .build();

        return new AssertableResponse(given().contentType(ContentType.JSON)
                .body(CommonHelper.serializeToJsonStringIgnoringNullableFields(new RootPOJO(header, body)))
                .post("/acc-json-api/saveAccount")
                .then());
    }
}
