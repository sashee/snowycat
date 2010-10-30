package hu.snowycat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openqa.selenium.server.SeleniumServer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.thoughtworks.selenium.SeleneseTestCase;

/**
 * The base class for web tests
 * 
 * @author sashee
 */
public abstract class SnowyCatTestCase extends SeleneseTestCase {

	/** After how many same images needed to be taken to continue in a single run */
	private Integer							captureNum					= null;
	/** After how many same images needed to be taken to finish a recursive run */
	private Integer							recurseCaptureNum			= null;

	/** The JDBC driver's class */
	private String							driverName;
	/** The database's URL */
	private String							url;
	/** The username to the DB */
	private String							userName;
	/** The password for the DB */
	private String							password;

	/** In compare mode, which set is used as the reference */
	private String							referenceSet				= null;
	/** Whether the current operation is to capture references or to compare captured shots */
	private Boolean							captureReference			= null;
	/** The name where the current execution will save the images */
	private String							setName						= null;
	/** Whether to block test recursion further */
	private Boolean							noRecursive					= null;
	/** Whether to capture only one image and store it */
	private Boolean							noAdditionalHiding			= null;
	/** Maps the hided coordinates to the taken pictures' number. The coordinates are in int[x,y] format */
	private Map<Integer, List<Integer[]>>	hidedCoordinates			= new HashMap<Integer, List<Integer[]>>();
	/** Whether to delete the temp directory adter finishing */
	private Boolean							deleteTempDir				= null;
	/** Whether to save the database and resources prior to execution and restore them after */
	private Boolean							savePack					= null;
	/** Whether to capture screenshots or just test the functionality */
	private Boolean							captureScreenshots			= null;
	/** Waits this milliseconds between screenshots taken */
	private Integer							waitBetweenScreenshotsTaken	= null;
	/** The resource directories that will be restored along with the db */
	private String[]						resourceDirs				= null;
	/** Whether to use the cache when importing the pack */
	private Boolean							disableCache				= null;

	/** Number of pictures currently taken */
	private int								pictureNum					= 0;

	/** Points to the snowycat/ directory */
	private File							snowyCatDir;
	/** Points to the snowycat/temp/ directory */
	private File							tempDir;

	/** A counter to the filenames in the temp directory. Needed to preserve chronology and still deny name collisions */
	private static int						currentTempCounter			= 0;

	private SeleniumServer					ss;

	@Override
	public void setUp() throws Exception {
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
		// Start the selenium server
		ss = new SeleniumServer();
		ss.start();
		// Sets the variables
		driverName = System.getProperty("driverName", "com.mysql.jdbc.Driver");
		url = System.getProperty("url", "jdbc:mysql://localhost:3306/emotion");
		userName = System.getProperty("userName", "emotion");
		password = System.getProperty("password", "emotion");
		// If these are already set via the setter methods, then don't overwrite
		if (captureNum == null) {
			captureNum = Integer.parseInt(System.getProperty("captureNum", "2"));
		}
		if (recurseCaptureNum == null) {
			recurseCaptureNum = Integer.parseInt(System.getProperty("recurseCaptureNum", "2"));
		}
		if (referenceSet == null) {
			referenceSet = System.getProperty("referenceSet", "reference");
		}
		if (captureReference == null) {
			captureReference = Boolean.parseBoolean(System.getProperty("captureReference", "false"));
		}
		if (setName == null) {
			setName = System.getProperty("setName", captureReference ? "reference" : "latest");
		}
		if (noRecursive == null) {
			noRecursive = false;
		}
		if (noAdditionalHiding == null) {
			noAdditionalHiding = false;
		}
		if (deleteTempDir == null) {
			deleteTempDir = Boolean.parseBoolean(System.getProperty("deleteTempDir", "true"));
		}
		if (savePack == null) {
			savePack = Boolean.parseBoolean(System.getProperty("savePack", "false"));
		}
		if (captureScreenshots == null) {
			captureScreenshots = Boolean.parseBoolean(System.getProperty("captureScreenshots", "false"));
		}
		if (waitBetweenScreenshotsTaken == null) {
			waitBetweenScreenshotsTaken = Integer.parseInt(System.getProperty("waitBetweenScreenshotsTaken", "0"));
		}
		if (resourceDirs == null) {
			resourceDirs = System.getProperty("resourceDirs", "").compareTo("") == 0 ? new String[] {} : System.getProperty("resourceDirs", "").split("[,]");
		}
		if (disableCache == null) {
			disableCache = Boolean.parseBoolean(System.getProperty("disableCache", "false"));
		}
		// If no screenshots are captured, then do not recurse
		if (captureScreenshots == false) {
			noRecursive = true;
		}
		// If screenshots are not taken, then do not modify directories
		if (captureScreenshots) {
			// Creating directories
			snowyCatDir = new File("snowycat");
			if (snowyCatDir.exists() == false) {
				System.out.println("Creating directory:" + snowyCatDir.getAbsolutePath());
				snowyCatDir.mkdir();
			}
			tempDir = new File(snowyCatDir, "temp");
			if (deleteTempDir) {
				deleteDirectory(tempDir);
			}
			if (tempDir.exists() == false) {
				System.out.println("Creating directory:" + tempDir.getAbsolutePath());
				tempDir.mkdir();
			}

			// Deletes the directory where the pics will be stored
			deleteDirectory(new File(new File(new File(snowyCatDir, "pics"), captureReference ? "referenceSets" : "tests"), setName));
		}

		// Saves the db and resources if needed
		if (savePack) {
			SnowyCat.exportPack(driverName, url, userName, password, "temp.tar", resourceDirs);
		}
		// Import the saved pack to set up the test
		SnowyCat.importPack(driverName, url, userName, password, getUsedPackName(), getLastRunTest(), disableCache, disableCache, resourceDirs);
		// If the first deletion of the resource dirs was successfull, then following deletions will be forced
		System.setProperty("forceDelete", "true");

		clearApplicationCache();
		// Starts the browser
		setUp(getTestUrl(), "*firefox");
	}

	/**
	 * Runs the test again in embedded mode. It will capture the screenshots and will not recurse further
	 * 
	 * @return The directory where the test stored the images
	 */
	private String recurseTest() throws Exception {
		SnowyCatTestCase sctc = getClass().newInstance();

		String recursedSetName = String.valueOf(currentTempCounter++);
		sctc.setSetName(recursedSetName);
		sctc.setNoRecursive(true);
		sctc.setNoAdditionalHiding(true);
		sctc.setHidedCoordinates(new HashMap<Integer, List<Integer[]>>(hidedCoordinates));
		sctc.setDeleteTempDir(false);
		sctc.setSavePack(false);

		sctc.setUp();
		sctc.getClass().getMethod(getName()).invoke(sctc);
		sctc.tearDown();

		return recursedSetName;
	}

	/**
	 * Compares 2 image sets and updates the hided coordinate's map if needed.
	 * 
	 * @param setName1
	 *            - The first image set's name
	 * @param setName2
	 *            - The second image set's name
	 * @return Whether the hided coordinate's map is changed
	 */
	private boolean compareRecursedTests(String setName1, String setName2) throws IOException {
		boolean changed = false;
		for (int i = 0; i < pictureNum; i++) {
			BufferedImage master = ImageIO.read(new File(new File(tempDir, setName1), "master-" + i + ".png"));
			BufferedImage pic = ImageIO.read(new File(new File(tempDir, setName2), "master-" + i + ".png"));
			BufferedImage mask = createMask(master, pic);
			int[] hidedPosition = getFirstBlackPixelPosition(mask);
			if (hidedPosition != null) {
				changed = true;
				getHidedCoordinates(i).add(new Integer[] { hidedPosition[0], hidedPosition[1] });
			}
		}
		return changed;
	}

	@Override
	public void tearDown() throws Exception {
		// Stops the browser
		super.tearDown();
		ss.stop();
		// Recursion takes place, but only if it is enabled and reference pictures are currently captured
		if (noRecursive == false && captureReference == true) {
			String setName1 = setName;
			String setName2 = recurseTest();
			// Runs until checks are passed
			check: while (true) {
				// Checks if 2 sets are the same
				if (compareRecursedTests(setName1, setName2) == false) {
					// If they are the same, some more tests are needed, defined in recurseCaptureNum
					for (int i = 1; i < recurseCaptureNum; i++) {
						setName2 = recurseTest();
						// Checks if they still passes
						if (compareRecursedTests(setName1, setName2) == true) {
							// If not, then the checking cannot end yet
							setName1 = recurseTest();
							setName2 = recurseTest();
							continue check;
						}
					}
					// If enough tests passed, then break
					break;
				} else {
					// If they are different, then another 2 runs needs to be compared
					setName1 = recurseTest();
					setName2 = recurseTest();
				}
			}
			// Create the directory for the reference images
			File picsDir = new File(snowyCatDir, "pics");
			if (picsDir.exists() == false) {
				System.out.println("Creating directory:" + picsDir.getAbsolutePath());
				picsDir.mkdir();
			}
			File referenceSetsDir = new File(picsDir, "referenceSets");
			if (referenceSetsDir.exists() == false) {
				System.out.println("Creating directory:" + referenceSetsDir.getAbsolutePath());
				referenceSetsDir.mkdir();
			}
			File setDir = new File(referenceSetsDir, setName);
			if (setDir.exists() == false) {
				System.out.println("Creating directory:" + setDir.getAbsolutePath());
				setDir.mkdir();
			}
			// Copy the final images
			for (File f : new File(tempDir, setName2).listFiles()) {
				copyImage(f, new File(setDir, f.getName()));
			}
			// Save the hided coordinate's list in XML format next to the images
			// Format:
			// <mask><hidedCoordinates><coordinate x="2" y="3"/></hidedCoordinates></mask>
			for (Entry<Integer, List<Integer[]>> e : hidedCoordinates.entrySet()) {
				Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Node root = d.createElement("mask");
				d.appendChild(root);

				Node hidedCoordinatesNode = d.createElement("hidedCoordinates");
				root.appendChild(hidedCoordinatesNode);

				for (Integer[] coord : e.getValue()) {
					Element hidedCoordinateNode = d.createElement("coordinate");
					hidedCoordinateNode.setAttribute("x", String.valueOf(coord[0]));
					hidedCoordinateNode.setAttribute("y", String.valueOf(coord[1]));
					hidedCoordinatesNode.appendChild(hidedCoordinateNode);
				}

				Source source = new DOMSource(d);

				// Prepare the output file
				File file = new File(setDir, "master-" + e.getKey() + "-mask.xml");
				Result result = new StreamResult(file);

				// Write the DOM document to the file
				Transformer xformer = TransformerFactory.newInstance().newTransformer();
				xformer.setOutputProperty(OutputKeys.INDENT, "yes");
				xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				xformer.transform(source, result);
			}
		}

		// Delete the temp dir if needed
		if (deleteTempDir && captureScreenshots) {
			deleteDirectory(tempDir);
		}
		// If the db and resources were saved, then restore them
		if (savePack) {
			SnowyCat.importPack(driverName, url, userName, password, "temp.tar", resourceDirs);
			SnowyCat.deletePack("temp.tar");
		}
		clearApplicationCache();
	}

	/**
	 * Reads the snowycat.js file
	 * 
	 * @return The contents of the snowycat.js file
	 */
	private String readSnowyCatJs() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/snowycat.js")));
		String line = "";
		StringBuilder sb = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			sb.append(line);
			sb.append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}

	/**
	 * Hides a HTML element that is shown at x,y coordinates on the screen
	 * 
	 * @param x
	 *            - The x coordinate
	 * @param y
	 *            - The y coordinate
	 */
	private void hideHtmlElementOnPosition(int x, int y) throws IOException {
		System.out.println("Hiding HTML element on position:" + x + "," + y);
		String snowyCatJs = readSnowyCatJs();
		snowyCatJs += "selectClosest(selectByZIndex(getElementsAtPosition(" + x + "," + y + "))).style.display='none';";
		selenium.getEval(snowyCatJs);
	}

	/**
	 * Returns the first black pixel (from the top) on the mask image, or null, if none found
	 * 
	 * @param mask
	 *            - The mask image
	 * @return The coordinates of the first black pixel or null if none found
	 */
	private int[] getFirstBlackPixelPosition(BufferedImage mask) {
		for (int y = 0; y < mask.getHeight(); y++) {
			for (int x = 0; x < mask.getWidth(); x++) {
				if (mask.getRGB(x, y) == Color.BLACK.getRGB()) {
					return new int[] { x, y };
				}
			}
		}
		return null;
	}

	/**
	 * Identifies and hides a HTML element that is marked movin in the mask
	 * 
	 * @param mask
	 *            - The mask
	 * @return The coordinates of the element, or null, if none found
	 */
	private int[] hideMovingElement(BufferedImage mask) throws IOException {
		System.out.println("Hiding a moving element");
		int[] elementPos = getFirstBlackPixelPosition(mask);
		System.out.println("First black pixel:" + elementPos);
		if (elementPos == null) {
			return null;
		}
		hideHtmlElementOnPosition(elementPos[0], elementPos[1]);
		return elementPos;
	}

	/**
	 * Creates a screenshot of the current browser window's content
	 * 
	 * @param kwargs
	 *            - Additional args to create the screenshot
	 * @return The screenshot
	 */
	private BufferedImage getScreenshot(String kwargs) throws IOException {
		String fileName = new File(tempDir, String.valueOf(currentTempCounter++) + ".png").getAbsolutePath();
		System.out.println("Creating screenshot to " + fileName);
		selenium.captureEntirePageScreenshot(fileName, kwargs);
		return ImageIO.read(new File(fileName));
	}

	/**
	 * Compares 2 images and creates a mask from them. The mask has black pixels on positions where the 2 images are different, and transparent where they are the same
	 * 
	 * @param master
	 *            - The first image
	 * @param pic
	 *            - The second image
	 * @return The mask
	 */
	private BufferedImage createMask(BufferedImage master, BufferedImage pic) throws IOException {
		// Creates the mask image
		BufferedImage mask = new BufferedImage(master.getWidth(), master.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < mask.getWidth(); i++) {
			for (int j = 0; j < mask.getHeight(); j++) {
				if (master.getRGB(i, j) == pic.getRGB(i, j)) {
					// Transparent where the 2 images are the same
					mask.setRGB(i, j, new Color(0, 0, 0, 0).getRGB());
				} else {
					// Black where the 2 images are different
					mask.setRGB(i, j, Color.BLACK.getRGB());
				}
			}
		}
		// Writes out the mask for debugging
		File maskFile = new File(tempDir, currentTempCounter++ + ".png");
		System.out.println("Saving mask file to " + maskFile.getName());
		ImageIO.write(mask, "png", maskFile);
		return mask;
	}

	/**
	 * Captures reference picture
	 * 
	 * @param setName
	 *            - The directory where the images will be saved
	 * @param kwargs
	 *            - Additional args to capture screenshots
	 */
	private void captureReferencePicture(String setName, String kwargs) throws IOException, InterruptedException {
		System.out.println("Capturing reference picture...");
		// The current number of the image taken
		int num = pictureNum++;
		// Create directory
		File setDir = new File(tempDir, setName);
		if (setDir.exists() == false) {
			System.out.println("Creating directory:" + setDir.getAbsolutePath());
			setDir.mkdir();
		}
		System.out.println("Capturing master image");

		// Hides elements already known as moving
		for (Integer[] hideCoord : getHidedCoordinates(num)) {
			hideHtmlElementOnPosition(hideCoord[0], hideCoord[1]);
		}

		BufferedImage master;
		// Captures multiple images only if needed
		if (noAdditionalHiding == false) {
			check: while (true) {
				// Gets 2 images
				master = getScreenshot(kwargs);
				BufferedImage pic = getScreenshot(kwargs);
				// Creates a mask of them
				BufferedImage mask = createMask(master, pic);
				// Compares them and hides a moving element
				int[] hidedPosition = hideMovingElement(mask);
				if (hidedPosition == null) {
					// If no hided element was found, take some other shots and check them too
					for (int i = 1; i < captureNum; i++) {
						pic = getScreenshot(kwargs);
						// Check the new image
						hidedPosition = hideMovingElement(createMask(master, pic));
						Thread.sleep(waitBetweenScreenshotsTaken);
						if (hidedPosition != null) {
							// If it does not pass the check, then another 2 images are needed and the counter resets
							getHidedCoordinates(num).add(new Integer[] { hidedPosition[0], hidedPosition[1] });
							continue check;
						}
					}
					// If enough same images were taken, then it is considered passed
					break;
				} else {
					// If they are different, then store a moving part's coordinates
					getHidedCoordinates(num).add(new Integer[] { hidedPosition[0], hidedPosition[1] });
				}
			}
		} else {
			// If only 1 image is needed, then take it
			master = getScreenshot(kwargs);
		}
		// Write the master image to the disk
		File masterFile = new File(setDir, "master-" + num + ".png");
		System.out.println("Writing master image to " + masterFile.getAbsolutePath());
		ImageIO.write(master, "png", masterFile);
		System.out.println(hidedCoordinates);
	}

	/**
	 * Captures a screenshot and compares it with a reference one. If they are different, then save it as failed.
	 * 
	 * @param setName
	 *            - The directory's name where the test's result will be stored
	 * @param kwargs
	 *            - Additional args to capture screenshots
	 */
	private void captureAndComparePicture(String setName, String kwargs) throws Exception {
		// The current number of the image taken
		int num = pictureNum++;
		System.out.println("Capturing test picture and comparing with reference set:" + referenceSet);
		// Creating directories
		File picsDir = new File(snowyCatDir, "pics");
		if (picsDir.exists() == false) {
			System.out.println("Creating directory:" + picsDir.getAbsolutePath());
			picsDir.mkdir();
		}
		File testsDir = new File(picsDir, "tests");
		if (testsDir.exists() == false) {
			System.out.println("Creating directory:" + testsDir.getAbsolutePath());
			testsDir.mkdir();
		}
		File setDir = new File(testsDir, setName);
		if (setDir.exists() == false) {
			System.out.println("Creating directory:" + setDir.getAbsolutePath());
			setDir.mkdir();
		}
		File referenceSetsDir = new File(picsDir, "referenceSets");
		if (referenceSetsDir.exists() == false) {
			System.out.println("Creating directory:" + referenceSetsDir.getAbsolutePath());
			referenceSetsDir.mkdir();
		}
		File referenceSetDir = new File(referenceSetsDir, referenceSet);
		if (referenceSetDir.exists() == false) {
			throw new RuntimeException("No reference set found:" + referenceSetDir.getAbsolutePath());
		}

		// Reading the mask file
		File mask = new File(referenceSetDir, "master-" + num + "-mask.xml");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(mask);
		// <mask><hidedCoordinates><coordinate x="2" y="3"/></hidedCoordinates></mask>
		// Hiding the elements specified in the mask file
		NodeList coordinates = ((Element) doc.getElementsByTagName("hidedCoordinates").item(0)).getElementsByTagName("coordinate");
		for (int i = 0; i < coordinates.getLength(); i++) {
			Element coordinate = (Element) coordinates.item(i);
			int x = Integer.parseInt(coordinate.getAttribute("x"));
			int y = Integer.parseInt(coordinate.getAttribute("y"));
			try {
				hideHtmlElementOnPosition(x, y);
			} catch (RuntimeException ite) {
				// If a javascript exception is thrown, then the page structure has been changed.
				System.out.println("Failed to hide HTML element at (" + x + "," + y + "), page structure is most likely modified. Test will most likely fail.");
			}
		}

		// Taking a screenshot
		System.out.println("Capturing screenshot");
		BufferedImage pic = getScreenshot(kwargs);

		// Reading the master image
		System.out.println("Reading master image");
		BufferedImage master = ImageIO.read(new File(referenceSetDir, "master-" + num + ".png"));
		// Diffing and comparing the pic and the master
		System.out.println("Diffing images");
		BufferedImage maskImage = createMask(master, pic);
		if (getFirstBlackPixelPosition(maskImage) != null) {
			// If they are different, then the test is failed
			// The master, the pic and a diff will be saved
			System.out.println("Test failed, writing screenshots");
			ImageIO.write(master, "png", new File(setDir, "test-" + num + "-master.png"));
			ImageIO.write(pic, "png", new File(setDir, "test-" + num + "-pic.png"));
			BufferedImage diff = new BufferedImage(master.getWidth(), master.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D diffGraphics = (Graphics2D) diff.getGraphics();
			diffGraphics.drawImage(master, 0, 0, null);
			diffGraphics.setXORMode(Color.WHITE);
			diffGraphics.drawImage(pic, 0, 0, null);
			ImageIO.write(diff, "png", new File(setDir, "test-" + num + "-diff.png"));
		}
		// If they are the same, the test passed
	}

	/**
	 * Captures a screenshot with a default kwargs
	 */
	public void captureEntirePageScreenshotToString() throws Exception {
		captureEntirePageScreenshotToString("background=#FFFFFF");
	}

	/**
	 * Captures a screenshot
	 * 
	 * @param kwargs
	 *            - Additional args to capture screenshots
	 */
	public void captureEntirePageScreenshotToString(String kwargs) throws Exception {
		if (captureScreenshots) {
			if (captureReference) {
				captureReferencePicture(setName, kwargs);
			} else {
				captureAndComparePicture(setName, kwargs);
			}
		}
	}

	/**
	 * Deletes a directory recursively
	 * 
	 * @param path
	 *            - The directory to be deleted
	 * @return If the directory is deleted successfully
	 */
	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	/**
	 * Copies an image to another location
	 * 
	 * @param from
	 *            - The place where the image currently is
	 * @param to
	 *            - The destination
	 */
	public static void copyImage(File from, File to) throws IOException {
		ImageIO.write(ImageIO.read(from), "png", to);
	}

	/** Clears the tested application's cache */
	public abstract void clearApplicationCache();

	/**
	 * Returns the filename of the pack which will be used as the origin
	 * 
	 * @return The filename of the pack export
	 */
	public abstract String getUsedPackName();

	/**
	 * Returns the test URL
	 * 
	 * @return The test URL
	 */
	public abstract String getTestUrl();

	@Override
	public void runBare() throws Throwable {
		Throwable exception = null;
		setUp();
		try {
			runTest();
		} catch (Throwable running) {
			exception = running;
		} finally {
			try {
				tearDown();
			} catch (Throwable tearingDown) {
				if (exception == null) exception = tearingDown;
			}
		}
		if (exception != null) {
			// If something went wrong, then delete the lasttestname.txt
			new File("snowycat", "lasttestname.txt").delete();
			throw exception;
		} else {
			// If everything went well, then the lasttestname.txt will hold this test's nams
			File lastTestFile = new File("snowycat", "lasttestname.txt");
			System.out.println("File:" + lastTestFile.getAbsolutePath());
			FileWriter fw = new FileWriter(lastTestFile);
			fw.write(getClass().getName() + "." + getName());
			fw.close();
		}
	}

	/**
	 * Returns the last run test's name if it is completes successfully
	 * 
	 * @return The name of the test
	 */
	private String getLastRunTest() throws IOException {
		File lastTestFile = new File("snowycat", "lasttestname.txt");
		if (lastTestFile.exists()) {
			BufferedReader reader = null;
			try {
				return new BufferedReader(new FileReader(lastTestFile)).readLine();
			} finally {
				if (reader != null) {
					reader.close();
				}
			}
		}
		return null;
	}

	/**
	 * Clicks on the first element returned by the XPath argument. It uses the browser's native Javascript to evaluate, so it handles parenthesis well unlike the current Selenium.
	 * 
	 * @param locator
	 *            - The locator of the element
	 */
	public void click(String locator) throws Exception {
		String snowyCatJs = readSnowyCatJs();
		snowyCatJs += "getElementXPath(window.document.evaluate( \"" + locator + "\", window.document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null ).singleNodeValue);";
		String elementXPath = selenium.getEval(snowyCatJs);
		if(elementXPath.compareTo("")==0){
			throw new Exception("ELement for locator["+locator+"] could not be found");
		}
		selenium.click("/" + elementXPath);
	}

	/**
	 * Types to the first element returned by the XPath argument. It uses the browser's native Javascript to evaluate, so it handles parenthesis well unlike the current Selenium.
	 * 
	 * @param locator
	 *            - The locator of the element
	 * @param text
	 *            - The text to type to the element
	 */
	public void type(String locator, String text) throws Exception {
		String snowyCatJs = readSnowyCatJs();
		snowyCatJs += "getElementXPath(window.document.evaluate( \"" + locator + "\", window.document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null ).singleNodeValue);";
		String elementXPath = selenium.getEval(snowyCatJs);
		if(elementXPath.compareTo("")==0){
			throw new Exception("ELement for locator["+locator+"] could not be found");
		}
		selenium.type("/" + elementXPath, text);
	}

	public void setCaptureNum(Integer captureNum) {
		this.captureNum = captureNum;
	}

	public void setReferenceSet(String referenceSet) {
		this.referenceSet = referenceSet;
	}

	public void setCaptureReference(Boolean captureReference) {
		this.captureReference = captureReference;
	}

	public void setSetName(String setName) {
		this.setName = setName;
	}

	public void setNoRecursive(Boolean noRecursive) {
		this.noRecursive = noRecursive;
	}

	public void setNoAdditionalHiding(Boolean noAdditionalHiding) {
		this.noAdditionalHiding = noAdditionalHiding;
	}

	public List<Integer[]> getHidedCoordinates(Integer forTest) {
		if (hidedCoordinates.containsKey(forTest) == false) {
			hidedCoordinates.put(forTest, new ArrayList<Integer[]>());
		}
		return hidedCoordinates.get(forTest);
	}

	public void setHidedCoordinates(Map<Integer, List<Integer[]>> hidedCoordinates) {
		this.hidedCoordinates = hidedCoordinates;
	}

	public void setDeleteTempDir(Boolean deleteTempOnStart) {
		this.deleteTempDir = deleteTempOnStart;
	}

	public void setSavePack(Boolean savePack) {
		this.savePack = savePack;
	}
}
