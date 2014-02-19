/*
 * Stores and accesses the data during the deducing process.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import org.jgrapht.*;
import teo.isgci.gc.*;
import teo.isgci.relation.*;

public interface DeducerData {

    /**
     * Create new tracedata, if the trace flag is set.
     */
    public TraceData newTraceData(String desc, Inclusion... is);

    /**
     * Return the graph on which we're deducing.
     */
    public DirectedGraph<GraphClass,Inclusion> getGraph();


    /**
     * Sort the given list of nodes by id.
     */
    public void sortByID(List<GraphClass> list);


    /**
     * Add a trivially deduced inclusion.
     */
    public Inclusion addTrivialEdge(GraphClass from, GraphClass to,
            TraceData tr);

    /**
     * Return true if gc is a temporary node.
     */
    public boolean isTempNode(GraphClass gc);

    /**
     * If a node for graphclass gc exists, return it, otherwise add it as a
     * temporary node.
     * @param gc GraphClass to ensure a node for
     */
    public GraphClass ensureTempNode(GraphClass gc);


    /**
     * If a node for graphclass gc exists, return it, otherwise add it as a
     * trivial node.
     * @param gc GraphClass to ensure a node for
     */
    public GraphClass ensureTrivialNode(GraphClass gc);
}

/* EOF */
