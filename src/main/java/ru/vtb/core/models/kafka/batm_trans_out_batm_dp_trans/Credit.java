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
public class Credit implements Jsonable {

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("productInstanceId")
    private String productInstanceId;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("index")
    private Integer index;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("registerType")
    private String registerType;

    @JsonProperty("direction")
    private String direction;

    @JsonProperty("registerId")
    private String registerId;

    @JsonProperty("productOperationId")
    private String productOperationId;
}