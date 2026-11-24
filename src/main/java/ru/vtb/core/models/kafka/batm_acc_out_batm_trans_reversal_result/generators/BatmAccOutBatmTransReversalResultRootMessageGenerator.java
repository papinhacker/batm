package ru.vtb.core.models.kafka.batm_acc_out_batm_trans_reversal_result.generators;

import ru.vtb.core.models.kafka.batm_acc_out_batm_trans_reversal_result.BatmAccOutBatmTransReversalResultRootMessage;

import java.util.Map;

public class BatmAccOutBatmTransReversalResultRootMessageGenerator {

    public static BatmAccOutBatmTransReversalResultRootMessage generateFromDbSource(Map<String, Object> dbData, String msgId) {
        return BatmAccOutBatmTransReversalResultRootMessage.builder()
                .originalMessageId(msgId)
                .atmId(dbData.get("atm_id").toString())
                .productInstanceId(dbData.get("product_instance_id").toString())
                .branchCode(dbData.get("branch_code").toString())
                .account(dbData.get("account_number").toString())
                .registerId(dbData.get("register_id").toString())
                .registerType(dbData.get("register_type").toString())
                .accountId(dbData.get("account_id").toString())
                .build();
    }
}
