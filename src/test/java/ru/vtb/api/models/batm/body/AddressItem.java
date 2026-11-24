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
public class AddressItem {

    @JsonProperty("addressLine")
    private String addressLine;

    @JsonProperty("addressLine3")
    private String addressLine3;

    @JsonProperty("addressLine4")
    private String addressLine4;

    @JsonProperty("attributes")
    private Attributes attributes;

    @JsonProperty("city")
    private String city;

    @JsonProperty("country")
    private String country;

    @JsonProperty("houseNumber")
    private String houseNumber;

    @JsonProperty("postalCode")
    private String postalCode;

    @JsonProperty("street")
    private String street;

    @JsonProperty("typeRef")
    private TypeRef typeRef;
}