package hu.snowycat;

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.hsqldb.Server;

/**
 * The base class of all SnowyCat tester. Starts the necessary servers and provides helper methods.
 * 
 * @author sashee
 */
public class BaseTester extends TestCase {

	/** The maximum port to be used by HSQLDB */
	private final int						hsqlMaxPort	= Integer.parseInt(System.getProperty("hsqldbMaxPort", "9010")) + 1;

	/** The minimum port to be used by HSQLDB */
	private final int						hsqlMinPort	= Integer.parseInt(System.getProperty("hsqldbMinPort", "9000"));

	/** The actual port used by HSQLDB */
	protected int							portNumber	= hsqlMinPort;

	/** The connection to the db */
	protected static Connection				c;

	/** The embedded Jetty server */
	private org.eclipse.jetty.server.Server	jettyServer;

	/** The context handler for the Jetty server. Used to deploy servlets */
	private ServletContextHandler			handler;

	/** The port used by the Jetty server */
	protected static final int				jettyPort	= Integer.parseInt(System.getProperty("jettyPort", "8082"));

	/** A temp directory that will be created in advance and deleted after each test */
	protected File							tempDirectory;

	/** A map containing all the system properties. These will be restored after each test */
	protected Map<Object, Object>			propertiesStore;

	@Override
	protected void setUp() throws Exception {
		// Start the Jetty server and deploy the tester servlet
		jettyServer = new org.eclipse.jetty.server.Server(jettyPort);
		handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		handler.setContextPath("/");
		jettyServer.setHandler(handler);
		addServlet(new HttpServlet() {
			protected void doGet(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp) throws javax.servlet.ServletException, java.io.IOException {
				resp.getWriter().write("It works: SnowyCat tester servlet");
			}
		}, "/*");
		jettyServer.start();
		// Delete the snowycat directory to have the test a clean run
		FileUtils.deleteDirectory(new File("snowycat"));
		// Load the db driver
		Class.forName("org.hsqldb.jdbcDriver").newInstance();
		// Start the hsqldb server on a new port. It is needed, because the hsql does not quit immediately and locks the port between tests for some time
		Server s = new Server();
		s.setSilent(true);
		s.setAddress("localhost");
		portNumber++;
		portNumber = (portNumber - hsqlMinPort) % (hsqlMaxPort - hsqlMinPort) + hsqlMinPort;
		s.setPort(portNumber);
		s.setDatabaseName(0, "testdb" + portNumber);
		s.setDatabasePath(0, "mem:testdb" + portNumber);
		s.start();
		// Get the connection
		c = DriverManager.getConnection(getConnectionString());
		// Create a temp directory which will be deleted after the test
		tempDirectory = new File("testtemp");
		FileUtils.deleteDirectory(tempDirectory);
		tempDirectory.mkdir();
		// Saves the current properties to be restored adter the test
		propertiesStore = new HashMap<Object, Object>(System.getProperties());

		// Create an empty pack
		System.setProperty("driverName", "org.hsqldb.jdbcDriver");
		System.setProperty("url", getConnectionString());
		System.setProperty("userName", "SA");
		System.setProperty("password", "");
		SnowyCat.exportPack("org.hsqldb.jdbcDriver", getConnectionString(), "SA", "", "nothing.tar", null);
	}

	@Override
	protected void tearDown() throws Exception {
		// Shut down the hsqldb
		c.createStatement().execute("SHUTDOWN IMMEDIATELY");
		// Stops the jetty server
		jettyServer.stop();
		// Cleans up
		if (tempDirectory != null) {
			FileUtils.deleteDirectory(tempDirectory);
		}
		FileUtils.deleteDirectory(new File("snowycat"));
		// Restore the properties
		System.getProperties().clear();
		System.getProperties().putAll(propertiesStore);
	}

	/**
	 * Adds a servlet to the given path
	 * 
	 * @param servlet
	 *            - The servlet to be added
	 * @param path
	 *            - The path where the servlet will be accessible
	 */
	protected void addServlet(Servlet servlet, String path) {
		handler.addServlet(new ServletHolder(servlet), "/" + path);
	}

	/**
	 * Returns the connection url to be used to connect to the db
	 * 
	 * @return The connection url
	 */
	protected String getConnectionString() {
		return "jdbc:hsqldb:hsql://localhost:" + portNumber + "/testdb" + portNumber;
	}

	/**
	 * Checks a table in the db if it is identical to the rows provided
	 * 
	 * @param tableName
	 *            - The name of the table in the db
	 * @param rows
	 *            - The list of the rows. Each row consists key->value pairs
	 * @return Whether the table in the db is identical with the rows provided
	 */
	protected boolean checkTable(String tableName, List<Map<String, Object>> rows) throws Exception {
		for (Map<String, Object> row : rows) {
			StringBuilder sql = new StringBuilder("select count(*) from " + tableName + " where ");
			for (Entry<String, Object> e : row.entrySet()) {
				sql.append(e.getKey() + "=? AND ");
			}
			PreparedStatement st = c.prepareStatement(sql.toString().substring(0, sql.length() - 5));
			int pos = 1;
			for (Entry<String, Object> e : row.entrySet()) {
				if (e.getValue().getClass().isAssignableFrom(String.class)) {
					st.setString(pos, (String) e.getValue());
				} else if (e.getValue().getClass().isAssignableFrom(Integer.class)) {
					st.setInt(pos, (Integer) e.getValue());
				} else if (e.getValue().getClass().isAssignableFrom(Date.class)) {
					st.setDate(pos, (Date) e.getValue());
				} else if (e.getValue().getClass().isAssignableFrom(Time.class)) {
					st.setTime(pos, (Time) e.getValue());
				} else if (e.getValue().getClass().isAssignableFrom(Timestamp.class)) {
					st.setTimestamp(pos, (Timestamp) e.getValue());
				}
				pos++;
			}
			ResultSet rs = st.executeQuery();
			rs.next();
			if (1 != rs.getInt(1)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks multiple tables named testtable{i}, where i is going from 0 to tableNum
	 * 
	 * @param tableNum
	 *            - The number of the tables to be checked
	 * @param rows
	 *            - The tables to be checked with
	 * @return Whether all tables are identical with the provided tables
	 */
	protected boolean checkTables(int tableNum, Map<String, List<Map<String, Object>>> rows) throws Exception {

		for (int i = 0; i < tableNum; i++) {
			if (checkTable("testtable" + i, rows.get("testtable" + i)) == false) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Generates random names to column types
	 * 
	 * @param columns
	 *            - The column types to generate name to
	 * @return The named columns with the provided types
	 */
	protected Map<String, Class<?>> generateColumnsMap(Class<?>... columns) {
		Map<String, Class<?>> result = new HashMap<String, Class<?>>();
		for (Class<?> c : columns) {
			result.put(RandomStringUtils.randomAlphabetic(10), c);
		}
		return result;
	}

	/**
	 * Creates table in the db based on the provided schema
	 * 
	 * @param tableName
	 *            - The name of the table
	 * @param columns
	 *            - The columns to be created with name and type
	 * @throws Exception
	 */
	protected void createTable(String tableName, Map<String, Class<?>> columns) throws Exception {
		StringBuilder sql = new StringBuilder();
		sql.append("create table " + tableName + " (");
		for (Entry<String, Class<?>> c : columns.entrySet()) {
			String type = null;
			if (c.getValue().isAssignableFrom(String.class)) {
				type = "longVARCHAR";
			} else if (c.getValue().isAssignableFrom(Integer.class)) {
				type = "int";
			} else if (c.getValue().isAssignableFrom(Date.class)) {
				type = "date";
			} else if (c.getValue().isAssignableFrom(Time.class)) {
				type = "time";
			} else if (c.getValue().isAssignableFrom(Timestamp.class)) {
				type = "timestamp";
			}
			if (type == null) {
				throw new RuntimeException("Type " + c.getValue() + " is not supported");
			}
			sql.append(c.getKey() + " " + type + ",");
		}
		sql.setCharAt(sql.length() - 1, ')');
		c.createStatement().execute(sql.toString());
	}

	/**
	 * Inserts a row to a table
	 * 
	 * @param tableName
	 *            - The name of the table
	 * @param row
	 *            - The row to be inserted
	 */
	private static void insertRow(String tableName, Map<String, Object> row) throws Exception {

		StringBuilder names = new StringBuilder();
		StringBuilder values = new StringBuilder();
		for (String e : row.keySet()) {
			names.append(e + ",");
			values.append("?,");
		}
		PreparedStatement st = c.prepareStatement("insert into " + tableName + " (" + names.toString().substring(0, names.length() - 1) + ") values (" + values.toString().substring(0, values.length() - 1) + ")");

		int pos = 1;
		for (Object value : row.values()) {
			Class<?> type = value.getClass();
			if (type.isAssignableFrom(String.class)) {
				st.setString(pos, (String) value);
			} else if (type.isAssignableFrom(Integer.class)) {
				st.setInt(pos, (Integer) value);
			} else if (type.isAssignableFrom(Date.class)) {
				st.setDate(pos, (Date) value);
			} else if (type.isAssignableFrom(Time.class)) {
				st.setTime(pos, (Time) value);
			} else if (type.isAssignableFrom(Timestamp.class)) {
				st.setTimestamp(pos, (Timestamp) value);
			}
			pos++;
		}
		st.execute();
	}

	/**
	 * Inserts multiple rows to a table
	 * 
	 * @param tableName
	 *            - The name of the table
	 * @param rows
	 *            - The rows to be inserted
	 */
	protected void insertRows(String tableName, List<Map<String, Object>> rows) throws Exception {
		for (Map<String, Object> row : rows) {
			insertRow(tableName, row);
		}
	}

	/**
	 * Inserts random data to a table
	 * 
	 * @param tableName
	 *            - The name of the table
	 * @param rows
	 *            - The number of rows to be inserted
	 * @param columns
	 *            - The definition of the columns
	 * @return The inserted data
	 */
	protected List<Map<String, Object>> insertRandomData(String tableName, int rows, Map<String, Class<?>> columns) throws Exception {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < rows; i++) {
			Map<String, Object> newRow = new HashMap<String, Object>();
			for (Entry<String, Class<?>> e : columns.entrySet()) {
				if (e.getValue().isAssignableFrom(String.class)) {
					newRow.put(e.getKey(), RandomStringUtils.randomAlphabetic(15));
				} else if (e.getValue().isAssignableFrom(Integer.class)) {
					newRow.put(e.getKey(), RandomUtils.nextInt());
				} else if (e.getValue().isAssignableFrom(Date.class)) {
					newRow.put(e.getKey(), new Date(System.currentTimeMillis() - RandomUtils.nextInt()));
				} else if (e.getValue().isAssignableFrom(Time.class)) {
					newRow.put(e.getKey(), new Time(System.currentTimeMillis() - RandomUtils.nextInt()));
				} else if (e.getValue().isAssignableFrom(Timestamp.class)) {
					newRow.put(e.getKey(), new Timestamp(System.currentTimeMillis() - RandomUtils.nextInt()));
				}
			}
			result.add(newRow);
		}
		insertRows(tableName, result);
		return result;
	}
}
