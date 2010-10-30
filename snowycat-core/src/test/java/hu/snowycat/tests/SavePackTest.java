package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;
import hu.snowycat.SnowyCatTestCase;

import java.io.File;
import java.io.FileWriter;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;

public class SavePackTest extends BaseTester {
	public void testSavePack() throws Throwable {
		File resource1Dir = new File(tempDirectory, "resource1");
		resource1Dir.mkdir();
		File resourceFile = new File(resource1Dir, "res.fil");
		resourceFile.createNewFile();
		FileWriter fw = new FileWriter(resourceFile);
		fw.write("1");
		fw.close();

		File resource2Dir = new File(tempDirectory, "resource1");
		resource2Dir.mkdir();
		File resource2File = new File(resource2Dir, "res.fil");
		resource2File.createNewFile();
		fw = new FileWriter(resource2File);
		fw.write("1");
		fw.close();

		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "simple.tar", new String[] { resource1Dir.getAbsolutePath(), resource2Dir.getAbsolutePath() });

		resourceFile.delete();
		resourceFile.createNewFile();
		fw = new FileWriter(resourceFile);
		fw.write("2");
		fw.close();

		resource2File.delete();
		resource2File.createNewFile();
		fw = new FileWriter(resource2File);
		fw.write("2");
		fw.close();

		System.setProperty("driverName", "org.hsqldb.jdbcDriver");
		System.setProperty("url", getConnectionString());
		System.setProperty("userName", "SA");
		System.setProperty("password", "");
		System.setProperty("savePack", "true");
		System.setProperty("resourceDirs", resource1Dir.getAbsolutePath() + "," + resource2Dir.getAbsolutePath());

		SavePackSCTest test = new SavePackSCTest();
		test.setName("testNoFailure");
		test.runBare();

		assertTrue(FileUtils.readFileToString(resourceFile).compareTo("2") == 0);
		assertTrue(FileUtils.readFileToString(resource2File).compareTo("2") == 0);
	}

	@Ignore
	public static class SavePackSCTest extends SnowyCatTestCase {
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
