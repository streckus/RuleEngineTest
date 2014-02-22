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
import teo.isgci.grapht.*;
import teo.isgci.gc.*;
import teo.isgci.relation.*;

/**
 * Check whether for every pair (from, to) in the list there is a path in
 * g. Failures are printed.
 */
public class RCheckReachability extends RCheck {
    Collection<Inclusion> edges;

    /** Run at the beginning of the deductions process */
    public void before(DeducerData d) {
        edges = new ArrayList<Inclusion>(d.getGraph().edgeSet());
    }

    /** Run at the end of the deductions process */
    public void after(DeducerData d) {
        System.out.println("RCheckReachability");
        boolean err = false;

        for (Inclusion e :  edges) {
            if (GAlg.getPath(d.getGraph(), e.getSuper(), e.getSub()) == null) {
                err = true;
                System.out.println(e);
            }
        }

        if (err)
            System.out.println("end RCheckReachability");
    }
}

/* EOF */

