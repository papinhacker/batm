package ru.vtb.core.models.kafka.batm_acc_in_batm_devices_acc.generators;

import org.apache.commons.lang3.RandomStringUtils;
import ru.vtb.core.models.common.CommonModelHelper;
import ru.vtb.core.models.kafka.batm_acc_in_batm_devices_acc.AccountsItem;
import ru.vtb.core.models.kafka.batm_acc_in_batm_devices_acc.BatmAccInBatmDevicesAccRootMessage;
import ru.vtb.core.utils.AccountsHelper;
import ru.vtb.core.utils.DBHelper;

import java.util.List;

public class BatmAccInBatmDevicesAccMessageGenerator {

    public static BatmAccInBatmDevicesAccRootMessage generate() {
//        String currency = List.of("840", "643", "978").get(ThreadLocalRandom.current().nextInt(3));
        String currency = "643";

        return BatmAccInBatmDevicesAccRootMessage.builder()
                .messageId(CommonModelHelper.generateRandomMessageId())
                .atmStatus("operating")
                .atmId(RandomStringUtils.insecure().nextNumeric(8))
                .atmType("atm")
                .absBaseBranch(DBHelper.Devices.getRandomFullAbsBranchCode())
                .accounts(List.of(AccountsItem.builder()
                        .currency(currency)
                        .number(AccountsHelper.generateRandomAccountNumber("810"))
                        .build()))
                .build();
    }
}
