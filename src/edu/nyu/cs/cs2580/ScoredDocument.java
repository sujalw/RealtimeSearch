package edu.nyu.cs.cs2580;

/**
 * Document with score.
 * 
 * @author fdiaz
 * @author congyu
 */
class ScoredDocument implements Comparable<ScoredDocument> {
	private Document _doc;
	private double _score;

	public ScoredDocument(Document doc, double score) {
		_doc = doc;
		_score = score;
	}

	public String asTextResult() {
		StringBuffer buf = new StringBuffer();
		buf.append(_doc._docid).append("\t");
		buf.append(_doc.getTitle()).append("\t");
		buf.append(_score).append("\t");
		
		if(_doc.getPageRank() != -1) {
			buf.append(_doc.getPageRank()).append("\t");
		}
		
		if(_doc.getNumViews() != -1) {
			buf.append(_doc.getNumViews()).append("\t");
		}
		
		return buf.toString();
	}

	public void set_score(double _score) {
		this._score = _score;
	}

	/**
	 * @CS2580: Student should implement {@code asHtmlResult} for final project.
	 */
	public String asHtmlResult() {
		return "";
	}

	public double get_score() {
		return _score;
	}
	
	public int getDocId() {
		return _doc._docid;
	}
	
	public String getTitle() {
		return _doc.getTitle();
	}

	@Override
	public int compareTo(ScoredDocument o) {
		if (this._score == o._score) {
			return 0;
		}
		return (this._score < o._score) ? 1 : -1;
	}
}
