package ru.vtb.core.utils.sql.results;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColumnComposer implements ResultSetComposer<Map<String, List<String>>> {

    private final ColumnIndexSupplier columnIndexSupplier;

    public ColumnComposer(ColumnIndexSupplier columnIndexSupplier) {
        this.columnIndexSupplier = columnIndexSupplier;
    }

    @Override
    public void composeLines(Map<String, List<String>> resultStorage, ResultSet resultSet) throws SQLException {
        ResultSetMetaData rsmd = resultSet.getMetaData();
        while (resultSet.next()) {
            for (int index : columnIndexSupplier.supply(rsmd)) {
                resultStorage.computeIfAbsent(rsmd.getColumnLabel(index), k -> new ArrayList<>())
                        .add(resultSet.getString(index));
            }
        }
    }

    @Override
    public Map<String, List<String>> createResultStorage() {
        return new HashMap<>();
    }
}
