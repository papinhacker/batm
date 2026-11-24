package ru.vtb.core.models.kafka.batm_acc_out_batm_trans_acc.generators;

import ru.vtb.core.models.kafka.batm_acc_in_batm_devices_acc.BatmAccInBatmDevicesAccRootMessage;
import ru.vtb.core.models.kafka.batm_acc_out_batm_trans_acc.BatmAccOutBatmTransAccRootMessage;
import ru.vtb.core.utils.DBHelper;

public class BatmAccOutBatmTransAccRootMessageGenerator {

    public static BatmAccOutBatmTransAccRootMessage convertFromBatmAccInBatmDevicesAccRootMessage
            (BatmAccInBatmDevicesAccRootMessage original) {
        return BatmAccOutBatmTransAccRootMessage.builder()
                .messageId(original.getMessageId())
                .atmId(original.getAtmId())
                .branchCode(DBHelper.TransAgrSrv.getBranchCodeByAbsBaseBranch(original.getAbsBaseBranch()))
                .account(original.getAccounts().get(0).getNumber())
                .currency(original.getAccounts().get(0).getCurrency())
                .deviceType(original.getAtmType())
                .build();
    }
}
