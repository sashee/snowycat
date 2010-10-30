package hu.snowycat.plugin;

import hu.snowycat.SnowyCat;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal import-pack
 * @author sashee
 * */

public class ImportPack extends AbstractMojo {
	private String	targetFileName;
	private String	driverName;
	private String	url;
	private String	userName;
	private String	password;
	private String	resourceDirs;
	private Boolean	disableCache;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// Load the properties from external files if they are present
		try {
			System.getProperties().load(new FileReader("snowycat.properties"));
			System.out.println("Properties loaded from snowycat.properties");
		} catch (IOException fnfe) {
		}
		try {
			System.getProperties().load(new FileReader("snowycat-" + InetAddress.getLocalHost().getHostName() + ".properties"));
			System.out.println("Properties loaded from snowycat-" + InetAddress.getLocalHost().getHostName() + ".properties");
		} catch (IOException fnfe) {
		}
		try {
			System.getProperties().load(new FileReader("snowycat-" + System.getProperty("user.name") + ".properties"));
			System.out.println("Properties loaded from snowycat-" + System.getProperty("user.name") + ".properties");
		} catch (IOException fnfe) {
		}
		// Initialize the arguments
		targetFileName = System.getProperty("targetFileName", "latest.tar");
		driverName = System.getProperty("driverName", "com.mysql.jdbc.Driver");
		url = System.getProperty("url", "jdbc:mysql://localhost:3306/emotion");
		userName = System.getProperty("userName", "emotion");
		password = System.getProperty("password", "emotion");
		resourceDirs = System.getProperty("resourceDirs", "");
		disableCache = Boolean.parseBoolean(System.getProperty("disableCache", "true"));
		try {
			SnowyCat.importPack(driverName, url, userName, password, targetFileName, null, disableCache, disableCache, resourceDirs.compareTo("") == 0 ? new String[] {} : resourceDirs.split("[,]"));
		} catch (Exception e) {
			throw new MojoExecutionException("", e);
		}
	}

}
