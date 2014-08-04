/*
 * A check on the derivation results
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.io.PrintWriter;
import java.util.Collection;

import teo.isgci.gc.GraphClass;
import teo.isgci.grapht.GAlg;
import teo.isgci.relation.AbstractRelation;
import teo.isgci.relation.Disjointness;
import teo.isgci.relation.Incomparability;
import teo.isgci.util.Itera;
import teo.isgci.util.Iterators;

/**
 * Check consistency for disjoint/incomparable relations.
 */
public class RCheckAbstractRelations {

    /** Run at the end of the deductions process */
	public void after(DeducerData d,
            Collection<AbstractRelation> relations, PrintWriter w) {
        boolean err = false;
        StringBuffer s = new StringBuffer();

        s.append("# RCheckAbstractRelations\n");
        for (AbstractRelation r : relations) {
            if (r instanceof Incomparability) {
                if (d.containsEdge(r.get1(), r.get2())  ||
                        d.containsEdge(r.get2(), r.get1())) {
                    err = true;
                    s.append(r + " : Inclusion\n");
                }
                continue;
            }

            if (!(r instanceof Disjointness))
                throw new RuntimeException("Unknown relation"+ r);

            for (GraphClass gc1 : new Itera<GraphClass>(Iterators.union(
                    Iterators.singleton(r.get1()),
                    GAlg.outNeighboursOf(d.getGraph(), r.get1()))))
                for (GraphClass gc2 : new Itera<GraphClass>(Iterators.union(
                        Iterators.singleton(r.get2()),
                        GAlg.outNeighboursOf(d.getGraph(), r.get2())))) {

                    if (d.containsEdge(gc1, gc2)) {
                        err = true;
                        s.append(r + " : Inclusion "+ d.getEdge(gc1, gc2) + "\n");
                    }
                    if (d.containsEdge(gc2, gc1)) {
                        err = true;
                        s.append(r + " : Inclusion "+ d.getEdge(gc2, gc1) + "\n");
                    }
                    if (gc1.getHereditariness() == GraphClass.Hered.INDUCED &&
                           gc2.getHereditariness()==GraphClass.Hered.INDUCED) {
                        err = true;
                        s.append(r + " : Induced-hereditary subclasses "+
                                gc1 +" "+ gc2 + "\n");
                    }
                }
        }
        if (err) {
            s.append("# end RCheckAbstractRelations\n");
            String all = s.toString();
            
            System.out.println(all);
            w.println(all);
        }
    }
}

/* EOF */

