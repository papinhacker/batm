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
public class Client {

    @JsonProperty("addresses")
    private Addresses addresses;

    @JsonProperty("companyName")
    private String companyName;

    @JsonProperty("finInstitutionRef")
    private FinInstitutionRef finInstitutionRef;

    @JsonProperty("ipsParametersSet")
    private IpsParametersSet ipsParametersSet;

    @JsonProperty("number")
    private String number;

    @JsonProperty("typeRef")
    private TypeRef typeRef;
}