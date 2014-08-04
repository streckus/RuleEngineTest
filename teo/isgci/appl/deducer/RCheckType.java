/*
 * A check on the derivation results
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.io.PrintWriter;

import teo.isgci.relation.Inclusion;

/**
 * Check that we have no relations between directed and undirected classes.
 */
public class RCheckType extends RCheck {

    /** Run at the end of the deductions process */
    public void after(DeducerData d, PrintWriter w) {
        boolean err = false;
        StringBuffer s = new StringBuffer();

        s.append("# RCheckType\n");
        for (Inclusion e : d.getGraph().edgeSet()) {
            if (e.getSuper().getDirected() != e.getSub().getDirected()) {
                err = true;
                s.append(e + "\n");
            }
        }

        if (err) {
            s.append("# end RCheckType\n");
            String all = s.toString();
            
            System.out.println(all);
            w.println(all);
        }
    }
}

/* EOF */

