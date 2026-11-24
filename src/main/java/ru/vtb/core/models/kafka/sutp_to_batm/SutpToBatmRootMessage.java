package ru.vtb.core.models.kafka.sutp_to_batm;

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
public class SutpToBatmRootMessage {

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("operdayDate")
    private String operdayDate;

    @JsonProperty("sessionNumber")
    private String sessionNumber;

    @JsonProperty("systemName")
    private String systemName;

    @JsonProperty("systemCode")
    private String systemCode;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("eventName")
    private String eventName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("parameters")
    private List<ParametersItem> parameters;

    @JsonProperty("eventDate")
    private String eventDate;
}