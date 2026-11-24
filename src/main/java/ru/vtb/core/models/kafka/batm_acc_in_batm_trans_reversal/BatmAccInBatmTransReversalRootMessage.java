package ru.vtb.core.models.kafka.batm_acc_in_batm_trans_reversal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmAccInBatmTransReversalRootMessage {

    @JsonProperty("transDate")
    private String transDate;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("currency")
    private String currency;
}