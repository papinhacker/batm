package ru.vtb.core.utils.sql;

import lombok.NonNull;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import ru.vtb.core.common.Log;
import ru.vtb.core.utils.sql.results.ColumnComposer;
import ru.vtb.core.utils.sql.results.ColumnIndexSupplier;
import ru.vtb.core.utils.sql.results.ResultSetComposer;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static ru.vtb.core.common.EnvConstants.*;

public class SQLHelper {

    public static final String SOLAR = "solar";
    public static final String SOLAR_ACC = "solar_acc";
    public static final String BATM_DEVICES = "batm_devices";
    public static final String BATM_CLEARING = "batm_clearing";
    public static final String BATM_ZOD = "batm_zod";
    public static final String BATM_BIN = "batm_bin_file";
    public static final String BATM_OUTBOX = "batm_outbox";
    public static final String BATM_ACC_ROUTE_S3 = "batm_acc_route_s3";
    public static final String BATM_ACC_ROUTE_PP = "batm_acc_route_pp";
    public static final String BATM_ACC = "batm_acc";
    public static final String BATM_TRANSACTIONS_AGR = "batm_transactions_agr";
    public static final String BATM_TRANSACTIONS_AGR_SRV = "batm_transactions_agr_srv";

    public static final String BATM_OPERATIONS_SIOR = "batm_operations_sior";
    public static final String BATM_SIOR_PRODUCT_ACC = "batm_sior_product_acc";

    private final String path;
    private final String login;
    private String password = DB_DEFAULT_PASSWORD;

    public SQLHelper(String dbPath) {
        String loginPostfix = "_admin";
        switch (dbPath) {
            case SOLAR -> {
                path = dbPath;
                login = SOLAR + loginPostfix;
            }
            case SOLAR_ACC -> {
                path = dbPath;
                login = SOLAR_ACC + loginPostfix;
            }
            case BATM_DEVICES -> {
                path = dbPath;
                login = BATM_DEVICES + loginPostfix;
            }
            case BATM_CLEARING -> {
                path = dbPath;
                login = BATM_CLEARING + loginPostfix;
            }
            case BATM_ZOD -> {
                path = dbPath;
                login = BATM_ZOD + loginPostfix;
            }
            case BATM_BIN -> {
                path = dbPath;
                login = BATM_BIN + loginPostfix;
                password = DB_BATM_BIN_PASSWORD;
            }
            case BATM_OUTBOX -> {
                path = dbPath;
                login = BATM_OUTBOX + loginPostfix;
                password = DB_BATM_OUTBOX_PASSWORD;
            }
            case BATM_ACC_ROUTE_S3 -> {
                path = dbPath;
                login = BATM_ACC_ROUTE_S3 + loginPostfix;
                password = DB_BATM_ACC_ROUTE_PASSWORD;
            }
            case BATM_ACC_ROUTE_PP -> {
                path = dbPath;
                login = BATM_ACC_ROUTE_PP + loginPostfix;
                password = DB_BATM_ACC_ROUTE_PASSWORD;
            }
            case BATM_ACC -> {
                path = dbPath;
                login = BATM_ACC + loginPostfix;
                password = DB_BATM_ACC_PASSWORD;
            }
            case BATM_TRANSACTIONS_AGR -> {
                path = dbPath;
                login = BATM_TRANSACTIONS_AGR + loginPostfix;
                password = DB_BATM_TRANSACTIONS_AGR_PASSWORD;
            }
            case BATM_TRANSACTIONS_AGR_SRV -> {
                path = dbPath;
                login = BATM_TRANSACTIONS_AGR_SRV + loginPostfix;
                password = DB_BATM_TRANSACTIONS_AGR_SRV_PASSWORD;
            }
            case BATM_OPERATIONS_SIOR -> {
                path = dbPath;
                login = BATM_OPERATIONS_SIOR + loginPostfix;
                password = DB_BATM_OPERATIONS_SIOR_PASSWORD;
            }
            case BATM_SIOR_PRODUCT_ACC -> {
                path = dbPath;
                login = BATM_SIOR_PRODUCT_ACC + loginPostfix;
                password = DB_BATM_SIOR_PRODUCT_ACC_PASSWORD;
            }
            default -> throw new UnsupportedOperationException("Database path must be provided");
        }
    }

    private Connection getNewConnection() throws SQLException {
        return DriverManager.getConnection(DB_URI + path, login, password);
    }

    /**
     * Выполняет передаваемый SQL запрос
     *
     * @param sqlQuery - SQL запрос для выполнения
     */
    public void executeUpdate(String sqlQuery) {
        Log.info("Отправляем запрос в бд '%s' запрос - '%s'".formatted(path, sqlQuery.replaceAll("[\t\n]", " ")));
        try (var connection = getNewConnection();
             var statement = connection.prepareStatement(sqlQuery)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            Log.error("Не получилось выполнить SQL запрос. Причина: " + e.getMessage());
        }
    }

    /**
     * Получает данные по заданным наименованиеям столбцов
     *
     * @param executeQuery - SQL запрос для выполнения
     * @param columnNames  - наименование колонок
     * @return ключ - это наименование колонки и лист значений по данному ключу
     */
    public Map<String, List<String>> getRowsFromDB(String executeQuery, String... columnNames) {
        if (!executeQuery.contains("*") && !executeQuery.contains("select exists (")) {
            throw new RuntimeException("Некорректный SQL запрос. Для поиска индекса нужного столбца по имени," +
                    "Необходимо указать в запросе выбор всех столбцов.");
        }
        Map<String, List<String>> result = getColumnValues(executeQuery, rsmd -> getColumnIndexes(rsmd, columnNames));
        if (result.isEmpty()) {
            String message = "Не удалось найти значений по запросу: \n" + executeQuery;
            Log.error(message);
            throw new RuntimeException(message);
        }
        return result;
    }

    /**
     * Выполняет запрос и собирает список строк
     *
     * @param sqlQuery - SQL запрос для выполнения
     * @param params   - параметры запроса
     * @return набор строк результатов
     */
    public List<Map<String, Object>> getRowLinesFromDB(String sqlQuery, Long timeoutInSecs, Object... params) {
        return getResultsAndCompose(ResultSetComposer.ROWS_COMPOSER, sqlQuery, timeoutInSecs, true, params);
    }

    public List<Map<String, Object>> getRowLinesFromDB(String sqlQuery, Object... params) {
        return getRowLinesFromDB(sqlQuery, 0L, true, params);
    }

    public List<Map<String, Object>> getRowLinesFromDB(String sqlQuery, Long timeoutInSecs, Boolean log, Object... params) {
        return getResultsAndCompose(ResultSetComposer.ROWS_COMPOSER, sqlQuery,timeoutInSecs, log, params);
    }

    public List<Map<String, Object>> getRowLinesFromDB(String sqlQuery, Boolean log, Object... params) {
        return getRowLinesFromDB(sqlQuery, 0L, log, params);
    }

    /**
     * Выполняет запрос и возвращает первое значение первой строки и первого столбца
     *
     * @param sqlQuery - SQL запрос для выполнения
     * @param params   - параметры запроса
     * @return набор строк результатов
     */
    public String getFirstFieldFromDB(String sqlQuery, Long timeoutInSecs, Object... params) {
        return getResultsAndCompose(ResultSetComposer.FIRST_ROW_FIRST_FIELD_COMPOSER, sqlQuery, timeoutInSecs, params).toString();
    }

    public String getFirstFieldFromDB(String sqlQuery, Object... params) {
        return getFirstFieldFromDB(sqlQuery, 0L, params);
    }

    /**
     * Выполняет запрос и достаёт из него только те колонки, номера которых отдаёт поставщик
     *
     * @param executeQuery        - SQL запрос для выполнения
     * @param columnIndexSupplier - поставщик индексов
     * @param params              - параметры запроса
     * @return набор "наименование колонки - значение строк колонки"
     */
    @NonNull
    private Map<String, List<String>> getColumnValues(String executeQuery, ColumnIndexSupplier columnIndexSupplier, Object... params) {
        return getResultsAndCompose(new ColumnComposer(columnIndexSupplier), executeQuery, 0L, params);
    }

    /**
     * Выполняет запрос и собирает его результаты согласно переданному сборщику
     *
     * @param resultSetComposer - сборщик результатов выполнения запросов
     * @param sqlQuery          - SQL запрос для выполнения
     * @param params            - параметры запроса
     * @return собранную в заданную сборщиком коллекцию результатов выполнения запросов
     */
    private <T> T getResultsAndCompose(ResultSetComposer<T> resultSetComposer, String sqlQuery, Long timeoutInSecs, Object... params) {
        return getResultsAndCompose(resultSetComposer, sqlQuery, timeoutInSecs, true, params);
    }

    private <T> T getResultsAndCompose(ResultSetComposer<T> resultSetComposer, String sqlQuery, Long timeoutInSecs, Boolean log, Object... params) {
        T result = resultSetComposer.createResultStorage();
        String logMessage = "Отправляем запрос в бд '%s' запрос - '%s'".formatted(path, sqlQuery.replaceAll("[\t\n]", " "));
        if (log) {
            Log.info(logMessage);
        } else {
            Log.debug(logMessage);
        }
        try (var connection = getNewConnection();
             var statement = connection.prepareStatement(sqlQuery)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            if (timeoutInSecs > 0) {
                Awaitility.await().pollInSameThread()
                        .timeout(timeoutInSecs, TimeUnit.SECONDS)
                        .pollInterval(fibonacci(TimeUnit.SECONDS).offset(2))
                        .ignoreExceptions()
                        .until(() -> {
                            ResultSet resultSet = statement.executeQuery();
                            return resultSet.next();
                        });
            }
            resultSetComposer.composeLines(result, statement.executeQuery());
        } catch (SQLException | ConditionTimeoutException e) {
            Log.warning("Unable to compose result of SQL query. Reason is: " + e.getMessage());
        }
        return result;
    }

    /**
     * Получает индексы колонок по их именам
     *
     * @param resultSetMetaData - метаданные для считывания индексов
     * @param columnNames       - имена колонок, для которых необходимо получить индексы
     * @return массив считанных данных
     */
    private int[] getColumnIndexes(ResultSetMetaData resultSetMetaData, String... columnNames) throws SQLException {
        int[] resultIndexes = new int[columnNames.length];
        int indexToFill = 0;

        final int totalColumnsCount = resultSetMetaData.getColumnCount();
        for (String name : columnNames) {
            int foundIndex = -1;
            for (int i = 1; foundIndex == -1 && i <= totalColumnsCount; i++) {
                if (resultSetMetaData.getColumnName(i).equalsIgnoreCase(name)) {
                    foundIndex = i;
                }
            }
            resultIndexes[indexToFill++] = foundIndex;
        }
        return resultIndexes;
    }
}
