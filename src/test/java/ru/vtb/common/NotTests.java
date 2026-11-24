package ru.vtb.common;

import io.qameta.allure.TmsLink;
import org.joda.time.DateTime;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.common.Log;
import ru.vtb.core.data.static_data.B2LMask;
import ru.vtb.core.data.static_data.Bin;
import ru.vtb.core.data.static_data.dto.TransactionDto;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.BatmClearingOutBatmTransAgrDataRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.TransactionsItem;
import ru.vtb.core.utils.*;
import ru.vtb.core.utils.sql.SQLHelper;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static ru.vtb.core.common.EnvConstants.*;
import static ru.vtb.core.data.static_data.B2LMask.*;
import static ru.vtb.core.data.static_data.KafkaTopic.BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA;
import static ru.vtb.core.data.static_data.MapsKeysConstants.*;
import static ru.vtb.core.data.static_data.S3Constants.BATM_OUTPUT;
import static ru.vtb.core.data.static_data.S3Constants.IF_BATM_WAY_IN;

public class NotTests extends BaseAPIState {

    @Test(description = "list s3")
    public void listS3Files() {
        s3Service.getObjectsNames(IF_BATM_WAY_IN).forEach(Log::info);
    }

    @Test(description = "Тест для проверки работы генератора")
    public void testb2lGenerator() {
        String suffix = "04.223";
        double count = 6300;

        String n1 = "298cto" + suffix;
        File f1 = new File(TEMP_FILES_PATH + n1);

        String n2 = "CORP_038ctd" + suffix;
        File f2 = new File(TEMP_FILES_PATH + n2);

        String n3 = "CORP_038cto" + suffix;
        File f3 = new File(TEMP_FILES_PATH + n3);

        String n4 = "D_038cto" + suffix;
        File f4 = new File(TEMP_FILES_PATH + n4);

        String n5 = "OPS_038ctd" + suffix;
        File f5 = new File(TEMP_FILES_PATH + n5);

        String n6 = "VTB24_038cto" + suffix;
        File f6 = new File(TEMP_FILES_PATH + n6);

        String n7 = "038cts" + suffix;
        File f7 = new File(TEMP_FILES_PATH + n7);

        List<TransactionsItem> bodies = new LinkedList<>();
        List<String> expectedRRNs = new ArrayList<>();
        List<String> actualRRNs = new ArrayList<>();
        int temp = 0;

        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, f1);
        var t1 = ((BatmClearingOutBatmTransAgrDataRootMessage) KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, n1, long_timeout)).getTransactions();
        Log.debug("t1 - " + t1.size());
        bodies.addAll(t1);
        var myJson1 = CommonHelper.deserializeJsonToObject(UfxHelper.generateJsonUfxFileFromBase2Like(n1), BatmClearingOutBatmTransAgrDataRootMessage.class);
        for (var t : myJson1.getTransactions()) {
            expectedRRNs.add(t.getRrn());
        }

        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, f2);
        var t2 = ((BatmClearingOutBatmTransAgrDataRootMessage) KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, n2, long_timeout)).getTransactions();
        Log.debug("t2 - " + t2.size());
        bodies.addAll(t2);
        var myJson2 = CommonHelper.deserializeJsonToObject(UfxHelper.generateJsonUfxFileFromBase2Like(n2), BatmClearingOutBatmTransAgrDataRootMessage.class);
        for (var t : myJson1.getTransactions()) {
            expectedRRNs.add(t.getRrn());
        }

        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, f3);
        var t3 = ((BatmClearingOutBatmTransAgrDataRootMessage) KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, n3, long_timeout)).getTransactions();
        Log.debug("t3 - " + t3.size());
        bodies.addAll(t3);
        var myJson3 = CommonHelper.deserializeJsonToObject(UfxHelper.generateJsonUfxFileFromBase2Like(n3), BatmClearingOutBatmTransAgrDataRootMessage.class);
        for (var t : myJson1.getTransactions()) {
            expectedRRNs.add(t.getRrn());
        }

        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, f4);
        var t4 = ((BatmClearingOutBatmTransAgrDataRootMessage) KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, n4, long_timeout)).getTransactions();
        Log.debug("t4 - " + t4.size());
        bodies.addAll(t4);
        var myJson4 = CommonHelper.deserializeJsonToObject(UfxHelper.generateJsonUfxFileFromBase2Like(n4), BatmClearingOutBatmTransAgrDataRootMessage.class);
        for (var t : myJson4.getTransactions()) {
            expectedRRNs.add(t.getRrn());
        }

        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, f5);
        var t5 = ((BatmClearingOutBatmTransAgrDataRootMessage) KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, n5, long_timeout)).getTransactions();
        Log.debug("t5 - " + t5.size());
        bodies.addAll(t5);
        var myJson5 = CommonHelper.deserializeJsonToObject(UfxHelper.generateJsonUfxFileFromBase2Like(n5), BatmClearingOutBatmTransAgrDataRootMessage.class);
        for (var t : myJson5.getTransactions()) {
            expectedRRNs.add(t.getRrn());
        }

        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, f6);
        var t6 = ((BatmClearingOutBatmTransAgrDataRootMessage) KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, n6, long_timeout)).getTransactions();
        Log.debug("t6 - " + t6.size());
        bodies.addAll(t6);
        var myJson6 = CommonHelper.deserializeJsonToObject(UfxHelper.generateJsonUfxFileFromBase2Like(n6), BatmClearingOutBatmTransAgrDataRootMessage.class);
        for (var t : myJson6.getTransactions()) {
            expectedRRNs.add(t.getRrn());
        }

        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, f7);
        var t7 = ((BatmClearingOutBatmTransAgrDataRootMessage) KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, n7, long_timeout)).getTransactions();
        Log.debug("t7 - " + t7.size());
        bodies.addAll(t7);
        var myJson7 = CommonHelper.deserializeJsonToObject(UfxHelper.generateJsonUfxFileFromBase2Like(n7), BatmClearingOutBatmTransAgrDataRootMessage.class);
        for (var t : myJson7.getTransactions()) {
            expectedRRNs.add(t.getRrn());
        }
        for (var t : bodies) {
            actualRRNs.add(t.getRrn());
        }
        for (String rrn : actualRRNs) {
            if (!expectedRRNs.contains(rrn)) {
                Log.debug(rrn);
            }
        }


        softAssertEquals(
                bodies.size(),
                (int) count,
                "Batm clearing обработал не все транзакции в json");
        softAssertAll();
    }

    @Test(description = "debug mail")
    public void debugMail() {
        Log.info("There is mail");
        String text = "Debug mail in %s".formatted(CommonHelper.getCurrentDateInFormat("(z) yyyy-MM-dd'T'HH.mm.ss:SSS"));
        Mailer mailer = MailerBuilder
                .withSMTPServer(SMTP_HOST, 587, SMTP_LOGIN, SMTP_PASSWORD)
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .withDebugLogging(true)
                .buildMailer();
        Email email = EmailBuilder.startingBlank()
                .from("at_si_batm@vtb.ru")
                .to("nazabelin@vtb.ru")
                .withSubject("Debug subject")
                .withPlainText(text)
                .buildEmail();
        mailer.sendMail(email);
        Log.info("Message was sent");
    }

    @Test(description = "UAT")
    public void generateDataForUAT() {
        String maskPostfix = "01"
                .concat(".")
                .concat(String.format("%03d", Integer.parseInt(new DateTime().dayOfYear().getAsString())));

        List<String[]> migratedCorp = new ArrayList<>();
        List<String[]> notMigratedCorp = new ArrayList<>();
        List<String[]> migratedNotCorp = new ArrayList<>();
        List<String[]> notMigratedNotCorp = new ArrayList<>();

        List<String[]> reversalMigratedCorp = new ArrayList<>();
        List<String[]> reversalNotMigratedCorp = new ArrayList<>();
        List<String[]> reversalMigratedNotCorp = new ArrayList<>();
        List<String[]> reversalNotMigratedNotCorp = new ArrayList<>();

        String b2l298cto = B2LHelper.generateBase2LikeFileName(_298cto, maskPostfix);
        String b2lCORP_038cto = B2LHelper.generateBase2LikeFileName(CORP_038cto, maskPostfix);
        String b2lCORP_038ctd = B2LHelper.generateBase2LikeFileName(CORP_038ctd, maskPostfix);
        String b2lVTB24_038cto = B2LHelper.generateBase2LikeFileName(VTB24_038cto, maskPostfix);
        String b2lD_038cto = B2LHelper.generateBase2LikeFileName(D_038cto, maskPostfix);
        String b2lOPS_038ctd = B2LHelper.generateBase2LikeFileName(OPS_038ctd, maskPostfix);
        String b2l038cts = B2LHelper.generateBase2LikeFileName(_038cts, maskPostfix);
        List<String> filenames = List.of(b2l298cto, b2lCORP_038cto, b2lCORP_038ctd, b2lVTB24_038cto, b2lD_038cto, b2lOPS_038ctd, b2l038cts);

        //todo добить до реального количества терминалов.
        List<String> migratedAtms = List.of("370256", "377735");
        List<String> notMigratedAtms = List.of("398661", "397507");
        List<String> atms = List.of("370256", "377735", "398661", "397507");

        for (String filename : filenames) {
            List<TransactionDto> buff = new ArrayList<>();
            List<Bin> bins = filename.equals(b2lD_038cto) ? Arrays.asList(Bin.values()) : Bin.getOnlyInternalVtbBins();
            for (String atmId : atms) {
                for (Bin bin : bins) {
                    switch (B2LMask.recognize(filename)) {
                        case _298cto -> {
                            buff.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6011")));
                        }
                        case CORP_038cto -> {
                            buff.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6011")));
                            buff.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6012")));
                        }
                        case CORP_038ctd -> {
                            buff.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6011")));
                            buff.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6012")));
                        }
                        //todo тут убрали 6012
                        case OPS_038ctd -> {
                            buff.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6011")));
                        }
                        case VTB24_038cto -> {
                            buff.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6011")));
                            buff.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6012")));
                        }
                        case D_038cto -> {
                            buff.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6011",
                                    AMOUNT_FEE, String.valueOf(ThreadLocalRandom.current().nextInt(10, 9999)))));
                            buff.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6536",
                                    AMOUNT_FEE, String.valueOf(ThreadLocalRandom.current().nextInt(10, 9999)))));
                            buff.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6011",
                                    AMOUNT_FEE, String.valueOf(ThreadLocalRandom.current().nextInt(10, 9999)))));
                            buff.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6012",
                                    AMOUNT_FEE, String.valueOf(ThreadLocalRandom.current().nextInt(10, 9999)))));
                            buff.add(B2LHelper.getNewCashInReversedTransactionReference(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6011",
                                    AMOUNT_FEE, String.valueOf(ThreadLocalRandom.current().nextInt(10, 9999)))));
                            buff.add(B2LHelper.getNewCashInReversedTransactionReference(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6012",
                                    AMOUNT_FEE, String.valueOf(ThreadLocalRandom.current().nextInt(10, 9999)))));
                        }
                        case _038cts -> {
                            buff.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6011")).addSelfCollectionAttributes());
                            buff.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                                    ACCOUNT_NUMBER, bin.genRandAccNum(),
                                    TERMINAL_ID, atmId,
                                    MCC, "6012")).addSelfCollectionAttributes());
                        }
                    }
                }
            }
            B2LHelper.generateBase2LikeFile(filename, buff);
            Log.debug(filename + " - " + buff.size());
            for (TransactionDto dto : buff) {

                B2LMask mask = B2LMask.recognize(filename);
                boolean isCorp = mask.isCorp();
                boolean isReversal = false;
                String operType;
                String aggPrefix;
                String atmId = dto.getTerminalID().trim();
                String accNum = getTerminalAccNum(atmId);
                String debit;
                String credit;
                switch (dto.getTC()) {
                    case "06" -> {
                        operType = "Cash-In";
                        aggPrefix = isCorp ? "WAY4_P_" : "BATM_P_";
                        debit = accNum;
                        credit = isCorp ? getCorpNzrAccNum(mask.getName(), operType) : getNotCorpNzrAccNum(mask.getName(), operType, atmId);
                    }
                    case "07" -> {
                        operType = "Cash-Withdrawal";
                        aggPrefix = isCorp ? "WAY4_P_" : "BATM_P_";
                        debit = isCorp ? getCorpNzrAccNum(mask.getName(), operType) : getNotCorpNzrAccNum(mask.getName(), operType, atmId);
                        credit = accNum;
                    }
                    case "26" -> {
                        operType = "Reversal-Cash-In";
                        aggPrefix = isCorp ? "WAY4_R_" : "BATM_R_";
                        isReversal = true;
                        debit = isCorp ? getCorpNzrAccNum(mask.getName(), operType) : getNotCorpNzrAccNum(mask.getName(), operType, atmId);
                        credit = accNum;
                    }
                    case "27" -> {
                        operType = "Reversal-Cash-Withdrawal";
                        aggPrefix = isCorp ? "WAY4_R_" : "BATM_R_";
                        isReversal = true;
                        debit = accNum;
                        credit = isCorp ? getCorpNzrAccNum(mask.getName(), operType) : getNotCorpNzrAccNum(mask.getName(), operType, atmId);
                    }
                    default -> {
                        operType = "error";
                        aggPrefix = "error";
                        debit = "error";
                        credit = "error";
                    }
                }

                if (migratedAtms.contains(atmId) && isCorp && !isReversal) {
                    migratedCorp.add(fillCsvLine(filename, mask.getName(), aggPrefix, credit, operType, dto, debit));
                }
                if (notMigratedAtms.contains(atmId) && isCorp && !isReversal) {
                    notMigratedCorp.add(fillCsvLine(filename, mask.getName(), aggPrefix, credit, operType, dto, debit));
                }
                if (migratedAtms.contains(atmId) && !isCorp && !isReversal) {
                    migratedNotCorp.add(fillCsvLine(filename, mask.getName(), aggPrefix, credit, operType, dto, debit));
                }
                if (notMigratedAtms.contains(atmId) && !isCorp && !isReversal) {
                    notMigratedNotCorp.add(fillCsvLine(filename, mask.getName(), aggPrefix, credit, operType, dto, debit));
                }

                //reversal
                if (migratedAtms.contains(atmId) && isCorp && isReversal) {
                    reversalMigratedCorp.add(fillCsvLine(filename, mask.getName(), aggPrefix, credit, operType, dto, debit));
                }
                if (notMigratedAtms.contains(atmId) && isCorp && isReversal) {
                    reversalNotMigratedCorp.add(fillCsvLine(filename, mask.getName(), aggPrefix, credit, operType, dto, debit));
                }
                if (migratedAtms.contains(atmId) && !isCorp && isReversal) {
                    reversalMigratedNotCorp.add(fillCsvLine(filename, mask.getName(), aggPrefix, credit, operType, dto, debit));
                }
                if (notMigratedAtms.contains(atmId) && !isCorp && isReversal) {
                    reversalNotMigratedNotCorp.add(fillCsvLine(filename, mask.getName(), aggPrefix, credit, operType, dto, debit));
                }
            }
        }
        Comparator<String[]> comp = Comparator.comparing(a -> a[4]);
        migratedCorp.sort(comp);
        notMigratedCorp.sort(comp);
        migratedNotCorp.sort(comp);
        notMigratedNotCorp.sort(comp);

        //reversal
        reversalMigratedCorp.sort(comp);
        reversalNotMigratedCorp.sort(comp);
        reversalMigratedNotCorp.sort(comp);
        reversalNotMigratedNotCorp.sort(comp);

        String[] header = new String[]{"Filename", "Mask", "Operation Type", "Atm id", "RRN", "Auth code", "Aggregation id", "MCC", "Debit", "Credit", "Amount", "Base branch", "Branch 2", "Date + Time"};

        migratedCorp.add(0, header);
        notMigratedCorp.add(0, header);
        migratedNotCorp.add(0, header);
        notMigratedNotCorp.add(0, header);

        //reversal
        reversalMigratedCorp.add(0, header);
        reversalNotMigratedCorp.add(0, header);
        reversalMigratedNotCorp.add(0, header);
        reversalNotMigratedNotCorp.add(0, header);

        CSVUtils.writeCSVToFile(migratedCorp, TEMP_FILES_PATH + "migratedCorp.csv");
        CSVUtils.writeCSVToFile(notMigratedCorp, TEMP_FILES_PATH + "notMigratedCorp.csv");
        CSVUtils.writeCSVToFile(migratedNotCorp, TEMP_FILES_PATH + "migratedNotCorp.csv");
        CSVUtils.writeCSVToFile(notMigratedNotCorp, TEMP_FILES_PATH + "notMigratedNotCorp.csv");

        //reversal
        CSVUtils.writeCSVToFile(reversalMigratedCorp, TEMP_FILES_PATH + "reversalMigratedCorp.csv");
        CSVUtils.writeCSVToFile(reversalNotMigratedCorp, TEMP_FILES_PATH + "reversalNotMigratedCorp.csv");
        CSVUtils.writeCSVToFile(reversalMigratedNotCorp, TEMP_FILES_PATH + "reversalMigratedNotCorp.csv");
        CSVUtils.writeCSVToFile(reversalNotMigratedNotCorp, TEMP_FILES_PATH + "reversalNotMigratedNotCorp.csv");
    }

    private static String[] fillCsvLine(String filename, String mask, String aggPrefix, String credit, String operType, TransactionDto dto, String debit) {
        String amount = String.format(Locale.US, "%.2f", Double.parseDouble(dto.getDestinationAmount()) / 100);
        String atmID = dto.getTerminalID().trim();
        String rrn = dto.getRetrievalReferenceNumber();
        String authCode = dto.getAuthorizationCode();
        var branches = getBranches(atmID);
//              "Filename", "Mask", "Operation Type", "Atm id", "RRN", "Auth code", "Aggregation id", "MCC", "Acc Number", "Acc Number Nzr", "Amount", "Base branch", "Branch 2", "Date + Time"});
        return new String[]{
                filename,
                mask,
                operType,
                atmID,
                rrn,
                authCode,
                aggPrefix + rrn + authCode,
                dto.getMerchantCategoryCode(),
                debit,
                credit,
                amount,
                branches.get("base_branch").toString(),
                branches.get("branch_2").toString(),
                CommonHelper.parseDateFromPatternToAnotherPattern("yyyyMMddHHmmss",
                        "yyyy-MM-dd HH:mm:ss",
                        dto.getTransactionDate() + dto.getTransactionTime())};
    }

    private static String getCorpNzrAccNum(String mask, String transType) {
        String query = """
                select account_number_nzr
                from accounting
                where mask_file_clearing = '%s'
                and currency_code = '643'
                and trans_type = '%s'
                """;
        return new SQLHelper("batm_transactions_agr").getFirstFieldFromDB(query.formatted(mask, transType));
    }

    private static String getNotCorpNzrAccNum(String mask, String transType, String atmId) {
        String query = """
                select a.account_number_nzr from accounting a
                inner join devices_acc da on da.base_branch = a.branch_bank_nzr and da.currency_code =a.currency_code
                where a.mask_file_clearing = '%s'
                and a.currency_code ='643'
                and a.trans_type = '%s'
                and da.atm_id = '%s'
                """;
        return new SQLHelper("batm_transactions_agr").getFirstFieldFromDB(query.formatted(mask, transType, atmId));
    }

    private static String getTerminalAccNum(String atmId) {
        String query = """
                select account_number  from devices_acc da where currency_code = '643'
                and atm_id = '%s'
                """;
        return new SQLHelper("batm_transactions_agr").getFirstFieldFromDB(query.formatted(atmId));
    }

    private static Map<String, Object> getBranches(String atmId) {
        String query = """
                select da.base_branch, bm.branch_2  from devices_acc da
                inner join branch_map bm on da.base_branch =bm.branch_5
                where da.atm_id='%s' and da.currency_code ='643'
                """;
        return new SQLHelper("batm_transactions_agr").getRowLinesFromDB(query.formatted(atmId)).get(0);
    }
}
