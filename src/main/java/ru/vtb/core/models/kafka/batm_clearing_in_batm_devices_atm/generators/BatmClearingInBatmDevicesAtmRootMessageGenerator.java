package ru.vtb.core.models.kafka.batm_clearing_in_batm_devices_atm.generators;

import ru.vtb.core.models.kafka.batm_clearing_in_batm_devices_atm.BatmClearingInBatmDevicesAtmRootMessage;

public class BatmClearingInBatmDevicesAtmRootMessageGenerator {

    public static BatmClearingInBatmDevicesAtmRootMessage generate(String atmId, String locationType) {
        return BatmClearingInBatmDevicesAtmRootMessage.builder()
                .atmId(atmId)
                .atmExploitationStatus("operating")
                .locationType(locationType)
                .build();
    }

    public static BatmClearingInBatmDevicesAtmRootMessage generate(String atmId) {
        return generate(atmId, "Y");
    }
}
