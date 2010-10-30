package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCatTestCase;
import hu.snowycat.plugin.ExportPack;
import hu.snowycat.plugin.ImportPack;

import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.util.Properties;

import org.junit.Ignore;

public class ExtrenalPropertiesFileTest extends BaseTester {
	public void testExternalPropertiesFile() throws Throwable {
		c.createStatement().execute("create table t (id int)");
		File defProps = new File("snowycat.properties");
		defProps.createNewFile();
		defProps.deleteOnExit();

		Properties defProperties = new Properties();
		defProperties.setProperty("driverName", "org.hsqldb.jdbcDriver");
		defProperties.setProperty("url", getConnectionString());
		defProperties.setProperty("userName", "SA");
		defProperties.setProperty("password", "");
		defProperties.setProperty("number", "2");
		FileWriter fw = new FileWriter(defProps);
		defProperties.store(fw, "");
		fw.close();

		new ExportPack().execute();
		new ImportPack().execute();

		ExternalPropertiesSCTest test = new ExternalPropertiesSCTest();
		test.setName("testExternalProperties");
		test.runBare();

		ResultSet rs = c.createStatement().executeQuery("select count(*) from t where id=2");
		rs.next();
		assertSame(rs.getInt(1), 1);

		System.getProperties().clear();
		System.getProperties().putAll(propertiesStore);

		File machineProps = new File("snowycat-" + InetAddress.getLocalHost().getHostName() + ".properties");
		machineProps.createNewFile();
		machineProps.deleteOnExit();

		Properties machineProperties = new Properties();
		machineProperties.setProperty("number", "3");
		fw = new FileWriter(machineProps);
		machineProperties.store(fw, "");
		fw.close();

		c.createStatement().execute("delete from t");

		test = new ExternalPropertiesSCTest();
		test.setName("testExternalProperties");
		test.runBare();

		rs = c.createStatement().executeQuery("select count(*) from t where id=3");
		rs.next();
		assertSame(rs.getInt(1), 1);

		System.getProperties().clear();
		System.getProperties().putAll(propertiesStore);

		File userProps = new File("snowycat-" + System.getProperty("user.name") + ".properties");
		userProps.createNewFile();
		userProps.deleteOnExit();

		Properties userProperties = new Properties();
		userProperties.setProperty("number", "4");
		fw = new FileWriter(userProps);
		userProperties.store(fw, "");
		fw.close();

		c.createStatement().execute("delete from t");

		test = new ExternalPropertiesSCTest();
		test.setName("testExternalProperties");
		test.runBare();

		rs = c.createStatement().executeQuery("select count(*) from t where id=4");
		rs.next();
		assertSame(rs.getInt(1), 1);
	}

	@Ignore
	public static class ExternalPropertiesSCTest extends SnowyCatTestCase {
		public void testExternalProperties() throws Exception {
			c.createStatement().execute("insert into t values (" + System.getProperty("number") + ")");
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
			return "latest.tar";
		}
	}
}
