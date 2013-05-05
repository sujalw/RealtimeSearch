package edu.nyu.cs.cs2580;

/**
 * 
 * @author sujal
 *
 */
public class MyPriorityQueue extends org.apache.lucene.util.PriorityQueue<TermCount> {

	public MyPriorityQueue(int maxSize) {
		initialize(maxSize);
	}

	@Override
	protected boolean lessThan(TermCount arg1, TermCount arg2) {
		return arg1.getCount() < arg2.getCount();
	}
}