package ru.vtb.core.data.static_data;

import org.apache.commons.lang3.RandomStringUtils;
import ru.vtb.core.utils.CommonHelper;

import java.util.List;

public enum Bin {

    MIR_VTB(List.of("0", "1", "2", "7", "9"), "МИР ВТБ", "wc"),
    MASTERCARD_VTB(List.of("5"), "Master card ВТБ", "mc"),
    //    MAESTRO_VTB(List.of("6"), "Maestro"),
    VISA_VTB(List.of("4"), "Visa ВТБ", "vc"),
    AMERICAN_EXPRESS(List.of("3"), "American Express", "xc"),
    UNION_PAY(List.of("6", "8"), "Union Pay", "uc");

    private final List<String> binRange;
    private final String cardName;
    private final String glCodePostfix;

    Bin(List<String> binRange, String cardName, String glCodePostfix) {
        this.binRange = binRange;
        this.cardName = cardName;
        this.glCodePostfix = glCodePostfix;
    }

    public static String genRandAccNumWithOnlyVtbBin() {
        return CommonHelper.getRandomFromList(getOnlyInternalVtbBins()).genRandAccNum();
    }

    public static Bin recognize(String cardNumber) {
        for (Bin bin : values()) {
            for (String subBin : bin.binRange) {
                if (cardNumber.startsWith(subBin)) {
                    return bin;
                }
            }
        }
        throw new RuntimeException("Unable to recognize bin of card - " + cardNumber);
    }

    public static String getGlCodePostFixByCardNumber(String cardNumber) {
        return recognize(cardNumber).glCodePostfix;
    }

    private static String generateBinPostfix(Bin bin) {
        String buff = CommonHelper.getRandomFromList(bin.binRange);
        if (VISA_VTB.binRange.contains(buff) || AMERICAN_EXPRESS.binRange.contains(buff) || UNION_PAY.binRange.contains(buff) /*предполагаем, что UNIPAY всегда 16 символов*/) {
            return buff.concat(RandomStringUtils.insecure().nextNumeric(16)).substring(0, 16);
        }
        return buff.concat(RandomStringUtils.insecure().nextNumeric(19)).substring(0, 19);
    }

    public static List<Bin> getOnlyInternalVtbBins() {
        return List.of(MIR_VTB, MASTERCARD_VTB, VISA_VTB);
    }

    public String genRandAccNum() {
        return generateBinPostfix(this);
    }
}
