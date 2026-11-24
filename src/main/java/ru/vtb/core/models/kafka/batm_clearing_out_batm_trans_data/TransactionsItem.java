package ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data;

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
public class TransactionsItem implements Jsonable {

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("amountFee")
    private Double amountFee;

    @JsonProperty("authCode")
    private String authCode;

    @JsonProperty("nameFile")
    private String nameFile;

    @JsonProperty("mcc")
    private String mcc;

    @JsonProperty("rrn")
    private String rrn;

    @JsonProperty("binRange")
    private String binRange;

    @JsonProperty("transType")
    private String transType;

    @JsonProperty("transDate")
    private String transDate;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("id")
    private String id;

    @JsonProperty("bankProductType")
    private String bankProductType;

    @JsonProperty("arn")
    private String arn;

    @JsonProperty("atmMigration")
    private Boolean atmMigration;
}