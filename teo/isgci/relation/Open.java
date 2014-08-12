/*
 * Two classes or parameters being in an open relation.
 * @author vector
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
 * Records an open relation between two classes or parameters.
 */
public class Open extends AbstractRelation {

    /**
     * Create a new Open relation between gc1 and gc2.
     * @param gc1
     * @param gc2
     */
    public Open(GraphClass gc1, GraphClass gc2) {
        super(gc1, gc2);
    }

    @Override
    public String toString() {
        return gc1.getID() +" open "+ gc2.getID();
    }
}

/* EOF */
