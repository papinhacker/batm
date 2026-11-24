package ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_trans_pp_data;

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
public class BatmAccRoutePpInBatmTransPpDataRootMessage implements Jsonable {

    @JsonProperty("vkusCode")
    private String vkusCode;

    @JsonProperty("setSorting")
    private String setSorting;

    @JsonProperty("service")
    private String service;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("description")
    private String description;

    @JsonProperty("productInstrId")
    private String productInstrId;

    @JsonProperty("equivalentFlag")
    private Integer equivalentFlag;

    @JsonProperty("debit")
    private Debit debit;

    @JsonProperty("credit")
    private Credit credit;

    @JsonProperty("operationDay")
    private String operationDay;

    @JsonProperty("transactionId")
    private String transactionId;
}