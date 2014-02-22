/*
 * Properness over complement classes.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import teo.isgci.gc.*;
import teo.isgci.relation.*;

@RProperTyping (
    type = ComplementClass.class
)
public class RProperComplement extends RProper<ComplementClass> {

    /**
     * Run this rule on d examining only classes in classes, which contains
     * only classes of type T.
     */
    public void run(DeducerData d, Iterable<GraphClass> classes) {
        for (GraphClass v1 : classes) {
            for (GraphClass v2 : classes) {
                properFromComplement(d,
                        (ComplementClass) v1, (ComplementClass) v2);
            // Run up to equality (eq important for self-compl. classes)
            if (v1 == v2)
                continue; //FIXME: Depends on ListIterator!
            }
        }
    }


    /**
     * Given two complement classes v1, v2, try to deduce properness of
     * inclusions in all possible directions.
     * Return true iff something new was deduced.
     */
    private void properFromComplement(DeducerData d,
            ComplementClass v1, ComplementClass v2) {
        GraphClass v3 = v1.getBase();
        GraphClass v4 = v2.getBase();
        Inclusion e, f;

        if (d.containsEdge(v1, v2)  &&  (e = d.getEdge(v1, v2)) != null  &&
                e.isProper()  &&  !(f = d.getEdge(v3, v4)).isProper()) {
            d.setProper(f, d.newTraceData("properFromComplement", e));
        }
        if (d.containsEdge(v3, v4)  &&  (e = d.getEdge(v3, v4)) != null  &&
                e.isProper()  &&  !(f = d.getEdge(v1, v2)).isProper()) {
            d.setProper(f, d.newTraceData("properFromComplement", e));
        }

        if (d.containsEdge(v2, v1)  &&  (e = d.getEdge(v2, v1)) != null  &&
                e.isProper()  &&  !(f = d.getEdge(v4, v3)).isProper()) {
            d.setProper(f, d.newTraceData("properFromComplement", e));
        }
        if (d.containsEdge(v4,v3)  &&  (e = d.getEdge(v4, v3)) != null  &&
                e.isProper()  &&  !(f = d.getEdge(v2, v1)).isProper()) {
            d.setProper(f, d.newTraceData("properFromComplement", e));
        }

        if (d.containsEdge(v1, v4)  &&  (e = d.getEdge(v1, v4)) != null  &&
                e.isProper()  &&  !(f = d.getEdge(v3, v2)).isProper()) {
            d.setProper(f, d.newTraceData("properFromComplement", e));
        }
        if (d.containsEdge(v3, v2)  &&  (e = d.getEdge(v3, v2)) != null  &&
                e.isProper()  &&  !(f = d.getEdge(v1, v4)).isProper()) {
            d.setProper(f, d.newTraceData("properFromComplement", e));
        }

        if (d.containsEdge(v4, v1)  &&  (e = d.getEdge(v4, v1)) != null  &&
                e.isProper()  &&  !(f = d.getEdge(v2, v3)).isProper()) {
            d.setProper(f, d.newTraceData("properFromComplement", e));
        }
        if (d.containsEdge(v2, v3)  &&  (e = d.getEdge(v2, v3)) != null  &&
                e.isProper()  &&  !(f = d.getEdge(v4, v1)).isProper()) {
            d.setProper(f, d.newTraceData("properFromComplement", e));
        }
    }
}

/* EOF */

