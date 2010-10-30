package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;
import hu.snowycat.SnowyCatTestCase;

import java.io.File;

import org.junit.Ignore;

public class NoCaptureByDefaultTest extends BaseTester {
	public void testNoCaptureByDefault() throws Throwable {
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "nothing.tar", null);

		System.setProperty("driverName", "org.hsqldb.jdbcDriver");
		System.setProperty("url", getConnectionString());
		System.setProperty("userName", "SA");
		System.setProperty("password", "");

		NoCaptureScreenshotByDefaultSCTest test = new NoCaptureScreenshotByDefaultSCTest();
		test.setName("testCaptureScreenshot");
		test.runBare();

		assertFalse(new File("snowycat", "pics").exists());
	}

	@Ignore
	public static class NoCaptureScreenshotByDefaultSCTest extends SnowyCatTestCase {

		public void testCaptureScreenshot() throws Exception {
			selenium.open("/?mode=testCaptureScreenshot");
			captureEntirePageScreenshotToString();
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
			return "nothing.tar";
		}
	}
}
