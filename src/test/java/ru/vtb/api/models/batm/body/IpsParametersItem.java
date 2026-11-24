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
public class IpsParametersItem {

    @JsonProperty("cardAcceptorIdCode")
    private String cardAcceptorIdCode;

    @JsonProperty("ipsParametersRef")
    private IpsParametersRef ipsParametersRef;

    @JsonProperty("mcc")
    private String mcc;
}