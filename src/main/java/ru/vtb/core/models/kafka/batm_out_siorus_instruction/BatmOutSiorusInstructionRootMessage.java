package ru.vtb.core.models.kafka.batm_out_siorus_instruction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmOutSiorusInstructionRootMessage {

    @JsonProperty("sendingStatus")
    private Boolean sendingStatus;

    @JsonProperty("emdId")
    private String emdId;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("transactionId")
    private String transactionId;
}