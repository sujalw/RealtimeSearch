/**
 * 
 */
package edu.nyu.cs.cs2580;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
/**
 * @author ravi
 *
 */
public class CorpusAnalyzerMapper extends Mapper<LongWritable, Text, Text, Text> {

	@Override
	public void map(LongWritable key, Text value, Context context)
			throws IOException, InterruptedException {

		String line = value.toString();
		String[] contents = line.split(" ");
		// Iteration starts with 2nd element because, first is outbound doc id followed by list of inbound doc id's.
		for(int i = 1; i < contents.length; i++) {
			context.write(new Text(contents[i]), new Text(contents[0]));
		}
	}
}
