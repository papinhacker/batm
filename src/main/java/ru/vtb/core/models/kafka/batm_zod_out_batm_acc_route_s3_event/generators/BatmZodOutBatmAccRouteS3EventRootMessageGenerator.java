package ru.vtb.core.models.kafka.batm_zod_out_batm_acc_route_s3_event.generators;

import org.apache.commons.lang3.RandomStringUtils;
import ru.vtb.core.data.static_data.MapsKeysConstants;
import ru.vtb.core.models.common.CommonModelHelper;
import ru.vtb.core.models.kafka.batm_acc_route_s3_in_batm_trans_agr_data.generator.BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator;
import ru.vtb.core.models.kafka.batm_zod_out_batm_acc_route_s3_event.BatmZodOutBatmAccRouteS3EventRootMessage;
import ru.vtb.core.models.kafka.batm_zod_out_batm_acc_route_s3_event.ParametersItem;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

import java.util.List;
import java.util.Map;

public class BatmZodOutBatmAccRouteS3EventRootMessageGenerator {

    private static final String bankingDate = DBHelper.TransAgr.getCurrentBankDate();

    public static BatmZodOutBatmAccRouteS3EventRootMessage generate(Map<String, String> fields) {
        String csvFilename = fields.getOrDefault(MapsKeysConstants.CSV_FILENAME, generateRandomAggregatorCSVFilename(Map.of()));
        String streamType = fields.getOrDefault(MapsKeysConstants.STREAM_TYPE, Integer.toString(BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator.getRandomStreamType()));
        String branchCode = fields.getOrDefault(MapsKeysConstants.ABS_BRANCH, DBHelper.Devices.getRandomFullAbsBranchCode());

        return BatmZodOutBatmAccRouteS3EventRootMessage.builder()
                .messageId(CommonModelHelper.generateRandomMessageId())
                .eventName("BATM-AggLoad")
                //убрали в новой поставновке.
                //.eventDate(CommonHelper.getCurrentDateInFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                .description("Выгрузка в агрегатор")
                .operdayDate(bankingDate)
                .systemCode("2213")
                .systemName("Бэк-Офис Ус и АДМ")
                .branchCode(branchCode)
                .parameters(List.of(
                        ParametersItem.builder()
                                .key("fileName")
                                .value(csvFilename)
                                .build(),
                        ParametersItem.builder()
                                .key("flowType")
                                .value(streamType)
                                .build(),
                        ParametersItem.builder()
                                .key("bankingDate")
                                .value(bankingDate)
                                .build()))
                .build();
    }

    public static String generateRandomAggregatorCSVFilename(Map<String, String> fields) {
        String branchCode = fields.getOrDefault(MapsKeysConstants.ABS_BRANCH, DBHelper.Devices.getRandomFullAbsBranchCode());
        String streamType = fields.getOrDefault(MapsKeysConstants.STREAM_TYPE, Integer.toString(BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator.getRandomStreamType()));
        String date = CommonHelper.parseDateFromPatternToAnotherPattern("yyyy-MM-dd", "yyyyMMdd", bankingDate);
        return "BATMWATRN_%s_%s_%s_%s.csv"
                .formatted(streamType, branchCode, date, RandomStringUtils.insecure().nextNumeric(2));
    }
}
