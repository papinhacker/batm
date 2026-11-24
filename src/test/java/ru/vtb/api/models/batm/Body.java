package ru.vtb.api.models.batm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vtb.api.models.batm.body.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Body {

    @JsonProperty("accessorRef")
    private AccessorRef accessorRef;

    @JsonProperty("agreementRef")
    private AgreementRef agreementRef;

    @JsonProperty("account")
    private AccountItem account;

    @JsonProperty("agreement")
    private Agreement agreement;

    @JsonProperty("classifier")
    private Classifier classifier;
}