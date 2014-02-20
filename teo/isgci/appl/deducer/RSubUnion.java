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
    superType = GraphClass.class,
    subType = UnionClass.class
)
public class RSubUnion extends RSub<GraphClass,UnionClass> {

    /**
     * Can union rules be used to deduce v1 >> v2?
     * gc1 >> gc2 = union(gc...) if gc1 >> gc, for all gc
     * Add an edge accordingly.
     */
    @Override
    protected void run(DeducerData d, GraphClass gc1, UnionClass gc2) {
        //System.err.println(gc1.getID() +" >> "+ gc2.getID());
        Set<GraphClass> hs2;
        ArrayList<Inclusion> traces = new ArrayList<Inclusion>();

        hs2 = new HashSet<GraphClass>(((UnionClass)gc2).getSet());
        if (gc1 instanceof UnionClass)
            hs2.removeAll(((UnionClass)gc1).getSet());
        else
            hs2.remove(gc1);
        
        //---- Check that each gc in hs2 is a subclass of v1 ----
        
        // for this part it's important that direct trivial inclusions
        // have already been found
        for (GraphClass gc : hs2) {
            if (!d.containsEdge(gc1,gc))
                return;
            traces.add(d.getEdge(gc1,gc));
        }
        d.addTrivialEdge(gc1, gc2,
                d.newTraceData("union", traces.toArray(new Inclusion[0])));
    }
}

/* EOF */

