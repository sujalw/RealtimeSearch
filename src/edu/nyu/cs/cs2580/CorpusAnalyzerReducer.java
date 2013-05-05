/**
 * 
 */
package edu.nyu.cs.cs2580;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
/**
 * @author ravi
 *
 */
public class CorpusAnalyzerReducer extends Reducer<Text, Text, Text, Text> {

	@Override
	public void reduce(Text key, Iterable<Text> values,Context context)
			throws IOException, InterruptedException {
			StringBuffer sb = new StringBuffer();
			for(Text value : values) {
				sb.append(value);
				sb.append(" ");
			}
			context.write(key, new Text(sb.toString()));
	}
}
