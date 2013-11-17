/*
 * Some relation between two classes.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.db;

import teo.isgci.gc.GraphClass;

/**
 * Records a relation between two classes. The one with lowest id (String
 * compareTo) is stored in gc1, the other one in gc2.
 */
public abstract class AbstractRelation extends RelationData {
    protected GraphClass gc1, gc2;

    public AbstractRelation(GraphClass gc1, GraphClass gc2) {
        if (gc1.getDirected() != gc2.getDirected())
            throw new IllegalArgumentException("Unmatched directedness "+
                    gc1.getID() +" "+ gc2.getID());
        if (gc1.getID().compareTo(gc2.getID()) <= 0) {
            this.gc1 = gc1;
            this.gc2 = gc2;
        } else {
            this.gc1 = gc2;
            this.gc2 = gc1;
        }
    }

    public GraphClass get1() {
        return gc1;
    }

    public GraphClass get2() {
        return gc2;
    }
}

/* EOF */
