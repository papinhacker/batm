package ru.vtb.core.models.kafka.batm_dp_in_batm_devices_atm.generators;

import ru.vtb.core.models.kafka.batm_dp_in_batm_devices_atm.BatmDpInBatmDevicesAtmRootMessage;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.BatmInSiorusDevicesRootMessage;

public class BatmDpInBatmDevicesAtmRootMessageGenerator {

    public static BatmDpInBatmDevicesAtmRootMessage generate(BatmInSiorusDevicesRootMessage from) {
        var b = from.getBody();
        return BatmDpInBatmDevicesAtmRootMessage.builder()
                .id(b.getId())
                .model(b.getModel())
                .serialNumber(b.getSerialNumber())
                .address(b.getAddress())
                .okatoCode(b.getOkatoCode())
                .status(b.getAtmExploitationStatus())
                .type(b.getFeature())
                .vendor(b.getVendor())
                .build();
    }
}
