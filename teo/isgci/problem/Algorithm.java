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

import java.util.Iterator;
import java.util.List;
import teo.isgci.gc.GraphClass;

public abstract class Algorithm {
    public abstract Problem getProblem();
    public abstract Complexity getComplexity();
    public abstract String getTimeBounds();
    public abstract List getRefs();
    public abstract GraphClass getGraphClass();
}

/* EOF */
