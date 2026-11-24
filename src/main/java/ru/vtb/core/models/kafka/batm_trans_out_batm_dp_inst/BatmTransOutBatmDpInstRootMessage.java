package ru.vtb.core.models.kafka.batm_trans_out_batm_dp_inst;

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
public class BatmTransOutBatmDpInstRootMessage implements Jsonable {

    @JsonProperty("dateCreation")
    private String dateCreation;

    @JsonProperty("authCode")
    private String authCode;

    @JsonProperty("routeGp")
    private Boolean routeGp;

    @JsonProperty("transId")
    private String transId;

    @JsonProperty("typeOperation")
    private String typeOperation;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("appointment")
    private String appointment;

    @JsonProperty("typeCreation")
    private String typeCreation;

    @JsonProperty("rrn")
    private String rrn;

    @JsonProperty("operdayDate")
    private String operdayDate;

    @JsonProperty("service")
    private String service;

    @JsonProperty("dateUpdate")
    private String dateUpdate;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("instructionId")
    private String instructionId;

    @JsonProperty("clearingFileName")
    private String clearingFileName;

    @JsonProperty("debit")
    private Debit debit;

    @JsonProperty("credit")
    private Credit credit;

    @JsonProperty("tbiCode")
    private String tbiCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("dateExecution")
    private String dateExecution;
}