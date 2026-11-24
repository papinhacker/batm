package ru.vtb.core.utils.sql.results;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Интерфейс, описывающий сборку результатов
 * выполнения SQL-запроса в какую-либо коллекцию
 *
 * @param <T> - тип коллекции результатов
 */
public interface ResultSetComposer<T> {

    /**
     * Собирает результаты в необходимую коллекцию
     *
     * @param resultStorage - коллекция, в которую собираются результаты
     * @param resultSet     - результат выполнения запроса
     * @throws SQLException при некорректной работе с результатом выполнения запроса
     */
    void composeLines(T resultStorage, ResultSet resultSet) throws SQLException;

    /**
     * Создаёт коллекцию-хранилище необходимого типа
     */
    T createResultStorage();

    /**
     * Построчный сборщик результатов.
     * Производит сборку в список строк, где каждая представляеет
     * собой {@link LinkedHashMap}, в которой ключ - название колонки
     */
    ResultSetComposer<List<Map<String, Object>>> ROWS_COMPOSER = new ResultSetComposer<>() {
        @Override
        public void composeLines(List<Map<String, Object>> resultStorage, ResultSet resultSet) throws SQLException {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int columnsCount = rsmd.getColumnCount();
            while (resultSet.next()) {
                Map<String, Object> rowMap = new LinkedHashMap<>();
                for (int i = 1; i <= columnsCount; i++) {
                    rowMap.put(rsmd.getColumnName(i), resultSet.getObject(i));
                }
                resultStorage.add(rowMap);
            }
        }

        @Override
        public List<Map<String, Object>> createResultStorage() {
            return new ArrayList<>();
        }
    };

    /**
     * Cборщик первого поля первой строки результата.
     * Производит сборку в строку
     */
    ResultSetComposer<StringBuilder> FIRST_ROW_FIRST_FIELD_COMPOSER = new ResultSetComposer<>() {
        @Override
        public void composeLines(StringBuilder resultStorage, ResultSet resultSet) throws SQLException {
            if (resultSet.next()) {
                resultStorage.append(resultSet.getObject(1));
            }
        }

        @Override
        public StringBuilder createResultStorage() {
            return new StringBuilder();
        }
    };
}
