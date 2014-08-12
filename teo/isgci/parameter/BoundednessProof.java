/*
 * Boundedness proof for a parameter on a particular graphclass. Works like
 * Algorithm for Problems
 * @author vector
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.parameter;

import java.util.List;

import teo.isgci.gc.GraphClass;
import teo.isgci.problem.AbstractAlgorithm;
import teo.isgci.problem.AbstractComplexity;
import teo.isgci.problem.AbstractProblem;

/**
 * Stores the boundedness for some parameter.
 */
public class BoundednessProof extends AbstractAlgorithm {
    /* The parameter that this proofs boundedness for. */
    protected GraphParameter parameter;
    /* Boundedness. */
    protected Boundedness boundedness;
    /* References. */
    protected List refs;
    /* On which graphclass was this proof defined. */
    protected GraphClass gc;

    /**
     * Create a new proof with given data.
     * @param parameter the parameter to prove boundedness for
     * @param gc the graphclass on which the boundedness is defined
     * @param boundedness the boundedness-value (bounded or unbounded)
     * @param refs references for this proof
     */
    BoundednessProof(GraphParameter parameter, GraphClass gc,
            Boundedness boundedness, List refs) {
        this.parameter = parameter;
        this.boundedness = boundedness;
        this.refs = refs;
        if (gc != null && gc.isPseudoClass())
            throw new IllegalArgumentException(
                    "Parameter defined on a parameter pseudoclass.");
        this.gc = gc;
    }

    /**
     * Set the GraphParameter for this proof.
     * @param parameter the GraphParameter to set
     */
    public void setParameter(GraphParameter parameter) {
        this.parameter = parameter;
    }

    /**
     * @return the parameter this is a proof for
     */
    public GraphParameter getParameter() {
        return parameter;
    }

    /**
     * Set the boundedness-value for this proof.
     * @param boundedness the Boundedness to set
     */
    public void setBoundedness(Boundedness boundedness) {
        this.boundedness = boundedness;
    }

    /**
     * @return the boundedness this is a proof for
     */
    public Boundedness getBoundedness() {
        return boundedness;
    }

    @Override
    public AbstractProblem getAbstractProblem() {
        return parameter;
    }

    @Override
    public AbstractComplexity getAbstractComplexity() {
        return boundedness;
    }

    @Override
    public List getRefs() {
        return refs;
    }

    /**
     * Set references for this proof.
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
     * Set the GraphClass for this proof. Throw an exception if this proof is
     * defined on a PseudoClass.
     * @param gc the GraphClass this proof is defined on.
     */
    public void setGraphClass(GraphClass gc) {
        if (gc.isPseudoClass())
            throw new IllegalArgumentException(
                    "Parameter defined on a parameter pseudoclass.");
        this.gc = gc;
    }

    @Override
    public String toString() {
        return "{" + (parameter != null ? parameter.getName() : "(null)")
                + " "
                + (boundedness != null ? boundedness.toString() : "(null)")
                + " on " + (gc != null ? gc.getID() : "(null)") + "}";
    }
}
