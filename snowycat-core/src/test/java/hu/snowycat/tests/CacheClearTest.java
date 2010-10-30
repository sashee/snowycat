package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;
import hu.snowycat.SnowyCatTestCase;

import org.junit.Ignore;

public class CacheClearTest extends BaseTester {
	public void testCacheClear() throws Throwable {
		System.setProperty("driverName", "org.hsqldb.jdbcDriver");
		System.setProperty("url", getConnectionString());
		System.setProperty("userName", "SA");
		System.setProperty("password", "");
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "cacheclear.tar", null);
		CacheClearSCTest test = new CacheClearSCTest();
		test.setName("testCacheClear");
		test.runBare();
		assertTrue(test.cacheCleared);
	}

	@Ignore
	public class CacheClearSCTest extends SnowyCatTestCase {

		public boolean	cacheCleared	= false;

		public void testCacheClear() {
			assertTrue(cacheCleared);
			cacheCleared = false;
		}

		@Override
		public void clearApplicationCache() {
			cacheCleared = true;
		}

		@Override
		public String getTestUrl() {
			return "http://localhost:" + jettyPort;
		}

		@Override
		public String getUsedPackName() {
			return "cacheclear.tar";
		}

	}
}
