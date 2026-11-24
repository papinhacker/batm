package ru.vtb.api.models.batm.generators;

import org.apache.commons.lang3.RandomStringUtils;
import ru.vtb.api.models.batm.Body;
import ru.vtb.api.models.batm.Header;
import ru.vtb.api.models.batm.RootPOJO;
import ru.vtb.api.models.batm.body.*;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.vtb.core.data.static_data.CodesNamesValues.*;
import static ru.vtb.core.data.static_data.MapsKeysConstants.*;

public class SaveAgreementGenerator {


    private static final String currentBankingDate = DBHelper.TransAgr.getCurrentBankDate();

    private static String agreementNumber;
    private static String contractNumber;
    private static String mcc;
    private static ProductRef productRef;
    private static Accessors accessors;
    private static Client client;
    private static String terminalType;
    private static String accountNumber;

    private static void initFields(Map<String, Object> fields) {
        agreementNumber = (String) fields.getOrDefault(AGREEMENT_NUMBER, generateRandomAgreementNumber());
        contractNumber = (String) fields.getOrDefault(CONTRACT_NUMBER, RandomStringUtils.insecure().nextNumeric(8));
        mcc = (String) fields.getOrDefault(MCC, "6011");
        terminalType = (String) fields.getOrDefault(TERMINAL_TYPE, ATM_TERMINAL);
        productRef = (ProductRef) fields.getOrDefault(PRODUCT_REF, new ProductRef(Parameters.builder().code(
                terminalType.equals(ATM_TERMINAL) ? ATM_PRODUCT : ADM_PRODUCT).build()));
        accessors = (Accessors) fields.get(ACCESSORS);
        client = (Client) fields.get(CLIENT);
        accountNumber = (String) fields.getOrDefault(ACCOUNT_NUMBER, generateRandomAccountNumber());
    }

    public static synchronized RootPOJO generateSaveAgreementModel(Map<String, Object> fields) {
        initFields(fields);

        AgreementRef agreementRef = AgreementRef.builder().parameters(Parameters.builder().number(agreementNumber).build()).build();
        FinInstitutionRef finInstitutionRef = new FinInstitutionRef(Parameters.builder().code(FIVE_ZEROES).build());
        BranchRef branchRef = new BranchRef(Parameters.builder().code(FIVE_ZEROES).build());

        AccessorTypeRef accessorTypeRef = new AccessorTypeRef(Parameters.builder().code(TERM).build());
        AttributeItem attributeItem = AttributeItem.builder().name(TERMINAL_TYPE).value(terminalType).build();
        AddAttributes addAttributes = new AddAttributes(List.of(attributeItem));

//        ClassifierItem classifierItem1 = ClassifierItem.builder()
//                .typeRef(TypeRef.builder().parameters(Parameters.builder().code(ATM_TYPE).build()).build())
//                .valueRef(ValueRef.builder().parameters(Parameters.builder().code(NCR).build()).build())
//                .validFrom(currentBankingDate)
//                .build();
        ClassifierItem classifierItem2 = ClassifierItem.builder()
                .typeRef(TypeRef.builder().parameters(Parameters.builder().code(TERM_TYPE).build()).build())
                .valueRef(ValueRef.builder().parameters(Parameters.builder().code(ATM).build()).build())
                .validFrom(currentBankingDate)
                .build();
        ClassifierItem classifierItem3 = ClassifierItem.builder()
                .typeRef(TypeRef.builder().parameters(Parameters.builder().code(W4_TERM_TYPE).build()).build())
                .valueRef(ValueRef.builder().parameters(Parameters.builder().code(OUR_ATM).build()).build())
                .validFrom(currentBankingDate)
                .build();

        Classifiers classifiers = new Classifiers(List.of(
//                classifierItem1,
                classifierItem2,
                classifierItem3));

        AccessorItem accessorItem = AccessorItem.builder()
                .accessorTypeRef(accessorTypeRef)
                .addAttributes(addAttributes)
                .number(contractNumber) // contract number in ufx
                .classifiers(classifiers)
                .build();
        if (accessors == null) {
            accessors = new Accessors(List.of(accessorItem));
        }

        AddressItem addressItem1 = AddressItem.builder()
                .typeRef(TypeRef.builder().parameters(Parameters.builder().code(ACTUAL_ADDRESS).build()).build())
                .country("RUS")
                .addressLine("999999, Дебагистан, г. Тестоград, ул Баговая, д. 404, кв. 500")
                .attributes(new Attributes(List.of(AttributeItem.builder()
                        .code(OKATO)
                        .attribute("40298")
                        .build())))
                .build();

        Addresses addresses = new Addresses(List.of(
                addressItem1));

        IpsParametersSet ipsParametersSet = new IpsParametersSet(Set.of(IpsParametersItem.builder()
                .ipsParametersRef(
                        IpsParametersRef.builder().parameters(
                                Parameters.builder().code(NSPK_MIR_DMS).build()
                        ).build())
                .cardAcceptorIdCode(RandomStringUtils.insecure().nextNumeric(9)) // static?
                .mcc(mcc) // static?
                .build()));

        if (client != null) {
            client = Client.builder()
                    .typeRef(client.getTypeRef())
                    .finInstitutionRef(FinInstitutionRef.builder().parameters(Parameters.builder().code(FIVE_ZEROES).build()).build())
                    .number(generateRandomClientNumber())
                    .companyName(CommonHelper.rusFaker.company().name())
                    .addresses(addresses)
                    .ipsParametersSet(client.getIpsParametersSet())
                    .build();
        } else {
            client = Client.builder()
                    .typeRef(TypeRef.builder().parameters(Parameters.builder().code("TERM_OWNER").build()).build())
                    .finInstitutionRef(FinInstitutionRef.builder().parameters(Parameters.builder().code(FIVE_ZEROES).build()).build())
                    .number(generateRandomClientNumber())
                    .companyName(CommonHelper.rusFaker.company().name())
                    .addresses(addresses)
                    .ipsParametersSet(ipsParametersSet)
                    .build();
        }

        AccountItem accountItem = AccountItem.builder()
                .accountTypeRef(AccountTypeRef.builder().parameters(Parameters.builder().code("20208").build()).build())
                .currency("810")
                .accountNumber(accountNumber)
                .openDate(currentBankingDate)
                .accountStatus("ACTIVE")
                .amount(0.0)
                .build();
        Accounts accounts = new Accounts(List.of(accountItem));

        Agreement agreement = Agreement.builder()
                .agreementRef(agreementRef)
                .productRef(productRef)
                .finInstitutionRef(finInstitutionRef)
                .branchRef(branchRef)
                .agreementClass("ACQUIRER")
                .serviceType("ACQUIRER")
                .number(agreementNumber)
                .signatureDate(currentBankingDate)
                .effectiveDate(currentBankingDate)
                .accessors(accessors)
                .client(client)
                .accounts(accounts)
                .currency("810")
                .build();

        Header header = generateSaveAgreementHeader();

        Body body = Body.builder()
                .agreement(agreement).build();

        return new RootPOJO(header, body);
    }

    public static String generateRandomAgreementNumber() {
        return String.join("-", RandomStringUtils.insecure().nextAlphabetic(1).toUpperCase(),
                RandomStringUtils.insecure().nextNumeric(4),
                RandomStringUtils.insecure().nextNumeric(4),
                RandomStringUtils.insecure().nextNumeric(6),
                "MK");
    }

    public static String generateRandomClientNumber() {
        return String.join("-", "A0001",
                RandomStringUtils.insecure().nextNumeric(6));
    }

    public static String generateRandomAccountNumber() {
        return "20208810" + RandomStringUtils.insecure().nextNumeric(12);
    }

    private static Header generateSaveAgreementHeader() {
//        Includes includes = new Includes(List.of(new IncludeItem("allSubEntitiesId", true)));
//        ResponseParams responseParams = new ResponseParams(includes);
        return HeaderGenerator.generateHeaderModel(Map.of());
    }
}
