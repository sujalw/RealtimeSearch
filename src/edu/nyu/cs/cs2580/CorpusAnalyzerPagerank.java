package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.swing.text.BadLocationException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {

	// Stores document info in the format : doc_id; uri; title; term count	
	static String _docInfoFile = "docinfo.inf";
	// Stores outbound link info in the format : doc_id inbound1 inbound2 ...inboundn
	static String _outboundInfoFile = "outboundLinks.inf";
	// Stores the page rank of all documents
	static String _pagerankInfoFile = "pagerank.tsv";
	// Stores inbound link info in the format : doc_id<\t>outbound1 outbound2 ...outboundn
	// NOTE : This directory should not exist before the program is ran.
	static String _inboundInfoDir = "mine";
	// Stores doc Id and number of out bound links present in that document.
	// Created during prepare and used during compute.
	Map<Integer, Integer> docIdAndOutboundSize = new HashMap<Integer, Integer>();

	// Contains all URI's present in the corpus.
	static Map< String, Integer> docNameAndId = new HashMap< String, Integer>();

	// Stores initial page rank for each document present in the corpus.
	Map<Integer, Double> oldDocPagerank = new HashMap<Integer, Double>();

	// Stores newer page rank for each document for each iteration.
	Map<Integer, Double> newDocPagerank = new HashMap<Integer, Double>();
	
	final String _inboundInfoFile = "part-r-00000";
	final String _docInfoDelim = ";";
	final double lambda = 0.1;
	final int numberofIter = 2;
	int _numberOfDocs = 0;

	/*public static void main(String args[]) throws InterruptedException, ClassNotFoundException {
		new CorpusAnalyzerPagerank();
	}
	
	public CorpusAnalyzerPagerank() {
		try {
			Options options = new Options("conf/engine.conf");
			CorpusAnalyzerPagerank capr = new CorpusAnalyzerPagerank(options);
			long start = System.currentTimeMillis();
			capr.createDocInfo();
			capr.prepare();
			capr.compute();
			long end = System.currentTimeMillis();
			System.out.println("time = " + (end - start));

		} catch (IOException e) { // TODO Auto-generated
			e.printStackTrace();
		}
	}*/
	
	public CorpusAnalyzerPagerank(Options options) {
		super(options);

		_docInfoFile = _options._indexPrefix + "/" + _docInfoFile;
		_outboundInfoFile = _options._indexPrefix + "/" + _outboundInfoFile;
		_inboundInfoDir = _options._indexPrefix + "/" + _inboundInfoDir;
		_pagerankInfoFile = _options._indexPrefix + "/" + _pagerankInfoFile;
	}

	/**
	 * This function processes the corpus as specified inside {@link _options}
	 * and extracts the "internal" graph structure from the pages inside the
	 * corpus. Internal means we only store links between two pages that are both
	 * inside the corpus.
	 * 
	 * Note that you will not be implementing a real crawler. Instead, the corpus
	 * you are processing can be simply read from the disk. All you need to do is
	 * reading the files one by one, parsing them, extracting the links for them,
	 * and computing the graph composed of all and only links that connect two
	 * pages that are both in the corpus.
	 * 
	 * Note that you will need to design the data structure for storing the
	 * resulting graph, which will be used by the {@link compute} function. Since
	 * the graph may be large, it may be necessary to store partial graphs to
	 * disk before producing the final graph.
	 *
	 * @throws IOException
	 */
	@Override
	public void prepare() throws IOException {
		System.out.println("Preparing " + this.getClass().getName());
		File f = new File(_outboundInfoFile);
		if(f.exists()) {
			f.delete();
		}
		createDocInfo();
		String corpusDirPath = _options._corpusPrefix;		
		int docId = 0;
		double initialPageRank;
		String outBoundLink = "";
		int outBoundSize;
		StringBuffer info;
		File corpusDir = new File(corpusDirPath);		
		for (File corpusFile : corpusDir.listFiles()) {
			if (corpusFile.getName().startsWith(".")) {
				continue;
			}
			System.out.println("Processing Doc ID for prepare function = "+docId);

			HeuristicLinkExtractor hle = new HeuristicLinkExtractor(corpusFile);
			info = new StringBuffer();
			info.append(docId);
			info.append(" ");
			outBoundSize = 0;
			while((outBoundLink = hle.getNextInCorpusLinkTarget()) != null) {
				if(docNameAndId.containsKey(outBoundLink)) {
					outBoundSize++;
					info.append(docNameAndId.get(outBoundLink));
					info.append(" ");
				}
			}
			info.append("\n");
			docIdAndOutboundSize.put(docId, outBoundSize);

			Utilities.writeToFile(_outboundInfoFile, info.toString(), true);

			// Initial Page rank for each document is assumed to be equally divided among all documents.
			initialPageRank = (double) 1 / _numberOfDocs;
			oldDocPagerank.put(docId, initialPageRank);
			docId++;
		}
		try {
			getInboundLinks(_outboundInfoFile, _inboundInfoDir);
		} catch (InterruptedException e) {
			System.out.println("InterruptedException:"+e.getMessage());
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("ClassNotFoundException:"+e.getMessage());
			e.printStackTrace();
		}
		//writeDocPagerankToFile();
	}

	/*Elements links = doc.select("a[href]"); // a with href
		  for (Element link : links) {
		     System.out.println("Href = "+link.attr("abs:href"));
		  }*/
	

	/**
	 * 
	 * @param corpusFile
	 * @return a set of out bound URI's present in this document. 
	 * The set only consists of valid URI's. i.e; for only existing files within the corpus. 
	 * @throws IOException
	 */
	public static Set<Integer> getOutboundURISet(File corpusFile) throws IOException, BadLocationException {
		Set<Integer> outboundURISet = new HashSet<Integer>(); 

		org.jsoup.nodes.Document doc = Jsoup.parse(corpusFile, "UTF-8");
		Elements links = doc.getElementsByTag("a");

		for(Element e : links) {
			//System.out.println(e.attr("href"));
			if(docNameAndId.containsKey(e.attr("href"))) {
				outboundURISet.add(docNameAndId.get(e.attr("href")));
			}
		}

		return outboundURISet;

		/*		BufferedReader reader = new BufferedReader(new FileReader(corpusFile));

		EditorKit kit = new HTMLEditorKit();

		HTMLDocument doc = (HTMLDocument)kit.createDefaultDocument();

		doc.putProperty("IgnoreCharsetDirective", new Boolean(true));

		kit.read(reader, doc, 0);

		//Get all <a> tags (hyperlinks)
		HTMLDocument.Iterator it = doc.getIterator(HTML.Tag.A);

		while (it.isValid())
		{
		    MutableAttributeSet mas = (MutableAttributeSet)it.getAttributes();

		    //get the HREF attribute value in the <a> tag
		    String link = (String)mas.getAttribute(HTML.Attribute.HREF);
		    if(docNames.contains(link)) {
				  outboundURISet.add(link);
			}
		    it.next();
		}*/

		/*while((line = br.readLine()) != null) {
		  if(line.matches(".*<a.*href.*>.*</a>.*")) { // Matches <a href tags in the file.
			  System.out.println("Line = "+line);
			  if(line != null && !line.equals("")) {
				  hrefLines = line.split(" ");
				  for(String semiLine : hrefLines) {
					  if(semiLine.startsWith("href=")) {
						  href = semiLine.substring(6, semiLine.length()-1); // Collects the URI Name.
						  if(docNames.contains(href)) {
							  outboundURISet.add(href);
						  }
					  }
				  }
			  }
		  }
	  }*/
	}

	/**
	 * 
	 * @param uri
	 * @return the doc id of a given URI(ie; doc name). If URI is not found, this function returns -1.
	 * @throws IOException
	 */
	/*public static int getDocidFromURI(String uri) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(_docInfoFile));
		String line;
		String contents[];
		while((line = br.readLine()) != null) {
			contents = line.split(";");
			if(contents[1].equals(uri)) {
				return Integer.parseInt(contents[0]);
			}
		}
		return -1;
	}*/

	/**
	 * Writes the out bound link Map to an external file. File is always appended.
	 */
	/*public void writeOutboundLinkToFile() {
		StringBuffer outBound; 
		Iterator<Entry<Integer, Vector<Integer>>> it = outboundLinkGraph.entrySet().iterator();
		while(it.hasNext()) {
			outBound = new StringBuffer();
			Map.Entry<Integer, Vector<Integer>> pairs = (Map.Entry<Integer, Vector<Integer>>)it.next();
			outBound.append(pairs.getKey());
			outBound.append(" ");
			for(int value : pairs.getValue()) {
				outBound.append(value);
				outBound.append(" ");
			}
			outBound.append("\n");
			Utilities.writeToFile(_outboundInfoFile, outBound.toString(), true);
		}
	}*/

	/**
	 * This function is used to obtain inbound link graph. It uses a Map Reduce Framework to create inbound-outbound pairs.
	 * It writes the output into external directory "mine".
	 * Please note, before running this function, the "mine" directory should not exist. This function will create on its own.
	 * @param inputFile
	 * @param outputPath
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ClassNotFoundException
	 */
	public int getInboundLinks(String inputFile, String outputPath) throws IOException, InterruptedException, ClassNotFoundException {
		Utilities.deleteDir(outputPath);
		
		Job job = new Job();
		job.setJarByClass(CorpusAnalyzerPagerank.class);
		job.setJobName("CorpusAnalyzerPagerank");
		FileInputFormat.addInputPath(job, new Path(inputFile));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		job.setMapperClass(CorpusAnalyzerMapper.class);
		job.setReducerClass(CorpusAnalyzerReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		return (job.waitForCompletion(true) ? 0 : 1);
	}

	/**
	 * This function computes the PageRank based on the internal graph generated
	 * by the {@link prepare} function, and stores the PageRank to be used for
	 * ranking.
	 * 
	 * Note that you will have to store the computed PageRank with each document
	 * the same way you do the indexing for HW2. I.e., the PageRank information
	 * becomes part of the index and can be used for ranking in serve mode. Thus,
	 * you should store the whatever is needed inside the same directory as
	 * specified by _indexPrefix inside {@link _options}.
	 *
	 * @throws IOException
	 */
	@Override
	public void compute() throws IOException {
		System.out.println("Computing using " + this.getClass().getName());
		File f = new File(_pagerankInfoFile);
		if(f.exists()) {
			f.delete();
		}
		File inBoundFile = new File(_inboundInfoDir + "/" + _inboundInfoFile);
		computePageRank(inBoundFile);
		writeDocPagerankToFile();
		return;
	}
	

	
	/**
	 * This function does the actual computation of page rank and stores in memory.
	 * oldDocPagerank stores previous set of page ranks for each document.
	 * newDocPagerank is created to store new set of page ranks based on values 
	 * taken from oldDocPagerank.
	 * @param outBoundFile
	 * @param inBoundFile
	 * @throws IOException
	 */
	public void computePageRank(File inBoundFile) throws IOException {
		BufferedReader br;
		for(int i = 0; i < numberofIter; i++) {
			br = new BufferedReader(new FileReader(inBoundFile));
			System.out.println("Interation Count = "+i);
			// creating an empty hash map to be loaded in this function.
			newDocPagerank = new HashMap<Integer, Double>();
			String line = "";
			String contents[];
			String inBoundDocs[];
			double ranksum;
			double currentPageRank;
			while((line = br.readLine()) != null) {
				contents = line.split("\t");
				inBoundDocs = contents[1].split(" ");
				ranksum = 0.0;
				currentPageRank = 0.0;
				for(String inDoc : inBoundDocs) {
					ranksum += (double) oldDocPagerank.get(Integer.parseInt(inDoc)) 
							/ (docIdAndOutboundSize.get(Integer.parseInt(inDoc)));
				}	
				double constant = (double)(lambda / _numberOfDocs);
				double variable = (double)((1 - lambda) * ranksum);
				currentPageRank = constant + variable;
				newDocPagerank.put(Integer.parseInt(contents[0]), currentPageRank);
			}
			//oldDocPagerank.clear();
			oldDocPagerank.putAll(newDocPagerank);
		}
		//newDocPagerank.putAll(oldDocPagerank);
	}

	/**
	 * Writes the document page rank info to an external file. File is never appended, as Map is flushed after writing.
	 */
	public void writeDocPagerankToFile() {
		StringBuffer eachEntry;
		//Iterator<Entry<Integer, Double>> it = newDocPagerank.entrySet().iterator();
		
		// Old map has the latest info as it is updated after iterations
		Iterator<Entry<Integer, Double>> it = oldDocPagerank.entrySet().iterator();
		while(it.hasNext()) {
			eachEntry = new StringBuffer();
			Map.Entry<Integer, Double> pairs = (Map.Entry<Integer, Double>)it.next();
			eachEntry.append(pairs.getKey());
			eachEntry.append("\t");
			eachEntry.append(pairs.getValue());
			eachEntry.append("\n");
			Utilities.writeToFile(_pagerankInfoFile, eachEntry.toString(), true);
		}
		// Flush the map so that next time it can hold newer page ranks.
		// oldDocPagerank = new HashMap<Integer, Double>();
	}
	/**
	 * 
	 * @param outBoundFile
	 * @param docId
	 * @return number of outbound links for a given document in the available corpus.
	 * @throws IOException
	 */
	/*public int getOutboundLinkSize(File outBoundFile, int docId) throws IOException {
		BufferedReader outBoundBR = new BufferedReader(new FileReader(outBoundFile));
		String line = "";
		String contents[];
		while((line = outBoundBR.readLine()) != null) {
			contents = line.split(" ");
			if(Integer.parseInt(contents[0]) == docId) {
				return Integer.parseInt(contents[1]);
			}
		}
		return 0;
	}*/
	
	/**
	 * During indexing mode, this function loads the PageRank values computed
	 * during mining mode to be used by the indexer.
	 *
	 * @throws IOException
	 */
	@Override
	public Object load() throws IOException {
		System.out.println("Loading using " + this.getClass().getName());
		Map<Integer, Double> loadPagerank = new HashMap<Integer, Double>();
		BufferedReader br = new BufferedReader(new FileReader(_pagerankInfoFile));
		String line = "";
		String contents[];
		while((line = br.readLine()) != null) {
			contents = line.split("\t");
			loadPagerank.put(Integer.parseInt(contents[0]), Double.parseDouble(contents[1]));
		}
		return loadPagerank;
	}



	/**
	 * This function will create Doc Info File containing Document Info in following format: as well as create a HashSet of 
	 * Doc ID; Doc URI; Doc Title; Term Count
	 * Also, 
	 * @return a set of all Doc URI's(i.e; doc names) present in corpus
	 * @throws IOException
	 */
	public void createDocInfo() throws IOException {
		String corpusDirPath = _options._corpusPrefix;
		System.out.println("Constructing Document Info from: " + corpusDirPath);
		File f = new File(_docInfoFile);
		if(f.exists()) {
			f.delete();
		}
		StringBuffer docInfo;
		int docId = 0;
		File corpusDir = new File(corpusDirPath);		
		for (File corpusFile : corpusDir.listFiles()) {
			if (corpusFile.getName().startsWith(".")) {
				continue;
			}
			_numberOfDocs++;
			docInfo = new StringBuffer();
			System.out.println("Processing Doc ID for creating Doc Info = "+docId);
			Document doc = Jsoup.parse(corpusFile, "UTF-8");
			if(doc == null) {
				continue;
			}
			String contents = doc.text();

			if (contents == null) {
				continue;
			}

			Vector<String> terms = Utilities.getStemmed(contents);
			String uri = doc.baseUri();
			uri = uri==null ? "" : uri;
			uri = new File(uri).getName();

			docNameAndId.put(uri, docId);

			String title = doc.title().trim();
			title = title.length()==0 ? uri : title;

			int wordsInDoc = terms.size();

			docInfo.append(docId);
			docInfo.append(_docInfoDelim);		
			docInfo.append(uri);
			docInfo.append(_docInfoDelim);
			docInfo.append(title);
			docInfo.append(_docInfoDelim);
			docInfo.append(wordsInDoc); // total words in the document
			docInfo.append("\n");

			Utilities.writeToFile(_docInfoFile, docInfo.toString(), true);
			docId++;

		}
	}
}
