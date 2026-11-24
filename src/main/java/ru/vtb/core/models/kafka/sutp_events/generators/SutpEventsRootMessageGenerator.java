package ru.vtb.core.models.kafka.sutp_events.generators;

import ru.vtb.core.utils.DBHelper;

import java.util.List;

public class SutpEventsRootMessageGenerator {

    private static final String bankingDate = DBHelper.TransAgr.getCurrentBankDate();
    private static final List<String> branches = DBHelper.Devices.getAbsBranchCodes();

    //генератор не нужен, только маппер из BatmToSutp
//    public static SutpEventsRootMessage generate() {
//        return SutpEventsRootMessage.builder()
//                .messageId(CommonModelHelper.generateRandomMessageId())
//                .eventName("BATM-AggLoad")
//                .eventDate(CommonHelper.getCurrentDateInFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
//                .description("Запрос на старт выгрузки проводок")
//                .operdayDate(bankingDate)
//                .sessionNumber("5")
//                .systemCode("2093")
//                .systemName("СУТП")
//                .branchCode(CommonHelper.getRandomFromList(branches))
//                .parameters(List.of(
//                        ParametersItem.builder()
//                                .key("fileName")
//                                .value(BatmZodOutBatmAccRouteS3EventRootMessageGenerator.generateRandomAggregatorCSVFilename(Map.of()))
//                                .build(),
//                        ParametersItem.builder()
//                                .key("flowType")
//                                .value("1")
//                                .build()))
//                .build();
//    }
}
