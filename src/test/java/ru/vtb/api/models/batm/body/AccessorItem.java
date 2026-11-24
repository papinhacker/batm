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
public class AccessorItem {

    @JsonProperty("accessorTypeRef")
    private AccessorTypeRef accessorTypeRef;

    @JsonProperty("addAttributes")
    private AddAttributes addAttributes;

    @JsonProperty("classifiers")
    private Classifiers classifiers;

    @JsonProperty("number")
    private String number;
}