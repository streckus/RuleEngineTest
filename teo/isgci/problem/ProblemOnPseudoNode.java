/*
 * Stores the algorithms of a problem on a parameter.
 * @author vector
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.problem;

import java.util.HashSet;

import teo.isgci.parameter.PseudoClass;

/**
 * Stores the algorithms and deduced complexities on a parameter. Complexities
 * are deduced in multiple steps, see the class Problem.
 */
public class ProblemOnPseudoNode {
    protected static final int STEPS = Problem.STEPS;
    /* The problem. */
    protected Problem problem;
    /* The node. */
    protected PseudoClass node;
    /* The algorithms. */
    protected HashSet<ParamAlgorithm> algos;
    /* The complexities, as deduced in the different steps. */
    protected ParamComplexity[] complexity;

    /**
     * Create a new ProblemOnPseudoNode.
     * @param p the Problem to solve
     * @param n the PseudoClass to solve it on
     */
    ProblemOnPseudoNode(Problem p, PseudoClass n) {
        if (!p.validFor(n))
            throw new IllegalArgumentException(p.getName()
                    + " not applicable to " + n.getID());
        problem = p;
        node = n;
        algos = new HashSet<ParamAlgorithm>();
        complexity = new ParamComplexity[STEPS];
        for (int i = 0; i < complexity.length; i++)
            complexity[i] = ParamComplexity.UNKNOWN;
    }

    /**
     * Update the complexity of this at the given step by distilling it with c.
     * @param c the Complexity to set
     * @param step the step to set it for
     */
    protected void updateComplexity(ParamComplexity c, int step)
            throws ComplexityClashException {
        c = c.distil(complexity[step]);
        for (; step < complexity.length; step++)
            complexity[step] = c;
    }

    /**
     * Return the complexity at the given step.
     * @param step the step to get the complexity for
     * @return the comeplexity derived in the given step 
     */
    ParamComplexity getComplexity(int step) {
        return complexity[step];
    }

    /**
     * Add an algorithm at the given deduction step.
     * @param a the Algorithm to add
     * @param step the step to set it for
     */
    void addAlgo(ParamAlgorithm a, int step) {
        algos.add(a);
        try {
            updateComplexity(a.getComplexity(), step);
        } catch (ComplexityClashException e) {
            System.err.println("Complexity clash for " + problem.getName()
                    + " on " + node + " " + a + " and " + algos);
        }

    }

    /**
     * @return the algorithms defined on this node.
     */
    HashSet<ParamAlgorithm> getAlgoSet() {
        return algos;
    }
}

/* EOF */
