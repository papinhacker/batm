package ru.vtb.core.models.kafka.sutp_to_batm.generators;

import ru.vtb.core.models.common.CommonModelHelper;
import ru.vtb.core.models.kafka.batm_zod_out_batm_acc_route_s3_event.generators.BatmZodOutBatmAccRouteS3EventRootMessageGenerator;
import ru.vtb.core.models.kafka.sutp_to_batm.ParametersItem;
import ru.vtb.core.models.kafka.sutp_to_batm.SutpToBatmRootMessage;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

import java.util.List;
import java.util.Map;

public class SutpToBatmRootMessageGenerator {

    private static final String bankingDate = DBHelper.TransAgr.getCurrentBankDate();
    private static final List<String> branches = DBHelper.Devices.getAbsBranchCodes();

    public static SutpToBatmRootMessage generate() {
        return SutpToBatmRootMessage.builder()
                .requestId(CommonModelHelper.generateRandomMessageId())
                .eventName("BATM-AggLoad")
                .eventDate(CommonHelper.getCurrentDateInFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                .description("Запрос на старт выгрузки проводок")
                .operdayDate(bankingDate)
                .sessionNumber("5")
                .systemCode("2093")
                .systemName("СУТП")
                .branchCode(CommonHelper.getRandomFromList(branches))
                .parameters(List.of(
                        ParametersItem.builder()
                                .key("fileName")
                                .value(BatmZodOutBatmAccRouteS3EventRootMessageGenerator.generateRandomAggregatorCSVFilename(Map.of()))
                                .build(),
                        ParametersItem.builder()
                                .key("flowType")
                                .value("1")
                                .build()))
                .build();
    }
}
