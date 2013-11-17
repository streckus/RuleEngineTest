/*
 * Two classes being incomparable.
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
 * Records incomparability of two classes.
 */
public class Incomparability extends AbstractRelation {

    public Incomparability(GraphClass gc1, GraphClass gc2) {
        super(gc1, gc2);
    }

    public String toString() {
        return gc1.getID() +" ~ "+ gc2.getID();
    }
}

/* EOF */
