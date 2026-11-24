package ru.vtb.api.tests.batm_dp;

import io.qameta.allure.TmsLink;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.common.Log;
import ru.vtb.core.data.static_data.dto.TransactionDto;
import ru.vtb.core.utils.B2LHelper;
import ru.vtb.core.utils.DBHelper;
import ru.vtb.core.utils.KafkaHelper;
import ru.vtb.core.utils.ZipUtils;
import ru.vtb.data_providers.BatmDpDP;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.vtb.core.data.static_data.B2LMask.CORP_038cto;
import static ru.vtb.core.data.static_data.B2LMask._298cto;
import static ru.vtb.core.data.static_data.KafkaTopic.BATM_TRANS_OUT_BATM_DP_STREAMING;
import static ru.vtb.core.data.static_data.MapsKeysConstants.*;
import static ru.vtb.core.data.static_data.MapsValuesConstants.*;
import static ru.vtb.core.data.static_data.S3Constants.BATM_OUTPUT;
import static ru.vtb.core.data.static_data.S3Constants.IF_BATM_WAY_IN;
import static ru.vtb.core.utils.B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions;

/**
 * Сьюта batm dp сущестсвует в виде одного большого параметризированного теста. Коммит 'batm dp tests refactoring' от 11.08.2025
 */
public class BatmDpTests extends BaseAPIState {


}
