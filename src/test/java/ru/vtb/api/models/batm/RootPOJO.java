package ru.vtb.api.models.batm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RootPOJO {

    @JsonProperty("header")
    private Header header;

    @JsonProperty("body")
    private Body body;
}