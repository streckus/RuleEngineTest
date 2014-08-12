/*
 * A parameter does not bound another.
 * @author vector
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.relation;

import teo.isgci.parameter.PseudoClass;

/**
 * The Relation "bounds" (<=) between parameters will be realized as Inclusion
 * between parameter pseudoclasses. For not <= this relation is needed.
 */
public class NotBounds extends AbstractRelation {
    /**
     * Create a new par1 not >= par2.
     * @param par1 the first parameter-PseudoClass
     * @param par2 the second parameter-PseudoClass
     */
    public NotBounds(PseudoClass par1, PseudoClass par2) {
        super(par1, par2);
        gc1 = par1;
        gc2 = par2;
    }

    @Override
    public String toString() {
        return gc1 + " not >= " + gc2;
    }
}

/* EOF */
