/*
 * Generates and caches node id's for graph classes.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.BitSet;

public class IDGenerator {
    /** The id strings start with this */
    private Integer prefix;  //intID String -> Integer
    /** Maps graph class names to ids (from the cache file) */
    private HashMap<String,Integer> cache; //intID String -> Integer
    /** Every true bit is a number that is handed out either in the cache file
     * or while running.
     */
    private BitSet used;

    /**
     * Create a new IDGenerator using the given prefix, reading the ids from
     * the given cachefile.
     */
    public IDGenerator(Integer prefix, String cachefile) {
        this.prefix = prefix;
        cache = new HashMap<String,Integer>();
        used = new BitSet();

        if (cachefile == null)
            return;

        try {
            readCache(cachefile);
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Warning: Cannot read class name cache file "+
                    cachefile);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Return the node id to use for classname. If classname exists in the
     * cache file, the same id is used, otherwise a new, unused id is returned.
     */
    public Integer getID(String classname) { //intID String -> Integer
        Integer res = cache.get(classname);
        if (res != null)
            return res;

        int id = used.length()-1;          // AUTO_0 not used

        if(id <= 0){
        	System.out.println(classname);
        	id=(1 << 3)|prefix;
        }else if((~(id^prefix)&7) == 7){
        	id += 8;
        } else {
        	System.out.println("Something went wrong with IDs in System."+classname);//intID Exception?
        }
        used.set(id);
        return id;
    }


    /**
     * Fill the cache from the given file. File format is "id\tclassname" per
     * line.
     */
    public void readCache(String filename) throws //intID everything
            FileNotFoundException, IOException{
        String line;
        int sep;
        BufferedReader in = new BufferedReader(new FileReader(filename));

        while ((line = in.readLine()) != null) {
            String[] parts = line.split("\t", 2);
            Integer tempID = Integer.parseInt(parts[0]);
            if (cache.put(parts[1], tempID) != null)
                throw new Error("Duplicate key "+ parts[1] +
                        "in name cache file.");
            if (!((~(tempID^prefix)&7) == 7))
                throw new Error("Cached name doesn't end with "+ prefix);
            used.set(tempID);
        }
        in.close();
    }
}

/* EOF */
