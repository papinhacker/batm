package ru.vtb.core.models.kafka.batm_in_siorus_devices.generators;

import org.apache.commons.lang3.RandomStringUtils;
import ru.vtb.core.data.static_data.CodesNamesValues;
import ru.vtb.core.data.static_data.MapsKeysConstants;
import ru.vtb.core.models.common.CommonModelHelper;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.BatmInSiorusDevicesRootMessage;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.Body;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.Header;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.body.AccountItem;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.body.Accounts;
import ru.vtb.core.utils.AccountsHelper;
import ru.vtb.core.utils.CommonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class BatmInSiorusDevicesMessageGenerator {

    private static String accountNumber;
    private static String atmExploitationStatus;
    private static String feature;
    private static String id;
    private static String vendor;
    private static String absBranchNumber;
    private static String absBaseBranchNumber;
    private static Accounts accounts;

    private static void initFields(Map<String, Object> fields) {
        accountNumber = (String) fields.getOrDefault(MapsKeysConstants.ACCOUNT_NUMBER, AccountsHelper.generateRandomAccountNumberWithDelimiter("-"));
        feature = (String) fields.getOrDefault(MapsKeysConstants.FEATURE, "Cash-Out"); // ATM
        id = (String) fields.getOrDefault(MapsKeysConstants.ID, RandomStringUtils.insecure().nextNumeric(8));
        atmExploitationStatus = (String) fields.getOrDefault(MapsKeysConstants.ATM_EXPLOITATION_STATUS, "operating");
        vendor = (String) fields.getOrDefault(MapsKeysConstants.VENDOR, CodesNamesValues.NCR);
        absBranchNumber = (String) fields.getOrDefault(MapsKeysConstants.ABS_BRANCH, RandomStringUtils.insecure().nextNumeric(4));
        absBaseBranchNumber = (String) fields.getOrDefault(MapsKeysConstants.ABS_BASE_BRANCH, getRandomAbsBaseBranchNumber());
        accounts = (Accounts) fields.getOrDefault(MapsKeysConstants.ACCOUNTS, new Accounts(List.of(
                AccountItem.builder()
                        .currency("643")
                        .number(accountNumber)
                        .build())));
    }

    public static BatmInSiorusDevicesRootMessage generate(Map<String, Object> fields) {
        initFields(fields);

        String address = CommonHelper.rusFaker.address().zipCode() + ", "
                + CommonHelper.rusFaker.address().state() + ", г. "
                + CommonHelper.rusFaker.address().cityName() + ", "
                + CommonHelper.rusFaker.address().streetName() + ", д."
                + CommonHelper.rusFaker.address().buildingNumber();

        Body body = Body.builder()
                .absBranch(absBranchNumber)
                .absBaseBranch(absBaseBranchNumber)
                .accounts(accounts)
                .address(address)
                .atmExploitationStatus(atmExploitationStatus)
                .feature(feature)
                .id(id)
                .vendor(vendor)
                .okatoCode("41417000000")
                .model("AtModel")
                .serialNumber("13-44010582")
                .build();

        Header header = generateSIORHeader();

        return new BatmInSiorusDevicesRootMessage(body, header);
    }

    public static Header generateSIORHeader() {
        return Header.builder()
                .messageDate(CommonModelHelper.getDeprecatedMessageDate())
                .messageId(CommonModelHelper.generateRandomMessageId())
                .system("SIORUS")
                .version("1.0")
                .build();
    }

    public static Body generateBodyFromDBSource(Map<String, Object> fromDB) {
        return Body.builder()
                .absBranch(fromDB.get("abs_branch").toString())
                .absBaseBranch(fromDB.get("abs_base_branch").toString())
                .address(fromDB.get("address").toString())
                .atmExploitationStatus(fromDB.get("atm_exploitation_status").toString())
                .feature(fromDB.get("feature").toString())
                .id(fromDB.get("atm_id").toString())
                .vendor(fromDB.get("vendor").toString())
                .okatoCode(fromDB.get("okato_code").toString())
                .model(fromDB.get("model").toString())
                .serialNumber(fromDB.get("serial_number").toString())
                .build();
    }

    public static String getRandomAbsBaseBranchNumber(String... excludeOf) {
        List<String> codeReferences = new ArrayList<>();
        codeReferences.add("0000");
        codeReferences.add("0002");
        codeReferences.add("0018");
        codeReferences.add("0040");
        codeReferences.add("0051");
        codeReferences.add("0055");
        codeReferences.add("0056");
        codeReferences.add("0080");
        codeReferences.add("4090");
        codeReferences.add("3779");
        if (excludeOf.length > 0) {
            codeReferences.remove(excludeOf[0]);
        }
        return codeReferences.get(ThreadLocalRandom.current().nextInt(codeReferences.size()));
    }
}
