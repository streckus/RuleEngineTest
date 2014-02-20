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
    superType = IntersectClass.class,
    subType = GraphClass.class
)
public class RSubIntersect extends RSub<IntersectClass,GraphClass> {

    /**
     * Can intersection rules be used to deduce v1 >> v2?
     * gc1 = intersect(gc...) >> gc2 if gc >> gc1, for all gc
     * Add an edge accordingly.
     */
    @Override
    protected void run(DeducerData d, IntersectClass gc1, GraphClass gc2) {
        Set<GraphClass> hs1;
        ArrayList<Inclusion> traces = new ArrayList<Inclusion>();

        hs1 = new HashSet<GraphClass>(gc1.getSet());
        if (gc2 instanceof IntersectClass)
            hs1.removeAll(((IntersectClass)gc2).getSet());
        else
            hs1.remove(gc2);
        
        
        //---- Check that each gc in hs1 is a superclass of v2 ----
        
        // for this part it's important that direct trivial inlcusions
        // have already been found
        for (GraphClass gc : hs1) {
            if (!d.containsEdge(gc,gc2))
                return;
            traces.add(d.getEdge(gc,gc2));
        }
        d.addTrivialEdge(gc1, gc2,
                d.newTraceData("intersect", traces.toArray(new Inclusion[0])));
    }
}

/* EOF */

