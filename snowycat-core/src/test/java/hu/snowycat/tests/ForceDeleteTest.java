package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;
import hu.snowycat.SnowyCatTestCase;

import java.io.File;
import java.io.FileWriter;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Ignore;

public class ForceDeleteTest extends BaseTester {
	public void testForceDelete() throws Throwable {
		File smallTargetDir = new File(tempDirectory, "smalltarget");
		smallTargetDir.mkdir();
		File smallFile = new File(smallTargetDir, "smallfile.fl");
		smallFile.createNewFile();
		FileWriter fw = new FileWriter(smallFile);
		for (int i = 0; i < 1; i++) {
			fw.write(RandomStringUtils.random(100));
		}
		fw.close();
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "sample.tar", new String[] { smallTargetDir.getAbsolutePath() });

		File bigTargetDir = new File(tempDirectory, "bigTarget");
		bigTargetDir.mkdir();
		File bigFile = new File(bigTargetDir, "bigfile.fl");
		bigFile.createNewFile();
		fw = new FileWriter(bigFile);
		for (int i = 0; i < 110; i++) {
			fw.write(RandomStringUtils.randomAlphanumeric(1000000));
		}
		fw.close();

		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "bigsample.tar", new String[] { bigTargetDir.getAbsolutePath() });

		try {
			SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "sample.tar", new String[] { bigTargetDir.getAbsolutePath() });
			fail();
		} catch (Exception e) {
		}

		System.setProperty("forceDelete", "true");
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "sample.tar", new String[] { bigTargetDir.getAbsolutePath() });
		System.clearProperty("forceDelete");

		System.setProperty("driverName", "org.hsqldb.jdbcDriver");
		System.setProperty("url", getConnectionString());
		System.setProperty("userName", "SA");
		System.setProperty("password", "");
		System.setProperty("captureScreenshots", "false");
		System.setProperty("captureReference", "false");
		System.setProperty("resourceDirs", bigTargetDir.getAbsolutePath());

		SnowyCatTestCase test = new ForceDeleteSCTest1();
		test.setName("testNothing");
		test.runBare();

		test = new ForceDeleteSCTest2();
		test.setName("testNothing");
		test.runBare();
	}

	@Ignore
	public static class ForceDeleteSCTest1 extends SnowyCatTestCase {

		public void testNothing() {
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
			return "bigsample.tar";
		}
	}

	@Ignore
	public static class ForceDeleteSCTest2 extends SnowyCatTestCase {

		public void testNothing() {
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
			return "sample.tar";
		}
	}
}
