/*
 * X ==> clique X for clique fixed
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.Collection;

import teo.isgci.gc.CliqueClass;
import teo.isgci.gc.GraphClass;
import teo.isgci.relation.Inclusion;

class RClassCliqueFixed implements RClass {

    /**
     * For every clique-fixed node X add temporary node clique X
     */
    public void run(DeducerData d, Collection<GraphClass> classes) {
        GraphClass con;
        Inclusion i;

        for (GraphClass gc : classes) {
            if (gc.isCliqueFixed()) {
                con = d.ensureTempNode(new CliqueClass(gc));
                i = d.addTrivialEdge(gc,con, d.newTraceData("clique-fixed"));
                if (i != null)
                	i.setConfirmed(gc.getConfirmed());
                i = d.addTrivialEdge(con,gc, d.newTraceData("clique-fixed"));
                if (i != null)
                	i.setConfirmed(gc.getConfirmed());
            }
        }
    }
}

/* EOF */
