/*
 * Defines a super sub relation determining rule.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import teo.isgci.gc.*;

@RSubTyping (
    superType = ComplementClass.class,
    subType = GraphClass.class
)
public class RSubComplement extends RSub<ComplementClass,GraphClass> {

    public void run(DeducerData d,
            Iterable<GraphClass> supers, Iterable<GraphClass> subs) {
        for (GraphClass gi : supers)
            for (GraphClass gj : supers) {  // subs is not used!
                run(d, (ComplementClass) gi, (ComplementClass) gj);
                // Run up to equality (eq important for self-compl. classes)
                if (gi == gj)
                    break;
            }
    }


    protected void run(DeducerData d,
            ComplementClass gc1, ComplementClass gc2) {
        GraphClass gc3 = gc1.getBase();
        GraphClass gc4 = gc2.getBase();

        if (d.containsEdge(gc1, gc2))
            d.addTrivialEdge(gc3, gc4,
                d.newTraceData("complement", d.getEdge(gc1, gc2)));
        if (d.containsEdge(gc3, gc4))
            d.addTrivialEdge(gc1, gc2,
                d.newTraceData("complement", d.getEdge(gc3, gc4)));

        if (d.containsEdge(gc2, gc1))
            d.addTrivialEdge(gc4, gc3,
                d.newTraceData("complement", d.getEdge(gc2, gc1)));
        if (d.containsEdge(gc4, gc3))
            d.addTrivialEdge(gc2, gc1,
                d.newTraceData("complement", d.getEdge(gc4, gc3)));

        if (d.containsEdge(gc1, gc4))
            d.addTrivialEdge(gc3, gc2,
                d.newTraceData("complement", d.getEdge(gc1, gc4)));
        if (d.containsEdge(gc3, gc2))
            d.addTrivialEdge(gc1, gc4,
                d.newTraceData("complement", d.getEdge(gc3, gc2)));

        if (d.containsEdge(gc4, gc1))
            d.addTrivialEdge(gc2, gc3,
                d.newTraceData("complement", d.getEdge(gc4, gc1)));
        if (d.containsEdge(gc2, gc3))
            d.addTrivialEdge(gc4, gc1,
                d.newTraceData("complement", d.getEdge(gc2, gc3)));
    }
}

/* EOF */

