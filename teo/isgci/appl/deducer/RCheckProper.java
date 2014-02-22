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
 * Proper subclasses cannot be equivalent.
 */
public class RCheckProper extends RCheck {

    /** Run at the end of the deductions process */
    public void after(DeducerData d) {
        GraphClass from, to;
        boolean err = false;

        System.out.println("RCheckProper");
        for (Inclusion e : d.getGraph().edgeSet()) {
            if (!e.isProper())
                continue;
            from = d.getGraph().getEdgeSource(e);
            to = d.getGraph().getEdgeTarget(e);
            if (d.containsEdge(to, from)) {
                err = true;
                System.out.println(e);
            }
        }

        if (err)
            System.out.println("end RCheckProper");
    }
}

/* EOF */

