package ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Header {

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("messageDate")
    private String messageDate;

    @JsonProperty("version")
    private String version;
}