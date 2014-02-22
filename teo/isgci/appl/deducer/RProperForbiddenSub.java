/*
 * Properness for probe classes
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import teo.isgci.grapht.*;
import teo.isgci.gc.*;
import teo.isgci.relation.*;

@RProperTyping (
    type = ForbiddenClass.class
)
public class RProperForbiddenSub extends RProper<ForbiddenClass> {

    /**
     * Mark A > B proper if A has a forbidden subclass that is not a subclass
     * of B, which is itself a forbidden class.
     */
    public void run(DeducerData d, Iterable<GraphClass> classes) {
        Inclusion e;

        for (GraphClass from : d.getGraph().vertexSet()) {
            if (from instanceof ForbiddenClass)
                continue;
            for (GraphClass to : classes) {
                //if (from == to) // Can't happen: to is ForbiddenClass
                    //continue;

                e = d.getEdge(from, to);
                if (e == null  ||  e.isProper()  || d.containsEdge(to, from))
                    continue;

                //---- Test all forbidden subs of from
                for (GraphClass v : GAlg.outNeighboursOf(d.getGraph(), from)) {
                    if (v == to || !(v instanceof ForbiddenClass))
                        continue;

                    if (!d.containsEdge(v, to)  &&  !d.containsEdge(to, v)) {
                        d.setProper(e,
                                d.newTraceData("properForbiddenSub "+ v));
                    }
                }
            }
        }
    }
}

/* EOF */

