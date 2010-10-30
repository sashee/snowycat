package hu.snowycat.tests;

import hu.snowycat.BaseTester;
import hu.snowycat.SnowyCat;

public class ForeignKeyTest extends BaseTester {

	public void testForeignKey() throws Exception {
		c.createStatement().execute("create table first (id int,fk2 int)");
		c.createStatement().execute("insert into first values(2,2)");

		c.createStatement().execute("create table second (id int,fk3 int)");
		c.createStatement().execute("insert into second values(2,2)");

		c.createStatement().execute("create table third (id int,fk1 int)");
		c.createStatement().execute("insert into third values(2,2)");

		c.createStatement().execute("ALTER TABLE first ADD PRIMARY KEY (id)");
		c.createStatement().execute("ALTER TABLE second ADD PRIMARY KEY (id)");
		c.createStatement().execute("ALTER TABLE third ADD PRIMARY KEY (id)");

		c.createStatement().execute("ALTER TABLE first ADD CONSTRAINT f1 FOREIGN KEY (fk2) REFERENCES second (id)");
		c.createStatement().execute("ALTER TABLE second ADD CONSTRAINT f2 FOREIGN KEY (fk3) REFERENCES third (id)");
		c.createStatement().execute("ALTER TABLE third ADD CONSTRAINT f3 FOREIGN KEY (fk1) REFERENCES first (id)");

		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "foreign.tar", null);

		c.createStatement().execute("ALTER TABLE first DROP CONSTRAINT f1");
		c.createStatement().execute("ALTER TABLE second DROP CONSTRAINT f2");
		c.createStatement().execute("ALTER TABLE third DROP CONSTRAINT f3");

		c.createStatement().execute("delete from first");
		c.createStatement().execute("delete from second");
		c.createStatement().execute("delete from third");

		c.createStatement().execute("ALTER TABLE first ADD CONSTRAINT f1 FOREIGN KEY (fk2) REFERENCES second (id)");
		c.createStatement().execute("ALTER TABLE second ADD CONSTRAINT f2 FOREIGN KEY (fk3) REFERENCES third (id)");
		c.createStatement().execute("ALTER TABLE third ADD CONSTRAINT f3 FOREIGN KEY (fk1) REFERENCES first (id)");

		SnowyCat.importPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "foreign.tar", null);
	}
}
