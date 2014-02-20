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

/** Called separately, therefore no @RSubTyping */
public class RSubDirect extends RSub<GraphClass,GraphClass> {

    TraceData tr;
    
    public void run(DeducerData d,
            Iterable<GraphClass> supers, Iterable<GraphClass> subs) {
        tr = d.newTraceData("direct");  // Save memory by reusing it
        super.run(d, supers, subs);
    }


    protected void run(DeducerData d, GraphClass gc1, GraphClass gc2) {
        if (gc2.subClassOf(gc1))
            d.addTrivialEdge(gc1, gc2, tr);
    }

}

/* EOF */

