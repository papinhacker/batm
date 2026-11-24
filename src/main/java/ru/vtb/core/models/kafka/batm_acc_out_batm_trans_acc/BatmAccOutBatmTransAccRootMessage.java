package ru.vtb.core.models.kafka.batm_acc_out_batm_trans_acc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmAccOutBatmTransAccRootMessage {

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("deviceType")
    private String deviceType;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("account")
    private String account;
}