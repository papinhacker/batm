package ru.vtb.core.data.static_data.dto;

import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import ru.vtb.core.data.static_data.Bin;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.vtb.core.data.static_data.MapsKeysConstants.*;
import static ru.vtb.core.utils.CommonHelper.getRandomFromList;

@Data
public class TransactionDto {

    public static final List<String> CORP_TO_JSON_M_T_TERMINALS = DBHelper.Clearing.getCorpToJsonDevicesIds(true);
    public static final List<String> CORP_TO_JSON_M_F_TERMINALS = DBHelper.Clearing.getCorpToJsonDevicesIds(false);
    public static final List<String> FL_TO_JSON_M_T_TERMINALS = DBHelper.Clearing.getFlToJsonDevicesIds(true);
    public static final List<String> FL_TO_JSON_M_F_TERMINALS = DBHelper.Clearing.getFlToJsonDevicesIds(false);

    private static final String CURRENT_BANK_DATE = DBHelper.TransAgrSrv.getCurrentBankDate();

    private String zeroLine = null;
    private String firstLine = null;
    private String secondLine = null;
    private String thirdLine = null;
    private String fourthLine = null;
    private String fifthLine = null;
    private String sixthLine = null;
    private String seventhLine = null;
    private String eighthLine = null;

    public boolean addLine(String line) {
        if (eighthLine != null) {
            return false;
        }
        String linePos = line.substring(3, 4);
        switch (linePos) {
            case "0" -> zeroLine = line;
            case "1" -> firstLine = line;
            case "2" -> secondLine = line;
            case "3" -> thirdLine = line;
            case "4" -> fourthLine = line;
            case "5" -> fifthLine = line;
            case "6" -> sixthLine = line;
            case "7" -> seventhLine = line;
            case "8" -> eighthLine = line;
        }
        return true;
    }

    public List<String> getAllInitializedLines() {
        List<String> lines = new ArrayList<>();
        if (zeroLine != null)
            lines.add(zeroLine);
        if (firstLine != null)
            lines.add(firstLine);
        if (secondLine != null)
            lines.add(secondLine);
        if (thirdLine != null)
            lines.add(thirdLine);
        if (fourthLine != null)
            lines.add(fourthLine);
        if (fifthLine != null)
            lines.add(fifthLine);
        if (sixthLine != null)
            lines.add(sixthLine);
        if (seventhLine != null)
            lines.add(seventhLine);
        if (eighthLine != null)
            lines.add(eighthLine);
        return lines;
    }

    public String getTC() {
        return zeroLine.substring(0, 2);
    }

    public String getTransactionDate() {
        if (StringUtils.isNotEmpty(eighthLine)) {
            return eighthLine.substring(4, 12);
        }
        return null;
    }

    public void setTransactionDate(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(eighthLine) && (sub = getTransactionDate()) != null) {
            StringBuilder builder = new StringBuilder(eighthLine);
            eighthLine = builder.replace(4, 12,
                    (sub.replaceAll("\\S+", stringToSet) + "              ").substring(0, 8)).toString();
        }
    }

    public String getTransactionTime() {
        if (StringUtils.isNotEmpty(eighthLine)) {
            return eighthLine.substring(12, 18);
        }
        return null;
    }

    public void setTransactionTime(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(eighthLine) && (sub = getTransactionTime()) != null) {
            StringBuilder builder = new StringBuilder(eighthLine);
            eighthLine = builder.replace(12, 18,
                    (sub.replaceAll("\\S+", stringToSet) + "              ").substring(0, 6)).toString();
        }
    }

    public String getAcquirerReferenceNumber() {
        if (StringUtils.isNotEmpty(zeroLine)) {
            return zeroLine.substring(26, 49);
        }
        return null;
    }

    public void setAcquirerReferenceNumber(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(zeroLine) && (sub = getAcquirerReferenceNumber()) != null) {
            StringBuilder builder = new StringBuilder(zeroLine);
            zeroLine = builder.replace(26, 49,
                    (sub.replaceAll("\\S+", stringToSet)
                            + "                          ").substring(0, 23)).toString();
        }
    }

    public String getAuthorizationCode() {
        if (StringUtils.isNotEmpty(zeroLine)) {
            return zeroLine.substring(151, 157);
        }
        return null;
    }

    public void setAuthorizationCode(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(zeroLine) && (sub = getAuthorizationCode()) != null) {
            StringBuilder builder = new StringBuilder(zeroLine);
            zeroLine = builder.replace(151, 157,
                    (sub.replaceAll("\\S+", stringToSet) + "      ").substring(0, 6)).toString();
        }
    }

    public String getRetrievalReferenceNumber() {
        if (StringUtils.isNotEmpty(eighthLine)) {
            return eighthLine.substring(18, 30);
        }
        return null;
    }

    public void setRetrievalReferenceNumber(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(eighthLine) && (sub = getRetrievalReferenceNumber()) != null) {
            StringBuilder builder = new StringBuilder(eighthLine);
            eighthLine = builder.replace(18, 30,
                    (sub.replaceAll("\\S+", stringToSet) + "              ").substring(0, 12)).toString();
        }
    }

    public String getTransactionIdentifier() {
        if (StringUtils.isNotEmpty(fifthLine)) {
            String result = fifthLine.substring(4, 19);
            return result.isBlank() ? null : result;
        }
        return null;
    }

    public void setTransactionIdentifier(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(fifthLine) && (sub = getTransactionIdentifier()) != null) {
            StringBuilder builder = new StringBuilder(fifthLine);
            fifthLine = builder.replace(4, 19,
                    (sub.replaceAll("\\S+", stringToSet) + "              ").substring(0, 15)).toString();
        }
    }

    //sic
    public String getMerchantCategoryCode() {
        if (StringUtils.isNotEmpty(zeroLine)) {
            return zeroLine.substring(132, 136);
        }
        return null;
    }

    //sic
    public void setMerchantCategoryCode(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(zeroLine) && (sub = getMerchantCategoryCode()) != null) {
            StringBuilder builder = new StringBuilder(zeroLine);
            zeroLine = builder.replace(132, 136,
                    (sub.replaceAll("\\S+", stringToSet) + "    ").substring(0, 4)).toString();
        }
    }

    public String getMerchantCountryCode() {
        if (StringUtils.isNotEmpty(zeroLine)) {
            return zeroLine.substring(129, 132); // without trim
        }
        return null;
    }

    public void setMerchantCountryCode(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(zeroLine) && (sub = getMerchantCountryCode()) != null) {
            StringBuilder builder = new StringBuilder(zeroLine);
            zeroLine = builder.replace(129, 132,
                    (sub.replaceAll("\\S+", stringToSet) + "   ").substring(0, 3)).toString();
        }
    }

    public String getMerchantCity() {
        if (StringUtils.isNotEmpty(zeroLine)) {
            return zeroLine.substring(116, 129);
        }
        return null;
    }

    public void setMerchantCity(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(zeroLine) && (sub = getMerchantCity()) != null) {
            StringBuilder builder = new StringBuilder(zeroLine);
            zeroLine = builder.replace(116, 129,
                    (sub.replaceAll("\\S+", stringToSet) + "              ").substring(0, 13)).toString();
        }
    }

    public String getMerchantName() {
        if (StringUtils.isNotEmpty(zeroLine)) {
            return zeroLine.substring(91, 116);
        }
        return null;
    }

    public void setMerchantName(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(zeroLine) && (sub = getMerchantName()) != null) {
            StringBuilder builder = new StringBuilder(zeroLine);
            zeroLine = builder.replace(91, 116, (sub.replaceAll("VTB Bank\\s?(CASH)?", stringToSet)
                    + "                            ").substring(0, 25)).toString();
        }
    }

    public String getCardAcceptorID() {
        if (StringUtils.isNotEmpty(firstLine)) {
            return firstLine.substring(80, 95);
        }
        return null;
    }

    public void setCardAcceptorID(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(firstLine) && (sub = getCardAcceptorID()) != null) {
            StringBuilder builder = new StringBuilder(firstLine);
            firstLine = builder.replace(80, 95, (sub.replaceAll("\\S+", stringToSet)
                    + "                 ").substring(0, 15)).toString();
        }
    }

    /**
     * При использовании рекомендуется применять .trim();
     */
    public String getTerminalID() {
        if (StringUtils.isNotEmpty(firstLine)) {
            return firstLine.substring(95, 103);
        }
        return null;
    }

    public void setTerminalID(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(firstLine) && (sub = getTerminalID()) != null) {
            StringBuilder builder = new StringBuilder(firstLine);
            firstLine = builder.replace(95, 103,
                    (sub.replaceAll("\\S+", stringToSet) + "              ").substring(0, 8)).toString();
        }
    }

    public String getAccountNumber() {
        if (StringUtils.isNotEmpty(zeroLine)) {
            return zeroLine.substring(4, 23);
        }
        return null;
    }

    public void setAccountNumber(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(zeroLine) && (sub = getAccountNumber()) != null) {
            StringBuilder builder = new StringBuilder(zeroLine);
            zeroLine = builder.replace(4, 23,
                    (sub.replaceAll("\\S+", stringToSet) + "0000000000000000000").substring(0, 19)).toString();
        }
    }

    public String getCardExpirationDate() {
        if (StringUtils.isNotEmpty(eighthLine) && !isPurchaseByCash()) {
            String result = eighthLine.substring(30, 34);
            return result.isBlank() ? null : result;
        }
        return null;
    }

    public void setCardExpirationDate(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(eighthLine) && (sub = getCardExpirationDate()) != null) {
            StringBuilder builder = new StringBuilder(eighthLine);
            eighthLine = builder.replace(30, 34,
                    (sub.replaceAll("\\S+", stringToSet) + "              ").substring(0, 4)).toString();
        }
    }

    public String getDestinationChannel() {
        if (StringUtils.isNotEmpty(eighthLine)) {
            return eighthLine.substring(66, 67);
        }
        return null;
    }

    public void setDestinationChannel(String charToSet) {
        if (StringUtils.isNotEmpty(eighthLine) && getDestinationChannel() != null) {
            StringBuilder builder = new StringBuilder(firstLine);
            firstLine = builder.replace(66, 67, Character.toString(charToSet.charAt(0))).toString();
        }
    }

    public String getDestinationCurrencyCode() {
        if (StringUtils.isNotEmpty(zeroLine)) {
            return zeroLine.substring(73, 76);
        }
        return null;
    }

    public void setDestinationCurrencyCode(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(zeroLine) && (sub = getDestinationCurrencyCode()) != null) {
            StringBuilder builder = new StringBuilder(zeroLine);
            zeroLine = builder.replace(73, 76,
                    (sub.replaceAll("\\S+", stringToSet) + "              ").substring(0, 3)).toString();
        }
    }

    /**
     * @return сумму транзакции в КОПЕЙКАХ
     */
    public String getDestinationAmount() {
        if (StringUtils.isNotEmpty(zeroLine)) {
            return zeroLine.substring(61, 73);
        }
        return null;
    }

    /**
     * @param stringToSet сумма транзакции в КОПЕЙКАХ
     */
    public void setDestinationAmount(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(zeroLine) && StringUtils.isNotBlank(sub = zeroLine.substring(61, 73))) {
            String zeroes = "000000000000";
            stringToSet = stringToSet.replaceFirst("0+", "");
            StringBuilder builder = new StringBuilder(zeroLine);
            zeroLine = builder.replace(61, 73,
                    (zeroes.substring(stringToSet.length()) + sub.replaceAll("\\S+", stringToSet) + zeroes).substring(0, 12)).toString();
        }
    }

    public String getDataContent() {
        if (StringUtils.isNotEmpty(thirdLine) && isPurchaseByCash()) {
            String content = thirdLine.substring(18);
            Matcher matcher = Pattern.compile("(?<=F103=).*?(?=;)").matcher(content);
            if (matcher.find()) {
                content = matcher.group();
                return content.isBlank() ? null : content;
            }
        }
        return null;
    }

    public void setDataContent(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(thirdLine) && (sub = getDataContent()) != null) {
            thirdLine = (thirdLine.replaceFirst(sub, stringToSet).concat("                        ")).substring(0, 168);
        }
    }

    public String getSurchargeAmount() {
        if (StringUtils.isNotEmpty(fourthLine)) {
            return fourthLine.substring(50, 58);
        }
        return null;
    }

    public void setSurchargeAmount(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(fourthLine) && StringUtils.isNotBlank(sub = fourthLine.substring(50, 58))) {
            StringBuilder builder = new StringBuilder(fourthLine);
            String zeroes = "00000000";
            stringToSet = stringToSet.replaceFirst("0+", "");
            fourthLine = builder.replace(50, 58,
                    (zeroes.substring(stringToSet.length()) + sub.replaceAll("\\S+", stringToSet) + zeroes).substring(0, 8)).toString();
        }
    }

    public String getP2PCRPPCode() {
        if (StringUtils.isNotEmpty(thirdLine)) {
            return thirdLine.substring(16, 20);
        }
        return null;
    }

    public void setP2PCRPPCode(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(thirdLine)) {
            sub = thirdLine.substring(16, 20);
            StringBuilder builder = new StringBuilder(thirdLine);
            thirdLine = builder.replace(16, 20,
                    (sub.replaceAll("\\S+", stringToSet) + "    ").substring(0, 4)).toString();
        }
    }

    public String getP2PCardNumber() {
        if (StringUtils.isNotEmpty(thirdLine)) {
            return thirdLine.substring(39, 56);
        }
        return null;
    }

    public void setP2PCardNumber(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(thirdLine)) {
            sub = thirdLine.substring(39, 56);
            StringBuilder builder = new StringBuilder(thirdLine);
            thirdLine = builder.replace(39, 56,
                    (sub.replaceAll("\\W+", stringToSet) + "0000000000000000000").substring(0, 16)).toString();
        }
    }

    public String getP2PRecipientCardNumber() {
        if (StringUtils.isNotEmpty(firstLine)) {
            return firstLine.substring(23, 39);
        }
        return null;
    }

    public void setP2PRecipientCardNumber(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(firstLine)) {
            sub = firstLine.substring(23, 39);
            StringBuilder builder = new StringBuilder(firstLine);
            firstLine = builder.replace(23, 39,
                    (sub.replaceAll("\\W+", stringToSet) + "0000000000000000").substring(0, 16)).toString();
        }
    }

    public String getP2PExpirationDate() {
        if (StringUtils.isNotEmpty(eighthLine)) {
            return eighthLine.substring(30, 37);
        }
        return null;
    }

    public void setP2PExpirationDate(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(eighthLine)) {
            sub = eighthLine.substring(30, 37);
            StringBuilder builder = new StringBuilder(eighthLine);
            eighthLine = builder.replace(30, 37,
                    (sub.replaceAll("\\S+", stringToSet) + "0000000").substring(0, 7)).toString();
        }
    }

    public String getSelfCollectionCurrencyCode() {
        if (StringUtils.isNotEmpty(zeroLine)) {
            return zeroLine.substring(88, 91);
        }
        return null;
    }

    public void setSelfCollectionCurrencyCode(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(zeroLine)) {
            sub = zeroLine.substring(88, 91);
            StringBuilder builder = new StringBuilder(zeroLine);
            zeroLine = builder.replace(88, 91,
                    (sub.replaceAll("\\S+", stringToSet) + "000").substring(0, 3)).toString();
        }
    }

    public String getSelfCollectionAmount() {
        if (StringUtils.isNotEmpty(zeroLine)) {
            return zeroLine.substring(76, 88);
        }
        return null;
    }

    public void setSelfCollectionAmount(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(zeroLine)) {
            sub = zeroLine.substring(76, 88);
            String zeroes = "000000000000";
            stringToSet = stringToSet.replaceFirst("0+", "");
            StringBuilder builder = new StringBuilder(zeroLine);
            zeroLine = builder.replace(76, 88,
                    (zeroes.substring(stringToSet.length()) + sub.replaceAll("\\S+", stringToSet) + zeroes).substring(0, 12)).toString();
        }
    }

    public String getF1115Tag() {
        if (StringUtils.isNotEmpty(thirdLine)) {
            String content = thirdLine.substring(4);
            Matcher matcher = Pattern.compile("(?<=F111.5=).*?(?=;)").matcher(content);
            if (matcher.find()) {
                content = matcher.group();
                return content.isBlank() ? null : content;
            }
        }
        return null;
    }

    public void setF1115Tag(String stringToSet) {
        String sub;
        if (StringUtils.isNotEmpty(thirdLine) && (sub = getF1115Tag()) != null) {
            thirdLine = (thirdLine.replaceFirst(sub, stringToSet).concat("                        ")).substring(0, 168);
        }
    }

    public TransactionDto generateRandomData() {
        boolean isPBC = isPurchaseByCash();
        Date currDate = new Date();
        setTransactionDate(CommonHelper.parseDateFromPatternToAnotherPattern("yyyy-MM-dd", "yyyyMMdd", CURRENT_BANK_DATE));
        setTransactionTime(new SimpleDateFormat("HHmmss").format(currDate));
        setAcquirerReferenceNumber(RandomStringUtils.insecure().nextNumeric(23));
        setAuthorizationCode(RandomStringUtils.insecure().nextNumeric(6));
        setRetrievalReferenceNumber(RandomStringUtils.insecure().nextNumeric(12));
        setTransactionIdentifier(RandomStringUtils.insecure().nextNumeric(15));
        setMerchantCategoryCode("6011"
                /*isPBC ? "4900" : List.of("6011", "6012", "6536")
                .get(ThreadLocalRandom.current().nextInt(0, 3))*/);
        setMerchantCountryCode("RU");
        setMerchantCity(CommonHelper.usFaker.address().cityName());
        setMerchantName("Open Bank");
        setCardAcceptorID(RandomStringUtils.insecure().nextAlphanumeric(15).toUpperCase());
        setTerminalID(getRandomFromList(FL_TO_JSON_M_T_TERMINALS)); // real length - 8
        setAccountNumber(isPBC ? "0000000000000000000" : Bin.genRandAccNumWithOnlyVtbBin());
        setCardExpirationDate(new SimpleDateFormat("yyMM").format(
                new DateTime().plusMonths(ThreadLocalRandom.current().nextInt(1, 99)).toDate()));
        setDestinationChannel("q");
        setDestinationCurrencyCode("643");
        setDestinationAmount(RandomStringUtils.insecure().nextNumeric(8)); // real length - 12
        setDataContent(isPBC ? "here is bag" : null);
        setSurchargeAmount(RandomStringUtils.insecure().nextNumeric(3)); // real length - 8
        return this;
    }

    public TransactionDto addP2PAttributes() {
        if (!getTC().equals("06")) {
            throw new RuntimeException("P2P транзакция обязательно должна быть Cash-in");
        }
        setP2PCRPPCode("CRPP");
        setP2PCardNumber(Bin.genRandAccNumWithOnlyVtbBin());
        setP2PExpirationDate(RandomStringUtils.insecure().nextNumeric(7));
        setP2PRecipientCardNumber(Bin.genRandAccNumWithOnlyVtbBin());

        return this;
    }

    public TransactionDto addSelfCollectionAttributes() {
        if (!getTC().equals("06")) {
            throw new RuntimeException("Самоинкассация возможна только в транзакции Cash-in");
        }
        Date currDate = new Date();
        setTransactionDate(new SimpleDateFormat("yyyyMMdd").format(currDate));
        setTransactionTime(new SimpleDateFormat("HHmmss").format(currDate));
        setDestinationAmount(RandomStringUtils.insecure().nextNumeric(8));
        setAccountNumber("0000000000000000");
        setCardExpirationDate("0000");
        setSelfCollectionCurrencyCode("643");
        setSelfCollectionAmount(getDestinationAmount());
        setDestinationAmount("000000000000");
        setRetrievalReferenceNumber("000000000000");
        setAuthorizationCode("000000");
        setF1115Tag(RandomStringUtils.insecure().nextNumeric(6) + RandomStringUtils.insecure().nextNumeric(12));
        return this;
    }

    public TransactionDto setSpecificData(Map<String, String> data) {
        String terminalId = data.get(TERMINAL_ID);
        if (terminalId != null) {
            setTerminalID(terminalId);
        }
        String rrn = data.get(RRN);
        if (rrn != null) {
            setRetrievalReferenceNumber(rrn);
        }
        String curr = data.get(CURRENCY);
        if (curr != null) {
            setDestinationCurrencyCode(curr);
        }
        String authCode = data.get(AUTH_CODE);
        if (authCode != null) {
            setAuthorizationCode(authCode);
        }
        String mcc = data.get(MCC);
        if (mcc != null) {
            setMerchantCategoryCode(mcc);
        }
        String fee = data.get(AMOUNT_FEE);
        if (fee != null) {
            if (Objects.isNull(fourthLine)) {
                fourthLine = "%s%s4                                              00000000                                                                                                              "
                        .formatted(getTC(), zeroLine.substring(2, 3));
            }
            setSurchargeAmount(fee);
        }
        String accountNumber = data.get(ACCOUNT_NUMBER);
        if (accountNumber != null) {
            if (accountNumber.length() < 16) {
                setAccountNumber(Bin.recognize(accountNumber).genRandAccNum());
            } else setAccountNumber(accountNumber);
        }
        String terminalType = data.get(TERMINAL_TYPE);
        if (terminalType != null) {
            switch (terminalType) {
                case CORP_TO_JSON_M_T -> setTerminalID(getRandomFromList(CORP_TO_JSON_M_T_TERMINALS));
                case FL_TO_JSON_M_T -> setTerminalID(getRandomFromList(FL_TO_JSON_M_T_TERMINALS));
                case FL_TO_JSON_M_F -> setTerminalID(getRandomFromList(FL_TO_JSON_M_F_TERMINALS));
                default -> throw new UnsupportedOperationException("Unknown terminal type was provided");
            }
        }
        return this;
    }

    private boolean isPurchaseByCash() {
        return getTC().matches("06|26")
                && !Set.of("6011", "6012", "6536").contains(getMerchantCategoryCode())
                && getAccountNumber().matches("[0A]+");
    }

    public boolean isP2P() {
        return getTC().equals("06") &&
                (getP2PCRPPCode().equals("CRPP") ||
                        getP2PCardNumber().trim().matches("[0-9]+") ||
                        getP2PExpirationDate().matches("0+") ||
                        !getP2PRecipientCardNumber().isBlank());
    }

    public boolean isSelfCollection() {
        return getTC().equals("06") &&
                !getSelfCollectionAmount().isBlank() &&
                !getSelfCollectionCurrencyCode().isBlank() &&
                getAccountNumber().matches("0+") &&
                getCardExpirationDate().matches("0+");
    }
}
