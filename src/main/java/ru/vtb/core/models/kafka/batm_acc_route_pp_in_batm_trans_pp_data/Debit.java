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
public class Debit implements Jsonable {

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("index")
    private Integer index;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("amountInCur")
    private Object amountInCur;

    @JsonProperty("ppDirection")
    private String ppDirection;

    @JsonProperty("account")
    private String account;
}