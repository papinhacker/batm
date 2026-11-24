package ru.vtb.core.models.kafka.batm_report_in_batm_trans_agr_transaction;

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
public class BatmReportInBatmTransAgrTransactionRootMessage implements Jsonable {

    @JsonProperty("authCode")
    private String authCode;

    @JsonProperty("debitAccountNumber")
    private String debitAccountNumber;

    @JsonProperty("transId")
    private String transId;

    @JsonProperty("amountClearing")
    private Object amountClearing;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("debitAmount")
    private Object debitAmount;

    @JsonProperty("creditAccountNumber")
    private String creditAccountNumber;

    @JsonProperty("rrn")
    private String rrn;

    @JsonProperty("mps")
    private String mps;

    @JsonProperty("system")
    private String system;

    @JsonProperty("transType")
    private String transType;

    @JsonProperty("creditBranchCode")
    private String creditBranchCode;

    @JsonProperty("debitBranchCode")
    private String debitBranchCode;

    @JsonProperty("transDate")
    private String transDate;

    @JsonProperty("operDay")
    private String operDay;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("bankProductType")
    private String bankProductType;

    @JsonProperty("creditAmount")
    private Object creditAmount;

    @JsonProperty("nameFileClearing")
    private String nameFileClearing;

    @JsonProperty("status")
    private String status;

    @JsonProperty("emdId")
    private String emdId;

    @JsonProperty("gpDate")
    private String gpDate;

    @JsonProperty("amountGp")
    private Double amountGp;

    @JsonProperty("gpStatus")
    private String gpStatus;

    @JsonProperty("transferDateGp")
    private String transferDateGp;

    @JsonProperty("ppksStatus")
    private String ppksStatus;

    @JsonProperty("transferDatePpks")
    private String transferDatePpks;
}