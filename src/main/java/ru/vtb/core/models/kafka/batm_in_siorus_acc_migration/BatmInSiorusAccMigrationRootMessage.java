package ru.vtb.core.models.kafka.batm_in_siorus_acc_migration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmInSiorusAccMigrationRootMessage {

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("absCode")
    private String absCode;

    @JsonProperty("productInstanceStatus")
    private Object productInstanceStatus;

    @JsonProperty("registerId")
    private String registerId;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("productInstanceId")
    private String productInstanceId;

    @JsonProperty("registerStatus")
    private String registerStatus;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("registerType")
    private String registerType;

    @JsonProperty("productInstanceType")
    private Object productInstanceType;

    @JsonProperty("accountNumber")
    private String accountNumber;
}