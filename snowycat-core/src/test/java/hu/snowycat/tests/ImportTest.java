package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;


public class ImportTest extends BaseTester {
	public void testImport() throws Exception {
		Map<String, List<Map<String, Object>>> rows = new HashMap<String, List<Map<String, Object>>>();
		final int ROWNUM = 100;
		final int TABLENUM = 3;
		Map<String, Class<?>> columns = generateColumnsMap(String.class, Integer.class, Date.class, Time.class, Timestamp.class);
		for (int i = 0; i < TABLENUM; i++) {
			createTable("testtable" + i, columns);
			rows.put("testtable" + i, insertRandomData("testtable" + i, ROWNUM, columns));
		}
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "first.tar", null);

		for (int i = 0; i < TABLENUM; i++) {
			c.createStatement().execute("delete from testtable" + i);
			insertRandomData("testtable" + i, ROWNUM + RandomUtils.nextInt(ROWNUM) - ROWNUM / 2, columns);
		}

		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "first.tar", null);

		assertTrue(checkTables(TABLENUM, rows));
	}
}
