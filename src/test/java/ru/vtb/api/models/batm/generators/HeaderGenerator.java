package ru.vtb.api.models.batm.generators;

import ru.vtb.api.models.batm.Header;
import ru.vtb.api.models.batm.header.Originator;
import ru.vtb.api.models.batm.header.Protocol;
import ru.vtb.api.models.batm.header.ResponseParams;
import ru.vtb.core.models.common.CommonModelHelper;

import java.util.Map;

import static ru.vtb.core.data.static_data.MapsKeysConstants.ORIGINATOR;
import static ru.vtb.core.data.static_data.MapsKeysConstants.RESPONSE_PARAMS;

public class HeaderGenerator {

    private static ResponseParams responseParams;
    private static Originator originator;

    private static void initFields(Map<String, Object> fields) {
        responseParams = (ResponseParams) fields.get(RESPONSE_PARAMS);
        originator = (Originator) fields.getOrDefault(ORIGINATOR, new Originator("IS-2213"));
    }

    public static synchronized Header generateHeaderModel(Map<String, Object> fields) {
        initFields(fields);

        Protocol protocol = new Protocol("solar-ws", "2.0");

        return Header.builder()
                .messageDate(CommonModelHelper.getMessageDate())
                .messageId(CommonModelHelper.generateRandomMessageId())
                .originator(originator)
                .protocol(protocol)
                .responseParams(responseParams)
                .build();
    }
}
