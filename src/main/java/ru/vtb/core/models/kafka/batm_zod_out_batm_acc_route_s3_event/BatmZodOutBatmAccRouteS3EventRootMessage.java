package ru.vtb.core.models.kafka.batm_zod_out_batm_acc_route_s3_event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmZodOutBatmAccRouteS3EventRootMessage {

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("operdayDate")
    private String operdayDate;

    @JsonProperty("systemName")
    private String systemName;

    @JsonProperty("sessionNumber")
    private String sessionNumber;

    @JsonProperty("systemCode")
    private String systemCode;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("eventName")
    private String eventName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("parameters")
    private List<ParametersItem> parameters;

    @JsonProperty("eventDate")
    private String eventDate;
}