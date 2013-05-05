package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * 
 * @author sujal
 *
 */
public class IndexerUtils {
	
	/**
	 * @author sujal
	 * 
	 * @param current
	 * @param list
	 * @param galloping
	 * @return
	 */
	public static int search(int current, Integer[] list, boolean galloping) {

		if (list == null || list.length == 0
				|| list[list.length - 1] < current) {
			return -1;
		}

		if (list[0] > current) {
			return list[0];
		}

		int low = 0, high = 0;
		int jump = 1;

		if (galloping) {
			// Through galloping, find a slot for binary search
			while ((high < list.length) && list[high] <= current) {

				low = high;
				// increase step size
				jump = jump << 1;
				high += jump;
			}

			if (high > (list.length - 1)) {
				high = list.length - 1;
			}
		} else {
			high = list.length - 1;
		}

		return binarySearch(list, low, high, current);
	}
	
	/*
	 * 
	 * 
	 */
	/**  
	 * Perform binary search over the given list to find a number > current.
	 * Returns -1 if no such number is found
	 * 
	 * @author sujal
	 *  
	 * @param list
	 * @param begin
	 * @param end
	 * @param current
	 * @return
	 */
	public static int binarySearch(Integer[] list, int begin, int end, int current) {

		if (list == null || list.length == 0) {
			return -1;
		}

		if (begin < 0 || end < 0 || begin >= list.length || end >= list.length) {
			return -1;
		}

		// if last number is less than current then return -1
		if (list[end] <= current) {
			return -1;
		} else {
			int mid;
			while (begin <= end) {
				mid = (begin + end) / 2;

				if (list[mid] <= current) {
					// search in right half
					begin = mid + 1;
				} else {
					// search in left half
					end = mid - 1;
				}
			}

			if (list[begin] > current) {
				return list[begin];
			} else {
				return list[begin + 1];
			}
		}
	}
	
	/**
	 * @author sujal
	 * 
	 * @param docIds
	 * @return true if all numbers in the given list are same. Else returns
	 *         false
	 */
	public static boolean isSame(int[] docIds) {

		if (docIds == null || docIds.length == 0) {
			return false;
		}

		if (docIds.length > 0) {
			int first = docIds[0];
			for (int i = 1; i < docIds.length; i++) {
				if (first != docIds[i]) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * @author sujal
	 * 
	 * @param docIds
	 * @return
	 */
	public static boolean continueSearch(int[] docIds) {

		if (docIds == null || docIds.length <= 0) {
			return false;
		}

		for (int docId : docIds) {
			// if atleast one term is not found, search should not continue
			if (docId == -1) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @author sujal
	 * 
	 * @param occurrences
	 * @return
	 */
	public static boolean isPhrase(int[] occurrences) {
		// if successive occurrences are in sequence, then they are the occurrences of a phrase
		boolean phrase = true;
		
		for(int i=1 ; i<occurrences.length ; i++) {
			if((occurrences[i] - occurrences[0]) != i) {
				phrase = false;
				break; 
			}
		}

		return phrase;
	}
	
	/**
	 * @author sujal
	 * 
	 * @param file1
	 * @param file2
	 * @param _options
	 * @param _termDoclistDelim
	 * @param _doclistDelim
	 * @param _docCountDelim
	 */
	public static void mergeIndexFiles(String file1, String file2, Options _options, String _termDoclistDelim, String _doclistDelim, String _docCountDelim) {

		if (file1 == null || file2 == null || file1.trim().length() == 0
				|| file2.trim().length() == 0) {
			return;
		}

		try {
			File f1 = new File(_options._indexPrefix + "/" + file1);
			File f2 = new File(_options._indexPrefix + "/" + file2);

			if (!f2.exists()) {
				return;
			} else if (f1.exists() && f2.exists()) {
				String f3Name = _options._indexPrefix + "/"
						+ System.currentTimeMillis() + ".idx";

				BufferedReader br1 = new BufferedReader(new FileReader(f1));
				BufferedReader br2 = new BufferedReader(new FileReader(f2));
				BufferedWriter bw = new BufferedWriter(new FileWriter(f3Name));

				String line1 = br1.readLine(), line2 = br2.readLine();
				String term1, term2;
				Scanner scanner1, scanner2;
				while ((line1 != null) && (line2 != null)) {

					if (line1.trim().length() == 0
							|| line2.trim().length() == 0) {
						break;
					}

					scanner1 = new Scanner(line1);
					scanner1.useDelimiter("["
							+ String.valueOf(_termDoclistDelim) + "\n]");
					scanner2 = new Scanner(line2);
					scanner2.useDelimiter("["
							+ String.valueOf(_termDoclistDelim) + "\n]");

					term1 = scanner1.next();
					term2 = scanner2.next();

					if (term1.compareTo(term2) < 0) {
						// add term1 info as it is to the file
						bw.write(line1);
						bw.newLine();

						line1 = br1.readLine();
					} else if (term1.compareTo(term2) > 0) {
						// add term2 info as it is to the file
						bw.write(line2);
						bw.newLine();

						line2 = br2.readLine();
					} else {
						// write any term as both are same
						bw.write(term1);
						bw.write(_termDoclistDelim);

						String tmp1 = scanner1.next();
						String tmp2 = scanner2.next();

						// write docIds in sorted manner
						int docid1 = Integer.parseInt(tmp1.split(String
								.valueOf(_doclistDelim))[0].split(String
								.valueOf(_docCountDelim))[0]);
						int docid2 = Integer.parseInt(tmp2.split(String
								.valueOf(_doclistDelim))[0].split(String
								.valueOf(_docCountDelim))[0]);

						if (docid1 < docid2) {
							bw.write(tmp1);
							// bw.write(_doclistDelim);
							bw.write(tmp2);
						} else {
							bw.write(tmp2);
							// bw.write(_doclistDelim);
							bw.write(tmp1);
						}

						bw.newLine();

						line1 = br1.readLine();
						line2 = br2.readLine();
					}
				}

				// copy the remaining info from non empty file
				if (line1 != null) {
					while (line1 != null) {
						if (line1.trim().length() == 0) {
							break;
						}

						bw.write(line1);
						bw.newLine();
						line1 = br1.readLine();
					}
				} else if (line2 != null) {
					while (line2 != null) {
						if (line2.trim().length() == 0) {
							break;
						}

						bw.write(line2);
						bw.newLine();
						line2 = br2.readLine();
					}
				}

				// close open streams
				br1.close();
				br2.close();
				bw.close();

				// delete old files
				f1.delete();
				f2.delete();

				new File(f3Name).renameTo(f1);
			} else if (!f1.exists()) {
				// here f2 should exist

				// just rename f2 to f1
				f2.renameTo(f1);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
