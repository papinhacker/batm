package ru.vtb.core.models.kafka.batm_acc_route_in_batm_trans_pp_data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import ru.vtb.core.annotations.Jsonable;
import ru.vtb.core.utils.CommonHelper;

import java.util.Comparator;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmAccRouteInBatmTransPpDataRootMessage implements Comparable<BatmAccRouteInBatmTransPpDataRootMessage>, Jsonable {

    @JsonProperty("vkusCode")
    private String vkusCode;

    @JsonProperty("setSorting")
    private Integer setSorting;

    @JsonProperty("service")
    private String service;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("description")
    private String description;

    @JsonProperty("messageDate")
    private String messageDate;

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

    @Override
    public int compareTo(@NotNull BatmAccRouteInBatmTransPpDataRootMessage o) {
        return Comparator
                .comparing(BatmAccRouteInBatmTransPpDataRootMessage::getTransactionId)
                .thenComparing(BatmAccRouteInBatmTransPpDataRootMessage::getVkusCode)
                .compare(this, o);
    }

    @Override
    public String toString() {
        return CommonHelper.serializeToJsonString(this);
    }
}