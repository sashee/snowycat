package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Map;

public class ExportImportEmptyTableTest extends BaseTester {
	public void testExportImportEmptyTable() throws Exception {
		System.out.println("If this test does not finish within a second, then it is deadlocked");
		final int TABLENUM = 10;
		Map<String, Class<?>> columns = generateColumnsMap(String.class, Integer.class, Date.class, Time.class, Timestamp.class);
		for (int i = 0; i < TABLENUM; i++) {
			createTable("testtable" + i, columns);
		}
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "first.tar", null);
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "first.tar", null);
	}
}
