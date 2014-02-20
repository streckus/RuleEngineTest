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
import teo.isgci.relation.*;

@RSubTyping (
    superType = ProbeClass.class,
    subType = ProbeClass.class
)
public class RSubProbe extends RSub<ProbeClass,ProbeClass> {

    @Override
    protected void run(DeducerData d, ProbeClass gc1, ProbeClass gc2) {
        GraphClass gc3 = gc1.getBase();
        GraphClass gc4 = gc2.getBase();
        
        if (d.containsEdge(gc3,gc4)) {
            d.addTrivialEdge(gc1, gc2,
                d.newTraceData("probeclass", d.getEdge(gc3,gc4)));
        }
    }
}

/* EOF */

