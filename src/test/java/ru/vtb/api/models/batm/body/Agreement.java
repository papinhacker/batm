package ru.vtb.api.models.batm.body;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agreement {

    @JsonProperty("accessors")
    private Accessors accessors;

    @JsonProperty("accounts")
    private Accounts accounts;

    @JsonProperty("agreementClass")
    private String agreementClass;

    @JsonProperty("agreementRef")
    private AgreementRef agreementRef;

    @JsonProperty("branchRef")
    private BranchRef branchRef;

    @JsonProperty("classifiers")
    private Classifiers classifiers;

    @JsonProperty("client")
    private Client client;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("effectiveDate")
    private String effectiveDate;

    @JsonProperty("finInstitutionRef")
    private FinInstitutionRef finInstitutionRef;

    @JsonProperty("number")
    private String number;

    @JsonProperty("productRef")
    private ProductRef productRef;

    @JsonProperty("serviceType")
    private String serviceType;

    @JsonProperty("signatureDate")
    private String signatureDate;
}