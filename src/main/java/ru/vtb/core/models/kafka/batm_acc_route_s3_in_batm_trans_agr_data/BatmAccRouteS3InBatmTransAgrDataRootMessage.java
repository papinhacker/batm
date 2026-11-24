package ru.vtb.core.models.kafka.batm_acc_route_s3_in_batm_trans_agr_data;

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
public class BatmAccRouteS3InBatmTransAgrDataRootMessage {

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("transactions")
    private List<TransactionsItem> transactions;
}