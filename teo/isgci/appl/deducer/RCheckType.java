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
import teo.isgci.util.*;

/**
 * Check that we have no relations between directed and undirected classes.
 */
public class RCheckType extends RCheck {

    /** Run at the end of the deductions process */
    public void after(DeducerData d) {
        boolean err = false;
        System.out.println("RCheckType");
        for (Inclusion e : d.getGraph().edgeSet()) {
            if (e.getSuper().getDirected() != e.getSub().getDirected()) {
                err = true;
                System.out.println(e);
            }
        }

        if (err)
            System.out.println("end RCheckType");
    }
}

/* EOF */

