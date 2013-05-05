package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

public class Utilities {

	static Set<String> stopWords = null;

	/**
	 * @author sujal
	 * @param v
	 * @return
	 */
	public static HashMap<String, Double> getTermFreq(Vector<String> v) {
		HashMap<String, Double> termFreq = new HashMap<String, Double>();

		for (String term : v) {
			Double freq = termFreq.get(term);
			if (freq == null) {
				termFreq.put(term, 1d);
			} else {
				termFreq.put(term, freq + 1d);
			}
		}

		return termFreq;
	}

	/**
	 * @author sujal
	 * @param vec1
	 * @param vec2
	 * @return dot product of given vectors. Returns -1 if either of the given
	 *         vectors is null
	 */
	public static double getDotProduct(Map<String, Double> vec1,
			Map<String, Double> vec2) {

		double dotProduct = 0;
		Map<String, Double> v1 = null, v2 = null;

		if (vec1 == null || vec2 == null) {
			System.out.println("Error: One of the vectors is null");
			return -1;
		}

		// v1 holds the smaller vector and v2 holds the larger one
		if (vec1.keySet().size() < vec2.keySet().size()) {
			v1 = vec1;
			v2 = vec2;
		} else {
			v1 = vec2;
			v2 = vec1;
		}

		for (String k : v1.keySet()) {
			Double tmp = v2.get(k);
			if (tmp == null) {
				tmp = 0.0;
			}

			dotProduct += v1.get(k) * tmp;
		}

		return dotProduct;
	}

	/**
	 * @author sujal
	 * @param vec1
	 * @param vec2
	 * @return dot product of given vectors. Returns -1 if either of the given
	 *         vectors is null
	 */
	public static double getDotProductInteger(HashMap<String, Integer> vec1,
			HashMap<String, Integer> vec2) {

		double dotProduct = 0;
		HashMap<String, Integer> v1 = null, v2 = null;

		if (vec1 == null || vec2 == null) {
			System.out.println("Error: One of the vectors is null");
			return -1;
		}

		// v1 holds the smaller vector and v2 holds the larger one
		if (vec1.keySet().size() < vec2.keySet().size()) {
			v1 = vec1;
			v2 = vec2;
		} else {
			v1 = vec2;
			v2 = vec1;
		}

		for (String k : v1.keySet()) {
			Integer tmp = v2.get(k);
			if (tmp == null) {
				tmp = 0;
			}

			dotProduct += v1.get(k) * tmp;
		}

		return dotProduct;
	}

	/**
	 * @author sujal
	 * @param vec
	 * @return null if given vector is null. Else returns the unit vector of a
	 *         given vector.
	 */
	public static <T, U extends Number> Map<T, Double> getNormalizedVector(
			Map<T, U> vec, double norm) {

		if (vec == null) {
			return null;
		}

		Map<T, Double> unitVec = new HashMap<T, Double>();
		double vecNorm = getVectorNorm(vec, norm); // get 2-norm of the vector

		for (T k : vec.keySet()) {
			unitVec.put(k, (vec.get(k).doubleValue() / vecNorm));
		}

		return unitVec;
	}

	/**
	 * @author sujal
	 * @param list
	 * @param norm
	 * @return
	 */
	public static List<Pair<String, Double>> getNormalizedVector(
			List<Pair<String, Double>> list, double norm) {

		if (list == null) {
			return null;
		}

		List<Pair<String, Double>> unitVec = new ArrayList<Pair<String, Double>>();
		double vecNorm = getVectorNorm(list, norm);
		for (Pair<String, Double> pair : list) {
			unitVec.add(new Pair<String, Double>(pair.getFirstElement(), pair
					.getSecondElement() / vecNorm));
		}

		return unitVec;
	}

	/**
	 * @author sujal
	 * @param vec
	 * @param p
	 *            required norm of a vector. p != 0 (does not handle for p ==
	 *            infinity)
	 * @return p-norm of the given vector
	 */
	public static <T, U extends Number> double getVectorNorm(Map<T, U> vec,
			Double p) {
		double norm = 0;

		if (p == 0 || vec == null) {
			return -1;
		}

		for (T k : vec.keySet()) {
			norm += Math.pow(vec.get(k).doubleValue(), p);
		}
		norm = Math.pow(norm, 1d / p);

		return norm;
	}

	/**
	 * @author sujal
	 * @param vec
	 * @param p
	 * @return
	 */
	public static double getVectorNorm(List<Pair<String, Double>> list, Double p) {
		double norm = 0;

		if (p == 0 || list == null) {
			return -1;
		}

		for (Pair<String, Double> pair : list) {
			norm += Math.pow(pair.getSecondElement(), p);
		}

		norm = Math.pow(norm, 1d / p);

		return norm;
	}

	/**
	 * @author sujal
	 * @param fileName
	 * @param contents
	 * @param append
	 */
	public static void writeToFile(String fileName, String contents,
			boolean append) {
		try {
			FileWriter fstream = new FileWriter(fileName, append);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(contents);
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @author sujal
	 * @param sdv
	 *            scored document vector
	 * @param query_map
	 */
	public static String generateOutput(Vector<ScoredDocument> sdv,
			Map<String, String> query_map, String sessionId) {

		String queryResponse = "";
		Output output = new Output(sdv, query_map);
		queryResponse = output.generateTextOutput(sessionId);

		return queryResponse;
	}

	/**
	 * @author sujal
	 * @param vec
	 * @param n
	 * @return vector with n-gram terms from given vector vec
	 */
	public static Vector<String> getNGram(Vector<String> vec, int n) {
		Vector<String> ngramVector = new Vector<String>();
		int vecLen = vec.size();

		if (n <= 0) {
			return null;
		}

		if (n > vecLen) {
			String phrase = vec.get(0);
			for (int i = 1; i < vecLen; i++) {
				phrase += " " + vec.get(i);
			}
			ngramVector.add(phrase);
		} else {
			for (int i = 0; i < vec.size() - n + 1; i++) {
				String phrase = "";

				phrase = vec.get(i);
				for (int j = 1; j < n; j++) {
					phrase += " " + vec.get(i + j);
				}
				ngramVector.add(phrase);
			}
		}

		return ngramVector;
	}

	/**
	 * @author sujal
	 * @param recallVec
	 * @param precisionVec
	 * @param recall
	 * @return precision at given recall
	 */
	public static double getPrecisionAtRecall(Vector<Double> recallVec,
			Vector<Double> precisionVec, double recall) {
		double precisionAtRecall = 0d;

		if (recallVec == null || precisionVec == null || recall < 0
				|| recall > 1) {
			return 0;
		}

		if (recallVec.size() != precisionVec.size()) {
			return 0;
		}

		// also need to check for valid values in recallVec and precisionVec

		int vecSize = precisionVec.size();
		Vector<Double> steppedPrecision = new Vector<Double>();
		Vector<Double> tmpPrecision = new Vector<Double>();

		// pre-process the precision array
		int i = 0;
		while (i < vecSize) {

			double currPrecision = precisionVec.get(i);
			double currRecall = recallVec.get(i);

			while (i < vecSize && recallVec.get(i) == currRecall) {
				tmpPrecision.add(currPrecision);
				i++;
			}
		}

		// process precision vector and convert it into a step functioned vector
		double maxPrecision = 0;
		for (i = vecSize - 1; i >= 0; i--) {
			if (tmpPrecision.get(i) > maxPrecision) {
				maxPrecision = tmpPrecision.get(i);
			}

			steppedPrecision.add(0, maxPrecision);
		}

		// search for precision at given recall
		for (i = 0; i < vecSize; i++) {
			if (recall <= recallVec.get(i)) {
				precisionAtRecall = steppedPrecision.get(i);
				break;
			}
		}

		return precisionAtRecall;
	}

	/**
	 * @author Amey
	 * @param sds
	 * @return
	 */
	public static Vector<ScoredDocument> sortScoredDocumentAsPer(
			Vector<ScoredDocument> sds) {
		if (sds.size() > 0) {
			Collections.sort(sds, new Comparator<ScoredDocument>() {
				@Override
				public int compare(final ScoredDocument obj1,
						final ScoredDocument obj2) {
					return obj2.compareTo(obj1);
				}
			});
		}
		return sds;
	}

	public static Map<String, Double> getTfIdf(Map<String, Double> vec1,
			Map<String, Double> vec2) {

		Map<String, Double> tfIdf = new HashMap<String, Double>();

		for (String term : vec1.keySet()) {
			tfIdf.put(term, vec1.get(term) * vec2.get(term));
		}

		return tfIdf;
	}

	/**
	 * @author sujal
	 * 
	 * @param contents
	 * @return
	 */
	public static Vector<String> getStemmed(String contents) {

		if (contents == null) {
			return null;
		}

		Vector<String> stemmedContents = new Vector<String>();

		Scanner s = new Scanner(contents.toLowerCase());
		s.useDelimiter("[^a-zA-Z0-9]");
		while (s.hasNext()) {
			String term = s.next();

			// System.out.println("Term = "+term);
			Stemmer stemmer = new Stemmer();
			stemmer.add(term.toCharArray(), term.length());
			stemmer.stem(); // code of stemmer is modified to compute just
							// step1()

			stemmedContents.add(stemmer.toString());
		}
		s.close();

		return stemmedContents;
	}

	public static void deleteFilesInDir(String dir) {
		File directory = new File(dir);
		if (!directory.exists()) {
			return;
		}

		File[] files = directory.listFiles();
		for (File f : files) {
			System.out.println("deleting : " + f.getName());
			f.delete();
		}
	}

	public static void deleteFilesInDir(String dir, String ext) {
		File directory = new File(dir);
		if (!directory.exists() || !directory.isDirectory()) {
			return;
		}

		FilenameFilter ff = createFileNameFilter(ext);
		File[] files = directory.listFiles(ff);
		for (File f : files) {
			System.out.println("deleting : " + f.getName());
			f.delete();
		}
	}

	/**
	 * 
	 * @param list
	 * @return maximum integer from the given list. Returns -1 if list is null
	 *         or empty. Should be used only for non-negative numbers.
	 */
	public static int getMax(int[] list) {

		if (list == null || list.length == 0) {
			return -1;
		}

		int max = list[0];

		for (int i : list) {
			max = Math.max(max, i);
		}

		return max;
	}

	public static int getMin(int[] list) {

		if (list == null || list.length == 0) {
			return -1;
		}

		int min = list[0];

		for (int i : list) {
			min = Math.min(min, i);
		}

		return min;
	}

	public static FilenameFilter createFileNameFilter(final String ext) {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File directory, String fileName) {
				return fileName.endsWith(ext);
			}
		};

		return filter;
	}

	public static <T, U extends Number> void sort(List<Pair<T, U>> info,
			final Map<Integer, String> docIdUriMapping, final boolean descending) {
		Collections.sort(info, new Comparator<Pair<T, U>>() {

			@Override
			public int compare(Pair<T, U> o1, Pair<T, U> o2) {
				if (descending) {
					if (o1.getSecondElement().equals(o2.getSecondElement())) {

						// if values are same, compare their uri as the tie
						// breaker
						return docIdUriMapping.get(o2.getFirstElement())
								.compareTo(
										docIdUriMapping.get(o1
												.getFirstElement()));
					} else {
						return o2.compareTo(o1);
					}
				} else {
					if (o1.getSecondElement().equals(o2.getSecondElement())) {
						// if values are same, compare their uri as the tie
						// breaker
						return docIdUriMapping.get(o1.getFirstElement())
								.compareTo(
										docIdUriMapping.get(o2
												.getFirstElement()));
					} else {
						return o1.compareTo(o2);
					}
				}
			}
		});
	}

	/**
	 * @author sujal
	 * @param dirPath
	 * @return
	 */
	public static boolean deleteDir(String dirPath) {

		boolean status = true;

		// Check for null or empty input
		if (dirPath == null || dirPath.trim().length() == 0) {
			return false;
		}

		File dir = new File(dirPath);
		if(! dir.exists()) {
			return true;
		}
		
		if (!dir.isDirectory()) {
			System.out.println("Error: " + dirPath + " is not a directory");
			return false;
		}

		// delete all files in the directory recursively
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				status = status && deleteDir(file.getAbsolutePath());
			} else {
				status = status && file.delete();
			}
		}

		// Now the directory is empty. So delete directory
		status = status && dir.delete();

		return status;
	}

	/**
	 * @author sujal
	 * @param word
	 * @return
	 */
	public static boolean isStopWord(String word) {

		if (stopWords == null) {
			loadStopWords();
		}

		return stopWords.contains(getStemmed(word).get(0));
	}

	/**
	 * @author sujal
	 */
	public static void loadStopWords() {

		// Ref: http://www.ranks.nl/resources/stopwords.html

		String stopWordsFile = "data/index/stopwords";
		String stopWordStr = readFile(stopWordsFile);

		stopWords = new HashSet<String>();
		stopWords.addAll(getStemmed(stopWordStr));
	}

	/**
	 * @author sujal
	 * @param file
	 * @return
	 */
	public static String readFile(String file) {
		StringBuilder sBuilder = new StringBuilder();

		if (file == null || file.trim().length() == 0) {
			return null;
		}

		try {
			BufferedReader br = new BufferedReader(new FileReader(
					new File(file)));
			String line = "";
			while ((line = br.readLine()) != null) {
				sBuilder.append(line);
				sBuilder.append("\n");
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sBuilder.toString();
	}

	public static HashMap<Integer, Integer> sortByComparator(
			Map<Integer, Integer> unsortMap) {

		List<Map.Entry<Integer, Integer>> list = new LinkedList<Map.Entry<Integer, Integer>>(
				unsortMap.entrySet());

		// sort list based on comparator
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o2)).getValue())
						.compareTo(((Map.Entry) (o1)).getValue());
			}
		});

		// put sorted list into map again
		// LinkedHashMap make sure order in which keys were inserted
		HashMap sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

}
