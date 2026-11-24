package ru.vtb.core.data.static_data;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.vtb.core.models.kafka.batm_acc_route_s3_in_batm_trans_agr_data.BatmAccRouteS3InBatmTransAgrDataRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.BatmClearingOutBatmTransAgrDataRootMessage;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
@Builder
public class SemiTransaction implements Comparable<SemiTransaction> {

    private final String accountNumber;
    private final String aggregationId;
    private final String operationCodeType;
    private final String glCode;
    private final Integer streamType;

    public static List<SemiTransaction> generateSemiTransactions(BatmClearingOutBatmTransAgrDataRootMessage obj) {
        List<SemiTransaction> result = new ArrayList<>();
        var randBody = CommonHelper.getRandomFromList(obj.getTransactions());
        String b2lFilename = randBody.getNameFile();
        String randAtmId = randBody.getAtmId();
        boolean areTwoStreams = !(DBHelper.TransAgr.getTerminalIdsToAggregator(false, false).contains(randAtmId) && b2lFilename.startsWith("298cto"));
        boolean isCorp = b2lFilename.startsWith("CORP");
        String aggIdPrefix = isCorp || !areTwoStreams ? "WAY4" : "BATM";
        String fileMask = B2LMask.recognize(b2lFilename).getName();
        for (var body : obj.getTransactions()) {
            String aggKey = body.getRrn() + body.getAuthCode();
            String glCodePostfix = isCorp ? "bc" : Bin.getGlCodePostFixByCardNumber(body.getBinRange());
            glCodePostfix = b2lFilename.startsWith("D_038cto") ? glCodePostfix.toUpperCase() : glCodePostfix.toLowerCase();
            String transType = body.getTransType();

            switch (transType) {
                case "Cash-In" -> {
                    result.add(SemiTransaction.builder()
                            .accountNumber("20208")
                            .aggregationId(aggIdPrefix + "_P_" + aggKey)
                            .operationCodeType("W4_VTB_DEB")
                            .glCode("Y1AT" + glCodePostfix)
                            .streamType(1)
                            .build());
                    if (areTwoStreams) {
                        result.add(SemiTransaction.builder()
                                .accountNumber(getNzrAccNum(fileMask, transType, body.getAtmId()))
                                .aggregationId(aggIdPrefix + "_P_" + aggKey)
                                .operationCodeType("W4_VTB_CRD")
                                .glCode("Y1AT" + glCodePostfix)
                                .streamType(2)
                                .build());
                    }
                }
                case "Reversal-Cash-In" -> {
                    result.add(SemiTransaction.builder()
                            .accountNumber("20208")
                            .aggregationId(aggIdPrefix + "_R_" + aggKey)
                            .operationCodeType("W4_VTB_CRD")
                            .glCode("y1AT" + glCodePostfix)
                            .streamType(1)
                            .build());
                    if (areTwoStreams) {
                        result.add(SemiTransaction.builder()
                                .accountNumber(getNzrAccNum(fileMask, transType, body.getAtmId()))
                                .aggregationId(aggIdPrefix + "_R_" + aggKey)
                                .operationCodeType("W4_VTB_DEB")
                                .glCode("y1AT" + glCodePostfix)
                                .streamType(2)
                                .build());
                    }
                }
                case "Cash-Withdrawal" -> {
                    result.add(SemiTransaction.builder()
                            .accountNumber("20208")
                            .aggregationId(aggIdPrefix + "_P_" + aggKey)
                            .operationCodeType("W4_VTB_CRD")
                            .glCode("A1AT" + glCodePostfix)
                            .streamType(1)
                            .build());
                    if (areTwoStreams) {
                        result.add(SemiTransaction.builder()
                                .accountNumber(getNzrAccNum(fileMask, transType, body.getAtmId()))
                                .aggregationId(aggIdPrefix + "_P_" + aggKey)
                                .operationCodeType("W4_VTB_DEB")
                                .glCode("A1AT" + glCodePostfix)
                                .streamType(2)
                                .build());
                    }
                }
                case "Reversal-Cash-Withdrawal" -> {
                    result.add(SemiTransaction.builder()
                            .accountNumber("20208")
                            .aggregationId(aggIdPrefix + "_R_" + aggKey)
                            .operationCodeType("W4_VTB_DEB")
                            .glCode("a1AT" + glCodePostfix)
                            .streamType(1)
                            .build());
                    if (areTwoStreams) {
                        result.add(SemiTransaction.builder()
                                .accountNumber(getNzrAccNum(fileMask, transType, body.getAtmId()))
                                .aggregationId(aggIdPrefix + "_R_" + aggKey)
                                .operationCodeType("W4_VTB_CRD")
                                .glCode("a1AT" + glCodePostfix)
                                .streamType(2)
                                .build());
                    }
                }
            }
        }

        return result;
    }

    public static List<SemiTransaction> generateSemiTransactions(BatmAccRouteS3InBatmTransAgrDataRootMessage obj) {
        List<SemiTransaction> result = new ArrayList<>();
        for (var body : obj.getTransactions()) {
            String accNum = body.getAccount();
            result.add(SemiTransaction.builder()
                    .accountNumber(accNum.startsWith("20208") ? accNum.substring(0, 5) : accNum)
                    .aggregationId(body.getAggregationId())
                    .operationCodeType(body.getOperationCodeType())
                    .glCode(body.getGlCode())
                    .streamType(body.getStreamType())
                    .build());
        }

        return result;
    }

    public static String getNzrAccNum(String fileMask, String transType, String atmId) {
        if (fileMask.startsWith("CORP")) {
            return DBHelper.TransAgr.getNzrAccountNumberForCorpCards(fileMask, transType);
        } else return DBHelper.TransAgr.getNzrAccountNumber(fileMask, transType, atmId);
    }

    @Override
    public int compareTo(@NotNull SemiTransaction o) {
        return Comparator
                .comparing(SemiTransaction::getAggregationId)
                .thenComparing(SemiTransaction::getOperationCodeType)
                .thenComparing(SemiTransaction::getAccountNumber)
                .thenComparing(SemiTransaction::getGlCode)
                .thenComparing(SemiTransaction::getStreamType)
                .compare(this, o);
    }
}
