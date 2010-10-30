package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCatTestCase;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Ignore;

public class XPathLocatorTest extends BaseTester {

	static private String	hiddenGet	= null;
	static private String	textGet		= null;

	public void testXPathLocator() throws Throwable {
		HttpServlet servlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				hiddenGet = req.getParameter("hidden");
				textGet = req.getParameter("text");
				resp.getWriter().write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n<html><body><div id=\"main\"><form><input type=\"text\" name=\"text\"><input type=\"submit\"><input type=\"hidden\" name=\"hidden\" value=\"first\"></form><form><input type=\"submit\"><input type=\"hidden\" name=\"hidden\" value=\"second\"></form></div></body></html>");
			}
		};
		addServlet(servlet, "xpath");
		SnowyCatTestCase test = new XPathSCTest();
		test.setName("testXPathLocator");
		test.runBare();

	}

	@Ignore
	public static class XPathSCTest extends SnowyCatTestCase {
		public void testXPathLocator() throws Exception {
			selenium.open("/xpath");
			click("(//div[@id='main']//input[@type='submit'])[2]");
			selenium.waitForPageToLoad("60000");
			assertEquals(hiddenGet, "second");
			selenium.open("/xpath");
			type("(//div[@id='main']//input[@type='text'])[1]", "testText");
			click("(//div[@id='main']//input[@type='submit'])[1]");
			selenium.waitForPageToLoad("60000");
			assertEquals(hiddenGet, "first");
			assertEquals(textGet, "testText");
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
