/*
 * An algorithm, parameterized algorithm or boundedness proof.
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

/**
 * An abstract interface to share common methods between different types of
 * algorithms.
 */
public abstract class AbstractAlgorithm {
    /**
     * @return the Problem or GraphParameter this AbstractAlgorithm is defined
     *         on
     */
    public abstract AbstractProblem getAbstractProblem();

    /**
     * @return the AbstractComplexity of this AbstractAlgorithm.
     */
    public abstract AbstractComplexity getAbstractComplexity();

    /**
     * @return the references for this AbstractAlgorithm
     */
    public abstract List getRefs();

    /**
     * @return the GraphClass this AbstractAlgorithm is definded on.
     */
    public abstract GraphClass getGraphClass();
}

/* EOF */
