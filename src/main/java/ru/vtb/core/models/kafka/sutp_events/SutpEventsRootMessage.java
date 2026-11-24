package ru.vtb.core.models.kafka.sutp_events;

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
public class SutpEventsRootMessage {

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("description")
    private String description;

    @JsonProperty("referenceId")
    private String referenceId;

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("statusDescription")
    private String statusDescription;

    @JsonProperty("operdayDate")
    private String operdayDate;

    @JsonProperty("sessionNumber")
    private String sessionNumber;

    @JsonProperty("systemName")
    private String systemName;

    @JsonProperty("systemCode")
    private String systemCode;

    @JsonProperty("eventName")
    private String eventName;

    @JsonProperty("parameters")
    private List<ParametersItem> parameters;

    @JsonProperty("eventDate")
    private String eventDate;

    @JsonProperty("statusCode")
    private Integer statusCode;
}