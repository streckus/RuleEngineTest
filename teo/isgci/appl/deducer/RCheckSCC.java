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
import teo.isgci.grapht.*;
import teo.isgci.gc.*;

/**
 * Print SCC that have merged as a result of deducing inclusions.
 */
public class RCheckSCC extends RCheck {
    private Map<GraphClass,Set<GraphClass> > sccBefore, sccAfter;

    /** Run at the beginning of the deductions process */
    public void before(DeducerData d) {
        sccBefore = GAlg.calcSCCMap(d.getGraph());
    }


    /** Run at the end of the deductions process */
    public void after(DeducerData d) {
        sccAfter = GAlg.calcSCCMap(d.getGraph());
        sanityCheckSCC(d, sccBefore, sccAfter);
    }


    /**
     * Print SCC that have merged as a result of deducing inclusions.
     */
    private void sanityCheckSCC(DeducerData d,
            Map<GraphClass,Set<GraphClass> > before,
            Map<GraphClass,Set<GraphClass> > after) {
        System.out.println("RCheckSCC");
        Set<GraphClass> vecBefore1, vecAfter1, vecBefore2, vecAfter2;
        // Maps after-SCC to beforeSCCs
        HashMap<Set<GraphClass>, Set<Set<GraphClass> > > scc =
                new HashMap<Set<GraphClass>, Set<Set<GraphClass> > >();
        HashSet<Set<GraphClass> > hs;

        for (GraphClass node1 : d.getGraph().vertexSet()) {
            if (before.get(node1) == null)
                continue;
            vecBefore1 = before.get(node1);
            vecAfter1 = after.get(node1);

            for (GraphClass node2 : d.getGraph().vertexSet()) {
                if (before.get(node2) == null)
                    continue;
                vecBefore2 = before.get(node2);
                vecAfter2 = after.get(node2);

                if (vecBefore1 != vecBefore2  &&  vecAfter1 == vecAfter2) {
                    if (scc.containsKey(vecAfter1))
                        scc.get(vecAfter1).add(vecBefore1);
                    else {
                        hs = new HashSet<Set<GraphClass> >();
                        hs.add(vecBefore1);
                        scc.put(vecAfter1, hs);
                    }
                    
                    if (scc.containsKey(vecAfter2))
                        scc.get(vecAfter2).add(vecBefore1);
                    else {
                        hs = new HashSet<Set<GraphClass> >();
                        hs.add(vecBefore1);
                        scc.put(vecAfter2, hs);
                    }
                }
            }
        }

        if (scc.isEmpty())
            return;

        for (Map.Entry<Set<GraphClass>, Set<Set<GraphClass> > > entry :
                scc.entrySet()) {
            vecAfter1 = entry.getKey();
            System.out.println("sccBefore: ");
            for (Set<GraphClass> bfor : entry.getValue()) {
                System.out.print("[");
                for (GraphClass gc : bfor)
                    System.out.print( gc.getID() +" ("+ gc.toString() +"), ");
                System.out.println("]");
            }
            System.out.print("sccAfter:\n[");
            for (GraphClass gc : vecAfter1)
                System.out.print( gc.getID() +" ("+ gc.toString() +"), ");
            System.out.println("]");
        }
        System.out.println("end RCheckSCC");
    }
}

/* EOF */

