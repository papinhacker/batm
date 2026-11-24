package ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_oper_sior_pp_result;

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
public class BatmAccRoutePpInBatmOperSiorPpResultRootJson implements Jsonable {

    @JsonProperty("emdId")
    private String emdId;

    @JsonProperty("creditProductOperationId")
    private String creditProductOperationId;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("messageDate")
    private String messageDate;

    @JsonProperty("debitProductOperationId")
    private String debitProductOperationId;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("errorDescription")
    private String errorDescription;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("errorSource")
    private String errorSource;

    @JsonProperty("transferDatePpks")
    private String transferDatePpks;
}