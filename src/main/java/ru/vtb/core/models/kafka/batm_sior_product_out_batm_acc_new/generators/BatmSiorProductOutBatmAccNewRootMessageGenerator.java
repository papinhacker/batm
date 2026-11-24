package ru.vtb.core.models.kafka.batm_sior_product_out_batm_acc_new.generators;

import org.apache.commons.lang3.RandomStringUtils;
import ru.vtb.core.models.common.CommonModelHelper;
import ru.vtb.core.models.kafka.batm_sior_product_out_batm_acc_new.BatmSiorProductOutBatmAccNewRootMessage;
import ru.vtb.core.utils.AccountsHelper;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

import java.util.UUID;

public class BatmSiorProductOutBatmAccNewRootMessageGenerator {

    public static BatmSiorProductOutBatmAccNewRootMessage generate() {
        return BatmSiorProductOutBatmAccNewRootMessage.builder()
                .messageId(CommonModelHelper.generateRandomMessageId())
                .atmId(RandomStringUtils.insecure().nextNumeric(8))
                .productInstanceId(UUID.randomUUID().toString())
                .productInstanceType("CashMachine")
                .branchCode(DBHelper.TransAgrSrv.getBranchCodeByAbsBaseBranch(
                        DBHelper.Devices.getRandomFullAbsBranchCode()))
                .productInstanceStatus("Active")
                .registerId(UUID.randomUUID().toString())
                .registerType("20208_USBanka")
                .accountId(UUID.randomUUID().toString())
                .openAt(CommonHelper.getCurrentDateInFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"))
                .accountNumber(AccountsHelper.generateRandomAccountNumber("810"))
                .currency("RUB")
                .statusProductInstanceAccountId("Active")
                .build();
    }
}
