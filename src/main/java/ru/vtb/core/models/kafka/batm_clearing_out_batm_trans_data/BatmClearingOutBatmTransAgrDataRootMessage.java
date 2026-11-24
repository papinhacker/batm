package ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmClearingOutBatmTransAgrDataRootMessage {

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("transactions")
    private List<TransactionsItem> transactions;
}