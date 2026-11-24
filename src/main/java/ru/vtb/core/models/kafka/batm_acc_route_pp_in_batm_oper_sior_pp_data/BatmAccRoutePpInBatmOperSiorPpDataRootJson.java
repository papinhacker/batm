package ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_oper_sior_pp_data;

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
public class BatmAccRoutePpInBatmOperSiorPpDataRootJson implements Jsonable {

    @JsonProperty("vkusCode")
    private String vkusCode;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("description")
    private String description;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("productInstrId")
    private String productInstrId;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("setSorting")
    private String setSorting;

    @JsonProperty("service")
    private String service;

    @JsonProperty("messageDate")
    private String messageDate;

    @JsonProperty("equivalentFlag")
    private Integer equivalentFlag;

    @JsonProperty("debit")
    private Debit debit;

    @JsonProperty("credit")
    private Credit credit;

    @JsonProperty("operationDay")
    private String operationDay;
}