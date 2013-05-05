package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {

	// Loads the doc info file
	static String _docInfoFile = "docinfo.inf";
	// Given Log file to obtain Num Views
	static String _logFile = "20130301-160000.log";
	// Stores the number of views of all documents
	static String _numViews = "numviews.tsv";
	// Contains all URI's present in the corpus.
	static Map<String, Integer> docNameAndId = new HashMap<String, Integer>();

	final String _docInfoDelim = ";";
	int _numberOfDocs = 0;

	/*
	 * public static void main(String args[]) { new LogMinerNumviews(); }
	 * 
	 * public LogMinerNumviews() { try { Options options = new
	 * Options("conf/engine.conf"); LogMinerNumviews capr = new
	 * LogMinerNumviews(options); long start = System.currentTimeMillis();
	 * capr.compute(); long end = System.currentTimeMillis();
	 * System.out.println("time = " + (end - start));
	 * 
	 * } catch (IOException e) { // TODO Auto-generated e.printStackTrace(); } }
	 */

	public LogMinerNumviews(Options options) {
		super(options);
		_logFile = _options._logPrefix + "/" + _logFile;
		_numViews = _options._indexPrefix + "/" + _numViews;
		_docInfoFile = _options._indexPrefix + "/" + _docInfoFile;
	}

	/**
	 * This function processes the logs within the log directory as specified by
	 * the {@link _options}. The logs are obtained from Wikipedia dumps and have
	 * the following format per line: [language]<space>[article]<space>[#views].
	 * Those view information are to be extracted for documents in our corpus
	 * and stored somewhere to be used during indexing.
	 * 
	 * Note that the log contains view information for all articles in Wikipedia
	 * and it is necessary to locate the information about articles within our
	 * corpus.
	 * 
	 * @throws IOException
	 */
	@Override
	public void compute() throws IOException {
		System.out.println("Computing using " + this.getClass().getName());
		File f = new File(_numViews);
		if (f.exists()) {
			f.delete();
		}
		// Loading Doc Info file
		loadDocNameIdMap();
		// Creating Num Views File
		File logF = new File(_logFile);
		createAndWriteNumViews(logF);
		return;
	}

	/**
	 * This function obtains number of views from the given log file for each
	 * document present in the corpus and writes num views to an external file.
	 * For documents whose info is not present in the log file, this function
	 * puts 0 for those documents.
	 * 
	 * @param logFile
	 * @throws IOException
	 */
	public void createAndWriteNumViews(File logFile) throws IOException {
		Map<Integer, Integer> numViewsMap = new HashMap<Integer, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(logFile));
		String line = "";
		String contents[];
		String currentURI;
		StringBuffer tempBuffer;
		while ((line = br.readLine()) != null) {
			contents = line.split(" ");
			currentURI = contents[1];
			tempBuffer = new StringBuffer();
			int i = 0;
			while (i < currentURI.length()) {
				char charecterAt = currentURI.charAt(i);
				if (charecterAt == '%') {
					tempBuffer.append("<percentage>");
				} else if (charecterAt == '+') {
					tempBuffer.append("<plus>");
				} else {
					tempBuffer.append(charecterAt);
				}
				i++;
			}
			currentURI = tempBuffer.toString();
			currentURI = URLDecoder.decode(currentURI, "ISO-8859-1");
			currentURI = currentURI.replaceAll("<percentage>", "%");
			currentURI = currentURI.replaceAll("<plus>", "+");
			if (docNameAndId.containsKey(currentURI)) {
				try {
					numViewsMap.put(docNameAndId.get(currentURI),
							Integer.parseInt(contents[2]));
				} catch (Exception e) {
					System.out.println("Exception caught in line=" + line);
					numViewsMap.put(docNameAndId.get(currentURI), 0);
				}
			}
		}
		// This loop checks and inserts 0 for those documents whose info is not
		// present in given log file.
		for (int i = 0; i < _numberOfDocs; i++) {
			if (!numViewsMap.containsKey(i)) {
				numViewsMap.put(i, 0);
			}
		}
		// Write the num views map to an external file.
		writeNumViewsToFile(numViewsMap);
	}

	/**
	 * Writes the num Views Map to an external file. File is always appended.
	 */
	public void writeNumViewsToFile(Map<Integer, Integer> numViewsMap) {
		StringBuffer info;
		Iterator<Entry<Integer, Integer>> it = numViewsMap.entrySet()
				.iterator();
		while (it.hasNext()) {
			info = new StringBuffer();
			Map.Entry<Integer, Integer> pairs = (Map.Entry<Integer, Integer>) it
					.next();
			info.append(pairs.getKey());
			info.append("\t");
			info.append(pairs.getValue());
			info.append("\n");
			Utilities.writeToFile(_numViews, info.toString(), true);
		}
	}

	public void loadDocNameIdMap() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(_docInfoFile));
		String line = "";
		String contents[];
		while ((line = br.readLine()) != null) {
			contents = line.split(_docInfoDelim);
			docNameAndId.put(contents[1], Integer.parseInt(contents[0]));
		}
		_numberOfDocs = docNameAndId.size();
	}

	/**
	 * During indexing mode, this function loads the NumViews values computed
	 * during mining mode to be used by the indexer.
	 * 
	 * @throws IOException
	 */
	@Override
	public Object load() throws IOException {
		System.out.println("Loading using " + this.getClass().getName());
		Map<Integer, Integer> loadNumviews = new HashMap<Integer, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(_numViews));
		String line = "";
		String contents[];
		while ((line = br.readLine()) != null) {
			contents = line.split("\t");
			loadNumviews.put(Integer.parseInt(contents[0]),
					Integer.parseInt(contents[1]));
		}
		return loadNumviews;
	}
}