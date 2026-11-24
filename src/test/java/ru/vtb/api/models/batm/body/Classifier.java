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
public class Classifier {

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("entityRef")
    private EntityRef entityRef;

    @JsonProperty("typeRef")
    private TypeRef typeRef;

    @JsonProperty("validFrom")
    private String validFrom;

    @JsonProperty("valueRef")
    private ValueRef valueRef;
}