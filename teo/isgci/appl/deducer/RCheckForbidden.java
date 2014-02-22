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

/**
 * Prints inclusions between ForbiddenClasses that were derived, but can't
 * be confirmed by ForbiddenClass.subClassOf.
 */
public class RCheckForbidden extends RCheck {


    /** Run at the end of the deductions process */
    public void after(DeducerData d) {
        System.out.println("RCheckForbidden");

        boolean err = false;
        GraphClass from, to;
        HashSet<GraphClass> hs = new HashSet<GraphClass>();
        DirectedGraph<GraphClass,Inclusion> inducedSub;
        
        for (Inclusion e : d.getGraph().edgeSet()) {
            from = d.getGraph().getEdgeSource(e);
            to = d.getGraph().getEdgeTarget(e);
            if (from instanceof ForbiddenClass && to instanceof ForbiddenClass)
                if (!to.subClassOf(from)) {
                    hs.add(from);
                    hs.add(to);
                }
        }
        if (hs.isEmpty())
            return;
            
        //TODO
        /*inducedSub = (ISGCIGraph) createSubgraph(hs.elements());
        inducedSub.contractSCC();
        inducedSub.transitiveReduction();*/
        inducedSub = new SimpleDirectedGraph<GraphClass,Inclusion>(
                Inclusion.class);
        GAlg.copyInduced(d.getGraph(), hs, inducedSub);
        
        for (Inclusion e : inducedSub.edgeSet()) {
            from = inducedSub.getEdgeSource(e);
            to = inducedSub.getEdgeTarget(e);
            if (!to.subClassOf(from)) {
                err = true;
                System.out.println(from +" ("+ from.getID()+ ") -> "+
                    to +" ("+ to.getID() +") ");
            }
        }
        if (err)
            System.out.println("end RCheckForbidden");
    }
}

/* EOF */

