/*
 * Algorithm for a problem on a particular graphparameter.
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

import teo.isgci.gc.GraphClass;
import teo.isgci.parameter.PseudoClass;

/**
 * Stores the complexity of some parameterized Algorithm on a parameter
 * PseudoClass.
 */
public class ParamAlgorithm extends AbstractAlgorithm {
    /* The problem that this algorithm solves. */
    protected Problem problem;
    /* The parameterized complexity class of this algorithm. */
    protected ParamComplexity complexity;
    /* Time bounds (O("something")). */
    protected String timeBounds;
    /* References. */
    protected List refs;
    /* On which parameter PseudoClass was this algorithm defined? */
    protected PseudoClass gc;

    /**
     * Create a new ParameAlgorithm
     * @param problem the Problem this algorithm solve
     * @param gc the PseudoClass this algorithm is defined on
     * @param complexity the parmeterized complexity class of this algorithm
     * @param bounds the time bounds of this algorithm
     * @param refs the references for this algorithm
     */
    ParamAlgorithm(Problem problem, PseudoClass gc,
            ParamComplexity complexity, String bounds, List refs) {
        this.problem = problem;
        this.complexity = complexity;
        timeBounds = bounds;
        this.refs = refs;
        this.gc = gc;
    }

    /**
     * @return the problem this algorithm solves
     */
    public Problem getProblem() {
        return problem;
    }

    /**
     * Set the complexity of this algorithm.
     * @param complexity the ParamComplexity to set
     */
    public void setComplexity(ParamComplexity complexity) {
        this.complexity = complexity;
    }

    /**
    * Set the complexity of this algorithm.
    * @param s a String representing the ParamComplexity to set
    */
    public void setComplexity(String s) {
        complexity = ParamComplexity.getComplexity(s);
    }

    /**
     * @return the parameterized complexity class of this algorithm
     */
    public ParamComplexity getComplexity() {
        return complexity;
    }

    /**
     * Set the timebounds of this algorithm
     * @param bounds the timebounds to set
     */
    public void setTimeBounds(String bounds) {
        timeBounds = bounds;
    }

    /**
     * @return the timebounds of this algorithm
     */
    public String getTimeBounds() {
        return timeBounds;
    }

    @Override
    public AbstractProblem getAbstractProblem() {
        return problem;
    }

    @Override
    public AbstractComplexity getAbstractComplexity() {
        return complexity;
    }

    @Override
    public List getRefs() {
        return refs;
    }

    /**
     * Set the references for this algorithm
     * @param v the references to set
     */
    public void setRefs(List v) {
        refs = v;
    }

    @Override
    public GraphClass getGraphClass() {
        return gc;
    }

    /**
     * Since an ParamAlgortihm is always defined on a parameter PseudoClass, we
     * can get it as such.
     * @return the PseudoClass this algorithm is defined on
     */
    public PseudoClass getPseudoClass() {
        return gc;
    }

    /**
     * Set the PseudoClass for this algorithm
     * @param gc the PseudoClass to set
     */
    public void setPseudoClass(PseudoClass gc) {
        this.gc = gc;
    }

    @Override
    public String toString() {
        return "{" + (problem != null ? problem.getName() : "(null)") + " "
                + (complexity != null ? complexity.toString() : "(null)")
                + (timeBounds != null ? "[" + timeBounds + "]" : "") + " on "
                + (gc != null ? gc : "(null)") + "}";
    }
}
