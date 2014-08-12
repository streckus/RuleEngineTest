package teo.isgci.appl.deducer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This Class changes the give txt/XML files such as the old IDs with prefixes like "gc_" get 
 * replaced with Integer IDs with 3 bit postfixes
 * Postfixes are:
 * 000 for User "gc_" classes
 * 001 for AUTO classes
 * 010 for Parameters and parameter PseudoClasses (added by vector)
 * 
 * For further Classes please accustomize the code!
 * @author ivo
 *
 */

public class ChangeIDsToInt {
	
	public static void main(String[] args) throws FileNotFoundException, IOException{
		
		/*change Paths to your own local paths*/
		
		String autocache="/home/ivo/workspace/git.graphclasses.org/persistent/i_autocache.txt";
		String autocacheOut="/home/ivo/workspace/git.graphclasses.org/persistent/autocache.txt";
		
		String names="/home/ivo/workspace/git.graphclasses.org/build/data/i_names.txt";
		String namesOut="/home/ivo/workspace/git.graphclasses.org/sbuild/data/names.txt";
		
		String masterdata1="/home/ivo/workspace/git.graphclasses.org/data/i_masterdata.xml";	
		String masterdata1out="/home/ivo/workspace/git.graphclasses.org/data/masterdata.xml";
		
		
		ChangeIDsToInt go = new ChangeIDsToInt();
		//go.changeIDs(autocache, autocacheOut);
		go.changeIDs(names, namesOut);
		//go.changeXMLIDs(masterdata1, masterdata1out);
	}

	/**
	 * Takes a Textfile with 2 columns, where the IDs are on the left side. 
	 * It replaces the old IDs such as "gc_" with new full Integer postfix IDs
	 * 
	 * -> Insert here your new custom classes!
	 * @param filename
	 * @param outputfile
	 * @throws FileNotFoundException
	 * @throws IOException
	 */

@SuppressWarnings("null")
public void changeIDs(String filename, String outputfile) throws
	FileNotFoundException, IOException{
	String line;
	BufferedReader in = new BufferedReader(new FileReader(filename));
	BufferedWriter out = new BufferedWriter(new FileWriter(outputfile));
	BitSet used2 = new BitSet();

	while ((line = in.readLine()) != null) {
		System.out.println(line);
		String[] parts = line.split("\t", 2);
 
		/*Here is the place to add your new classes*/
		
		/*AUTO*/
		if(parts[0].startsWith("AUTO_")){
			String[] parts2 = parts[0].split("_", 2);
			int id = Integer.parseInt(parts2[1]);
			id=(id<<3)|1; //AUTO intID -> 001
			if(!used2.get(id)){
				used2.set(id);
				out.write(id + "\t" + parts[1]);
				out.newLine();
				System.out.println(id + "\t" + parts[1]);
				continue;
				}
			}
		/*USER*/
		if(parts[0].startsWith("gc_")){
			String[] parts2 = parts[0].split("_", 2);
			int id = Integer.parseInt(parts2[1]);
			id=(id<<3); //gc=000 intID
			if(!used2.get(id)){
				used2.set(id);
				out.write(id + "\t" + parts[1]);
				out.newLine();
				System.out.println(id + "\t" + parts[1]);
				continue;
				}
			}
		/*PARAMETER (added by vector)*/
		if(parts[0].startsWith("par_")){
		        String[] parts2 = parts[0].split("_", 2);
		        int id = Integer.parseInt(parts2[1]);
		        id=(id<<3)|2; //parameter intID -> 010
		        if(!used2.get(id)){
		                used2.set(id);
		                out.write(id + "\t" + parts[1]);
		                out.newLine();
		                System.out.println(id + "\t" + parts[1]);
		                continue;
		                }
		        }
		}
	System.out.println("done");
	in.close();
	out.close();
	}

/**
 * Takes XML Files.
 * Replaces old IDs with prefixes such as "gc_" with full Integer IDs with a 3 bit postfix.
 * @param filename
 * @param outputfile
 * @throws IOException
 * @throws FileNotFoundException
 */
public void changeXMLIDs(String filename, String outputfile) 
		throws IOException, FileNotFoundException{
	String line;
	BufferedReader in = new BufferedReader(new FileReader(filename));
	BufferedWriter out = new BufferedWriter(new FileWriter(outputfile));
	
	String  regex   = "AUTO_\\d*|gc_\\d*|par_\\d*"; // par added by vector
	
	/*Reads line by line and replaces old IDs via Regex*/
	
	while ((line = in.readLine()) != null) {
		System.out.println(line);
		Matcher matcher = Pattern.compile( regex ).matcher( line );
	
		StringBuffer sb = new StringBuffer( line.length() );

		while ( matcher.find() ){
			String temp[] = matcher.group().split("_",2);
			
			/*here is the place to insert your new custom classes */
			
			/*AUTO*/
			if(temp[0].compareTo("AUTO")==0){
				Integer nID=(Integer.parseInt(temp[1])<<3)|1;
				matcher.appendReplacement( sb, nID.toString() );
			}
			/*USER*/
			if(temp[0].compareTo("gc")==0){
				Integer nID=(Integer.parseInt(temp[1])<<3);
				matcher.appendReplacement( sb, nID.toString() );
			}
			/*PARAMETER (added by vector)*/
			if(temp[0].compareTo("par")==0){
			        Integer nID=(Integer.parseInt(temp[1])<<3)|2;
			        matcher.appendReplacement( sb, nID.toString() );
			}
		}
		matcher.appendTail( sb );
		out.write(sb.toString());
		out.newLine();
	System.out.println( sb );
	}
	
	
	
	in.close();
	out.close();
}
}
