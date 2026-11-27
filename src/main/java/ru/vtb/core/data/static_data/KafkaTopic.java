package ru.vtb.core.data.static_data;

import ru.vtb.core.utils.kafka.KafkaClusterConfig;

import ru.vtb.core.models.kafka.batm_acc_in_batm_devices_acc.BatmAccInBatmDevicesAccRootMessage;
import ru.vtb.core.models.kafka.batm_acc_in_batm_trans_acc_result.BatmAccInBatmTransAccResultRootMessage;
import ru.vtb.core.models.kafka.batm_acc_in_batm_trans_reversal.BatmAccInBatmTransReversalRootMessage;
import ru.vtb.core.models.kafka.batm_acc_out_batm_trans_acc.BatmAccOutBatmTransAccRootMessage;
import ru.vtb.core.models.kafka.batm_acc_out_batm_trans_reversal_result.BatmAccOutBatmTransReversalResultRootMessage;
import ru.vtb.core.models.kafka.batm_acc_route_in_batm_trans_pp_data.BatmAccRouteInBatmTransPpDataRootMessage;
import ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_oper_sior_pp_data.BatmAccRoutePpInBatmOperSiorPpDataRootJson;
import ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_trans_pp_data.BatmAccRoutePpInBatmTransPpDataRootMessage;
import ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_trans_pp_result.BatmAccRoutePpOutBatmTransPpResultRootMessage;
import ru.vtb.core.models.kafka.batm_acc_route_s3_in_batm_trans_agr_data.BatmAccRouteS3InBatmTransAgrDataRootMessage;
import ru.vtb.core.models.kafka.batm_acc_route_s3_out_batm_trans_agr_result.BatmAccRouteS3OutBatmTransAgrResultRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_in_batm_devices_atm.BatmClearingInBatmDevicesAtmRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.BatmClearingOutBatmTransAgrDataRootMessage;
import ru.vtb.core.models.kafka.batm_dp_in_batm_devices_atm.BatmDpInBatmDevicesAtmRootMessage;
import ru.vtb.core.models.kafka.batm_in_siorus_acc_migration.BatmInSiorusAccMigrationRootMessage;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.BatmInSiorusDevicesRootMessage;
import ru.vtb.core.models.kafka.batm_in_siorus_instruction.BatmInSiorusInstructionRootMessage;
import ru.vtb.core.models.kafka.batm_out_siorus_instruction.BatmOutSiorusInstructionRootMessage;
import ru.vtb.core.models.kafka.batm_report_in_batm_trans_agr_transaction.BatmReportInBatmTransAgrTransactionRootMessage;
import ru.vtb.core.models.kafka.batm_sior_product_out_batm_acc_new.BatmSiorProductOutBatmAccNewRootMessage;
import ru.vtb.core.models.kafka.batm_trans_out_batm_dp_inst.BatmTransOutBatmDpInstRootMessage;
import ru.vtb.core.models.kafka.batm_trans_out_batm_dp_trans.BatmTransOutBatmDptransRootMessage;
import ru.vtb.core.models.kafka.batm_zod_in_batm_acc_route_s3_load_result.BatmZodInBatmAccRouteS3LoadResultRootMessage;
import ru.vtb.core.models.kafka.batm_zod_out_batm_acc_route_s3_event.BatmZodOutBatmAccRouteS3EventRootMessage;
import ru.vtb.core.models.kafka.sutp_events.SutpEventsRootMessage;
import ru.vtb.core.models.kafka.sutp_to_batm.SutpToBatmRootMessage;

import java.util.Arrays;
import java.util.List;

public enum KafkaTopic {

    SOLAR_OUT_FILE_PROC_NOTIFICATION("solar-out-file-proc-notification"),
    SOLAR_OUT_RUN_JOB("solar-out-run-job"),
    SUTP_EVENTS("sutp-events", SutpEventsRootMessage.class),
    SUTP_TO_BATM("sutp-to-batm", SutpToBatmRootMessage.class),
    BATM_IN_SIORUS_DEVICES("batm-in-siorus-devices", BatmInSiorusDevicesRootMessage.class),
    BATM_IN_SIORUS_INSTRUCTION("batm-in-siorus-instruction", BatmInSiorusInstructionRootMessage.class),
    BATM_OUT_SIORUS_INSTRUCTION("batm-out-siorus-instruction", BatmOutSiorusInstructionRootMessage.class),
    BATM_CLEARING_IN_BATM_DEVICES_ATM("batm_clearing-in-batm_devices-atm", BatmClearingInBatmDevicesAtmRootMessage.class),
    SOLAR_OUT_ACCOUNT_PROCESS_TXN("solar-out-account-process-txn"),
    BATM_ACC_ROUTE_IN_BATM_TRANS_S3_DATA("batm_acc_route-in-batm_trans-s3_data", BatmAccRouteS3InBatmTransAgrDataRootMessage.class),
    BATM_ACC_ROUTE_IN_BATM_TRANS_PP_DATA("batm_acc_route-in-batm_trans-pp_data", BatmAccRouteInBatmTransPpDataRootMessage.class),
    BATM_ACC_ROUTE_OUT_BATM_TRANS_S3_RESULT("batm_acc_route-out-batm_trans-s3_result"),
    BATM_ACC_ROUTE_OUT_BATM_TRANS_PP_RESULT("batm_acc_route-out-batm_trans-pp_result"),
    BATM_ACC_ROUTE_S3_IN_BATM_TRANS_AGR_DATA("batm_acc_route_s3-in-batm_trans_agr-data", BatmAccRouteS3InBatmTransAgrDataRootMessage.class),
    BATM_ACC_ROUTE_S3_OUT_BATM_TRANS_AGR_RESULT("batm_acc_route_s3-out-batm_trans_agr-result", BatmAccRouteS3OutBatmTransAgrResultRootMessage.class),
    BATM_ZOD_IN_BATM_ACC_ROUTE_S3_LOAD_RESULT("batm_zod-in-batm_acc_route_s3-load_result", BatmZodInBatmAccRouteS3LoadResultRootMessage.class),
    BATM_ZOD_OUT_EVENT("batm_zod-out_event"),
    BATM_ZOD_OUT_BATM_ACC_ROUTE_S3_EVENT("batm_zod-out-batm_acc_route_s3-event", BatmZodOutBatmAccRouteS3EventRootMessage.class),
    BATM_ZOD_OUT_BATM_TRANS_AGR_ZOD("batm_zod-out-batm_trans_agr-zod"),
    BATM_ZOD_IN_BATM_TRANS_AGR_ZOD_RESULT("batm_zod-in-batm_trans_agr-zod_result"),
    BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA("batm_clearing-out-batm_trans_agr-data", BatmClearingOutBatmTransAgrDataRootMessage.class),
    BATM_ACC_IN_BATM_DEVICES_ACC("batm_acc-in-batm_devices-acc", BatmAccInBatmDevicesAccRootMessage.class),
    BATM_ACC_OUT_BATM_TRANS_ACC("batm_acc-out-batm_trans-acc", BatmAccOutBatmTransAccRootMessage.class),
    BATM_ACC_ROUTE_PP_IN_BATM_OPER_SIOR_PP_DATA("batm_acc_route_pp-in-batm_oper_sior-pp_data", BatmAccRoutePpInBatmOperSiorPpDataRootJson.class),
    BATM_ACC_ROUTE_PP_IN_BATM_OPER_SIOR_PP_RESULT("batm_acc_route_pp-in-batm_oper_sior-pp_result"),
    BATM_ACC_ROUTE_PP_OUT_BATM_OPER_SIOR_PP_RESULT("batm_acc_route_pp-out-batm_oper_sior-pp_result"),
    BATM_ACC_IN_BATM_TRANS_ACC_RESULT("batm_acc-in-batm_trans-acc_result", BatmAccInBatmTransAccResultRootMessage.class),
    BATM_ACC_IN_BATM_TRANS_REVERSAL("batm_acc-in-batm_trans-reversal", BatmAccInBatmTransReversalRootMessage.class),
    BATM_ACC_OUT_BATM_TRANS_REVERSAL_RESULT("batm_acc-out-batm_trans-reversal_result", BatmAccOutBatmTransReversalResultRootMessage.class),
    BATM_IN_SIORUS_ACC_MIGRATION("batm-in-siorus-acc_migration", BatmInSiorusAccMigrationRootMessage.class),
    BATM_SIOR_PRODUCT_OUT_BATM_ACC_NEW("batm_sior_product-out-batm_acc_new", BatmSiorProductOutBatmAccNewRootMessage.class),
    BATM_ACC_ROUTE_PP_IN_BATM_TRANS_PP_DATA("batm_acc_route_pp-in-batm_trans-pp_data", BatmAccRoutePpInBatmTransPpDataRootMessage.class),
    BATM_TRANS_OUT_BATM_DP_INST("batm_trans-out-batm_dp-inst", BatmTransOutBatmDpInstRootMessage.class),
    BATM_TRANS_OUT_BATM_DP_TRANS("batm_trans-out-batm_dp-trans", BatmTransOutBatmDptransRootMessage.class),
    BATM_REPORT_IN_BATM_TRANS_AGR_TRANSACTION("batm_report-in-batm_trans_agr-transaction", BatmReportInBatmTransAgrTransactionRootMessage.class),
    BATM_ACC_ROUTE_PP_OUT_BATM_TRANS_PP_RESULT("batm_acc_route_pp-out-batm_trans-pp_result", BatmAccRoutePpOutBatmTransPpResultRootMessage.class),
    BATM_TRANS_OUT_BATM_DP_STREAMING("batm_trans-out-batm_dp-streaming"),
    BATM_DP_IN_BATM_DEVICES_ATM("batm_dp-in-batm_devices-atm", BatmDpInBatmDevicesAtmRootMessage.class),

    //TODO имя топика UASP для ФИ на тестовом контуре (ift_); для preprod нужен префикс контура (docs/kafka-design.md)
    UASP_RETAIL_ADM_ATM_CASH_MONEY_TRANSFER_INSTRUCTION("ift_batm_uasp_retail_adm_atm_cash_money_transfer_instruction", KafkaCluster.UASP),
    //TODO имя топика UASP для ФТ на тестовом контуре (ift_); для preprod нужен префикс контура (docs/kafka-design.md)
    UASP_RETAIL_ADM_ATM_CASH_MONEY_TRANSFER_TRANSACTION("ift_batm_uasp_retail_adm_atm_cash_money_transfer_transaction", KafkaCluster.UASP),
    ODS_BATMADMIN_OUTBOX("tsss.batmadmin.outbox", KafkaCluster.ODS);

    private final String topicName;
    private final Class<?> defaultDeserializationClass;
    private final KafkaCluster cluster;

    KafkaTopic(String topicName, Class<?> clazz, KafkaCluster cluster) {
        this.topicName = topicName;
        this.defaultDeserializationClass = clazz;
        this.cluster = cluster;
    }

    KafkaTopic(String topicName, Class<?> clazz) {
        this(topicName, clazz, KafkaCluster.BATM);
    }

    KafkaTopic(String topicName, KafkaCluster cluster) {
        this(topicName, String.class, cluster);
    }

    KafkaTopic(String topicName) {
        this(topicName, String.class, KafkaCluster.BATM);
    }

    public static List<String> getTopicsNames(KafkaCluster cluster) {
        return Arrays.stream(values()).filter(t -> t.getCluster() == cluster).map(KafkaTopic::getTopicName).toList();
    }

    /**
     * @return имя топика на контуре: префикс кластера (kafka.&lt;имя&gt;.topic.prefix, по умолчанию пусто) + имя
     */
    public String getTopicName() {
        return KafkaClusterConfig.of(cluster).getTopicPrefix() + this.topicName;
    }

    public Class<?> getDefClass() {
        return this.defaultDeserializationClass;
    }

    public KafkaCluster getCluster() {
        return this.cluster;
    }
}
