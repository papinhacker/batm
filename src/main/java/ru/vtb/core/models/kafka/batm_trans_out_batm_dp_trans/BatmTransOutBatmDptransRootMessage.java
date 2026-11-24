package ru.vtb.core.models.kafka.batm_trans_out_batm_dp_trans;

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
public class BatmTransOutBatmDptransRootMessage implements Jsonable {

    @JsonProperty("vkusCode")
    private String vkusCode;

    @JsonProperty("dateCreation")
    private String dateCreation;

    @JsonProperty("typeOperation")
    private String typeOperation;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("appointment")
    private String appointment;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("operdayDate")
    private String operdayDate;

    @JsonProperty("setSorting")
    private String setSorting;

    @JsonProperty("service")
    private String service;

    @JsonProperty("dateUpdate")
    private String dateUpdate;

    @JsonProperty("instructionId")
    private String instructionId;

    @JsonProperty("debit")
    private Debit debit;

    @JsonProperty("credit")
    private Credit credit;

    @JsonProperty("status")
    private String status;

    @JsonProperty("dateExecution")
    private String dateExecution;
}