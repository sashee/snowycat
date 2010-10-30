package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

public class NullValuesTest extends BaseTester {
	@SuppressWarnings("unchecked")
	public void testNullValues() throws Exception {
		c.createStatement().execute("create table nulltable (number INT PRIMARY KEY, string LONGVARCHAR)");
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

		rows.add(MapUtils.putAll(new HashMap<String, Object>(), new Object[] { "number", 1, "string", "abc" }));
		rows.add(MapUtils.putAll(new HashMap<String, Object>(), new Object[] { "number", 2 }));
		insertRows("nulltable", rows);
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "null.tar", null);

		c.createStatement().execute("delete from nulltable");
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "null.tar", null);
		assertTrue(checkTable("nulltable", rows));

		c.createStatement().execute("delete from nulltable where number=2");
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "null.tar", null);
		assertTrue(checkTable("nulltable", rows));

		c.createStatement().execute("delete from nulltable where number=2");
		c.createStatement().execute("insert into nulltable (number) values (3)");
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "null.tar", null);
		assertTrue(checkTable("nulltable", rows));

		c.createStatement().execute("drop table nulltable");
		c.createStatement().execute("CREATE TABLE MBCategory (  uuid_ varchar(75) DEFAULT NULL,  categoryId bigint NOT NULL,  groupId bigint DEFAULT NULL,  companyId bigint DEFAULT NULL,  userId bigint DEFAULT NULL,  userName varchar(75) DEFAULT NULL,  createDate datetime DEFAULT NULL,  modifiedDate datetime DEFAULT NULL,  parentCategoryId bigint DEFAULT NULL,  name varchar(75) DEFAULT NULL,  description longvarchar,  threadCount int DEFAULT NULL,  messageCount int DEFAULT NULL,  lastPostDate datetime DEFAULT NULL,  PRIMARY KEY (categoryId),  UNIQUE (uuid_,groupId))");
		c.createStatement().execute("INSERT INTO MBCategory (uuid_, categoryId, groupId, companyId, userId, userName, createDate, modifiedDate, parentCategoryId, name, description, threadCount,messageCount, lastPostDate) VALUES('a18bb084-06bb-4b0b-bff9-6198be57b92b', 0, 0, 0, 0, '', NULL, NULL, 0, '', '', 6, 6, '2010-04-23 22:42:21');");
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "null.tar", null);
		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "null.tar", null);
	}
}
