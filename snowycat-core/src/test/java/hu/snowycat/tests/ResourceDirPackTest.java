package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;

public class ResourceDirPackTest extends BaseTester {
	public void testResourceDirPack() throws Exception {
		final int DIR_NUM = 15;
		String[] fileContents = new String[DIR_NUM];
		String[] dirPaths = new String[DIR_NUM];
		List<Integer> num = new ArrayList<Integer>();
		for (int i = 0; i < DIR_NUM; i++) {
			num.add(i);
			File resource1Dir = new File(tempDirectory, "resource" + i);
			resource1Dir.mkdir();
			dirPaths[i] = resource1Dir.getAbsolutePath();
			File resourceFile = new File(resource1Dir, "res.fil");
			resourceFile.createNewFile();
			FileWriter fw = new FileWriter(resourceFile);
			fileContents[i] = RandomStringUtils.randomAlphanumeric(150);
			fw.write(fileContents[i]);
			fw.close();
		}
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "multipledirs.tar", dirPaths);
		Collections.shuffle(num);
		String[] newDirPaths = new String[DIR_NUM];
		for (int i = 0; i < DIR_NUM; i++) {
			newDirPaths[i] = dirPaths[num.get(i)];
			FileUtils.deleteDirectory(new File(newDirPaths[i]));
		}
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "multipledirs.tar", newDirPaths);
		for (int i = 0; i < DIR_NUM; i++) {
			assertTrue(FileUtils.readFileToString(new File(new File(newDirPaths[i]), "res.fil")).compareTo(fileContents[i]) == 0);
		}
	}
}
