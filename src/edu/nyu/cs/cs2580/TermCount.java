package edu.nyu.cs.cs2580;

public class TermCount implements Comparable<TermCount> {
	private String term;
	private int count;
	
	public TermCount(String t, int cnt) {
		term = t;
		count = cnt;
	}

	public String getTerm() {
		return term;
	}

	public int getCount() {
		return count;
	}

	@Override
	public int compareTo(TermCount arg) {
		Integer count1 = count;
		Integer count2 = arg.getCount();
		
		return count2.compareTo(count1);
	}
}
