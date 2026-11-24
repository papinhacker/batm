package ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_trans_pp_result;

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
public class BatmAccRoutePpOutBatmTransPpResultRootMessage implements Jsonable {

    @JsonProperty("transferDatePpks")
    private String transferDatePpks;

    @JsonProperty("transferDateGp")
    private String transferDateGp;

    @JsonProperty("gpDate")
    private String gpDate;

    @JsonProperty("originalMessageId")
    private String originalMessageId;

    @JsonProperty("creditProductOperationId")
    private String creditProductOperationId;

    @JsonProperty("debitProductOperationId")
    private String debitProductOperationId;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("errorDescription")
    private String errorDescription;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("emdId")
    private String emdId;
}