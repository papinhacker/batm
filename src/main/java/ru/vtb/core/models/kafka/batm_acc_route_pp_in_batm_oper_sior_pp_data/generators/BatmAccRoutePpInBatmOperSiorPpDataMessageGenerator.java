package ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_oper_sior_pp_data.generators;

import ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_oper_sior_pp_data.BatmAccRoutePpInBatmOperSiorPpDataRootJson;
import ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_oper_sior_pp_data.Credit;
import ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_oper_sior_pp_data.Debit;
import ru.vtb.core.models.kafka.batm_in_siorus_instruction.BatmInSiorusInstructionRootMessage;

public class BatmAccRoutePpInBatmOperSiorPpDataMessageGenerator {

    public static BatmAccRoutePpInBatmOperSiorPpDataRootJson generate(BatmInSiorusInstructionRootMessage original) {
        var builder = BatmAccRoutePpInBatmOperSiorPpDataRootJson.builder()
                .messageId(original.getMessageId())
//                .messageDate(original.getMessageDate())
                .service("batm_operations_sior")
                .operationDay(original.getOperationDay())
//                .productInstrId(original.getProductInstrId())
                .vkusCode(original.getVkusCode())
                .setSorting(original.getSetSorting())
                .equivalentFlag(original.getEquivalentFlag())
                .eventType(original.getEventType());
        Debit debit = null;
        Credit credit = null;
        int index = 0;
        if (original.getIsDebitMigrated()) {
            debit = Debit.builder()
                    .productRegisterId(original.getDebitProductRegId())
                    .ppDirection(original.getDirectionDt())
                    .productRegisterType(original.getRegisterTypeDt())
                    .branchCode(original.getDebitBranch())
                    .currency(original.getDebitCurrencyLat3())
                    .amountInCur(original.getDebitAmount())
                    .index(++index)
                    .build();
        }
        if (original.getIsCreditMigrated()) {
            credit = Credit.builder()
                    .productRegisterId(original.getCreditProductRegId())
                    .ppDirection(original.getDirectionKt())
                    .productRegisterType(original.getRegisterTypeKt())
                    .branchCode(original.getCreditBranch())
                    .currency(original.getCreditCurrencyLat3())
                    .amountInCur(original.getCreditAmount())
                    .index(++index)
                    .build();
        }
        return builder
                .debit(debit)
                .credit(credit)
                .build();
    }
}
