/*
 * A check on the derivation results
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import teo.isgci.grapht.*;
import teo.isgci.gc.*;
import teo.isgci.relation.*;

/**
 * Prints pairs of ForbiddenClasses that have no inclusion, but no witness
 * for this is found.
 */
public class RCheckForbiddenNonEdge extends RCheck {

    /** Run at the end of the deductions process */
    public void after(DeducerData d) {
        boolean err = false;

        System.out.println("RCheckForbiddenNonEdge");

        for (GraphClass gc1 : d.getGraph().vertexSet()) {
            if (!(gc1 instanceof ForbiddenClass))
                continue;
            for (GraphClass gc2 : d.getGraph().vertexSet()) {
                if (gc2 == gc1  ||  !(gc2 instanceof ForbiddenClass)  ||
                        d.containsEdge(gc1, gc2))
                    continue;

                StringBuilder s = new StringBuilder();
                boolean b = ((ForbiddenClass) gc2).notSubClassOf(
                        (ForbiddenClass) gc1, s);
                if (!b) {
                    err = true;
                    System.out.println("Unconfirmed non-inclusion "+
                            gc1 + " ("+ gc1.getID()+ ") -> "+
                            gc2 +" ("+ gc2.getID() +") ");
                }
            }
        }
        if (err) 
             System.out.println("end sanityCheckForbiddenNonEdge");
    }
}

/* EOF */

