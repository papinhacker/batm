package ru.vtb.core.models.kafka.batm_in_siorus_instruction.generators;

import org.apache.commons.lang3.RandomStringUtils;
import ru.vtb.core.models.common.CommonModelHelper;
import ru.vtb.core.models.kafka.batm_in_siorus_instruction.BatmInSiorusInstructionRootMessage;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class BatmInSiorusInstructionMessageGenerator {

    public static BatmInSiorusInstructionRootMessage generate() {
        double amount = CommonHelper.formatDouble(ThreadLocalRandom.current().nextDouble(1, 9999));
        var builder = BatmInSiorusInstructionRootMessage.builder()
                .messageId(CommonModelHelper.generateRandomMessageId())
                .messageDate(CommonModelHelper.getMessageDate())
                .productInstrId(UUID.randomUUID().toString())
                .vkusCode("2213.0016.0011")
                .setSorting("1")
                .directionDt("Increase")
                .directionKt("Decrease")
                .tbiCode("2213.0031")
                .atmId("AT" + RandomStringUtils.insecure().nextNumeric(4))
                .equivalentFlag(1)
                .operationDay(CommonHelper.getCurrentDateInFormat("yyyy-MM-dd"))

                .isDebitMigrated(true)
                .debitProductRegId(DBHelper.OperSior.getValidDebitProductRegId())
                .debitProductInstanceId(UUID.randomUUID().toString())
                .registerTypeDt("20208_USBanka")
                .debitAccountId(UUID.randomUUID().toString())
                .debitBranch("96")
                .debitMdmId(Long.valueOf(RandomStringUtils.insecure().nextNumeric(13)))
                .debitAmount(amount)
                .debitCurrencyLat3("RUB")

                .isCreditMigrated(true)
                .creditProductRegId(DBHelper.OperSior.getValidCreditProductRegId())
                .creditProductInstanceId(UUID.randomUUID().toString())
                .registerTypeKt("60323_ProchDebitor_NedostachaUS")
                .creditAccountId(UUID.randomUUID().toString())
                .creditBranch("96")
                .creditMdmId(Long.valueOf(RandomStringUtils.insecure().nextNumeric(13)))
                .creditAmount(amount)
                .creditCurrencyLat3("RUB")

                .paymentPurpose("AT " + UUID.randomUUID())
                .eventType("direct");

        return builder.build();
    }

    public static BatmInSiorusInstructionRootMessage generate(Map<String, Object> dbData) {
        return BatmInSiorusInstructionRootMessage.builder()
                .messageId(dbData.get("message_id").toString())
                .messageDate(dbData.get("message_date").toString())
                .productInstrId(dbData.get("product_instr_id").toString())
                .vkusCode(dbData.get("vkus_code").toString())
                .setSorting(dbData.get("set_sorting").toString())
                .directionDt(dbData.get("debit_direction").toString())
                .directionKt(dbData.get("credit_direction").toString())
                .tbiCode(dbData.get("tbi_code").toString())
                .atmId(dbData.get("atm_id").toString())
                .equivalentFlag(Integer.valueOf(dbData.get("equivalent_flag").toString()))
                .operationDay(dbData.get("operation_day").toString())

                .isDebitMigrated(Boolean.valueOf(dbData.get("is_debit_migrated").toString()))
                .debitProductRegId(dbData.get("debit_product_reg_id").toString())
                .debitProductInstanceId(dbData.get("debit_product_instance_id").toString())
                .registerTypeDt(dbData.get("register_type_dt").toString())
                .debitAccountId(dbData.get("debit_account_id").toString())
                .debitBranch(dbData.get("debit_branch").toString())
                .debitMdmId(Long.valueOf(dbData.get("debit_mdm_id").toString()))
                .debitAmount(Double.valueOf(dbData.get("debit_amount").toString()))
                .debitCurrencyLat3(dbData.get("debit_currency").toString())

                .isCreditMigrated(Boolean.valueOf(dbData.get("is_credit_migrated").toString()))
                .creditProductRegId(dbData.get("credit_product_reg_id").toString())
                .creditProductInstanceId(dbData.get("credit_product_instance_id").toString())
                .registerTypeKt(dbData.get("register_type_kt").toString())
                .creditAccountId(dbData.get("credit_account_id").toString())
                .creditBranch(dbData.get("credit_branch").toString())
                .creditMdmId(Long.valueOf(dbData.get("credit_mdm_id").toString()))
                .creditAmount(Double.valueOf(dbData.get("credit_amount").toString()))
                .creditCurrencyLat3(dbData.get("credit_currency").toString())

                .paymentPurpose(dbData.get("payment_purpose").toString())
                .eventType(dbData.get("event_type").toString())
                .build();
    }
}
