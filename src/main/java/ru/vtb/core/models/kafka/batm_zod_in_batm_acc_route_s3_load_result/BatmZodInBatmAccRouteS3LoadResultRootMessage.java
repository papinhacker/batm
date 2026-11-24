package ru.vtb.core.models.kafka.batm_zod_in_batm_acc_route_s3_load_result;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmZodInBatmAccRouteS3LoadResultRootMessage {

    @JsonProperty("originalMessageId")
    private String originalMessageId;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("status")
    private String status;
}