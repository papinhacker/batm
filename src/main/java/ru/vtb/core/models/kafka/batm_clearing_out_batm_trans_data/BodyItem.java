package ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BodyItem {

    @JsonProperty("transDate")
    private String transDate;

    @JsonProperty("transType")
    private String transType;

    @JsonProperty("bankProductType")
    private String bankProductType;

    @JsonProperty("arn")
    private String arn;

    @JsonProperty("nameFile")
    private String nameFile;

    @JsonProperty("authCode")
    private String authCode;

    @JsonProperty("rrn")
    private String rrn;

    @JsonProperty("trn")
    private String trn;

    @JsonProperty("sic")
    private String sic;

    @JsonProperty("country")
    private String country;

    @JsonProperty("city")
    private String city;

    @JsonProperty("merchantName")
    private String merchantName;

    @JsonProperty("merchantID")
    private String merchantID;

    @JsonProperty("cardNumber")
    private String cardNumber;

    @JsonProperty("cardType")
    private String cardType;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("cardExpiry")
    private String cardExpiry;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("amountFee")
    private Double amountFee;

    @JsonProperty("atmMigration")
    private Boolean atmMigration;
}