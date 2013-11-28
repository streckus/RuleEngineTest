/*
 * Two classes being disjoint.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.relation;

import teo.isgci.gc.GraphClass;

/**
 * Records disjointness of two classes.
 */
public class Disjointness extends AbstractRelation {
    public Disjointness(GraphClass gc1, GraphClass gc2) {
        super(gc1, gc2);
    }

    public String toString() {
        return gc1.getID() +" 0 "+ gc2.getID();
    }
}

/* EOF */
