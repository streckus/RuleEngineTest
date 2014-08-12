/*
 * The database of ISGCI.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl;

import java.util.*;
import org.jgrapht.Graph;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import teo.isgci.grapht.*;
import teo.isgci.xml.*;
import teo.isgci.gc.GraphClass;
import teo.isgci.parameter.GraphParameter;
import teo.isgci.parameter.PseudoClass;
import teo.isgci.problem.Problem;
import teo.isgci.util.LessLatex;
import teo.isgci.relation.*;

/**
 * The Database of the information system.
 * inclGraph is the inclusion graph proper.
 * getClassNames() returns a sorted list of all class names.
 */
public final class DataSet {

    private static boolean initialized;
    
    private static String date;
    private static String nodecount, edgecount;

    /** The inclusion graph */
    public static SimpleDirectedGraph<GraphClass,Inclusion> inclGraph;
    /** Maps classnames to nodes */
    protected static TreeMap<String,GraphClass> names;
    /** Maps graphclasses to their SCCs */
    protected static Map<GraphClass, Set<GraphClass> > sccs;

    /** Problems */
    public static Vector<Problem> problems;
    /** Parameters. */
    public static Vector<GraphParameter> parameters;

    /** Relations not in inclGraph */
    public static List<AbstractRelation> relations;

    static {
        initialized = false;
    }

    /** Load all the data.
     */
    public static void init(Resolver loader, String file) {
        if (initialized)
            return;

        inclGraph = new SimpleDirectedGraph<GraphClass,Inclusion>(
                Inclusion.class);
        problems = new Vector<Problem>();
        parameters = new Vector<GraphParameter>(); // added by vector
        load(loader, file, inclGraph, problems, parameters);

        // Sort problems
        Collections.sort(problems, new Comparator<Problem>() {
            public int compare(Problem o1, Problem o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        // Sort parameters (added by vector)
        Collections.sort(parameters, new Comparator<GraphParameter>() {
            public int compare(GraphParameter o1, GraphParameter o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        // Gather the classnames
        names = new TreeMap<String,GraphClass>(new LessLatex());
        for (GraphClass gclass : inclGraph.vertexSet())
            if (!gclass.isPseudoClass())
                names.put(gclass.toString(), gclass);

        // Gather the SCCs
        sccs = GAlg.calcSCCMap(inclGraph);

        initialized = true;
    }


    public static void load(Resolver loader, String file,
            SimpleDirectedGraph<GraphClass,Inclusion> graph,
            Vector<Problem> problems, Vector<GraphParameter> parameters) {
        // parameters added by vector
        ISGCIReader gcr = new ISGCIReader(graph, problems, parameters);
        XMLParser xml=new XMLParser(loader.openInputSource(file),
                gcr, loader.getEntityResolver());
        xml.parse();
        date = gcr.getDate();
        nodecount = gcr.getNodeCount();
        edgecount = gcr.getEdgeCount();
        relations = gcr.getRelations();
    }

    
    /**
     * Returns the names of the available graphclasses ordered alphabetically.
     */
    public static Set<String> getClassNames() {
        return Collections.unmodifiableSet(names.keySet());
    }


    /**
     * Returns the nodes of the available graphclasses ordered alphabetically.
     */
    public static Collection<GraphClass> getClasses() {
        return Collections.unmodifiableCollection(names.values());
    }


    /**
     * Return the node in inclGraph belonging to the given classname.
     */
    public static GraphClass getClass(String name) {
        return names.get(name);
    }


    /**
     * Return the set of classes equivalent to the given one.
     */
    public static Set<GraphClass> getEquivalentClasses(GraphClass gc) {
        return sccs.get(gc);
    }


    /**
     * Return the problem with the given name.
     */
    public static Problem getProblem(String name) {
        for (int i = 0; i < problems.size(); i++)
            if (name.equals( ((Problem) problems.elementAt(i)).getName() )) {
                return (Problem) problems.elementAt(i);
            }
        return null;
    }
    
    /**
     * Return the parameter with the given name.
     */
    public static GraphParameter getParameter(String name){
        for (int i = 0; i < parameters.size(); i++)
            if (name.equals(parameters.elementAt(i).getName())) {
                return parameters.elementAt(i);
            }
        return null;
    }

    public static String getDate() {
        return date;
    }        
    
    public static String getNodeCount() {
        return nodecount;
    }    
    
    public static String getEdgeCount() {
        return edgecount;
    }    
    

}

/* EOF */
