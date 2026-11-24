package ru.vtb.api.models.batm.body;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parameters {

    @JsonProperty("accessorTypeRef")
    private AccessorTypeRef accessorTypeRef;

    @JsonProperty("code")
    private String code;

    @JsonProperty("number")
    private String number;
}