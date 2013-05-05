package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author sujal
 * 
 */
public class Bhattacharyya {
	public static void main(String[] args) {
		
		// Check for invalid invocation of program
		if (args.length != 2) {
			System.out.println("Error in parameters...");
			System.out
					.println("Usage: java -cp src edu.nyu.cs.cs2580.Bhattacharyya <PATH-TO-PRF-OUTPUT> <PATH-TO-OUTPUT>");
		} else {
			File prfOutputFile = new File(args[0]);
			if (!prfOutputFile.exists()) {
				System.out.println("Error: File " + args[0]
						+ " does not exist.");
				System.exit(0);
			}

			File outputFile = new File(args[1]);
			if (outputFile.exists()) {
				System.out.println("Error: File " + args[1]
						+ " should be empty.");
				System.exit(0);
			}

			computeBhattacharyyaCoeff(prfOutputFile, outputFile);
		}
	}

	private static void computeBhattacharyyaCoeff(File prfOutputDir,
			File outputFile) {

		FilenameFilter fFilter = Utilities.createFileNameFilter(".tsv");
		File[] prfFiles = prfOutputDir.listFiles(fFilter);

		final String delim = "\t";
		StringBuilder sBuilder;

		/*
		 * Loop through all pairs of prf output files to compute Bhattacharyya
		 * coefficient between each pair
		 */
		String query1="", query2="";
		for (int fileIndex1 = 0; fileIndex1 < prfFiles.length; fileIndex1++) {
			query1 = prfFiles[fileIndex1].getName();
			query1 = query1.substring(0, query1.lastIndexOf('.'));
			
			sBuilder = new StringBuilder();
			for (int fileIndex2 = fileIndex1 + 1; fileIndex2 < prfFiles.length; fileIndex2++) {
				query2 = prfFiles[fileIndex2].getName();
				query2 = query2.substring(0, query2.lastIndexOf('.'));
				
				// Compute Bhattacharyya coefficient between current pair of queries
				double coeff = getBhattacharyyaCoeff(prfFiles[fileIndex1],
						prfFiles[fileIndex2]);
				
				sBuilder.append(query1);
				sBuilder.append(delim);
				sBuilder.append(query2);
				sBuilder.append(delim);
				sBuilder.append(String.valueOf(coeff));
				sBuilder.append("\n");
			}
			
			// Write the Bhattacharyya for current pair to queries to the output file
			Utilities.writeToFile(outputFile.getPath(), sBuilder.toString(), true);
		}
	}

	private static double getBhattacharyyaCoeff(File file1, File file2) {

		Map<String, Double> termProb1 = new HashMap<String, Double>();
		Map<String, Double> termProb2 = new HashMap<String, Double>();

		double coeff = -1;
		final String termProbDelim = "\t";

		BufferedReader br;

		try {
			String line;
			String[] termCountInfo;

			br = new BufferedReader(new FileReader(file1));
			while ((line = br.readLine()) != null) {
				if (line.trim().length() > 0) {
					termCountInfo = line.split(termProbDelim);
					termProb1.put(termCountInfo[0],
							Double.parseDouble(termCountInfo[1]));
				}
			}
			br.close();

			br = new BufferedReader(new FileReader(file2));
			while ((line = br.readLine()) != null) {
				if (line.trim().length() > 0) {
					System.out.println("line = " + line);
					termCountInfo = line.split(termProbDelim);
					System.out.println("termCountInfo[0] = " + termCountInfo[0]);
					termProb2.put(termCountInfo[0],
							Double.parseDouble(termCountInfo[1]));
				}
			}
			br.close();

			coeff = getBhattacharyyaCoeff(termProb1, termProb2);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return coeff;
	}

	private static double getBhattacharyyaCoeff(Map<String, Double> termProb1,
			Map<String, Double> termProb2) {
		double coeff = 0;

		if (termProb1.keySet().size() <= termProb2.keySet().size()) {
			for (String term : termProb1.keySet()) {
				Double prob1 = termProb1.get(term);
				Double prob2 = termProb2.get(term);

				prob1 = prob1 == null ? 0 : prob1;
				prob2 = prob2 == null ? 0 : prob2;

				coeff += Math.sqrt(prob1 * prob2);
			}
		}

		return coeff;
	}
}
