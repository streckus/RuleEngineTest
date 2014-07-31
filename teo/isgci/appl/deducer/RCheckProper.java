/*
 * A check on the derivation results
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.io.PrintWriter;

import teo.isgci.gc.GraphClass;
import teo.isgci.relation.Inclusion;

/**
 * Proper subclasses cannot be equivalent.
 */
public class RCheckProper extends RCheck {

    /** Run at the end of the deductions process */
    public void after(DeducerData d, PrintWriter w) {
        GraphClass from, to;
        boolean err = false;
        
        StringBuffer s = new StringBuffer();

        s.append("RCheckProper\n");
        for (Inclusion e : d.getGraph().edgeSet()) {
            if (!e.isProper())
                continue;
            from = d.getGraph().getEdgeSource(e);
            to = d.getGraph().getEdgeTarget(e);
            if (d.containsEdge(to, from)) {
                err = true;
                s.append(e.toString() + "\n");
            }
        }

        if (err) {
            s.append("end RCheckProper");
            
            String all = s.toString();
            w.println(all);
            System.out.println(all);
        }
    }
}

/* EOF */

