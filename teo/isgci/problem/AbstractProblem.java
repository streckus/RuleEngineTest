/*
 * Either a Problem or a GraphParameter.
 * @author vector
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.problem;

import java.util.List;

import org.jgrapht.DirectedGraph;

import teo.isgci.gc.GraphClass;
import teo.isgci.parameter.GraphParameter;
import teo.isgci.relation.Inclusion;

/**
 * Stores the information about a graph problem or a graph parameter.
 */
public abstract class AbstractProblem {

    /* The name of this AbstractProblem */
    protected String name;
    /*
     * If directed == null, this problem is applicable to both directed and
     * undirected graphs, otherwise only to the set type.
     */
    protected GraphClass.Directed directed;
    /* Inclusion graph */
    protected DirectedGraph<GraphClass, Inclusion> graph;
    /* References for this problem */
    List refs;

    /**
     * Create a new AbstractProblem for all graphclasses
     * @param name the name of the AbstractProblem
     * @param g the inclusiongraph this is defined on
     */
    protected AbstractProblem(String name,
            DirectedGraph<GraphClass, Inclusion> g) {
        this(name, g, null);
    }

    /**
     * Create an abstract problem.
     * @param name
     *            the name of the problem/parameter ("Independent set")
     * @param g
     *            the graph of classes for which the problem/parameter exists
     * @param directed
     *            to which graphs the problem applies (null for all)
     */
    protected AbstractProblem(String name,
            DirectedGraph<GraphClass, Inclusion> g,
            GraphClass.Directed directed) {
        this.name = name;
        this.graph = g;
        this.directed = directed;
    }

    /**
     * @return the name of this AbstractProblem
     */
    public String getName() {
        return name;
    }

    /**
     * Set to which graphs (directed/undirected/both==null) this problem
     * applies.
     * @param d the directedness.
     */
    public abstract void setDirected(GraphClass.Directed d);

    /**
     * @return true iff this problem is applicable to directed graphs.
     */
    public boolean forDirected() {
        return directed == null || directed == GraphClass.Directed.DIRECTED;
    }

    /**
     * @return true iff this problem is applicable to undirected graphs.
     */
    public boolean forUndirected() {
        return directed == null || directed == GraphClass.Directed.UNDIRECTED;
    }

    /**
     * @return true iff this problem is applicable to the given class.
     */
    public boolean validFor(GraphClass gc) {
        if (gc.isDirected() && forDirected())
            return true;
        if (gc.isUndirected() && forUndirected())
            return true;
        return false;
    }

    /**
     * Set the refs for this AbstractProblem
     * @param refs the refs to add
     */
    public void setRefs(List refs) {
        this.refs = refs;
    }

    /**
     * @return the refs for this AbstractProblem
     */
    public List getRefs() {
        return refs;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Return a string representation of the given Complexity or Boundedness.
     * The string chosen depends on whether this problem is a parameter or not.
     * @param c the AbstractComplexity to get a String for
     * @return the String representation of c
     */
    public String getComplexityString(AbstractComplexity c) {
        return c.getComplexityString();
    }

    // ================ Static variables for Deduction process ===============
    /* Whether we are doing deductions. */
    protected static boolean deducing;
    /* The parameters. */
    protected static List<GraphParameter> parameters;
    /* The problems. */
    protected static List<Problem> problems;

    /**
     * Distribute/deduce the algorithms, complexities, boundedness proofs and
     * boundedness values. Deductions are done interleaved for problems and
     * graphparameters.
     */
    public static void distributeAbstractComplexities() {
        Problem.distributeComplexitiesBasic();
        GraphParameter.distributeBoundednessBasic();
        Problem.distributeComplexitiesBasic();
        GraphParameter.distributeBoundednessBasic();

        Problem.distributeComplexities();
        GraphParameter.distributeBoundednesses();

        Problem.distributeComplexitiesBasic();
        GraphParameter.distributeBoundednessBasic();
        Problem.distributeComplexitiesBasic();
        GraphParameter.distributeBoundednessBasic();
    }
}

/* EOF */
