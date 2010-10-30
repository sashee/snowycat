package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;

import java.io.File;
import java.io.FileWriter;

import org.apache.commons.lang.RandomStringUtils;

public class ResourceDirNumIncorrentFailureTest extends BaseTester {
	public void testResourceDirNumIncorrentFailure() throws Exception {
		File resource1Dir = new File(tempDirectory, "resource1");
		resource1Dir.mkdir();
		File resourceFile = new File(resource1Dir, "res.fil");
		resourceFile.createNewFile();
		FileWriter fw = new FileWriter(resourceFile);
		fw.write(RandomStringUtils.randomAlphanumeric(150));
		fw.close();

		File resource2Dir = new File(tempDirectory, "resource2");
		resource2Dir.mkdir();
		resourceFile = new File(resource2Dir, "res.fil");
		resourceFile.createNewFile();
		fw = new FileWriter(resourceFile);
		fw.write(RandomStringUtils.randomAlphanumeric(150));
		fw.close();

		File resource3Dir = new File(tempDirectory, "resource3");
		resource3Dir.mkdir();

		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "multipledirs.tar", new String[] { resource1Dir.getAbsolutePath(),
				resource2Dir.getAbsolutePath() });

		try {
			SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "multipledirs.tar", new String[] { resource1Dir.getAbsolutePath() });
			fail();
		} catch (Exception e) {
		}

		try {
			SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "multipledirs.tar", new String[] { resource1Dir.getAbsolutePath(),
					resource2Dir.getAbsolutePath(), resource3Dir.getAbsolutePath() });
			fail();
		} catch (Exception e) {
		}
	}
}
