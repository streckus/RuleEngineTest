/*
 * A check on the derivation results
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import teo.isgci.grapht.*;
import teo.isgci.gc.*;
import teo.isgci.relation.*;
import teo.isgci.util.*;

/**
 * Check consistency for disjoint/incomparable relations.
 */
public class RCheckAbstractRelations /*extends RCheck*/ {

    /** Run at the end of the deductions process */
    public void after(DeducerData d,
            Collection<AbstractRelation> relations) {
        boolean err = false;

        System.out.println("RCheckAbstractRelations");
        for (AbstractRelation r : relations) {
            if (r instanceof Incomparability) {
                if (d.containsEdge(r.get1(), r.get2())  ||
                        d.containsEdge(r.get2(), r.get1())) {
                    err = true;
                    System.out.println("Inclusion exists for "+ r);
                }
                continue;
            }

            // Support for NotBounds and Open added by vector
            if (!(r instanceof Disjointness) && !(r instanceof NotBounds)
                    && !(r instanceof Open))
                throw new RuntimeException("Unknown relation"+ r);

            if (!(r instanceof NotBounds))
                for (GraphClass gc1 : new Itera<GraphClass>(Iterators.union(
                        Iterators.singleton(r.get1()),
                        GAlg.outNeighboursOf(d.getGraph(), r.get1()))))
                    for (GraphClass gc2 : new Itera<GraphClass>(Iterators.union(
                            Iterators.singleton(r.get2()),
                            GAlg.outNeighboursOf(d.getGraph(), r.get2())))) {

                        if (d.containsEdge(gc1, gc2)) {
                            err = true;
                            System.out.println("Inclusion "+ d.getEdge(gc1, gc2)
                                    + " exists for "+ r);
                        }
                        if (d.containsEdge(gc2, gc1)) {
                            err = true;
                            System.out.println("Inclusion "+ d.getEdge(gc2, gc1)
                                    + " exists for " + r);
                        }
                        if (gc1.getHereditariness()==GraphClass.Hered.INDUCED &&
                            gc2.getHereditariness()==GraphClass.Hered.INDUCED) {
                            err = true;
                            System.out
                                    .println("Induced-hereditary subclasses "+
                                            gc1 +" "+ gc2 +" exists for "+ r);
                        }
                    }
        }
        if (err)
            System.out.println("end RCheckAbstractRelations");
    }
}

/* EOF */

