package ru.vtb.core.models.kafka.batm_dp_in_batm_devices_atm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmDpInBatmDevicesAtmRootMessage {

    @JsonProperty("serialNumber")
    private String serialNumber;

    @JsonProperty("address")
    private String address;

    @JsonProperty("vendor")
    private String vendor;

    @JsonProperty("okatoCode")
    private String okatoCode;

    @JsonProperty("model")
    private String model;

    @JsonProperty("updateTime")
    private String updateTime;

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("status")
    private String status;
}