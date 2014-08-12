/*
 * Either a SimpleAlgorithm or a CoAlgorithm.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.problem;

public abstract class Algorithm extends AbstractAlgorithm {
    // Abstracted by vector to support GraphParameters
    public abstract Problem getProblem();
    @Override
    public AbstractProblem getAbstractProblem() {
        return getProblem();
    }
    public abstract Complexity getComplexity();
    @Override
    public AbstractComplexity getAbstractComplexity() {
        return getComplexity();
    }
    public abstract String getTimeBounds();
}

/* EOF */
