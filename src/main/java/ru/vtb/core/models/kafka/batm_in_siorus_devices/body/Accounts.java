package ru.vtb.core.models.kafka.batm_in_siorus_devices.body;

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
public class Accounts {

    @JsonProperty("account")
    private List<AccountItem> account;
}