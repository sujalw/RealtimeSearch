/**
 * 
 */
package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author ravi
 *
 */
public class Test {
	
	public Test() {
		String pr = "data/index/pagerank.tsv";
		String nv = "data/index/numviews.tsv";
		
		List<Integer> did_pr = new ArrayList<Integer>();
		List<Integer> did_nv = new ArrayList<Integer>();
		
		BufferedReader br = null;
		String line = "";
		String[] info = null;
		String delim = "\t";
		StringBuilder sb;
		try {
			br = new BufferedReader(new FileReader(new File(pr)));
			while((line=br.readLine()) != null) {
				info = line.split(delim);
				did_pr.add(Integer.parseInt(info[0]));
			}
			br.close();
			Collections.sort(did_pr);
			
			sb = new StringBuilder();
			for(Integer id : did_pr) {
				sb.append(String.valueOf(id));
				sb.append("\n");
			}
			
			Utilities.writeToFile("data/pr_dids.txt", sb.toString(), false);
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		try {
			br = new BufferedReader(new FileReader(new File(nv)));
			while((line=br.readLine()) != null) {
				info = line.split(delim);
				did_nv.add(Integer.parseInt(info[0]));
			}
			br.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		Collections.sort(did_nv);
		
		sb = new StringBuilder();
		for(Integer id : did_nv) {
			sb.append(String.valueOf(id));
			sb.append("\n");
		}
		
		Utilities.writeToFile("data/nv_dids.txt", sb.toString(), false);
	}

	public static void main(String[] args) {
		Test t = new Test();
		/*
		try {
			File f = new File("/home/ravi/websearch/hw3/data/log/20130301-160000.log");
			t.test3(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		/*try {
			//File file = new File("/home/ravi/websearch/hw3/data/wiki/20th_Century_Fox");
			//t.test(file);
			File file2 = new File("/home/ravi/websearch/hw3/data/wiki/20th_Century_Fox");
			t.test2(file2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/ 
	}
	
	public void test3(File input) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(input));
		String line = "";
		String contents[];
		String currentURI;
		StringBuffer tempBuffer;
		while((line=reader.readLine()) != null) {
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
			System.out.println(currentURI);
		}
	}
	
	public void test2(File corpusFile)  throws IOException {
		
		try {
			org.jsoup.nodes.Document doc = Jsoup.parse(corpusFile, "UTF-8");
			Elements links = doc.getElementsByTag("a");

			for(Element e : links) {
				System.out.println(e.attr("href"));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void test(File string) throws IOException, BadLocationException {

		BufferedReader reader = new BufferedReader(new FileReader(string));

		EditorKit kit = new HTMLEditorKit();

		HTMLDocument doc = (HTMLDocument)kit.createDefaultDocument();
		
		//uncomment this line incase you get character set issues
		doc.putProperty("IgnoreCharsetDirective", new Boolean(true));

		kit.read(reader, doc, 0);

		//Get all <a> tags (hyperlinks)
		HTMLDocument.Iterator it = doc.getIterator(HTML.Tag.A);

		while (it.isValid())
		{
		    MutableAttributeSet mas = (MutableAttributeSet)it.getAttributes();
		    
		    //get the HREF attribute value in the <a> tag
		    String link = (String)mas.getAttribute(HTML.Attribute.HREF);
		    System.out.println(link);
		   
		    it.next();
		}
	}
}
