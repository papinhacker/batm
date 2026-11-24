package ru.vtb.core.models.kafka.batm_acc_route_s3_in_batm_trans_agr_data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionsItem implements Comparable<TransactionsItem> {

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("operationCodeType")
    private String operationCodeType;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("streamType")
    private Integer streamType;

    @JsonProperty("glCode")
    private String glCode;

    @JsonProperty("aggregationId")
    private String aggregationId;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("operationDay")
    private String operationDay;

    @JsonProperty("account")
    private String account;

    @Override
    public int compareTo(@NotNull TransactionsItem o) {
        return Comparator.comparing(TransactionsItem::getAggregationId, Comparator.nullsFirst(String::compareTo))
                .thenComparing(TransactionsItem::getAccount)
                .compare(this, o);
    }
}