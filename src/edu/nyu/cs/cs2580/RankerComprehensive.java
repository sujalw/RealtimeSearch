package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @author sujal
 * @CS2580: Implement this class for HW3 based on your {@code RankerFavorite}
 *          from HW2. The new Ranker should now combine both term features and
 *          the document-level features including the PageRank and the NumViews.
 */
public class RankerComprehensive extends Ranker {

	final private double lambda = 0.5;

	/*
	 * Used when processing the index files for expanding query
	 */
	final String _termDoclistDelim = ";";
	final String _docCountDelim = ":";
	final String _doclistDelim = " ";
	final String _termCntDelim = ":";

	String _numViewsScoreFile = "numviews.tsv";
	String _pagerankScoreFile = "pagerank.tsv";

	Map<Integer, Integer> numViewsScore = null;
	Map<Integer, Double> pageRankScore = null;
	Map<Integer, Double> topicRelevanceScore = null;

	Map<Integer, Double> topicRelevanceScoreNormalized = null;
	Map<Integer, Double> numViewsScoreNormalized = null;
	Map<Integer, Double> pageRankScoreNormalized = null;
	
	Map<Integer, String> docidTitleMap = null;

	public RankerComprehensive(Options options, CgiArguments arguments,
			Indexer indexer) {
		super(options, arguments, indexer);
		System.out.println("Using Ranker: " + this.getClass().getSimpleName());

		// add dir paths to the file names
		_numViewsScoreFile = options._indexPrefix + "/" + _numViewsScoreFile;
		_pagerankScoreFile = options._indexPrefix + "/" + _pagerankScoreFile;

		numViewsScore = getNumViewsInfo();
		pageRankScore = getPageRankInfo();

		// normalize the info
		numViewsScoreNormalized = Utilities.getNormalizedVector(numViewsScore, 1);
		pageRankScoreNormalized = Utilities.getNormalizedVector(pageRankScore, 1);
	}

	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
		PriorityQueue<ScoredDocument> retrieval_results = new PriorityQueue<ScoredDocument>();
		// query.processQuery();

		ScoredDocument scoredDoc = null;

		// combine page rank as well as numviews score
		float topicRelevanceWeight = 0.4f;
		float pageRankWeight = 0.4f;
		float numViewsWeight = 0.2f;
		double prScore = 0;
		int nvScore = 0;

		Document doc = null;
		double combinedScore = 0;
		
		topicRelevanceScore = new HashMap<Integer, Double>();
		docidTitleMap = new HashMap<Integer, String>();

		DocumentIndexed di = (DocumentIndexed) _indexer.nextDoc(query, -1);
		while (di != null) {
			scoredDoc = scoreDocument(query, di);			
			topicRelevanceScore.put(scoredDoc.getDocId(), scoredDoc.get_score());			
			docidTitleMap.put(scoredDoc.getDocId(), scoredDoc.getTitle());
			
			di = (DocumentIndexed) _indexer.nextDoc(query, di._docid);
		}
		
		// normalize
		topicRelevanceScoreNormalized = Utilities.getNormalizedVector(topicRelevanceScore, 1);
		
		/*
		 * Combine all scores and take the desired number of top results
		 */
		for(Integer docid : topicRelevanceScore.keySet()) {
			double trScoreNormalized = topicRelevanceScoreNormalized.get(docid);
			double prScoreNormalized = pageRankScoreNormalized.get(docid);
			double nvScoreNormalized = numViewsScoreNormalized.get(docid);
			
			prScore = pageRankScore.get(docid);
			nvScore = numViewsScore.get(docid);
			
			combinedScore = trScoreNormalized * topicRelevanceWeight
							+ prScoreNormalized * pageRankWeight
							+ nvScoreNormalized * numViewsWeight;
			
			
			
			doc = new Document(docid);
			doc.setNumViews(nvScore);			
			doc.setPageRank((float) prScore);
			doc.setTitle(docidTitleMap.get(docid));
			
			retrieval_results.add(new ScoredDocument(doc, combinedScore));
		}
		
		/*

			pageRank = pageRankScore.get(scoredDoc.getDocId());
			numViews = numViewsScore.get(scoredDoc.getDocId());

			// combine page rank as well as numviews score
			combinedScore = topicRelevanceWeight * scoredDoc.get_score()
					+ pageRankWeight
					* pageRankScoreNormalized.get(scoredDoc.getDocId())
					+ numViewsWeight
					* numViewsScoreNormalized.get(scoredDoc.getDocId());

			doc = new Document(scoredDoc.getDocId());
			doc.setNumViews(numViews);
			doc.setPageRank((float) pageRank);

			retrieval_results.add(new ScoredDocument(doc, combinedScore));
			di = (DocumentIndexed) _indexer.nextDoc(query, di._docid);
		}*/

		// return only top numResults elements
		Vector<ScoredDocument> sortedResults = new Vector<ScoredDocument>();
		for (int i = 0; i < numResults && retrieval_results.peek() != null; i++) {
			sortedResults.add(retrieval_results.poll());
		}

		return sortedResults;
	}

	public List<Pair<String, Double>> runQuery(Query query, int numResults,
			int numDocs, int numTerms) {
		PriorityQueue<ScoredDocument> retrieval_results = new PriorityQueue<ScoredDocument>();
		// query.processQuery();

		DocumentIndexed di = (DocumentIndexed) _indexer.nextDoc(query, -1);
		while (di != null) {
			retrieval_results.add(scoreDocument(query, di));
			di = (DocumentIndexed) _indexer.nextDoc(query, di._docid);
		}

		// get top 'numDocs' documents to get expanded query
		Vector<ScoredDocument> topResults = new Vector<ScoredDocument>();
		for (int i = 0; i < numDocs && retrieval_results.peek() != null; i++) {
			topResults.add(retrieval_results.poll());
		}

		System.out.println("top docs = ");
		for (ScoredDocument sd : topResults) {
			System.out.println(sd.getDocId());
		}
		System.out.println("===============");

		List<Pair<String, Double>> expandedQuery = getExpandedQuery(topResults,
				numTerms);

		// Write this info to a file with filename=<query>.exp
		String prfDirName = "data/prf_output";
		File prfDir = new File(prfDirName);
		if (prfDir.exists()) {
			prfDir.delete();
		}

		prfDir.mkdir();

		String expandedQueryFileName = prfDirName + "/" + query._query + ".tsv";
		StringBuilder sBuilder = new StringBuilder();
		for (Pair<String, Double> pair : expandedQuery) {
			sBuilder.append(pair.getFirstElement());
			sBuilder.append("\t");
			sBuilder.append(String.valueOf(pair.getSecondElement()));
			sBuilder.append("\n");
		}
		Utilities
				.writeToFile(expandedQueryFileName, sBuilder.toString(), false);

		return expandedQuery;
	}

	public ScoredDocument scoreDocument(Query query, DocumentIndexed doc) {
		// TODO: adjust term frequency and total word count for doc due to
		// phrases

		double score = getLMPScore(query, doc);

		// TODO: restore original term frequencies and total word count

		return new ScoredDocument(doc, score);
	}

	public double getLMPScore(Query query, Document d) {
		return runquery(query, d._docid).get_score();
	}

	public ScoredDocument runquery(Query query, int docid) {

		// query.processQuery();

		// Build query vector
		// Vector<String> qv = Utilities.getStemmed(query._query);
		Vector<String> qv = new Vector<String>();
		for (String term : query._tokens) {
			qv.add(term);
		}

		DocumentIndexed dIndexed = (DocumentIndexed) _indexer.getDoc(docid);

		double score = 0.0;
		for (int i = 0; i < qv.size(); ++i) {
			score += Math.log((1 - lambda)
					* (getQueryLikelihood(qv.get(i), docid))
					+ (lambda)
					* (_indexer.corpusTermFrequency(qv.get(i)) / _indexer
							.totalTermFrequency()));
		}

		// antilog
		score = Math.pow(Math.E, score);

		return new ScoredDocument(dIndexed, score);
	}

	public double getQueryLikelihood(String term, int docid) {

		DocumentIndexed dIndexed;
		int termFreqInDoc = 0;
		long totalWordsInDoc = 0;

		dIndexed = (DocumentIndexed) _indexer.getDoc(docid);

		// TODO: termFreqInDoc and totalWordsInDoc should change because of
		// presence of phrases
		termFreqInDoc = _indexer.documentTermFrequency(term, dIndexed.getUrl());
		totalWordsInDoc = dIndexed.getTotalWords();

		double ql = 0d;

		if (totalWordsInDoc > 0) {
			ql = termFreqInDoc * 1.0d / totalWordsInDoc;
		}

		return ql;
	}

	private Map<Integer, Double> getPageRankInfo() {
		Map<Integer, Double> info = new HashMap<Integer, Double>();

		String line = "";
		String delim = "\t";
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(
					_pagerankScoreFile)));
			String[] pageRankInfo = null;
			while ((line = br.readLine()) != null) {
				pageRankInfo = line.split(delim);
				info.put(Integer.parseInt(pageRankInfo[0]),
						Double.parseDouble(pageRankInfo[1]));
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return info;
	}

	private Map<Integer, Integer> getNumViewsInfo() {

		Map<Integer, Integer> info = new HashMap<Integer, Integer>();

		String line = "";
		String delim = "\t";
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(
					_numViewsScoreFile)));
			String[] numViewsInfo = null;
			while ((line = br.readLine()) != null) {
				numViewsInfo = line.split(delim);
				info.put(Integer.parseInt(numViewsInfo[0]),
						Integer.parseInt(numViewsInfo[1]));
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return info;
	}

	/**
	 * @author sujal
	 * 
	 * @param topResults
	 * @param numTerms
	 * @return
	 */
	private List<Pair<String, Double>> getExpandedQuery(
			Vector<ScoredDocument> topResults, int numTerms) {

		/*
		 * Loop through the entire index and get the word count of all the words
		 * in the documents in topResults. Then that the top 'numterms' words to
		 * generate expanded query.
		 * 
		 * Compressed index is assumed
		 */

		File indexDir = new File(_options._indexPrefix);

		FilenameFilter filter = Utilities.createFileNameFilter(".idx");
		for (File indexFile : indexDir.listFiles(filter)) {
			String outputFileName = _options._indexPrefix + "/"
					+ indexFile.getName() + ".cnt";
			processDocumentForWordCount(topResults, numTerms, indexFile,
					outputFileName);
		}

		// pick top 'numTerms' terms based on their counts
		getTopTerms(numTerms);

		// Compute total word count in all documents in topresults
		long totWordCnt = 0;
		for (ScoredDocument sDoc : topResults) {
			DocumentIndexed dIndexed = (DocumentIndexed) _indexer.getDoc(sDoc
					.getDocId());

			// adding 1 for smoothing in case a document does not contain any
			// word.
			totWordCnt += 1 + dIndexed.getTotalWords();
		}

		/*
		 * Read the final .cnt file to get the count of top 'numTerms' terms and
		 * compute the term probabilities
		 */
		FilenameFilter cntFilter = Utilities.createFileNameFilter(".cnt");
		File[] cntFiles = indexDir.listFiles(cntFilter);

		/*
		 * There should be exactly 1 .cnt file, which is obtained by merging all
		 * the .cnt files to get top terms only.
		 */
		if (cntFiles.length != 1) {
			return null;
		}

		// Map<String, Double> termProb = new LinkedHashMap<String, Double>();
		List<Pair<String, Double>> termProb = new ArrayList<Pair<String, Double>>();

		String line = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(cntFiles[0]));
			String[] termCountInfo;
			while ((line = br.readLine()) != null) {
				termCountInfo = line.split(_termCntDelim);
				termProb.add(new Pair<String, Double>(termCountInfo[0], Double
						.parseDouble(termCountInfo[1]) / totWordCnt));
			}
			br.close();

			// delete the .cnt file
			cntFiles[0].delete();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// normalize the term probabilities so that the sum of all probabilities
		// is 1
		termProb = Utilities.getNormalizedVector(termProb, 1);

		return termProb;
	}

	private void getTopTerms(int numTerms) {
		File indexDir = new File(_options._indexPrefix);
		FilenameFilter ff = Utilities.createFileNameFilter(".cnt");
		File[] cntFiles = indexDir.listFiles(ff);

		int noOfFiles = cntFiles.length;

		/*
		 * A .cnt file is created for each .idx file and contains count of each
		 * term in in the given set of top results. All these .cnt files are
		 * then merged together pair-wise and keeps only top required terms.
		 * Finally only 1 .cnt file should remain. So after every iteration of
		 * merging, the total number of .cnt files should decrease.
		 * 
		 * This variable is used to keep track of this. The program is exited if
		 * the number of files does not decrease because otherwise the system
		 * will eventually crash as the program goes into infinite loop. This
		 * may happen if the files are not deleted.
		 */
		int prevNoOfFiles;

		//System.out.println("no of files = " + noOfFiles);
		/*
		 * Loop till all .cnt files are not merged into just 1 file
		 */
		while (noOfFiles != 1) {

			prevNoOfFiles = noOfFiles;

			/*
			 * All .cnt files are meged in pairs. So if the no. of .cnt files is
			 * odd, ignore the last file
			 */
			if (noOfFiles % 2 != 0) {
				noOfFiles--;
			}

			/*
			 * Loop through all the .cnt files and merge them in pairs. Only top
			 * numTerms are required after merging.
			 */
			for (int i = 0; i < noOfFiles; i += 2) {
				// System.out.println("mrgng : " + cntFiles[i].getAbsolutePath()
				// + ", " + cntFiles[i + 1].getAbsolutePath());
				mergeTermCountFiles(
						cntFiles[i],
						cntFiles[i + 1],
						_options._indexPrefix + "/"
								+ System.currentTimeMillis() + ".cnt", numTerms);
			}

			// Get the number of remaining .cnt files
			cntFiles = indexDir.listFiles(ff);
			noOfFiles = cntFiles.length;

			if (noOfFiles >= prevNoOfFiles) {
				System.out.println("Error: Problem in merging .cnt files !!!");
				System.exit(0);
			}
		}
	}

	private void mergeTermCountFiles(File file1, File file2, String outputFile,
			int numTerms) {

		// return if the inputs are null
		if (file1 == null || file2 == null || outputFile == null
				|| outputFile.trim().length() == 0) {
			return;
		}

		// return if the input files does not exist
		if (!file1.exists() || !file2.exists()) {
			return;
		}

		/*
		 * System.out.println("merging : " + file1.getAbsolutePath() + ", " +
		 * file2.getAbsolutePath()); System.out.println("output file = " +
		 * outputFile);
		 */

		try {
			BufferedReader br1 = new BufferedReader(new FileReader(file1));
			BufferedReader br2 = new BufferedReader(new FileReader(file2));
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));

			int termsMerged = 0;
			String line1 = br1.readLine(), line2 = br2.readLine();
			Scanner scanner1, scanner2;
			String term1, term2;
			int count1, count2;

			while ((line1 != null) && (line2 != null)) {

				scanner1 = new Scanner(line1);
				scanner1.useDelimiter("[" + _termCntDelim + "\n]");
				scanner2 = new Scanner(line2);
				scanner2.useDelimiter("[" + _termCntDelim + "\n]");

				term1 = scanner1.next();
				count1 = scanner1.nextInt();

				term2 = scanner2.next();
				count2 = scanner2.nextInt();

				// write the term with greater count to the file
				if (count1 >= count2) {
					bw.write(term1);
					bw.write(_termCntDelim);
					bw.write(String.valueOf(count1));
					bw.newLine();

					line1 = br1.readLine();
				} else {
					bw.write(term2);
					bw.write(_termCntDelim);
					bw.write(String.valueOf(count2));
					bw.newLine();

					line2 = br2.readLine();
				}

				termsMerged++;
				if (termsMerged >= numTerms) {
					break;
				}
			}

			if (termsMerged < numTerms) {
				// add the remaining info from non-null file, if any
				if (line1 == null) {
					while (line2 != null) {
						scanner2 = new Scanner(line2);
						scanner2.useDelimiter("[" + _termCntDelim + "\n]");

						term2 = scanner2.next();
						count2 = scanner2.nextInt();

						bw.write(term2);
						bw.write(_termCntDelim);
						bw.write(String.valueOf(count2));
						bw.newLine();

						line2 = br2.readLine();
					}
				} else {
					while (line1 != null) {
						scanner1 = new Scanner(line1);
						scanner1.useDelimiter("[" + _termCntDelim + "\n]");

						term1 = scanner1.next();
						count1 = scanner1.nextInt();

						bw.write(term1);
						bw.write(_termCntDelim);
						bw.write(String.valueOf(count1));
						bw.newLine();

						line1 = br1.readLine();
					}
				}
			}

			br1.close();
			br2.close();
			bw.close();

			// System.out.println("deleting file1");
			file1.delete();
			// System.out.println("deleting file2");
			file2.delete();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @author sujal
	 * 
	 * @param topResults
	 * @param indexFile
	 * @param outputFile
	 */
	private void processDocumentForWordCount(Vector<ScoredDocument> topResults,
			int numTerms, File indexFile, String outputFile) {

		/*
		 * Get all the required docids from the input list of documents
		 */
		List<Integer> docIds = new ArrayList<Integer>();
		for (ScoredDocument sDoc : topResults) {
			docIds.add(sDoc.getDocId());
		}

		/*
		 * Sort the docids in ascending order. This may make the process of
		 * computing term-probability from the index efficiently.
		 * 
		 * Assumption: Number of docs to consider for query expansion is not
		 * very large and so the overhead of sorting should not affect the
		 * overall efficiency.
		 */
		Collections.sort(docIds);

		try {
			BufferedReader br = new BufferedReader(new FileReader(indexFile));
			String line = "";

			MyPriorityQueue topterms = new MyPriorityQueue(numTerms);

			while ((line = br.readLine()) != null) {
				if (line.trim().length() != 0) {
					Scanner scanner = new Scanner(line);
					scanner.useDelimiter("[" + _termDoclistDelim + "\n]");

					String term = scanner.next();

					/*
					 * Total term count for 'term' in all the documents in the
					 * input list docIds
					 */
					int termCnt = 0;

					String[] docInfo = scanner.next().split(_doclistDelim);

					// index to loop through all documents for current term
					int docInfoIndex = 0;

					// Holds document id and list of occurrences of current term
					String[] doc_occ = docInfo[docInfoIndex]
							.split(_docCountDelim);

					// Document id read from index (i.e. docInfo)
					int docIdInfo = Integer.parseInt(doc_occ[0]);

					/*
					 * Loop through all the document ids in docIds and compute
					 * combined term count for the current term
					 */
					for (int docIndex = 0; docIndex < docIds.size(); docIndex++) {
						int docIdTop = docIds.get(docIndex);

						/*
						 * As document ids in docIds as well as in the index are
						 * sorted, skip all the docids in the index till they
						 * are smaller than the current id from docIds.
						 */
						while (docIdInfo < docIdTop) {

							docInfoIndex++;
							if (docInfoIndex >= docInfo.length) {
								// no more documents are left in the doclist of
								// current term
								break;
							}

							docIdInfo = Integer.parseInt(docInfo[docInfoIndex]
									.split(_docCountDelim)[0]);
						}

						if (docInfoIndex >= docInfo.length) {
							// no more documents are left in the doclist of
							// current term
							break;
						}

						/*
						 * Check whether the docid pointed by docInfoIndex
						 * matches with the current docid from docIds. Add the
						 * term count in case of match, else continue the search
						 * for next docid
						 */
						if (docIdInfo == docIdTop) {
							// Decode the occurrences and add the total number
							// of occurrences
							doc_occ = docInfo[docInfoIndex]
									.split(_docCountDelim);
							String allOccurrences = doc_occ[1];
							int noOfOcc = DecompressionUtility.getDecoded(
									allOccurrences).size();
							termCnt += noOfOcc;

							docInfoIndex++;
							if (docInfoIndex >= docInfo.length) {
								break;
							}

							doc_occ = docInfo[docInfoIndex]
									.split(_docCountDelim);
							docIdInfo = Integer.parseInt(doc_occ[0]);
						}
					}

					/*
					 * Maintain only top 'numTerms' terms. Ignore all the stop words
					 */
					
					if(! Utilities.isStopWord(term)) {
						topterms.insertWithOverflow(new TermCount(term, termCnt));
					}
				}
			}
			br.close();

			/*
			 * Accumulate the processed info to the buffer. The info contains
			 * term and total term count in all the documents in input list.
			 */

			StringBuilder sBuilder = new StringBuilder();
			StringBuilder sBuilderTmp = new StringBuilder();
			TermCount termCount;
			while ((termCount = topterms.pop()) != null) {

				/*
				 * Save items in reverse order as the priority queue gives
				 * elements in ascending order.
				 */

				sBuilderTmp = new StringBuilder();
				sBuilderTmp.append(termCount.getTerm());
				sBuilderTmp.append(_termCntDelim);
				sBuilderTmp.append(String.valueOf(termCount.getCount()));
				sBuilderTmp.append("\n");

				sBuilder.insert(0, sBuilderTmp);
			}

			// write term-count info to the output file
			System.out.println("writing to = " + outputFile);
			Utilities.writeToFile(outputFile, sBuilder.toString(), false);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void expandQuery(Vector<ScoredDocument> docs, int m) {
		if (_indexer instanceof IndexerInvertedDoconly) {
			System.out.println("doconly");
			IndexerInvertedDoconly iido = (IndexerInvertedDoconly) _indexer;
		} else if (_indexer instanceof IndexerInvertedOccurrence) {
			IndexerInvertedOccurrence iio = (IndexerInvertedOccurrence) _indexer;
		} else if (_indexer instanceof IndexerInvertedCompressed) {
			IndexerInvertedCompressed iic = (IndexerInvertedCompressed) _indexer;
		}
	}

	public RankerComprehensive() {
		super(null, null, null);

		testProcTermCnt();
		// testPriorityQueue();
		// testJsoup();
		// testMergeCntFiles();
		// testLinkedHashmap();
	}

	public static void main(String[] args) {
		new RankerComprehensive();
	}

	private void testLinkedHashmap() {
		Map<String, Integer> m = new HashMap<String, Integer>();
		m.put("a", 0);
		m.put("b", 0);
		m.put("c", 0);
		m.put("d", 0);
		m.put("e", 0);

		for (String k : m.keySet()) {
			System.out.println(k);
		}
	}

	private void testMergeCntFiles() {
		File f1 = new File("data/index/t.idx.cnt");
		File f2 = new File("data/index/w.idx.cnt");
		String outputfile = "data/index/o1.cnt";

		RankerComprehensive rc = new RankerComprehensive(null, null, null);
		rc.mergeTermCountFiles(f1, f2, outputfile, 10);
	}

	private void testPriorityQueue() {
		/*
		 * PriorityQueue<TermCount> pq = new PriorityQueue<TermCount>();
		 * 
		 * pq.add(new TermCount("t1", 100)); pq.add(new TermCount("t2", 500));
		 * pq.add(new TermCount("t3", 200)); pq.add(new TermCount("t4", 0));
		 * pq.add(new TermCount("t5", 700));
		 * 
		 * while(pq.peek() != null) { System.out.println(pq.poll().getCount());
		 * }
		 */

		MyPriorityQueue mpq = new MyPriorityQueue(2);
		mpq.insertWithOverflow(new TermCount("t1", 100));
		mpq.insertWithOverflow(new TermCount("t5", 500));
		mpq.insertWithOverflow(new TermCount("t2", 200));
		mpq.insertWithOverflow(new TermCount("t3", 0));
		mpq.insertWithOverflow(new TermCount("t4", 700));

		TermCount tc;
		while ((tc = mpq.pop()) != null) {
			System.out.println(tc.getCount());
		}
	}

	private void testJsoup() {
		String corpusFile = "data/wiki/Zimbabwe";
		try {
			org.jsoup.nodes.Document doc = Jsoup.parse(new File(corpusFile),
					"UTF-8");
			Elements links = doc.getElementsByTag("a");

			for (Element e : links) {
				System.out.println(e.attr("href"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void testProcTermCnt() {

		try {
			Vector<ScoredDocument> v = new Vector<ScoredDocument>();
			v.add(new ScoredDocument(new Document(1362), 0));
			v.add(new ScoredDocument(new Document(5784), 0));
			v.add(new ScoredDocument(new Document(3896), 0));
			v.add(new ScoredDocument(new Document(5586), 0));
			v.add(new ScoredDocument(new Document(3026), 0));
			v.add(new ScoredDocument(new Document(9811), 0));
			v.add(new ScoredDocument(new Document(7407), 0));
			v.add(new ScoredDocument(new Document(1134), 0));
			v.add(new ScoredDocument(new Document(4558), 0));
			v.add(new ScoredDocument(new Document(6549), 0));

			Options options = new Options("conf/engine.conf");
			IndexerInvertedCompressed indexer = new IndexerInvertedCompressed(
					options);
			indexer.loadIndex();

			RankerComprehensive r = new RankerComprehensive(options, null,
					indexer);

			QueryPhrase processedQuery = new QueryPhrase("tendulkar");
			processedQuery.processQuery();

			List<Pair<String, Double>> termProb = r.runQuery(processedQuery,
					10, 10, 10);

			System.out.println("======================");
			StringBuffer response = new StringBuffer();
			// output the terms and their probabilities
			for (Pair<String, Double> pair : termProb) {
				response.append(pair.getFirstElement());
				response.append(":");
				response.append(pair.getSecondElement());
				response.append("\n");
			}

			System.out.println(response.toString());

			// File indexFile = new File(options._indexPrefix + "/t_test.idx");
			// String outputFile = options._indexPrefix + "/t_test_output.cnt";

			// processDocumentForWordCount(v, indexFile, outputFile);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
