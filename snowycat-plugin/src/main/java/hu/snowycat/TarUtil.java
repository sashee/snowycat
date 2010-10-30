package hu.snowycat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Provides static methods to work with tar archives
 * 
 * @author sashee
 */
public class TarUtil {

	/**
	 * Adds a whole directory to the tar archive
	 * 
	 * @param tarOut
	 *            - The tar output stream
	 * @param dir
	 *            - The directory to be addid
	 * @param baseDir
	 *            - The base directory, paths will be relative to this in the archive
	 * @param inTarDirName
	 *            - The root directory in the tar archive where the directory will be added
	 */
	public static void addDirectoryToTar(TarArchiveOutputStream tarOut, File dir, String baseDir, String inTarDirName) throws IOException {
		if (dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				addDirectoryToTar(tarOut, f, baseDir, inTarDirName);
			}
		} else {
			String fileName = dir.getAbsolutePath().substring(baseDir.length());
			if (fileName.startsWith("/")) {
				fileName = fileName.substring(1);
			}
			fileName = inTarDirName + "/" + fileName;
			TarArchiveEntry e = new TarArchiveEntry(dir, fileName);
			tarOut.putArchiveEntry(e);
			FileInputStream fs = new FileInputStream(dir);
			IOUtils.copy(fs, tarOut);
			fs.close();
			tarOut.closeArchiveEntry();
		}
	}
}
