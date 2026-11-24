package ru.vtb.core.models.kafka.batm_clearing_in_batm_devices_atm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vtb.core.annotations.Jsonable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatmClearingInBatmDevicesAtmRootMessage implements Jsonable {

    @JsonProperty("atmId")
    private String atmId;

    @JsonProperty("atmExploitationStatus")
    private String atmExploitationStatus;

    @JsonProperty("locationType")
    private String locationType;
}