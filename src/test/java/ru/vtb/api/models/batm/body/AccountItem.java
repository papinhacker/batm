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
public class AccountItem {

    @JsonProperty("accountStatus")
    private String accountStatus;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("openDate")
    private String openDate;

    @JsonProperty("accountRef")
    private AccountRef accountRef;

    @JsonProperty("accountTypeRef")
    private AccountTypeRef accountTypeRef;

    @JsonProperty("agreementRef")
    private AgreementRef agreementRef;

    @JsonProperty("accountCategory")
    private String accountCategory;
}