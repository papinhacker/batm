package ru.vtb.core.models.kafka.batm_acc_out_batm_trans_reversal_result;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmAccOutBatmTransReversalResultRootMessage {

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("registerId")
    private String registerId;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("productInstanceId")
    private String productInstanceId;

    @JsonProperty("originalMessageId")
    private String originalMessageId;

    @JsonProperty("registerType")
    private String registerType;

    @JsonProperty("account")
    private String account;
}