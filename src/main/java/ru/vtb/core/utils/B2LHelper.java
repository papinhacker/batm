package ru.vtb.core.utils;

import ru.vtb.core.data.static_data.B2LMask;
import ru.vtb.core.data.static_data.dto.TransactionDto;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static ru.vtb.core.common.EnvConstants.FILE_SEPARATOR;
import static ru.vtb.core.common.EnvConstants.TEMP_FILES_FOLDER;
import static ru.vtb.core.data.static_data.MapsKeysConstants.AMOUNT_FEE;

public class B2LHelper {

    public static List<TransactionDto> getBase2LikeTransactionsFromFile(String path) {
        List<TransactionDto> transactions = new LinkedList<>();

        List<String> lines = FileHelper.readLinesFromTextFile(path);
        TransactionDto currentTransaction = new TransactionDto();
        for (String line : lines) {
            if (line.substring(0, 2).matches("06|26|07|27|99") && !currentTransaction.addLine(line)) {
                transactions.add(currentTransaction);
                currentTransaction = new TransactionDto();
                currentTransaction.addLine(line);
            }
        }
        return transactions;
    }

    public static File generateBase2LikeFile(String path, Map<String, String> data, int... count) {
        List<TransactionDto> transactions = generateBase2LikeTransactions(data, count.length > 0 ? count[0] : 1);
        return generateBase2LikeFile(path, transactions);
    }

    public static File generateBase2LikeFile(String filename, TransactionDto transaction) {
        return generateBase2LikeFile(filename, List.of(transaction));
    }

    public static File generateBase2LikeFile(String filename, List<TransactionDto> transactions) {
        String path = TEMP_FILES_FOLDER + FILE_SEPARATOR + filename;
        List<String> lines = new ArrayList<>();
        for (TransactionDto dto : transactions) {
            lines.addAll(dto.getAllInitializedLines());
        }
        lines.add("9999");
        return FileHelper.writeLinesToFile(lines, path);
    }

    private static List<TransactionDto> generateBase2LikeTransactions(Map<String, String> data, int count) {
        List<TransactionDto> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            transactions.add(getNewCashInTransactionReference(data));
        }
        return transactions;
    }

    public static String generateBase2LikeFileName(B2LMask mask, String... postfix) {
        List<Integer> ints = new ArrayList<>();
        ints.add(ThreadLocalRandom.current().nextInt(1, 6));
        ints.add(ThreadLocalRandom.current().nextInt(21, 26));
        String buff = postfix.length > 0 ? postfix[0] : String.format("%02d", ints.get(ThreadLocalRandom.current().nextInt(ints.size())))
                .concat(".")
                .concat(CommonHelper.getNumericDayOfYearBeforeCurrentDate());
        return mask.getName().concat(buff);

    }

    public static String substringFileMask(String b2lFilename) {
        return b2lFilename.replaceAll("(\\d{2}\\.\\d{3})$", "");
    }

    @Deprecated(forRemoval = true)
    public static TransactionDto getNewPurchaseByCashTransactionReference() {
        TransactionDto dto = new TransactionDto();
        dto.setZeroLine("0620AAAAAAAAAAAAAAAA000   78888841292900000173488100618511018000000300000643000000100000643VTB Bank                 Moskva       RU 490000000     1009N2082325 0 0100000");
        dto.setFirstLine("0621            000000 2200244602296863                                         VB24           151190  000000000000  000000  0 9                             000000000  ");
        dto.setThirdLine("0623            BBAT=GA;F103=402684;F111.2=429267012699;F111.4=42926;                                                                                                   ");
        dto.setFifthLine("0625               000000100000643          0000 000000000000                              000000000000000 0000000000000000000000000000              0000000000000000   ");
        dto.setEighthLine("0628202410181206174292670126992407090151190  VB24           16    q 888884      8032Altufevskoe shosse 1          127562    206                    00000000             ");
        return dto;
    }

    @Deprecated(forRemoval = true)
    public static TransactionDto getNewPurchaseByCashReversedTransactionReference() {
        TransactionDto dto = new TransactionDto();
        dto.setZeroLine("2620AAAAAAAAAAAAAAAA000   78888841292900000173488100618511018000000300000643000000100000643VTB Bank                 Moskva       RU 490000000     1009N2082325 0 0100000");
        dto.setFirstLine("2621            000000 2200244602296863                                         VB24           151190  000000000000  000000  0 9                             000000000  ");
        dto.setThirdLine("2623            BBAT=GA;F103=402684;F111.2=429267012699;F111.4=42926;                                                                                                         ");
        dto.setFifthLine("2625               000000100000643          0000 000000000000                              000000000000000 0000000000000000000000000000              0000000000000000   ");
        dto.setEighthLine("2628202410181206174292670126992407090151190  VB24           16    q 888884      8032Altufevskoe shosse 1          127562    206                    00000000             ");
        return dto;
    }

    public static TransactionDto getNewCashInTransactionReference(Map<String, String> data) {
        TransactionDto dto = new TransactionDto();
        dto.setZeroLine("06204893470540070387000   78888844292900000172751100418511018000000010000643000000010000643VTB Bank                 Moskva       RU 601100000     1009N2594925 2 0500000");
        dto.setFirstLine("0621            000000                                                          VB24           387053  000000000000  000000  0 9                             000000000  ");
        dto.setThirdLine("0623            CR                                                                                                                                                      ");
        dto.setFifthLine("0625               00000001000064300        0000 000000000000                              000000000000000 0000000000000000000000000000              0000000000000000   ");
        dto.setSeventhLine("062720001241018604020643        C8E9A81D01EE3C003D97CBB3ECC81780010A800004000003A000000000000000000F030000160000000006000000D32CFE84                                    ");
        dto.setEighthLine("0628202410181430434292670025872503001387053  VB24           16    q 888884      8023Altufevskoe shosse 1          127562    201                    00000000             ");
        return dto.generateRandomData().setSpecificData(data);
    }

    public static TransactionDto getNewCashInReversedTransactionReference(Map<String, String> data) {
        TransactionDto dto = new TransactionDto();
        dto.setZeroLine("26204893470540070387000   78888844292900000172751100418511018000000010000643000000010000643VTB Bank                 Moskva       RU 601100000     1009N2594925 2 0500000");
        dto.setFirstLine("2621            000000                                                          VB24           387053  000000000000  000000  0 9                             000000000  ");
        dto.setThirdLine("2623            CR                                                                                                                                                      ");
        dto.setFifthLine("2625               00000001000064300        0000 000000000000                              000000000000000 0000000000000000000000000000              0000000000000000   ");
        dto.setSeventhLine("262720001241018604020643        C8E9A81D01EE3C003D97CBB3ECC81780010A800004000003A000000000000000000F030000160000000006000000D32CFE84                                    ");
        dto.setEighthLine("2628202410181430434292670025872503001387053  VB24           16    q 888884      8023Altufevskoe shosse 1          127562    201                    00000000             ");
        return dto.generateRandomData().setSpecificData(data);
    }

    public static TransactionDto getNewWithdrawTransactionReference(Map<String, String> data) {
        TransactionDto dto = new TransactionDto();
        dto.setZeroLine("07005368292015341868000   78888844292900000172868100418511018000000010000643000000010000643VTB Bank CASH            Moskva       RU 601100000   9 1009N2908305 2 0500000");
        dto.setFirstLine("0701            000000                                                          VB24           387053  000000000000  000000  0 9 0                           000000000  ");
        dto.setFifthLine("0705               00000001000064300        0000 000000000000                              000000000000000 0000000000000000000000000000              0000000000000000   ");
        dto.setSeventhLine("070701001241018604020643        6602E0B3014E39008F9B055AEDF5107F10A08000040000000322000000000100000000000000000000000100FF                                              ");
        dto.setEighthLine("0708202410181448354292670127103103001387053  VB24           16    q 888884      8018Altufevskoe shosse 1          127562    201                    00000000             ");
        return dto.generateRandomData().setSpecificData(data);
    }

    public static TransactionDto getNewWithdrawReversedTransactionReference(Map<String, String> data) {
        TransactionDto dto = new TransactionDto();
        dto.setZeroLine("27005368292015341868000   78888844292900000172868100418511018000000010000643000000010000643VTB Bank CASH            Moskva       RU 601100000   9 1009N2908305 2 0500000");
        dto.setFirstLine("2701            000000                                                          VB24           387053  000000000000  000000  0 9 0                           000000000  ");
        dto.setFifthLine("2705               00000001000064300        0000 000000000000                              000000000000000 0000000000000000000000000000              0000000000000000   ");
        dto.setSeventhLine("270701001241018604020643        6602E0B3014E39008F9B055AEDF5107F10A08000040000000322000000000100000000000000000000000100FF                                              ");
        dto.setEighthLine("2708202410181448354292670127103103001387053  VB24           16    q 888884      8018Altufevskoe shosse 1          127562    201                    00000000             ");
        return dto.generateRandomData().setSpecificData(data);
    }

    public static TransactionDto getNewSelfCollectionCashInTransactionReference(Map<String, String> data) {
        TransactionDto dto = new TransactionDto();
        dto.setZeroLine("06200000000000000000000   78888846209905532827977100418510728000000000000   000000010000643Bank VTB (PAO)           g. Moskva    RU 601200000     1009NI9AE9K5 0 0100000");
        dto.setFirstLine("0621            000000                                                          VB24           375273  000000000000  000000  0 9                             000000000  ");
        dto.setThirdLine("0623            BBAT=GA;F103=8;F104=8729984720;F111.2=000000000220;F111.4=00000;F111.5=621350898620980306;                                                              ");
        dto.setEighthLine("0628202607281835380000000002200000   375273  VB24           00    L               00D. 1, SH. ALTUF'EVSKOE        127106                           00000000             ");
        return dto.setSpecificData(data).addSelfCollectionAttributes();
    }

    public static String convertB2lFileNameToUfx(String b2l) {
        String currentYear = new SimpleDateFormat("yyyy").format(new Date());
        return b2l
                .replace("CORP_", "")
                .replace("D_038", "238")
                .replace("VTB24_038", "138")
                .replace("cto", "ctoufx")
                .replace("ctd", "ctdufx")
                .replace("cta", "ctaufx")
                .concat("_").concat(currentYear).concat(".xml");
    }

    public static String convertB2lFileNameToJsonUfx(String b2l) {
        String currentYear = new SimpleDateFormat("yyyy").format(new Date());
        return b2l.concat("_").concat(currentYear).concat(".json");
    }

    public static TransactionDto copyTransactionData(TransactionDto from, TransactionDto to) {
        if (to.getAllInitializedLines().isEmpty() || from.getAllInitializedLines().isEmpty()) {
            throw new RuntimeException("Both classes must be initialized before copying transaction data");
        }
        to.setTransactionDate(from.getTransactionDate());
        to.setTransactionTime(from.getTransactionTime());
        to.setAcquirerReferenceNumber(from.getAcquirerReferenceNumber());
        to.setAuthorizationCode(from.getAuthorizationCode());
        to.setRetrievalReferenceNumber(from.getRetrievalReferenceNumber());
        to.setTransactionIdentifier(from.getTransactionIdentifier());
        to.setMerchantCategoryCode(from.getMerchantCategoryCode());
        to.setMerchantCountryCode(from.getMerchantCountryCode());
        to.setMerchantCity(from.getMerchantCity());
        to.setMerchantName(from.getMerchantName());
        to.setCardAcceptorID(from.getCardAcceptorID());
        to.setTerminalID(from.getTerminalID());
        to.setAccountNumber(from.getAccountNumber());
        to.setCardExpirationDate(from.getCardExpirationDate());
        to.setDestinationChannel(from.getDestinationChannel());
        to.setDestinationCurrencyCode(from.getDestinationCurrencyCode());
        to.setDestinationAmount(from.getDestinationAmount());
        to.setDataContent(from.getDataContent());
        to.setSurchargeAmount(from.getSurchargeAmount());
        return to;
    }

    public static List<TransactionDto> getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map<String, String> data) {
        TransactionDto withdrawTransaction = B2LHelper.getNewWithdrawTransactionReference(data);
        TransactionDto withdrawReversedTransaction = B2LHelper.getNewWithdrawReversedTransactionReference(data);
        B2LHelper.copyTransactionData(withdrawTransaction, withdrawReversedTransaction);
        return List.of(withdrawTransaction, withdrawReversedTransaction);
    }

    public static List<TransactionDto> getBundleOfCashInAndCashInReversedTransactions(Map<String, String> data) {
        TransactionDto cashInTransaction = B2LHelper.getNewCashInTransactionReference(data);
        TransactionDto cashInReversedTransaction = B2LHelper.getNewCashInReversedTransactionReference(data);
        B2LHelper.copyTransactionData(cashInTransaction, cashInReversedTransaction);
        return List.of(cashInTransaction, cashInReversedTransaction);
    }

    public static List<TransactionDto> getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map<String, String> data) {
        List<TransactionDto> trans = new ArrayList<>(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(data));
        trans.add(B2LHelper.getNewCashInTransactionReference(data));
        return trans;
    }

    public static List<TransactionDto> getAllTransactionTypes(Map<String, String> data) {
        var cashIn = new ArrayList<>(getBundleOfCashInAndCashInReversedTransactions(data));
        var withdraw = new ArrayList<>(getBundleOfWithdrawalAndWithdrawalReversedTransactions(data));
        cashIn.addAll(withdraw);
        return cashIn;
    }
}
