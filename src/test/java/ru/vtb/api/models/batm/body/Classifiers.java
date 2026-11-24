package ru.vtb.api.models.batm.body;

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
public class Classifiers {

    @JsonProperty("classifier")
    private List<ClassifierItem> classifier;
}