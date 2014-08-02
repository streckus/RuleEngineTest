/*
 * Store data for tracing deductions.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import teo.isgci.gc.GraphClass;
import teo.isgci.grapht.Annotation;
import teo.isgci.relation.Inclusion;


/**
 * Stores data for tracing deductions.
 * Every TraceData has a description string and a list of prerequisite edges.
 * The description string describes why the conclusion to which the tracedata
 * is attached was derived from the preequisites.
 * Note that the conclusion itself is not stored.
 */
class TraceData {
    private String desc;                        // Description.
    private Inclusion[] prereqs;                // Prerequisite edges.


    /**
     * Create a new TraceData. The varargs array is stored without copying!
     */
    public TraceData(String desc, Inclusion... is) {
        this.desc = desc;
        prereqs = is;
    }


    public TraceData(String desc, ArrayList<Inclusion> v) {
        this.desc = desc;
        prereqs = v.toArray(new Inclusion[0]);
    }

    
    public String getDesc() {
        return desc;
    }


    /**
     * Print the TraceData stored in traceAnn, belonging to e to the given
     * writer. Dependencies are retrieved recursively and printed indented.
     */
    public static void print(PrintWriter writer, Inclusion e,
            Annotation<GraphClass,Inclusion,TraceData> traceAnn) {
        print(writer, e, traceAnn, "");
    }


    /**
     * Print the TraceData stored in traceAnn, belonging to e to the given
     * writer. Dependencies are retrieved recursively and printed indented.
     */
    private static void print(PrintWriter writer, Inclusion edge,
            Annotation<GraphClass,Inclusion,TraceData> traceAnn,
            String prefix) {
        writer.print(prefix);
        writer.print(edge);
        writer.print("  ");
        TraceData td = traceAnn.getEdge(edge);
        if (td != null) {
            writer.print(td.desc);
            writer.println();
            for (Inclusion e : td.prereqs) {
                writer.print(prefix+" ");
                writer.println(e);
            }
        } else {
            writer.print("(no tracedata)"); 
            writer.println();
        }
    }
    
    public static void sqlPrint(PreparedStatement statement, Inclusion edge,
    		Annotation<GraphClass, Inclusion, TraceData> traceAnn) throws SQLException {
    	// 1: Sub
    	// 2: Super
    	// 3: Type
    	// 4: Dependencies
    	// Set sub class
    	int sub = edge.getSub().getID();
    	statement.setInt(1, sub);
    	// Set super class
    	int sup = edge.getSuper().getID();
    	statement.setInt(2, sup);
    	// Set type
    	statement.setString(3, traceAnn.getEdge(edge).desc);
    	
    	JsonArray arr = new JsonArray();

    	for (Inclusion e : traceAnn.getEdge(edge).prereqs) {
    		JsonObject dep = new JsonObject();
    		dep.addProperty("sub", e.getSub().getID());
    		dep.addProperty("sup", e.getSuper().getID());
    		arr.add(dep);
    	}
    	
    	statement.setString(4, arr.getAsString());
    	
    	statement.addBatch();
    }
}

/* EOF */
