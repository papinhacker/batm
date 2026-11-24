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
public class Protocol {

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;
}