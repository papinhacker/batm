package ru.vtb.core.utils.sql.results;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Поставщик индекса колонок, с которых
 * необходимо вытащить данные результата запроса
 */
@FunctionalInterface
public interface ColumnIndexSupplier {

    int[] supply(ResultSetMetaData resultSetMetaData) throws SQLException;
}
