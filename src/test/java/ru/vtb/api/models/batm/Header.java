package ru.vtb.api.models.batm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vtb.api.models.batm.header.Originator;
import ru.vtb.api.models.batm.header.Protocol;
import ru.vtb.api.models.batm.header.ResponseParams;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Header {

    @JsonProperty("messageDate")
    private String messageDate;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("originator")
    private Originator originator;

    @JsonProperty("protocol")
    private Protocol protocol;

    @JsonProperty("responseParams")
    private ResponseParams responseParams;
}