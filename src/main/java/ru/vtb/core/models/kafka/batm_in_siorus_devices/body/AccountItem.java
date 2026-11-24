package ru.vtb.core.models.kafka.batm_in_siorus_devices.body;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountItem {

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("number")
    private String number;
}