package ru.vtb.api.tests.batm_operations_sior;

import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_oper_sior_pp_data.BatmAccRoutePpInBatmOperSiorPpDataRootJson;
import ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_oper_sior_pp_data.generators.BatmAccRoutePpInBatmOperSiorPpDataMessageGenerator;
import ru.vtb.core.models.kafka.batm_in_siorus_instruction.generators.BatmInSiorusInstructionMessageGenerator;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;
import ru.vtb.core.utils.KafkaHelper;

import java.util.List;

import static ru.vtb.core.data.static_data.KafkaTopic.*;

public class BatmOperationsSiorTests extends BaseAPIState {

    @Test(description = "Проверка  обработки успешного сообщения об операции от Сиорус")
    @TmsLink("BATM-416")
    public void testBATM416() {
        var incomingJson = BatmInSiorusInstructionMessageGenerator.generate();
        String msgId = incomingJson.getMessageId();
        KafkaHelper.sendMessage(BATM_IN_SIORUS_INSTRUCTION, CommonHelper.serializeToJsonString(incomingJson));

        var operData = DBHelper.OperSior.getOperationData(msgId);
        var aggregatedData = BatmInSiorusInstructionMessageGenerator.generate(operData);

        softAssertEquals(incomingJson.toJson().replaceFirst("T\\d{2}:\\d{2}:\\d{2}\\.\\d+", ""),
                aggregatedData.toJson().replaceFirst("\\W\\d{2}:\\d{2}:\\d{2}\\.\\d+", ""),
                "Переданные данные неверно записались в БД сервиса");
        var finInsData = DBHelper.OperSior.getFinancialInstructionData(msgId);
        softAssertEquals(finInsData.get("status").toString(),
                "Processing",
                "В таблицу 'financial_instruction' записался неверный статус операции");
        var finTranData = DBHelper.OperSior.getFinancialTransactionData(msgId);
        softAssertEquals(finTranData.get("status").toString(),
                "Accepted",
                "В таблицу 'financial_transaction' записался неверный статус операции");

        var kafkaResp = (BatmAccRoutePpInBatmOperSiorPpDataRootJson) KafkaHelper.getLastRecord(BATM_ACC_ROUTE_PP_IN_BATM_OPER_SIOR_PP_DATA, msgId, long_timeout);
        kafkaResp.setTransactionId(null);
        kafkaResp.setDescription(null);
        kafkaResp.setProductInstrId(null);
        var expectedKafkaResp = BatmAccRoutePpInBatmOperSiorPpDataMessageGenerator.generate(incomingJson);
        softAssertEquals(kafkaResp.toJson(),
                expectedKafkaResp.toJson(),
                "Сервис неврно обработал операцию");

        String transId = finTranData.get("id").toString();
        KafkaHelper.getLastRecord(BATM_ACC_ROUTE_PP_OUT_BATM_OPER_SIOR_PP_RESULT, List.of(transId, "SUCCESS_PP"), long_timeout);
        KafkaHelper.getLastRecord(BATM_ACC_ROUTE_PP_OUT_BATM_OPER_SIOR_PP_RESULT, List.of(transId, "SUCCESS_GP"), orcTimeout);

        var newFinTranData = DBHelper.OperSior.getFinancialTransactionData(msgId);
        softAssertEquals(newFinTranData.get("status").toString(),
                "Executed",
                "Сервис не обновил/неверно обновил статус операции");
        softAssertNotEquals(
                newFinTranData.get("updated").toString(),
                finTranData.get("updated").toString(),
                "Сервис не обновил последнее время обновления операции");
        softAssertTrue(Boolean.parseBoolean(newFinTranData.get("sent_to_dp").toString()),
                "В БД записался неверный статус получения ответа от ГП");

        var newFinInsData = DBHelper.OperSior.getFinancialInstructionData(msgId);
        softAssertEquals(newFinInsData.get("status").toString(),
                "Executed",
                "Сервис не обновил/неверно обновил статус операции");
        softAssertNotEquals(
                newFinInsData.get("updated").toString(),
                finInsData.get("updated").toString(),
                "Сервис не обновил последнее время обновления операции");
        softAssertTrue(Boolean.parseBoolean(newFinInsData.get("sent_to_dp").toString()),
                "В БД записался неверный статус получения ответа от ГП");

        KafkaHelper.getFirstRecord(BATM_OUT_SIORUS_INSTRUCTION, transId, timeout);

        softAssertAll();
    }
}
