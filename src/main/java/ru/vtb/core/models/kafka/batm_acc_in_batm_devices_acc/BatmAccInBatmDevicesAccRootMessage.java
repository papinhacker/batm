package ru.vtb.core.models.kafka.batm_acc_in_batm_devices_acc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vtb.core.annotations.Jsonable;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmAccInBatmDevicesAccRootMessage implements Jsonable {

    @JsonProperty("atmType")
    private String atmType;

    @JsonProperty("absBaseBranch")
    private String absBaseBranch;

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("accounts")
    private List<AccountsItem> accounts;

    @JsonProperty("atmStatus")
    private String atmStatus;
}