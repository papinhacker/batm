package ru.vtb.core.models.kafka.batm_sior_product_out_batm_acc_new;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmSiorProductOutBatmAccNewRootMessage {

    @JsonProperty("productInstanceId")
    private String productInstanceId;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("statusProductInstanceAccountId")
    private String statusProductInstanceAccountId;

    @JsonProperty("registerType")
    private String registerType;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("productInstanceStatus")
    private String productInstanceStatus;

    @JsonProperty("registerId")
    private String registerId;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("productInstanceType")
    private String productInstanceType;

    @JsonProperty("openAt")
    private String openAt;
}