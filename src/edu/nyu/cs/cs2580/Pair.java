package edu.nyu.cs.cs2580;

/**
 * This class represents a pair of values of any type.
 * @author sujal
 * 
 * @param <T>
 * @param <U>
 */
public class Pair<T, U extends Number> implements Comparable<Pair<T, U>>{
    private final T t;
    private final U u;

    public Pair(T t, U u) {         
        this.t= t;
        this.u= u;
     }
    
    public T getFirstElement() {
    	return t;
    }
    
    public U getSecondElement() {
    	return u;
    }

	@Override
	public int compareTo(Pair<T, U> o) {
		Number num1 = this.u;
		Number num2 = o.u;
		
		if(num1.doubleValue() < num2.doubleValue()) {
			return -1;
		} else if(num1.doubleValue() > num2.doubleValue()) {
			return 1;
		} else {
			return 0;
		}
	}
 }
