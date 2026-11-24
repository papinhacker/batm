package ru.vtb.core.models.kafka.batm_in_siorus_instruction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vtb.core.annotations.Jsonable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmInSiorusInstructionRootMessage implements Jsonable {

    @JsonProperty("vkusCode")
    private String vkusCode;

    @JsonProperty("debitAccount")
    private String debitAccount;

    @JsonProperty("registerTypeKt")
    private String registerTypeKt;

    @JsonProperty("isDebitMigrated")
    private Boolean isDebitMigrated;

    @JsonProperty("debitMdmId")
    private Long debitMdmId;

    @JsonProperty("isCreditMigrated")
    private Boolean isCreditMigrated;

    @JsonProperty("productInstrId")
    private String productInstrId;

    @JsonProperty("setSorting")
    private String setSorting;

    @JsonProperty("directionDt")
    private String directionDt;

    @JsonProperty("creditProductRegId")
    private String creditProductRegId;

    @JsonProperty("creditProductInstanceId")
    private String creditProductInstanceId;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("equivalentFlag")
    private Integer equivalentFlag;

    @JsonProperty("operationDay")
    private String operationDay;

    @JsonProperty("stornoTransactionId")
    private String stornoTransactionId;

    @JsonProperty("debitCurrencyLat3")
    private String debitCurrencyLat3;

    @JsonProperty("creditMdmId")
    private Long creditMdmId;

    @JsonProperty("registerTypeDt")
    private String registerTypeDt;

    @JsonProperty("creditAccount")
    private String creditAccount;

    @JsonProperty("creditCurrencyLat3")
    private String creditCurrencyLat3;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("debitAccountId")
    private String debitAccountId;

    @JsonProperty("debitBranch")
    private String debitBranch;

    @JsonProperty("debitAmount")
    private Double debitAmount;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("valueDate")
    private String valueDate;

    @JsonProperty("debitProductRegId")
    private String debitProductRegId;

    @JsonProperty("debitProductInstanceId")
    private String debitProductInstanceId;

    @JsonProperty("cbo")
    private String cbo;

    @JsonProperty("directionKt")
    private String directionKt;

    @JsonProperty("creditBranch")
    private String creditBranch;

    @JsonProperty("messageDate")
    private String messageDate;

    @JsonProperty("creditAccountId")
    private String creditAccountId;

    @JsonProperty("paymentPurpose")
    private String paymentPurpose;

    @JsonProperty("creditAmount")
    private Double creditAmount;

    @JsonProperty("tbiCode")
    private String tbiCode;
}