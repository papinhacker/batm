package ru.vtb.core.models.kafka.batm_acc_route_s3_in_batm_trans_agr_data.generator;

import org.apache.commons.lang3.RandomStringUtils;
import ru.vtb.core.data.static_data.Bin;
import ru.vtb.core.data.static_data.MapsKeysConstants;
import ru.vtb.core.models.common.CommonModelHelper;
import ru.vtb.core.models.kafka.batm_acc_route_s3_in_batm_trans_agr_data.BatmAccRouteS3InBatmTransAgrDataRootMessage;
import ru.vtb.core.models.kafka.batm_acc_route_s3_in_batm_trans_agr_data.TransactionsItem;
import ru.vtb.core.utils.DBHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator {

    private static final String bankingDate = DBHelper.TransAgr.getCurrentBankDate();

    public static BatmAccRouteS3InBatmTransAgrDataRootMessage generate(int... count) {
        int resCount = count.length > 0 ? count[0] : 1;
        return generate(resCount, Map.of());
    }

    public static BatmAccRouteS3InBatmTransAgrDataRootMessage generate(int count, Map<String, Object> fields) {
        List<TransactionsItem> bodies = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            bodies.add(generateTransactionsItem(fields));
        }
        return new BatmAccRouteS3InBatmTransAgrDataRootMessage(CommonModelHelper.generateRandomMessageId(), bodies);
    }

    private static TransactionsItem generateTransactionsItem(Map<String, Object> fields) {
        String glCode = (String) fields.getOrDefault(MapsKeysConstants.GL_CODE, "Y1AT" + Bin.getGlCodePostFixByCardNumber("4"));
        String branchCode = (String) fields.getOrDefault(MapsKeysConstants.ABS_BRANCH, DBHelper.Devices.getRandomFullAbsBranchCode());
        int streamType = (int) fields.getOrDefault(MapsKeysConstants.STREAM_TYPE, getRandomStreamType());

        boolean cashIn = glCode.startsWith("Y");
        boolean reversal = glCode.startsWith("a");
        return TransactionsItem.builder()
                .atmId("AT" + RandomStringUtils.insecure().nextNumeric(6))
                .glCode(glCode)
                .aggregationId("WAY4_%s_%s".formatted(reversal ? "R" : "P", RandomStringUtils.insecure().nextNumeric(18)))
                .streamType(streamType)
                .account("20208810" + RandomStringUtils.insecure().nextNumeric(12))
                .operationCodeType(cashIn || reversal ? "W4_VTB_CRD" : "W4_VTB_DEB")
                .operationDay(bankingDate)
                .branchCode(branchCode)
                .currency("RUR")
                .amount(Double.parseDouble(String.format(Locale.US, "%.2f", ThreadLocalRandom.current().nextDouble(20000))))
                .build();
    }

    public static int getRandomStreamType() {
        return ThreadLocalRandom.current().nextInt(1, 3);
    }
}
