package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkiptableCacheTest extends BaseTester {
	public void testSkiptableCache() throws Exception {
		final int ROWNUM = 10;
		final int TABLENUM = 100;
		Map<String, Class<?>> columns = generateColumnsMap(String.class, Integer.class, Date.class, Time.class, Timestamp.class);
		Map<String, List<Map<String, Object>>> rows = new HashMap<String, List<Map<String, Object>>>();
		for (int i = 0; i < TABLENUM; i++) {
			createTable("testtable" + i, columns);
			rows.put("testtable" + i, insertRandomData("testtable" + i, ROWNUM, columns));
		}

		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "second.tar", null);
		for (int i = 0; i <= TABLENUM / 20; i++) {
			c.createStatement().execute("delete from testtable" + i);
			insertRandomData("testtable" + i, ROWNUM, columns);
		}
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "second.tar", "unknown", false, false, null);
		for (int i = 0; i <= TABLENUM / 20; i++) {
			c.createStatement().execute("delete from testtable" + i);
			insertRandomData("testtable" + i, ROWNUM, columns);
		}

		c.createStatement().execute("delete from testtable" + (TABLENUM - 1));
		List<Map<String, Object>> newRow = insertRandomData("testtable" + (TABLENUM - 1), ROWNUM, columns);

		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "second.tar", "unknown", false, false, null);
		assertTrue(checkTable("testtable" + (TABLENUM - 1), newRow));
		assertTrue(checkTables(TABLENUM / 20, rows));
	}
}
