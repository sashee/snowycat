package hu.snowycat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.stream.StreamingDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.xml.sax.InputSource;

/**
 * Provides the methods for the pack management.
 * 
 * @author sashee
 */
public class SnowyCat {

	/**
	 * Exports a pack to snowycat/pack directory.
	 * 
	 * @param driverName
	 *            - The driver name to access the db
	 * @param url
	 *            - The url to the db
	 * @param userName
	 *            - User name for the db
	 * @param password
	 *            - Password for the db
	 * @param targetFileName
	 *            - The file name of the target pack
	 * @param resourceDirs
	 *            - The resource directories which will be in the pack
	 */
	public static void exportPack(String driverName, String url, String userName, String password, String targetFileName, String[] resourceDirs) throws Exception {
		// If no resource dirs are provided, then use an empty String[]
		if (resourceDirs == null) {
			resourceDirs = new String[] {};
		}
		// The target file
		File targetFile = new File("snowycat" + File.separatorChar + "pack", targetFileName);
		System.out.println("Exporting database to:" + targetFileName);

		// Create the directories if not exists
		File parentDirectory = new File(targetFile.getParent());
		if (parentDirectory.exists() == false) {
			File grandparentDirectory = new File(parentDirectory.getParent());
			if (grandparentDirectory.exists() == false) {
				System.out.println("Creating directory: " + grandparentDirectory);
				grandparentDirectory.mkdir();
			}
			System.out.println("Creating directory: " + parentDirectory);
			parentDirectory.mkdir();
		}

		// Create the data set to the memory. It is needed, because the tar archive needs to know the exact size in advance
		StringWriter w = new StringWriter();
		IDatabaseTester tester = new JdbcDatabaseTester(driverName, url, userName, password);
		FlatXmlDataSet.write(tester.getConnection().createDataSet(), w);

		// Create the tar archive
		TarArchiveOutputStream w2 = new TarArchiveOutputStream(new FileOutputStream(targetFile));
		TarArchiveEntry e = new TarArchiveEntry("db1.db");

		// Create the tar entry for the database
		e.setSize(w.getBuffer().length());
		w2.putArchiveEntry(e);
		w2.write(w.getBuffer().toString().getBytes());
		w2.closeArchiveEntry();

		// Iterate and save the resource dirs to the tar archive
		int i = 0;
		for (String s : resourceDirs) {
			TarUtil.addDirectoryToTar(w2, new File(s), s, String.valueOf(i));
			i++;
		}
		// Close the archive
		w2.close();
	}

	/**
	 * Imports a pack
	 * 
	 * @param driverName
	 *            - The driver name to access the db
	 * @param url
	 *            - The url to the db
	 * @param userName
	 *            - User name for the db
	 * @param password
	 *            - Password for the db
	 * @param targetFileName
	 *            - The name of the pack to be imported
	 * @param resourceTargetDirs
	 *            - The directories where to restore the pack's contents
	 */
	public static void importPack(String driverName, String url, String userName, String password, String targetFileName, String[] resourceTargetDirs) throws Exception {
		importPack(driverName, url, userName, password, targetFileName, null, true, true, resourceTargetDirs);
	}

	/**
	 * Imports a pack
	 * 
	 * @param driverName
	 *            - The driver name to access the db
	 * @param url
	 *            - The url to the db
	 * @param userName
	 *            - User name for the db
	 * @param password
	 *            - Password for the db
	 * @param targetFileName
	 *            - The name of the pack to be imported
	 * @param currentDatabaseState
	 *            - The current state of the database. If not known, the null.
	 * @param disableCache
	 *            - Whether to disable cache use when importing
	 * @param dontWriteCache
	 *            - Whether to disable cache write after the import
	 * @param resourceTargetDirs
	 *            - The directories where to restore the pack's contents
	 */
	public static void importPack(String driverName, String url, String userName, String password, String targetFileName, String currentDatabaseState, boolean disableCache, boolean dontWriteCache, String[] resourceTargetDirs) throws Exception {
		// If no resource dirs are provided, then use an empty String[]
		if (resourceTargetDirs == null) {
			resourceTargetDirs = new String[] {};
		}
		// The target file
		File targetFile = new File("snowycat" + File.separatorChar + "pack", targetFileName);
		System.out.println("Importing database from:" + targetFileName);

		// Check whether the provided resource directories are more than 100mb. If they are, then the user might be mistyped it, so don't delete anything
		// If forceDelete is true, then no size check is needed
		if (Boolean.parseBoolean(System.getProperty("forceDelete", "false")) == false) {
			long sumSize = 0;
			for (String s : resourceTargetDirs) {
				sumSize += new File(s).exists() ? FileUtils.sizeOfDirectory(new File(s)) : 0;
			}
			// If more than 100Mb
			if (sumSize > 100 * 1000 * 1000) {
				throw new Exception("The directories you specified to be replaced are more then 100Mb total size. You may mistyped the paths. If you are sure these are the right directories, set the forceDelete property to \"true\"");
			}
		}

		// Check that the user provided the same amount of resource directory path as in the archive
		TarArchiveInputStream checkerTarInput = new TarArchiveInputStream(new FileInputStream(targetFile));
		TarArchiveEntry entry;
		int resourceDirs = -1;
		while ((entry = checkerTarInput.getNextTarEntry()) != null) {
			resourceDirs = Math.max(resourceDirs, entry.getName().contains("/") ? Integer.parseInt(entry.getName().substring(0, entry.getName().indexOf("/"))) : -1);
		}
		checkerTarInput.close();
		// If they does not match, then the import fails
		if (resourceDirs + 1 != resourceTargetDirs.length) {
			throw new Exception("The number of resource directories you specified does not mach that in the archive. Resource directories in the archive:" + resourceDirs);
		}

		// The real archive input used to import the db
		// The db is always the first entry
		TarArchiveInputStream tarInput = new TarArchiveInputStream(new FileInputStream(targetFile));
		tarInput.getNextTarEntry();

		// Construct the dataset from the contents of the archive
		IDataSet ds = new StreamingDataSet(new FlatXmlProducer(new InputSource(tarInput)));

		IDatabaseTester tester = new JdbcDatabaseTester(driverName, url, userName, password);
		tester.setDataSet(tester.getConnection().createDataSet());

		// Fill the skiptables cache
		List<String> skippedTables = CacheUtil.getSkiptableCacheForTransition(currentDatabaseState, targetFileName);
		ITableIterator it = ds.iterator();
		while (it.next()) {
			String tableName = it.getTableMetaData().getTableName();
			// If the skiptables cache contains the current table, then skip it
			if (disableCache == false && skippedTables.contains(tableName)) {
				continue;
			}

			// Calculate the differences between the dataset and the db
			Map<LinkedHashMap<String, String>, Integer>[] diffs = DbUtil.findDifferences(tester.getDataSet().getTable(tableName), it.getTable());
			Map<LinkedHashMap<String, String>, Integer> deletion = diffs[0];
			Map<LinkedHashMap<String, String>, Integer> insertion = diffs[1];

			// If they are the same, then add it to the skiptables cache
			if (deletion.size() == 0 && insertion.size() == 0) {
				skippedTables.add(tableName);
			} else {
				// Not same
				// Needed to disable foreign keys
				try {
					tester.getConnection().getConnection().createStatement().execute("SET REFERENTIAL_INTEGRITY false");
				} catch (SQLException se) {
				}
				// Modify the database to be the same with the dataset
				DbUtil.removeRowsFromDb(tester.getConnection().getConnection(), deletion, tester.getDataSet().getTable(tableName));
				DbUtil.insertRowsToDb(tester.getConnection().getConnection(), insertion, tester.getDataSet().getTable(tableName));
				// Reendable foreign key checks
				try {
					tester.getConnection().getConnection().createStatement().execute("SET REFERENTIAL_INTEGRITY true");
				} catch (SQLException se) {
				}
			}
		}

		// If needed, then write the cache
		if (dontWriteCache == false && currentDatabaseState != null) {
			CacheUtil.saveSkiptablesCacheForTransition(currentDatabaseState, targetFileName, skippedTables);
		}

		// Restore the resource directories

		// Delete the current contents of the resource directories
		int i = 0;
		for (String s : resourceTargetDirs) {
			new File(s).mkdirs();
			FileUtils.deleteDirectory(new File(s));
			i++;
			new File(s).mkdir();
		}

		// Reopen the tar archive. Needed, because it is now closed
		try {
			tarInput.close();
		} catch (Exception e) {

		}
		tarInput = new TarArchiveInputStream(new FileInputStream(targetFile));
		// Skip the first entry (the db)
		TarArchiveEntry e = tarInput.getNextTarEntry();
		while ((e = tarInput.getNextTarEntry()) != null) {
			// Restore the directory in the archive to the destination dir
			int resourceDirNum = Integer.parseInt(e.getName().substring(0, e.getName().indexOf("/")));
			File f = new File(new File(resourceTargetDirs[resourceDirNum]), e.getName().substring(String.valueOf(resourceDirNum).length() + 1));
			f.getParentFile().mkdirs();
			FileOutputStream fos = new FileOutputStream(f);
			IOUtils.copy(tarInput, fos);
			fos.close();
		}
		// Close the file
		tarInput.close();
	}

	/**
	 * Deletes a pack
	 * 
	 * @param targetFileName
	 *            - The file name of the pack
	 */
	public static void deletePack(String targetFileName) {
		File targetFile = new File("snowycat" + File.separatorChar + "pack", targetFileName);
		targetFile.delete();
	}

}
