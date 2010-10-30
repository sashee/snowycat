package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;

import java.sql.ResultSet;

public class DisableCacheTest extends BaseTester {
	public void testDisableCache() throws Exception {
		c.createStatement().execute("create table t (id int)");
		c.createStatement().execute("insert into t values(2)");
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "simple.tar", null);
		// Import to let it cache
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "simple.tar", "simple.tar", false, false, null);
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "simple.tar", "simple.tar", false, false, null);
		c.createStatement().execute("insert into t values(3)");
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "simple.tar", "simple.tar", true, false, null);
		ResultSet rs = c.createStatement().executeQuery("select count(*) from t");
		rs.next();
		assertSame(rs.getInt(1), 1);
	}
}
