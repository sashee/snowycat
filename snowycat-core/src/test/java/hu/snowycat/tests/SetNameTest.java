package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;
import hu.snowycat.SnowyCatTestCase;

import java.io.File;

import javax.servlet.http.HttpServlet;

import org.junit.Ignore;

public class SetNameTest extends BaseTester {

	public void testSetName() throws Throwable {
		SetNameTestServlet servlet = new SetNameTestServlet();
		addServlet(servlet, "setname");
		System.setProperty("driverName", "org.hsqldb.jdbcDriver");
		System.setProperty("url", getConnectionString());
		System.setProperty("userName", "SA");
		System.setProperty("password", "");
		System.setProperty("captureScreenshots", "true");
		System.setProperty("captureReference", "true");
		System.setProperty("setName", "first");
		System.setProperty("waitBetweenScreenshotsTaken", "500");
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "nothing.tar", null);

		SetNameSCTest test = new SetNameSCTest();
		test.setName("testCaptureScreenshot");
		test.runBare();

		servlet.text = "Hello world2";

		System.setProperty("setName", "second");

		test = new SetNameSCTest();
		test.setName("testCaptureScreenshot");
		test.runBare();

		System.setProperty("captureScreenshots", "true");
		System.setProperty("captureReference", "false");
		System.setProperty("setName", "secondTest");
		System.setProperty("referenceSet", "second");

		test = new SetNameSCTest();
		test.setName("testCaptureScreenshot");
		test.runBare();

		servlet.text = "Hello world";

		System.setProperty("setName", "firstTest");
		System.setProperty("referenceSet", "first");

		test = new SetNameSCTest();
		test.setName("testCaptureScreenshot");
		test.runBare();

		System.setProperty("setName", "thirdTest");
		System.setProperty("referenceSet", "second");

		test = new SetNameSCTest();
		test.setName("testCaptureScreenshot");
		test.runBare();

		File picDir = new File(new File(new File("snowycat", "pics"), "tests"), "firstTest");
		assertTrue(picDir.listFiles().length == 0);

		picDir = new File(new File(new File("snowycat", "pics"), "tests"), "secondTest");
		assertTrue(picDir.listFiles().length == 0);

		picDir = new File(new File(new File("snowycat", "pics"), "tests"), "thirdTest");
		assertTrue(picDir.listFiles().length > 0);
	}

	@Ignore
	public static class SetNameSCTest extends SnowyCatTestCase {
		public void testCaptureScreenshot() throws Exception {
			selenium.open("/setname");
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

	private class SetNameTestServlet extends HttpServlet {
		private String	text	= "Hello world";

		protected void doGet(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp) throws javax.servlet.ServletException, java.io.IOException {
			resp.getWriter().write("<html><body><h1>" + text + "</h1></body></html>");
		};
	}
}
