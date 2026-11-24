package ru.vtb.core.utils;

import ru.vtb.core.utils.sql.SQLHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static ru.vtb.core.common.EnvConstants.TIMEOUT;
import static ru.vtb.core.utils.sql.SQLHelper.*;

public class DBHelper {

    public static final SQLHelper solar = new SQLHelper(SOLAR);
    public static final SQLHelper solarAcc = new SQLHelper(SOLAR_ACC);
    public static final SQLHelper batmDevices = new SQLHelper(BATM_DEVICES);
    public static final SQLHelper batmClearing = new SQLHelper(BATM_CLEARING);
    public static final SQLHelper batmZod = new SQLHelper(BATM_ZOD);
    public static final SQLHelper batmBin = new SQLHelper(BATM_BIN);
    public static final SQLHelper batmOutbox = new SQLHelper(BATM_OUTBOX);
    public static final SQLHelper batmAccRouteS3 = new SQLHelper(BATM_ACC_ROUTE_S3);
    public static final SQLHelper batmAccRoutePP = new SQLHelper(BATM_ACC_ROUTE_PP);
    public static final SQLHelper batmAcc = new SQLHelper(BATM_ACC);
    public static final SQLHelper batmTransAgr = new SQLHelper(BATM_TRANSACTIONS_AGR);
    public static final SQLHelper batmTransAgrSrv = new SQLHelper(BATM_TRANSACTIONS_AGR_SRV);
    public static final SQLHelper batmOperSior = new SQLHelper(BATM_OPERATIONS_SIOR);
    public static final SQLHelper batmProductSior = new SQLHelper(BATM_SIOR_PRODUCT_ACC);

    private DBHelper() {
    }

    /**
     * @param agreementNumber
     * @return
     */
    public static List<Map<String, Object>> getServiceProviderDataByAgreementNumber(String agreementNumber) {
        String query = """
                select
                bfi.code as fin_code,
                bp.code as product_code,
                bat.code as acsr_type_code,
                cct.code as client_type_code,
                cc.cl_number as client_number,
                ba.fin_institution_id as fin_institution_id,
                ba.currency as currency,
                ba.signature_date as signature_date,
                ba.effective_date as effective_date,
                ba.banking_date as banking_date,
                ba.audit_date as audit_date,
                bb.code as branch_code,
                ba2."attributes"::json ->> 'TERMINAL_TYPE' as accessor_type
                from bo_agreement ba
                inner join bo_fin_institution bfi on ba.fin_institution_id = bfi.id
                inner join bo_product bp on ba.product_id = bp.id
                inner join bo_accessor ba2 on ba.id = ba2.agreement_id
                inner join bo_accessor_type bat on bat.id = ba2.accessor_type_id
                inner join crm_client cc on ba.client_id = cc.id
                inner join crm_client_type cct on cc.type_id = cct.id
                inner join bo_branch bb on bb.id = ba.branch_id
                where agr_number = '%s'
                """;
        return solar.getRowLinesFromDB(String.format(query, agreementNumber));
    }

    /**
     * Данные по транзакции через RRN
     *
     * @param rrn
     * @return
     */
    public static Map<String, Object> getTransactionDataByRRN(String rrn) {
        String query = """
                select *
                from bo_txn
                where ret_ref_number = '%s'
                ORDER BY txn_date
                LIMIT 1
                """;
        return solar.getRowLinesFromDB(String.format(query, rrn), TIMEOUT).get(0);
    }

    public static Map<String, Object> getTransactionDataByRRN(String rrn, String txnDirection, long... timeout) {
        String query = """
                select *
                from bo_txn
                where ret_ref_number = '%s'
                and txn_direction = '%s'
                ORDER BY txn_date
                LIMIT 1
                """;
        return solar.getRowLinesFromDB(String.format(query, rrn, txnDirection), timeout.length > 0 ? timeout[0] : TIMEOUT).get(0);
    }

    /**
     * Возвращает данные по старому договору
     *
     * @param agreementNumber - номер договора
     * @return
     */
    public static Map<String, Object> getClassifierDataByAgreementNumber(String agreementNumber, long timeoutIsSecs) {
        String query = """
                select cdet.code as type,
                ba.agr_number,
                cct.code as status_type,
                ccv.code as state,
                cc.*
                from cls_classifier cc
                inner join cls_domain_entity_type cdet on cdet.id = cc.domain_entity_type_id
                inner join cls_classifier_type cct on cct.id = cc.classifier_type_id
                inner join cls_classifier_value ccv on ccv.id = cc.classifier_value_id
                inner join bo_agreement ba on ba.id = cc.domain_entity_id
                where ba.agr_number = '%s'
                """;
        return solar.getRowLinesFromDB(String.format(query, agreementNumber), timeoutIsSecs).get(0);
    }

    public static Map<String, Object> getClassifierDataByAgreementId(String agreementId) {
        String query = """
                select *
                from cls_classifier cc
                inner join cls_classifier_type cct on cc.classifier_type_id = cct.id
                inner join cls_classifier_value ccv on cc.classifier_value_id = ccv.id
                where ccv.code = 'TO_CLOSING'
                and cct.code = 'AGREEMENT_STATUS'
                and domain_entity_id = '%s'
                """;
        return solar.getRowLinesFromDB(String.format(query, agreementId)).get(0);
    }

    /**
     * Возвращает данные о договоре
     *
     * @return
     */
    public static Map<String, Object> getAgreementDataByAgreementId(String agreementId) {
        String query = """
                select *
                from bo_agreement
                where id = '%s'
                """;
        return solar.getRowLinesFromDB(String.format(query, agreementId)).get(0);
    }

    /**
     * Возвращает данные о связи договора и УС
     *
     * @return
     */
    public static Map<String, Object> getAgreementAndTerminalRelationshipDataByAgreementId(String agreementId) {
        String query = """
                select *
                from bo_accessor
                where agreement_id = '%s'
                """;
        return solar.getRowLinesFromDB(String.format(query, agreementId)).get(0);
    }

    /**
     * Возвращает данные о договоре и связанных с ним устройствах
     *
     * @param agreementNumber
     * @return
     */
    public static Map<String, Object> getAgreementDataWithLinkedDevices(String agreementNumber) {
        String query = """
                select ba.state, ba.access_number, ba2.client_id, ba2.*
                from bo_accessor ba
                inner join bo_agreement ba2 on ba2.id = ba.agreement_id
                where ba2.agr_number = '%s'
                """;
        return solar.getRowLinesFromDB(String.format(query, agreementNumber)).get(0);
    }

    /**
     * Возвращает данные о договоре через номер устройства
     *
     * @param accessorNumber
     * @return
     */
    public static List<Map<String, Object>> getAgreementDataByAccessorNumber(String accessorNumber, long timeoutInSecs) {
        String query = """
                select
                bfi.code as fin_code,
                bp.code as product_code,
                bat.code as acsr_type_code,
                cct.code as client_type_code,
                cc.cl_number as client_number,
                ba.fin_institution_id as fin_institution_id,
                ba.currency as currency,
                ba.id,
                ba.signature_date as signature_date,
                ba.effective_date as effective_date,
                ba.banking_date as banking_date,
                ba.audit_date as audit_date,
                bb.code as branch_code,
                ba2."attributes"::json ->> 'TERMINAL_TYPE' as accessor_type
                from bo_agreement ba
                inner join bo_fin_institution bfi on ba.fin_institution_id = bfi.id
                inner join bo_product bp on ba.product_id = bp.id
                inner join bo_accessor ba2 on ba.id = ba2.agreement_id
                inner join bo_accessor_type bat on bat.id = ba2.accessor_type_id
                inner join crm_client cc on ba.client_id = cc.id
                inner join crm_client_type cct on cc.type_id = cct.id
                inner join bo_branch bb on bb.id = ba.branch_id
                where ba2.access_number = '%s'
                and (state isnull or state = 'I')
                """;
        return solar.getRowLinesFromDB(String.format(query, accessorNumber), timeoutInSecs);
    }

    public static String getProcessedBinFileMetadataByBinFilename(String filename) {
        String query = """
                select status
                from bch_file_job bfj
                where file_name = '%s'
                """;
        return solar.getFirstFieldFromDB(query.formatted(filename), TIMEOUT);
    }

    public static String getAgreementIdByAgreementNumber(String agreementNumber) {
        String query = """
                select id
                from bo_agreement ba
                where agr_number = '%s'
                """;
        return solar.getFirstFieldFromDB(query.formatted(agreementNumber));
    }

    /**
     * Возвращает данные о связи договора и счета
     *
     * @return
     */
    public static List<Map<String, Object>> getAgreementAndAccountRelationshipDataByAgreementId(String agreementId) {
        String query = """
                select *
                from acc_account
                where agreement_id = '%s'
                """;
        return solarAcc.getRowLinesFromDB(String.format(query, agreementId));
    }

    public static String getDebitOrCreditAccountsNumber(String agreementId) {
        String query = """
                select account_number
                from acc_account
                where agreement_id = '%s'
                and status = 'A'
                and account_number like '20208810%%'
                """;
        return solarAcc.getFirstFieldFromDB(String.format(query, agreementId));
    }

    /**
     * Возвращает данные о связи договора и счета
     *
     * @return
     */
    public static Map<String, Object> getAgreementAndAccountRelationshipDataByAccountNumber(String accountNumber) {
        String query = """
                select *
                from acc_account
                where account_number = '%s'
                """;
        return solarAcc.getRowLinesFromDB(String.format(query, accountNumber)).get(0);
    }

    /**
     * Возвращает данные о связи договора и счета
     *
     * @return
     */
    public static Map<String, Object> getAgreementAndAccountRelationshipDataByAccountNumberAndAgreementId(String accountNumber, String agreementId) {
        String query = """
                select *
                from acc_account
                where account_number = '%s'
                and agreement_id = '%s'
                """;
        return solarAcc.getRowLinesFromDB(String.format(query, accountNumber, agreementId)).get(0);
    }

    public static List<Map<String, Object>> getTransactionShowcaseDataByAggregationKey(String aggregationKey, long... timeout) {
        String query = """
                select paefa.aggregation_key, paefa.txn_direction, paefa.account_number, paefa. export_status, paefa.gl_code
                from prj_acc_entry_for_aggreg paefa
                where paefa.aggregation_key = '%s'
                """;
        return solarAcc.getRowLinesFromDB(query.formatted(aggregationKey), timeout.length > 0 ? timeout[0] : TIMEOUT);
    }

    private static List<String> getFirstColumnResults(List<Map<String, Object>> table) {
        List<String> result = new ArrayList<>();
        table.forEach(s -> result.add(s.values().stream().map(Object::toString).toList().get(0)));
        return result;
    }

    private static String formatListForInStatement(List<String> list) {
        return String.join(",", list.stream().map(s -> "'" + s + "'").toList());
    }

    public static class Devices {
        private Devices() {
        }

        public static List<Map<String, Object>> getDeviceDataByDeviceId(String deviceId, long... timeoutInSecs) {
            String query = """
                    select *
                    from devices
                    where ATM_id = '%s'
                    """;
            return batmDevices.getRowLinesFromDB(String.format(query, deviceId), timeoutInSecs.length > 0 ? timeoutInSecs[0] : TIMEOUT);
        }

        public static Map<String, Object> getDeviceHistoryDataByDeviceID(String deviceId) {
            String query = """
                    select *
                    from devices_history_action
                    where atm_id = '%s'
                    """;
            return batmDevices.getRowLinesFromDB(query.formatted(deviceId), TIMEOUT).get(0);
        }

        public static List<String> getAbsBaseBranchCodeReference(boolean is511) {
            String query = """
                    select abs_base_branch
                    from branch_match bm
                    where abs_branch_main %s= '00000'
                    """;
            return getFirstColumnResults(batmDevices.getRowLinesFromDB(query.formatted(is511 ? "" : "!"), TIMEOUT));
        }

        public static String getAbsBranchMainByAbsBaseBranch(String absBaseBranch) {
            String query = """
                    select abs_branch_main
                    from branch_match
                    where abs_base_branch = '%s'
                    """;
            return batmDevices.getFirstFieldFromDB(query.formatted(absBaseBranch));
        }

        public static List<String> getAbsBranchCodes() {
            String query = """
                    select abs_branch_main
                    from branch_match bm
                    """;
            return getFirstColumnResults(batmDevices.getRowLinesFromDB(query, TIMEOUT));
        }

        public static String getRandomFullAbsBranchCode(String... excludeOf) {
            List<String> branchCodes = Devices.getAbsBranchCodes();
            if (excludeOf.length > 0)
                branchCodes.remove(excludeOf[0]);
            return branchCodes.get(ThreadLocalRandom.current().nextInt(branchCodes.size()));
        }

        public static Map<String, Object> getDeviceAccountsDataByDeviceID(String deviceId) {
            String query = """
                    select *
                    from devices_accounts da
                    where ATM_ID = '%s'
                    """;
            return batmDevices.getRowLinesFromDB(query.formatted(deviceId), TIMEOUT).get(0);
        }

        public static void setIsInAccStatus(String deviceId, boolean status) {
            String query = """
                    update devices_accounts
                    set is_in_acc = '%s'
                    where ATM_ID = '%s'
                    """;
            batmDevices.executeUpdate(query.formatted(status ? "T" : "F", deviceId));
        }
    }

    public static class Clearing {
        private Clearing() {
        }

        public static Map<String, Object> getDeviceDataByID(String deviceId) {
            String query = """
                    select *
                    from device
                    where atm_id = '%s'
                    """;
            return batmClearing.getRowLinesFromDB(query.formatted(deviceId), TIMEOUT).get(0);
        }

        public static List<String> getCorpToJsonDevicesIds(Boolean... atmMigration) {
            String query = """
                    select atm_id
                    from device
                    where corp_to_json = 'Y'
                    %s
                    """;
            return getFirstColumnResults(batmClearing.getRowLinesFromDB(query.formatted(determineAtmMigration(atmMigration)), false));
        }

        public static List<String> getFlToJsonDevicesIds(Boolean... atmMigration) {
            String query = """
                    select atm_id
                    from device
                    where fl_to_json = 'Y'
                    and location_type = 'Y'
                    %s
                    """;
            return getFirstColumnResults(batmClearing.getRowLinesFromDB(query.formatted(determineAtmMigration(atmMigration)), false));
        }

        private static String determineAtmMigration(Boolean... atmMigration) {
            Boolean buff = atmMigration.length == 0 ? null : atmMigration[0];
            if (buff != null)
                return "and atm_migration = '%s'".formatted(buff ? "T" : "F");
            else return "";
        }

        /**
         * Возвращает дату последней обработки шестого рейса клиринга
         * (направление сообщение в sutp-events о завершении банковского дня)
         */
        public static String getSendingMessageZodDate() {
            String query = """
                    select sending_message_zod
                    from zod_management zm
                    """;
            return batmClearing.getFirstFieldFromDB(query);
        }

        /**
         * Возвращает время ежедневной отправки сообщения в СУТП о закрытии банковского дня
         * (при отсутствии полного набора шесторого рейса клиринга в этот день)
         */
        public static String getTimeStartInManagementZod() {
            String query = """
                    select time_start
                    from zod_management zm
                    """;
            return batmClearing.getFirstFieldFromDB(query);
        }

        public static Map<String, Object> getDataOfClearingTransactions(String authCode, String rrn) {
            String query = """
                    select *
                    from clearing_data cd
                    where auth_code = '%s'
                    and rrn = '%s'
                    """;
            return batmClearing.getRowLinesFromDB(query.formatted(authCode, rrn)).get(0);
        }

        /**
         * Обновляет дату последней обработки шестого рейса клиринга (направление сообщение в sutp-events о завершении банковского дня)
         *
         * @param date
         */
        public static void updateSendingMessageZodDate(String date) {
            String query = """
                    update zod_management
                    set sending_message_zod = '%s'
                    """;
            batmClearing.executeUpdate(query.formatted(date));
        }

        /**
         * Обновляет время ежедневной отправки сообщения в СУТП о закрытии банковского дня
         * (при отсутствии полного набора шесторого рейса клиринга в этот день)
         *
         * @param time
         */
        public static void updateTimeStartInManagementZod(String time) {
            String query = """
                    update zod_management
                    set time_start = '%s'
                    """;
            batmClearing.executeUpdate(query.formatted(time));
        }
    }

    public static class BatmBin {
        private BatmBin() {
        }

        public static String getBinFileProcessingStatusByFilename(String filename) {
            String query = """
                    select status
                    from bin_file
                    where "name" = '%s'
                    """;
            return batmBin.getFirstFieldFromDB(query.formatted(filename), TIMEOUT);
        }

        public static String getDateOfDeletedFileFromS3ByFilename(String filename) {
            String query = """
                    select date_batm_output_bin_batm_way_in
                    from bin_file_deleted_s3 bfds
                    where batm_output_bin_batm_way_in = '%s'
                    """;
            return batmBin.getFirstFieldFromDB(query.formatted(filename), TIMEOUT);
        }
    }

    public static class BatmDP {
        private BatmDP() {
        }

        public static Map<String, Object> getOperationDetails(String accountDocumentId) {
            String query = """
                    select *
                    from operation_details
                    where object_id = '%s'
                    """;
            return batmOutbox.getRowLinesFromDB(query.formatted(accountDocumentId)).get(0);
        }

        public static List<Map<String, Object>> getOutboxMessagesData(String instructionId, String transactionId) {
            String query = """
                    select *
                    from batm_outbox.msg_content_0
                    where "content"::text like '%%%s%%'
                    or "content"::text like '%%%s%%'
                    union all
                    select *
                    from batm_outbox.msg_content_1
                    where "content"::text like '%%%s%%'
                    or "content"::text like '%%%s%%'
                    """;
            return batmOutbox.getRowLinesFromDB(query.formatted(instructionId, transactionId, instructionId, transactionId), TIMEOUT);
        }

        public static List<String> getOutboxMessagesSentData(List<String> messageIds) {
            String query = """
                    select kafka_partition
                    from msg_sent
                    where message_id in (%s)
                    """;
            return getFirstColumnResults(batmOutbox.getRowLinesFromDB(query.formatted(formatListForInStatement(messageIds))));
        }

        /**
         * Запись об инструкции без ожидания (для поллинга)
         *
         * @param instructionId - id инструкции
         */
        public static List<Map<String, Object>> getInstructionRows(String instructionId) {
            String query = """
                    select *
                    from batm_outbox.instruction
                    where instruction_id = '%s'
                    """;
            return batmOutbox.getRowLinesFromDB(query.formatted(instructionId), false);
        }

        /**
         * Запись о транзакции без ожидания (для поллинга)
         *
         * @param transactionId - id транзакции
         */
        public static List<Map<String, Object>> getTransactionRows(String transactionId) {
            String query = """
                    select *
                    from batm_outbox.transaction
                    where id = '%s'
                    """;
            return batmOutbox.getRowLinesFromDB(query.formatted(transactionId), false);
        }

        /**
         * message_id записей из msg_content_0 и msg_content_1 по ключу
         *
         * @param key - id инструкции или транзакции
         */
        public static List<String> getOutboxMessageIdsByKey(String key) {
            String query = """
                    select message_id
                    from batm_outbox.msg_content_0
                    where key = '%s'
                    union all
                    select message_id
                    from batm_outbox.msg_content_1
                    where key = '%s'
                    """;
            return getFirstColumnResults(batmOutbox.getRowLinesFromDB(query.formatted(key, key), TIMEOUT));
        }

        public static List<Map<String, Object>> getOutboxMessagesSent(List<String> messageIds) {
            String query = """
                    select *
                    from msg_sent
                    where message_id in (%s)
                    """;
            return batmOutbox.getRowLinesFromDB(query.formatted(formatListForInStatement(messageIds)), TIMEOUT);
        }
    }

    public static class BatmAccRouteS3 {
        private BatmAccRouteS3() {
        }

        public static Map<String, Object> getAccRouteS3TransactionFileDataByMessageId(String messageId) {
            String query = """
                    select *
                    from transaction_files
                    where message_id = '%s'
                    """;
            return batmAccRouteS3.getRowLinesFromDB(query.formatted(messageId), TIMEOUT).get(0);
        }

        public static List<Map<String, Object>> getAccRouteS3TransactionsDataByTransactionFileId(String transactionFileId) {
            String query = """
                    select *
                    from transactions
                    where transaction_file_id = '%s'
                    """;
            return batmAccRouteS3.getRowLinesFromDB(query.formatted(transactionFileId), 480L);
        }

        public static void truncateTransactions() {
            String query = """
                    truncate table transactions
                    """;
            batmAccRouteS3.executeUpdate(query);
        }
    }

    public static class BatmAcc {
        private BatmAcc() {
        }

        public static Map<String, Object> getAccountDataFromBatmAcc(String accNum, String atmId) {
            String query = """
                    select *
                    from active_accounts
                    where account_number = '%s' and atm_id = '%s'
                    """;
            return batmAcc.getRowLinesFromDB(query.formatted(accNum, atmId), TIMEOUT).get(0);
        }

        public static Map<String, Object> getAccountHistoryDataFromBatmAcc(String accNum, String atmId) {
            String query = """
                    select *
                    from history_accounts
                    where account_number = '%s' and atm_id = '%s'
                    """;
            return batmAcc.getRowLinesFromDB(query.formatted(accNum, atmId), TIMEOUT).get(0);
        }

        public static void decreaseHistoryAccountsStartTime(String atmId) {
            String query = """
                    update history_accounts
                    set date_start = '2020-01-01 10:10:10'
                    where atm_id = '%s'
                    """;
            batmAcc.executeUpdate(query.formatted(atmId));
        }
    }

    public static class Zod {
        private Zod() {
        }

        public static Map<String, Object> getSutpRequestData(String requestId) {
            String query = """
                    select *
                    from sutp_request
                    where request_id = '%s'
                    """;
            return batmZod.getRowLinesFromDB(query.formatted(requestId), TIMEOUT).get(0);
        }
    }

    public static class TransAgr {

        private TransAgr() {
        }

        public static String getCurrentBankDate() {
            String query = """
                    select operday_date
                    from current_operday
                    """;
            return getFirstColumnResults(batmTransAgr.getRowLinesFromDB(query)).get(0);
        }

        public static void setCurrentBankDate(String date) {
            String query = """
                        update current_operday
                        set operday_date = '%s'
                    """;
            batmTransAgr.executeUpdate(query.formatted(date));
        }

        public static List<Map<String, Object>> getTransactionsData(String messageId) {
            String query = """
                    select
                    trans_date,
                    trans_type,
                    bank_product_type,
                    arn,
                    name_file_clearing,
                    auth_code,
                    sic,
                    rrn,
                    trn,
                    card_number,
                    atm_id,
                    amount,
                    currency,
                    amount_fee,
                    atm_migration
                    from clearing_data
                    where message_id = '%s'
                    """;
            return batmTransAgr.getRowLinesFromDB(query.formatted(messageId), TIMEOUT);
        }

        public static Map<String, Object> getTransactionsMetaData(String messageId) {
            String query = """
                        select response_date_datamart, response_status_datamart
                        from clearing_data
                        where message_id = '%s'
                    """;
            return batmTransAgr.getRowLinesFromDB(query.formatted(messageId), TIMEOUT).get(0);
        }

        public static Map<String, Object> getProductInstrIdAndTransId(String fileMask, String transType, String rrn) {
            String query = """
                        select product_instr_id, trans_id
                        from clearing_data
                        where name_file_clearing = '%s'
                        and trans_type = '%s'
                        and rrn = '%s'
                    """;
            return batmTransAgr.getRowLinesFromDB(query.formatted(fileMask, transType, rrn), TIMEOUT).get(0);
        }

        public static List<String> get20208AtmIds(List<String> originalAtmIds) {
            String query = """
                        select da.atm_id
                        from devices_acc da
                        left join pilot_accounting_devices pad2 on da.atm_id = pad2.atm_id
                        where da.atm_id in (%s)
                        and pad2.atm_id isnull
                        and account_number like '20208%%'
                    """;
            return getFirstColumnResults(batmTransAgr.getRowLinesFromDB(query.formatted(formatListForInStatement(originalAtmIds))));
        }

        public static String getNzrAccountNumber(String fileMask, String transType, String atmId, String... currency) {
            String currBuff = currency.length > 0 ? currency[0] : "643";
            String query = """
                    select account_number_nzr
                    from accounting a
                    inner join devices_acc da on a.branch_bank_nzr = da.base_branch and a.currency_code = da.currency_code
                    where a.mask_file_clearing = '%s'
                    and a.trans_type = '%s'
                    and da.currency_code = '%s'
                    and da.atm_id ='%s'
                    """;
            return getFirstColumnResults(batmTransAgr.getRowLinesFromDB(query.formatted(fileMask, transType, currBuff, atmId))).get(0);
        }

        public static String getNzrAccountNumberForCorpCards(String fileMask, String transType, String... currency) {
            String currBuff = currency.length > 0 ? currency[0] : "643";
            String query = """
                    select account_number_nzr
                    from accounting a
                    where a.mask_file_clearing = '%s'
                    and a.trans_type = '%s'
                    and a.currency_code = '%s'
                                      
                    """;
            return getFirstColumnResults(batmTransAgr.getRowLinesFromDB(query.formatted(fileMask, transType, currBuff))).get(0);
        }

        public static String getTransactionDescription(String fileMask, String transType, String atmId) {
            String query = """
                    select appointment
                    from accounting a
                    inner join devices_acc da on a.branch_bank_nzr = da.base_branch and a.currency_code = da.currency_code
                    where a.mask_file_clearing = '%s'
                    and a.trans_type = '%s'
                    and da.currency_code = '643'
                    and da.atm_id ='%s'
                    """;
            return getFirstColumnResults(batmTransAgr.getRowLinesFromDB(query.formatted(fileMask, transType, atmId))).get(0);
        }

        public static String getTransactionDescriptionForCorpCards(String fileMask, String transType) {
            String query = """
                    select appointment
                    from accounting a
                    where a.mask_file_clearing = '%s'
                    and a.trans_type = '%s'
                    and a.currency_code = '643'
                    """;
            return getFirstColumnResults(batmTransAgr.getRowLinesFromDB(query.formatted(fileMask, transType))).get(0);
        }

        public static Map<String, Object> getAccountRegisterIdAndType(String atmId, String... currency) {
            String currBuff = currency.length > 0 ? currency[0] : "643";
            String query = """
                    select account_register, account_register_type
                    from devices_acc da
                    where atm_id = '%s'
                    and currency_code = '%s'
                    """;
            return batmTransAgr.getRowLinesFromDB(query.formatted(atmId, currBuff)).get(0);
        }

        public static Map<String, String> getVkusCodes() {
            Map<String, String> result = new HashMap<>();
            String query = """
                        select mask_file_clearing, trans_type, vkus_code
                        from code_vkus cv
                    """;
            for (var line : batmTransAgr.getRowLinesFromDB(query)) {
                result.put(line.get("mask_file_clearing").toString() + line.get("trans_type").toString(), line.get("vkus_code").toString());
            }
            return result;
        }

        public static Map<String, Object> getBranchAndBaseBranchCodes(String atmId) {
            String query = """
                        select da.base_branch, bm.branch_2
                        from devices_acc da
                        inner join branch_map bm on da.base_branch = bm.branch_5
                        where da.atm_id = '%s'
                        and da.currency_code = '643'
                    """;
            return batmTransAgr.getRowLinesFromDB(query.formatted(atmId)).get(0);
        }

        public static List<String> getTerminalIdsToAggregator(boolean migrated, boolean... log) {
            String bool = migrated ? "T" : "F";
            boolean logBuff = log.length <= 0 || log[0];
            String query1 = """
                    select atm_id
                    from pilot_accounting_devices
                    where is_agregator = 'T'
                    """;
            List<String> aggregatorAtms = getFirstColumnResults(batmTransAgr.getRowLinesFromDB(query1, logBuff));
            String query2 = """
                    select atm_id
                    from device
                    where atm_id in (%s)
                    and atm_migration = '%s'
                    """;
            return getFirstColumnResults(batmClearing.getRowLinesFromDB(query2.formatted(
                    formatListForInStatement(aggregatorAtms),
                    bool
            ), logBuff));
        }

        public static List<String> getTerminalIdsToGenerator() {
            String query1 = """
                    select atm_id
                    from pilot_accounting_devices
                    where is_generator = 'T'
                    """;
            List<String> aggregatorAtms = getFirstColumnResults(batmTransAgr.getRowLinesFromDB(query1));
            String query2 = """
                    select atm_id
                    from device
                    where atm_id in (%s)
                    and atm_migration = 'T'
                    """;
            return getFirstColumnResults(batmClearing.getRowLinesFromDB(query2
                    .formatted(formatListForInStatement(aggregatorAtms))));
        }
    }

    public static class TransAgrSrv {
        private TransAgrSrv() {
        }

        public static Map<String, Object> getTransactionGpData(String id) {
            String query = """
                    select *
                    from transaction_gp
                    where trans_id = '%s'
                    """;
            return batmTransAgrSrv.getRowLinesFromDB(query.formatted(id), TIMEOUT).get(0);
        }

        public static Map<String, Object> getInstructionGpData(String id) {
            String query = """
                    select *
                    from instruction_gp
                    where trans_id = '%s'
                    """;
            return batmTransAgrSrv.getRowLinesFromDB(query.formatted(id), TIMEOUT).get(0);
        }

        public static Map<String, Object> getAccountingGpData(String id) {
            String query = """
                    select *
                    from accounting_gp
                    where id_transaction = '%s'
                    """;
            return batmTransAgrSrv.getRowLinesFromDB(query.formatted(id), TIMEOUT).get(0);
        }

        public static Map<String, Object> getClearingData(String id) {
            String query = """
                    select *
                    from clearing_data
                    where id = '%s'
                    """;
            return batmTransAgrSrv.getRowLinesFromDB(query.formatted(id), TIMEOUT).get(0);
        }

        public static String getCurrentBankDate() {
            String query = """
                    select operday
                    from current_operday
                    """;
            return getFirstColumnResults(batmTransAgrSrv.getRowLinesFromDB(query)).get(0);
        }

        public static void setCurrentBankDate(String date) {
            String query = """
                        update current_operday
                        set operday = '%s'
                    """;
            batmTransAgrSrv.executeUpdate(query.formatted(date));
        }

        public static String getBranchCodeByAbsBaseBranch(String absBaseBranch) {
            String query = """
                    select branch_code
                    from branch_transit
                    where branch_code_transit = '%s'
                    """;
            return batmTransAgrSrv.getFirstFieldFromDB(query.formatted(absBaseBranch));
        }

        public static String getRandomBranchCode(String... excludeOf) {
            String query = """
                    select branch_code
                    from branch_transit
                    """;
            List<String> branchCodes = getFirstColumnResults(batmTransAgrSrv.getRowLinesFromDB(query));
            if (excludeOf.length > 0)
                branchCodes.remove(excludeOf[0]);
            return CommonHelper.getRandomFromList(branchCodes);
        }
    }

    public static class OperSior {
        private OperSior() {
        }

        public static String getValidCreditProductRegId() {
            String query = """
                    select distinct credit_product_reg_id
                    from operation
                    where status = 'SUCCESS'
                    and credit_product_reg_id notnull
                    and register_type_kt = '60323_ProchDebitor_NedostachaUS'
                    """;
            return CommonHelper.getRandomFromList(getFirstColumnResults(batmOperSior.getRowLinesFromDB(query)));
        }

        public static String getValidDebitProductRegId() {
            String query = """
                    select distinct debit_product_reg_id
                    from operation
                    where status = 'SUCCESS'
                    and debit_product_reg_id notnull
                    and register_type_dt = '20208_USBanka'
                    """;
            return CommonHelper.getRandomFromList(getFirstColumnResults(batmOperSior.getRowLinesFromDB(query)));
        }

        public static Map<String, Object> getFinancialTransactionData(String msgId) {
            String query = """
                    select ft.sent_to_dp, ft.status, ft.updated, ft.id
                    from operation o
                    inner join financial_transaction ft on o.financial_transaction_id = ft.id
                    where o.message_id = '%s'
                    """;
            return batmOperSior.getRowLinesFromDB(query.formatted(msgId)).get(0);
        }

        public static Map<String, Object> getFinancialInstructionData(String msgId) {
            String query = """
                    select fi.sent_to_dp, fi.status, fi.updated, fi.id
                    from operation o
                    inner join financial_instruction fi on o.financial_instruction_id = fi.id
                    where o.message_id = '%s'
                    """;
            return batmOperSior.getRowLinesFromDB(query.formatted(msgId)).get(0);
        }

        public static Map<String, Object> getOperationData(String msgId) {
            String query = """
                    select *
                    from operation
                    where message_id = '%s'
                    """;
            return batmOperSior.getRowLinesFromDB(query.formatted(msgId), TIMEOUT).get(0);
        }
    }
}
