package ru.vtb.core.utils;

import ru.vtb.core.data.static_data.B2LMask;
import ru.vtb.core.data.static_data.dto.TransactionDto;
import ru.vtb.core.models.common.CommonModelHelper;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.BatmClearingOutBatmTransAgrDataRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.TransactionsItem;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static ru.vtb.core.common.EnvConstants.FILE_SEPARATOR;
import static ru.vtb.core.common.EnvConstants.TEMP_FILES_FOLDER;

public class UfxHelper {

    public static String generateJsonUfxFileFromBase2Like(String filename) {
        String json = generateJsonUfxFromBase2Like(filename);
//        Log.debug("Generated json ufx file: " + json);
        return json;
    }

    public static String generateJsonUfxFromBase2Like(String filename) {
        String pathToBase2Like = TEMP_FILES_FOLDER + FILE_SEPARATOR + filename;
        return CommonHelper.serializeToJsonStringIgnoringNullableFields(
                generateJSONUfxModelsBasedOnBase2LikeTransactions(
                        B2LHelper.getBase2LikeTransactionsFromFile(pathToBase2Like),
                        filename));
    }

    private static boolean isProcessableTransaction(TransactionDto dto, String b2lFilename) {
        if (dto.isP2P())
            return false;
        String mcc = dto.getMerchantCategoryCode();
        String tc = dto.getTC();
        switch (B2LMask.recognize(b2lFilename)) {
            case _298cto -> {
                if (tc.matches("06|07|27") && mcc.equals("6011"))
                    return true;
            }
            case CORP_038cto -> {
                if (tc.matches("07|27") && mcc.matches("6011|6012"))
                    return true;
            }
            case CORP_038ctd, OPS_038ctd, _038cts -> {
                if (tc.matches("06") && mcc.matches("6011|6012"))
                    return true;
            }
            case VTB24_038cto -> {
                if (tc.matches("06|07|27") && mcc.matches("6011|6012"))
                    return true;
            }
            case D_038cto -> {
                switch (tc) {
                    case "06" -> {
                        if (mcc.matches("6011|6012|6536"))
                            return true;
                    }
                    case "26", "07", "27" -> {
                        if (mcc.matches("6011|6012"))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public static BatmClearingOutBatmTransAgrDataRootMessage generateJSONUfxModelsBasedOnBase2LikeTransactions(List<TransactionDto> transactions, String b2lFilename) {
        List<TransactionsItem> bodies = new LinkedList<>();
        for (TransactionDto dto : transactions) {
            if (isProcessableTransaction(dto, b2lFilename)) {
                bodies.add(generateJsonBodyModelBasedOnBase2LikeTransaction(dto, b2lFilename));
            }
        }

        return BatmClearingOutBatmTransAgrDataRootMessage.builder()
                .messageId(CommonModelHelper.generateRandomMessageId())
                .transactions(bodies)
                .build();
    }

    private static TransactionsItem generateJsonBodyModelBasedOnBase2LikeTransaction(TransactionDto dto, String b2lFilename) {
        String date = dto.getTransactionDate();
        String time = dto.getTransactionTime();
        String dateTime = CommonHelper.parseDateFromPatternToAnotherPattern("yyyyMMddHHmmss",
                "yyyy-MM-dd'T'HH:mm:ss",
                date + time);

        String transTypeCode;
        switch (dto.getTC()) {
            case "06" -> transTypeCode = "Cash-In";
            case "07" -> transTypeCode = "Cash-Withdrawal";
            case "26" -> transTypeCode = "Reversal-Cash-In";
            case "27" -> transTypeCode = "Reversal-Cash-Withdrawal";
            default -> throw new IllegalStateException("Unexpected value: " + dto.getTC());
        }

        String bankProductType;
        B2LMask fileMask = B2LMask.recognize(b2lFilename);
        switch (fileMask) {
            case CORP_038ctd, CORP_038cto, _038cts -> bankProductType = "CORP_CARD";
            case _298cto -> bankProductType = "ONUS_CARD";
            case D_038cto, VTB24_038cto, OPS_038ctd -> bankProductType = "ExVTB24";
            default -> throw new IllegalStateException("Файл '%s' имеет неизвестный парефикс".formatted(b2lFilename));
        }
        String tag;
        String scAuthCode = null;
        String scRrn = null;
        String scAmount = null;
        String scCurrency = null;
        boolean sc = dto.isSelfCollection() && fileMask.equals(B2LMask._038cts);
        if (sc) {
            tag = dto.getF1115Tag();
            scAuthCode = tag.substring(0, 6);
            scRrn = tag.substring(6);
            scAmount = dto.getSelfCollectionAmount();
            scCurrency = dto.getSelfCollectionCurrencyCode();
        }
        String accountNumber = dto.getAccountNumber();
        accountNumber = accountNumber.substring(16, 19).matches("[0]+") ? accountNumber.substring(0, 16) : accountNumber;
        String terminalId = dto.getTerminalID().trim();
        String fee = dto.getSurchargeAmount();
        return TransactionsItem.builder()
                .transDate(dateTime)
                .transType(transTypeCode)
                .bankProductType(bankProductType)
                .arn(dto.getAcquirerReferenceNumber())
                .nameFile(b2lFilename)
                .authCode(sc ? scAuthCode : dto.getAuthorizationCode())
                .rrn(sc ? scRrn : dto.getRetrievalReferenceNumber())
                .mcc(dto.getMerchantCategoryCode())
                .binRange(accountNumber.substring(0, 8))
                .atmId(terminalId)
                .currency(sc ? scCurrency : dto.getDestinationCurrencyCode())
                .amount(CommonHelper.formatDouble(Double.parseDouble(sc ? scAmount : dto.getDestinationAmount()) / 100))
                .amountFee(fee != null && !fee.matches("0+") ? Double.parseDouble(dto.getSurchargeAmount()) / 100 : null)
                .atmMigration(TransactionDto.FL_TO_JSON_M_T_TERMINALS.contains(dto.getTerminalID().trim()) && (fileMask.equals(B2LMask._298cto)) ? true : null)
                .build();
    }
}
