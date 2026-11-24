package ru.vtb.core.models.kafka.batm_in_siorus_devices;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmInSiorusDevicesRootMessage {

    @JsonProperty("body")
    private Body body;

    @JsonProperty("header")
    private Header header;
}