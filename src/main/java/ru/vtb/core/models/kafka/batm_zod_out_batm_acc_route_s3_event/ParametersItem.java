package ru.vtb.core.models.kafka.batm_zod_out_batm_acc_route_s3_event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametersItem {

    @JsonProperty("value")
    private String value;

    @JsonProperty("key")
    private String key;
}