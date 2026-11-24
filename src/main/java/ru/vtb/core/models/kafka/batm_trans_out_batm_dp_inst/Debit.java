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
public class Debit implements Jsonable {

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("amount")
    private Object amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("account")
    private String account;
}