package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;

import java.sql.ResultSet;

public class SimpleExportImportTest extends BaseTester {
	public void testSimpleExportImport() throws Exception {
		c.createStatement().execute("create table valami (id int)");
		c.createStatement().execute("insert into valami values(2)");
		c.createStatement().execute("insert into valami values(3)");

		ResultSet rs = c.createStatement().executeQuery("select count(*) from valami");
		rs.next();
		assertEquals(2, rs.getInt(1));

		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "first.tar", null);

		rs = c.createStatement().executeQuery("delete from valami");
		rs = c.createStatement().executeQuery("select count(*) from valami");
		rs.next();
		assertEquals(0, rs.getInt(1));

		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "first.tar", null);

		rs = c.createStatement().executeQuery("select count(*) from valami");
		rs.next();
		assertEquals(2, rs.getInt(1));
	}
}
