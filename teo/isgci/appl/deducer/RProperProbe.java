/*
 * Properness for probe classes
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
    type = ProbeClass.class
)
public class RProperProbe extends RProper<ProbeClass> {

    /* Mark probe X < probe Y if X < Y, unless we already know otherwise. */
    public void run(DeducerData d, Iterable<GraphClass> classes) {
        for (GraphClass x : classes) {
            for (GraphClass y : classes) {
                if (x == y)
                    continue;
                if (d.containsEdge(x, y))
                    properFromProbe(d, (ProbeClass) x, (ProbeClass) y);
            }
        }
    }


    /**
     * Mark probe X < probe Y if X < Y, unless we already know otherwise.
     */
    private void properFromProbe(DeducerData d, ProbeClass x, ProbeClass y){
        GraphClass basex = x.getBase();
        GraphClass basey = y.getBase();
        Inclusion e = d.getEdge(x, y);
        Inclusion basee = d.getEdge(basex, basey);

        if (!e.isProper()  &&  !d.containsEdge(y, x)  &&
                basee != null  &&  basee.isProper()) {
            d.setProper(e, d.newTraceData("ProperFromProbe", basee));
        }
    }
}

/* EOF */

