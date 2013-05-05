package edu.nyu.cs.cs2580;

public class TermProbability {
	String term;
	float probability;

	public TermProbability(String t, float p) {
		term = t;
		probability = p;
	}

	public String getTerm() {
		return term;
	}

	public float getProbability() {
		return probability;
	}
}
