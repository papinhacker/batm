package ru.vtb.core.models.kafka.batm_acc_route_in_batm_trans_pp_data.gerators;

import ru.vtb.core.data.static_data.B2LMask;
import ru.vtb.core.data.static_data.SemiTransaction;
import ru.vtb.core.models.kafka.batm_acc_route_in_batm_trans_pp_data.BatmAccRouteInBatmTransPpDataRootMessage;
import ru.vtb.core.models.kafka.batm_acc_route_in_batm_trans_pp_data.Credit;
import ru.vtb.core.models.kafka.batm_acc_route_in_batm_trans_pp_data.Debit;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.BatmClearingOutBatmTransAgrDataRootMessage;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BatmAccRouteInBatmTransPpDataMessageGenerator {

    private static final Map<String, String> VKUS_CODES = DBHelper.TransAgr.getVkusCodes();

    public static List<BatmAccRouteInBatmTransPpDataRootMessage> generateBatmAccRouteInBatmTransPpDataMessages(BatmClearingOutBatmTransAgrDataRootMessage input) {
        List<BatmAccRouteInBatmTransPpDataRootMessage> result = new ArrayList<>();
        var randBody = CommonHelper.getRandomFromList(input.getTransactions());
        String randAtmId = randBody.getAtmId();
        String fileMask = B2LMask.recognize(randBody.getNameFile()).getName();

        var productRegisters = DBHelper.TransAgr.getAccountRegisterIdAndType(randAtmId);
        String productRegisterId = productRegisters.get("account_register").toString();
        String productRegisterType = productRegisters.get("account_register_type").toString();
        String branchCode = DBHelper.TransAgr.getBranchAndBaseBranchCodes(randAtmId).get("branch_2").toString();

        for (var body : input.getTransactions()) {
            Debit debit = null;
            Credit credit = null;

            var metadataIds = DBHelper.TransAgr.getProductInstrIdAndTransId(
                    body.getNameFile(),
                    body.getTransType(),
                    body.getRrn());
            String transType = body.getTransType();
            String nzrAccNum = SemiTransaction.getNzrAccNum(fileMask, transType, body.getAtmId());
            String desc = getToGenerTransDesc(fileMask, transType, body.getAtmId());

            String curr = "RUB";//body.getCurrency();
            String amount = body.getAmount().toString();
            String creditBranch = "97";
            switch (transType) {
                case "Cash-Withdrawal", "Reversal-Cash-In" -> {
                    debit = Debit.builder()
                            .accountNumber(nzrAccNum)
                            .branchCode(branchCode)
                            .currency(curr)
                            .amountInCur(amount)
                            .build();
                    credit = Credit.builder()
                            .productRegisterId(productRegisterId)
                            .ppDirection("Decrease")
                            .productRegisterType(productRegisterType)
                            .branchCode(creditBranch)
                            .currency(curr)
                            .amountInCur(amount)
                            .build();
                }
                case "Reversal-Cash-Withdrawal", "Cash-In" -> {
                    debit = Debit.builder()
                            .productRegisterId(productRegisterId)
                            .ppDirection("Increase")
                            .productRegisterType(productRegisterType)
                            .branchCode(branchCode)
                            .currency(curr)
                            .amountInCur(amount)
                            .build();
                    credit = Credit.builder()
                            .accountNumber(nzrAccNum)
                            .branchCode(creditBranch)
                            .currency(curr)
                            .amountInCur(amount)
                            .build();
                }
            }

            result.add(BatmAccRouteInBatmTransPpDataRootMessage.builder()
                    .service("batm_transactions_agr")
                    .operationDay(body.getTransDate().split("T")[0])
                    .productInstrId(metadataIds.get("product_instr_id").toString())
                    .transactionId(metadataIds.get("trans_id").toString())
                    .vkusCode(VKUS_CODES.get(fileMask + transType))
                    .setSorting(1)
                    .equivalentFlag(1)
                    .description(desc)
                    .debit(debit)
                    .credit(credit)
                    .build());
        }

        return result;
    }

    private static String getToGenerTransDesc(String fileMask, String transType, String atmId) {
        if (fileMask.startsWith("CORP")) {
            return DBHelper.TransAgr.getTransactionDescriptionForCorpCards(fileMask, transType);
        } else return DBHelper.TransAgr.getTransactionDescription(fileMask, transType, atmId);
    }
}
