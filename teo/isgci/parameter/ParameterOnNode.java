/*
 * Stores the boundedness proofs of a parameter on a node.
 * @author vector
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.parameter;

import java.util.HashSet;

import teo.isgci.gc.GraphClass;
import teo.isgci.problem.ComplexityClashException;

/**
 * Stores the boundedness proofs and deduced boundedness values on a node.
 * Boundedness values are deduced in multiple steps, see the class Parameter.
 */
public class ParameterOnNode {
    protected static final int STEPS = GraphParameter.STEPS;
    /* The parameter. */
    protected GraphParameter parameter;
    /* The node. */
    protected GraphClass node;
    /* The boundedness proofs. */
    protected HashSet<BoundednessProof> proofs;
    /* The boundedness values, as deduces in the different steps. */
    protected Boundedness[] boundedness;

    /**
     * Create a new ParameterOnNode with given data
     * @param p the GraphParameter for which this is defined
     * @param n the GraphClass this stores boundedness information for
     */
    ParameterOnNode(GraphParameter p, GraphClass n) {
        if (!p.validFor(n))
            throw new IllegalArgumentException(p.getName()
                    + " not applicable to " + n.getID());
        parameter = p;
        node = n;
        proofs = new HashSet<BoundednessProof>();
        boundedness = new Boundedness[STEPS];
        for (int i = 0; i < boundedness.length; i++)
            boundedness[i] = Boundedness.UNKNOWN;
    }

    /**
     * Update the boundedness of this at the given step by distilling b.
     * @param b the boundedness to update to
     * @param step the current deduction step
     */
    protected void updateBoundedness(Boundedness b, int step)
            throws ComplexityClashException {
        b = b.distil(boundedness[step]);
        for (; step < boundedness.length; step++)
            boundedness[step] = b;
    }

    /**
     * Return the boundedness at the given step.
     * @param step the step to get the derived boundedness for
     * @return the boundedness at the given step
     */
    Boundedness getBoundedness(int step) {
        return boundedness[step];
    }

    /**
     * Add a proof at the given deduction step.
     * @param b the proof to add
     * @parem step the deduction step to set the proof for
     */
    void addProof(BoundednessProof b, int step) {
        proofs.add(b);
        try {
            updateBoundedness(b.getBoundedness(), step);
        } catch (ComplexityClashException e) {
            System.err.println("Complexity clash for " + parameter.getName()
                    + " on " + node + " " + b + " and " + proofs);
        }
    }

    /**
     * Return the proofs defined on this node.
     * @return a set of proofs for this ParameterOnNode
     */
    HashSet<BoundednessProof> getProofSet() {
        return proofs;
    }
}

/* EOF */
