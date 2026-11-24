package ru.vtb.api.models.batm.header;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncludeItem {

    @JsonProperty("data")
    private String data;

    @JsonProperty("include")
    private boolean include;
}