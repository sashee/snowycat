package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;
import hu.snowycat.SnowyCatTestCase;

import java.sql.ResultSet;

import junit.framework.AssertionFailedError;

import org.junit.Ignore;

public class FailureNoCacheTest extends BaseTester{
	public void testFailureNoCache() throws Throwable {
		c.createStatement().execute("create table t (id int)");
		c.createStatement().execute("insert into t values(2)");
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "simple.tar", null);
		// Import to let it cache
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "simple.tar", null);
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "simple.tar", null);

		System.setProperty("driverName", "org.hsqldb.jdbcDriver");
		System.setProperty("url", getConnectionString());
		System.setProperty("userName", "SA");
		System.setProperty("password", "");

		try {
			TestFailureNoCacheSCTest test = new TestFailureNoCacheSCTest();
			test.setName("testFailure");
			test.runBare();
			throw new Exception();
		} catch (AssertionFailedError afe) {
		}

		NoFailTestSCTest test = new NoFailTestSCTest();
		test.setName("testNoFailure");
		test.runBare();

		ResultSet rs = c.createStatement().executeQuery("select count(*) from t where id=2");
		rs.next();
		assertSame(rs.getInt(1), 1);
	}

	@Ignore
	public static class TestFailureNoCacheSCTest extends SnowyCatTestCase {
		public void testFailure() throws Exception {
			c.createStatement().execute("delete from t");
			c.createStatement().execute("insert into t values(3)");
			fail();
		}

		@Override
		public void clearApplicationCache() {
		}

		@Override
		public String getTestUrl() {
			return "http://localhost:" + jettyPort;
		}

		@Override
		public String getUsedPackName() {
			return "simple.tar";
		}
	}

	@Ignore
	public static class NoFailTestSCTest extends SnowyCatTestCase {
		public void testNoFailure() throws Exception {
		}

		@Override
		public void clearApplicationCache() {
		}

		@Override
		public String getTestUrl() {
			return "http://localhost:" + jettyPort;
		}

		@Override
		public String getUsedPackName() {
			return "simple.tar";
		}
	}
}
