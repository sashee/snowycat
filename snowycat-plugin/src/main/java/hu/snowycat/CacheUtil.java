package hu.snowycat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides static methods to access and use the cache for db transitions
 * 
 * @author sashee
 */
public class CacheUtil {
	/**
	 * Finds and returns the appropriate Element in the cache xml, or null, if no cache entry is found.
	 * 
	 * @param doc
	 *            - The cache's xml document
	 * @param from
	 *            - The current db state
	 * @param to
	 *            - The desired state
	 * @return The transition Element in the xml if it is present for the transition
	 */
	public static Element findTransitionElementInCache(Document doc, String from, String to) throws Exception {
		// <cache><transition from="" to=""><skiptable name=""/>..</transition></cache>
		NodeList transitions = doc.getElementsByTagName("transition");
		for (int i = 0; i < transitions.getLength(); i++) {
			Element transition = (Element) transitions.item(i);
			if (transition.getAttribute("from").compareTo(from) == 0 && transition.getAttribute("to").compareTo(to) == 0) {
				return transition;
			}
		}
		return null;
	}

	/**
	 * Returns the list of tables that are in the skiptables cache for the given transition
	 * 
	 * @param from
	 *            - The current db state
	 * @param to
	 *            - The target db state
	 * @return The list of table's name that are in the skiptables cache
	 */
	public static List<String> getSkiptableCacheForTransition(String from, String to) throws Exception {
		if (from == null) {
			return new ArrayList<String>();
		}
		File cacheFile = new File("snowycat", "dbcache.xml");
		if (cacheFile.exists() == false) {
			return new ArrayList<String>();
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(cacheFile);
		// <cache><transition from="" to=""><skiptable name=""/>..</transition></cache>
		Element transition = findTransitionElementInCache(doc, from, to);
		if (transition != null) {
			List<String> result = new ArrayList<String>();
			NodeList skipTables = transition.getElementsByTagName("skiptable");
			for (int j = 0; j < skipTables.getLength(); j++) {
				result.add(((Element) skipTables.item(j)).getAttribute("name"));
			}
			return result;
		}
		return new ArrayList<String>();
	}

	/**
	 * Saves the skiptables cache for the given transition.
	 * 
	 * @param from
	 *            - The origin of the transition
	 * @param to
	 *            - The destination of the transition
	 * @param skipTables
	 *            - The tables to be skipped next time
	 */
	public static void saveSkiptablesCacheForTransition(String from, String to, List<String> skipTables) throws Exception {
		File cacheFile = new File("snowycat", "dbcache.xml");
		Document doc;
		Node root;
		if (cacheFile.exists() == false) {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			root = doc.createElement("cache");
			doc.appendChild(root);
		} else {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(cacheFile);
			root = doc.getElementsByTagName("cache").item(0);
		}
		// <cache><transition from="" to=""><skiptable name=""/>..</transition></cache>
		Element transition = findTransitionElementInCache(doc, from, to);
		if (transition != null) {
			transition.getParentNode().removeChild(transition);
		}

		Element newTransition = doc.createElement("transition");
		newTransition.setAttribute("from", from);
		newTransition.setAttribute("to", to);
		for (String s : skipTables) {
			Element skipTableElement = doc.createElement("skiptable");
			skipTableElement.setAttribute("name", s);
			newTransition.appendChild(skipTableElement);
		}
		root.appendChild(newTransition);

		// Remove text nodes from <cache>
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.TEXT_NODE) {
				root.removeChild(children.item(i));
			}
		}

		Source source = new DOMSource(doc);

		// Prepare the output file
		Result result = new StreamResult(cacheFile);

		// Write the DOM document to the file
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		xformer.transform(source, result);
	}
}
