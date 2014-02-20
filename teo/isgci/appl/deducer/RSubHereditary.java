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
    superType = HereditaryClass.class,
    subType = GraphClass.class
)
public class RSubHereditary extends RSub<HereditaryClass,GraphClass> {

    @Override
    protected void run(DeducerData d, HereditaryClass gc1, GraphClass gc2) {
        GraphClass gc3 = gc1.getBase();
        
        if (d.containsEdge(gc3,gc2)  && 
                gc2.getHereditariness().compareTo(
                gc1.getHereditariness()) >= 0) {
            d.addTrivialEdge(gc1, gc2,
                d.newTraceData("hereditariness", d.getEdge(gc3,gc2)));
        }
    }
}

/* EOF */

