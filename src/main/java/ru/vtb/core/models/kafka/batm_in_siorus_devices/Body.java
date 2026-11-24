package ru.vtb.core.models.kafka.batm_in_siorus_devices;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.body.Accounts;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Body {

    @JsonProperty("absBranch")
    private String absBranch;

    @JsonProperty("absBaseBranch")
    private String absBaseBranch;

    @JsonProperty("accounts")
    private Accounts accounts;

    @JsonProperty("address")
    private String address;

    @JsonProperty("atmExploitationStatus")
    private String atmExploitationStatus;

    @JsonProperty("feature")
    private String feature;

    @JsonProperty("id")
    private String id;

    @JsonProperty("vendor")
    private String vendor;

    @JsonProperty("okatoCode")
    private String okatoCode;

    @JsonProperty("model")
    private String model;

    @JsonProperty("serialNumber")
    private String serialNumber;
}