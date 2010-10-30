package hu.snowycat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.RowOutOfBoundsException;
import org.dbunit.dataset.datatype.DataType;

/**
 * Static methods to manipulate db with dbUnit
 * 
 * @author sashee
 */
public class DbUtil {
	/**
	 * Inserts the given rows to the given table via the given connection. The rows are presented in a map which maps a linked map that stores the name->values of the columns, and
	 * the number of the same row to be inserted.
	 * 
	 * @param connection
	 *            - The connection to execute the SQL's against
	 * @param insertion
	 *            - The rows to be inserted to the table
	 * @param table
	 *            - Rows will be inserted to this table. Needed only for the metadata
	 */
	public static void insertRowsToDb(Connection connection, Map<LinkedHashMap<String, String>, Integer> insertion, ITable table) throws Exception {
		for (Entry<LinkedHashMap<String, String>, Integer> e : insertion.entrySet()) {
			for (int i = 0; i < e.getValue(); i++) {
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < e.getKey().size(); j++) {
					sb.append("?,");
				}
				StringBuilder sb2 = new StringBuilder();
				for (Entry<String, String> rows : e.getKey().entrySet()) {
					sb2.append(rows.getKey() + ",");
				}
				PreparedStatement ps = connection.prepareStatement("INSERT INTO " + table.getTableMetaData().getTableName() + " (" + sb2.toString().substring(0, sb2.length() - 1) + ") VALUES(" + sb.toString().substring(0, sb.length() - 1) + ")");
				int curr = 1;
				for (Entry<String, String> rows : e.getKey().entrySet()) {
					Column c = getColumnForName(table, rows.getKey());
					Object data = c.getDataType().typeCast(rows.getValue());
					if (data == null) {
						ps.setNull(curr, c.getDataType().getSqlType());
					} else {
						c.getDataType().setSqlValue(data, curr, ps);
					}
					curr++;
				}
				ps.execute();
			}
		}
	}

	/**
	 * Removes the given rows from given table via the given connection.
	 * 
	 * @param connection
	 *            - The connection to execute the SQL's against
	 * @param insertion
	 *            - The rows to be removed from the table
	 * @param table
	 *            - Rows will be removed from this table. Needed only for the metadata
	 */
	public static void removeRowsFromDb(Connection connection, Map<LinkedHashMap<String, String>, Integer> deletion, ITable table) throws Exception {
		for (Entry<LinkedHashMap<String, String>, Integer> e : deletion.entrySet()) {
			for (int i = 0; i < e.getValue(); i++) {
				StringBuilder sb = new StringBuilder();
				for (Entry<String, String> rows : e.getKey().entrySet()) {
					sb.append(rows.getKey() + "=? AND ");
				}
				PreparedStatement ps = connection.prepareStatement("DELETE FROM " + table.getTableMetaData().getTableName() + " WHERE " + sb.toString().substring(0, sb.length() - 5));
				int curr = 1;
				for (Entry<String, String> rows : e.getKey().entrySet()) {
					Column c = getColumnForName(table, rows.getKey());
					Object data = c.getDataType().typeCast(rows.getValue());
					if (data == null) {
						ps.setNull(curr, c.getDataType().getSqlType());
					} else {
						c.getDataType().setSqlValue(data, curr, ps);
					}
					curr++;
				}
				ps.execute();
			}
		}
	}

	/**
	 * Returns the named column of the given table
	 * 
	 * @param table
	 *            - The table
	 * @param name
	 *            - The name of the column to return
	 * @return The column with the given name, or null if none was found
	 */
	public static Column getColumnForName(ITable table, String name) throws DataSetException {
		for (Column c : table.getTableMetaData().getColumns()) {
			if (c.getColumnName().compareTo(name) == 0) {
				return c;
			}
		}
		return null;
	}

	/**
	 * Finds the differences between the two tables. The result is returned in [deletion,insertion]. These modifications are required to make the first table have the same rows as
	 * the second.
	 * 
	 * @param t1
	 *            - The table to be modified
	 * @param t2
	 *            - The reference table
	 * @return The needed modifications to make the first table equal to the second. The result is returned in [deletion,insertion] format
	 */
	@SuppressWarnings("unchecked")
	public static Map<LinkedHashMap<String, String>, Integer>[] findDifferences(ITable t1, ITable t2) throws Exception {
		Map<LinkedHashMap<String, String>, Integer> deletion = new HashMap<LinkedHashMap<String, String>, Integer>();
		Map<LinkedHashMap<String, String>, Integer> insertion = new HashMap<LinkedHashMap<String, String>, Integer>();
		for (int i = 0; i < t1.getRowCount(); i++) {
			LinkedHashMap<String, String> row = getRowFromTable(t1, i);
			if (containsRow(insertion, row)) {
				removeRow(insertion, row);
			} else {
				insertRow(deletion, row);
			}
		}

		int j = 0;
		try {
			while (true) {
				LinkedHashMap<String, String> row = getRowFromTable(t2, j);
				if (containsRow(deletion, row)) {
					removeRow(deletion, row);
				} else {
					insertRow(insertion, row);
				}
				j++;
			}
		} catch (RowOutOfBoundsException roobe) {
		}

		return new Map[] { deletion, insertion };

	}

	/**
	 * Compares two rows.
	 * 
	 * @param row1
	 *            - The first row
	 * @param row2
	 *            - The second row
	 * @return Whether they are identical
	 */
	public static boolean compareRows(Map<String, String> row1, Map<String, String> row2) {
		if (row1.size() != row2.size()) {
			return false;
		}
		for (Entry<String, String> e : row1.entrySet()) {
			if (e.getValue() == null ^ row2.get(e.getKey()) == null) {
				return false;
			}
			if (e.getValue() != null && (row2.containsKey(e.getKey()) == false || row2.get(e.getKey()).compareTo(e.getValue()) != 0)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Tests whether the rows collection contains the given row
	 * 
	 * @param collection
	 *            - The row collection
	 * @param row
	 *            - The row to check with
	 * @return Whether the collection contains the row
	 */
	public static boolean containsRow(Map<LinkedHashMap<String, String>, Integer> collection, Map<String, String> row) {
		for (Map<String, String> v : collection.keySet()) {
			if (compareRows(v, row)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes a row from the row collection in place
	 * 
	 * @param collection
	 *            - The row collection from the row will be removed
	 * @param row
	 *            - The row to be removed
	 */
	public static void removeRow(Map<LinkedHashMap<String, String>, Integer> collection, LinkedHashMap<String, String> row) {
		for (LinkedHashMap<String, String> v : collection.keySet()) {
			if (compareRows(v, row)) {
				collection.put(v, collection.get(v) - 1);
				if (collection.get(v) == 0) {
					collection.remove(v);
				}
				return;
			}
		}
	}

	/**
	 * Inserts a row to the row collection in place
	 * 
	 * @param collection
	 *            - The row collection where the row will be inserted
	 * @param row
	 *            - The row to be inserted
	 */
	public static void insertRow(Map<LinkedHashMap<String, String>, Integer> collection, LinkedHashMap<String, String> row) {
		for (LinkedHashMap<String, String> v : collection.keySet()) {
			if (compareRows(v, row)) {
				collection.put(v, collection.get(v) + 1);
				return;
			}
		}
		collection.put(row, 1);
	}

	/**
	 * Gets the nth row from the table
	 * 
	 * @param table
	 *            - The table to get the row from
	 * @param rowNum
	 *            - The number of the row
	 * @return The row
	 */
	public static LinkedHashMap<String, String> getRowFromTable(ITable table, int rowNum) throws DataSetException {
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		boolean checkedColumns = false;
		for (Column c : table.getTableMetaData().getColumns()) {
			if (table.getValue(rowNum, c.getColumnName()) != null) {
				result.put(c.getColumnName(), DataType.asString(table.getValue(rowNum, c.getColumnName())));
			}
			checkedColumns = true;
		}
		if (checkedColumns == false) {
			throw new RowOutOfBoundsException();
		}
		return result;
	}
}
