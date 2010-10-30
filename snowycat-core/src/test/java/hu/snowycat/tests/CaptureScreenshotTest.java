package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;
import hu.snowycat.SnowyCatTestCase;

import java.io.File;

import javax.servlet.http.HttpServlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Ignore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CaptureScreenshotTest extends BaseTester {

	public void testCaptureScreenshot() throws Throwable {
		CaptureScreenshotTestServlet servlet = new CaptureScreenshotTestServlet();
		addServlet(servlet, "capturescreenshot");
		System.setProperty("driverName", "org.hsqldb.jdbcDriver");
		System.setProperty("url", getConnectionString());
		System.setProperty("userName", "SA");
		System.setProperty("password", "");
		System.setProperty("captureScreenshots", "true");
		System.setProperty("captureReference", "true");
		System.setProperty("waitBetweenScreenshotsTaken", "500");
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "nothing.tar", null);

		CaptureScreenshotSCTest test = new CaptureScreenshotSCTest();
		test.setName("testCaptureScreenshot");
		test.runBare();

		File mask = new File(new File(new File(new File("snowycat", "pics"), "referenceSets"), "reference"), "master-0-mask.xml");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(mask);
		// <mask><hidedCoordinates><coordinate x="2" y="3"/></hidedCoordinates></mask>
		assertTrue(((Element) doc.getElementsByTagName("hidedCoordinates").item(0)).getElementsByTagName("coordinate").getLength() == 2);

		System.setProperty("driverName", "org.hsqldb.jdbcDriver");
		System.setProperty("url", getConnectionString());
		System.setProperty("userName", "SA");
		System.setProperty("password", "");
		System.setProperty("captureScreenshots", "true");
		System.setProperty("captureReference", "false");

		test = new CaptureScreenshotSCTest();
		test.setName("testCaptureScreenshot");
		test.runBare();

		File picDir = new File(new File(new File("snowycat", "pics"), "tests"), "latest");
		assertEquals(picDir.listFiles().length, 0);

		servlet.text = "Hello world2";

		System.setProperty("driverName", "org.hsqldb.jdbcDriver");
		System.setProperty("url", getConnectionString());
		System.setProperty("userName", "SA");
		System.setProperty("password", "");
		System.setProperty("captureScreenshots", "true");
		System.setProperty("captureReference", "false");

		test = new CaptureScreenshotSCTest();
		test.setName("testCaptureScreenshot");
		test.runBare();

		picDir = new File(new File(new File("snowycat", "pics"), "tests"), "latest");
		assertTrue(picDir.listFiles().length > 0);
	}

	@Ignore
	public static class CaptureScreenshotSCTest extends SnowyCatTestCase {

		public void testCaptureScreenshot() throws Exception {
			selenium.open("/capturescreenshot");
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

	private class CaptureScreenshotTestServlet extends HttpServlet {
		private String	text	= "Hello world";

		protected void doGet(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp) throws javax.servlet.ServletException, java.io.IOException {
			resp.getWriter().write("<html><body><h1>" + text + "</h1><div><script language=\"javascript\">document.write(Math.random());</script></div><div id=\"dynamic\"><script language=\"javascript\">function a(){document.getElementById('dynamic').innerHTML=\"\"+Math.random();} setInterval('a()',100);</script>Random szam</div></body></html>");
		};
	}
}
