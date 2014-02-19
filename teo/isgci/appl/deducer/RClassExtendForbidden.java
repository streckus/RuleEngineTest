/*
 * A-free \cap B-free ==> (A,B)-free
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import teo.isgci.gc.*;
import teo.isgci.grapht.*;

class RClassExtendForbidden implements RClass {


    /**
     * Adds (A,B)-free for all classes A-free \cap B-free.
     * The parameter classes is ignored and instead the data is grabbed from
     * the relation graph itself.
     */
    public void run(DeducerData d, Collection<GraphClass> classes) {
        int i, k;
        Set<GraphClass> sccVec;
        boolean temp;
        Set hs1, hs2;               // candidates for replaces
        /* These two vectors contain all IntersectClasses and the set of
         * classses whose intersection makes up this IntersectClass. interHS[i]
         * starts from the definition of interGC[i], but as many elements as
         * possible will be replaced by a ForbiddenClass.
         */
        ArrayList<IntersectClass> interGC = new ArrayList<IntersectClass>();
        ArrayList<Set<GraphClass> > interHS =
                new ArrayList<Set<GraphClass> >();
        /* Contains the ForbiddenClass that we will consider replacing into
         * interHS. */
        ArrayList<ForbiddenClass> forbid = new ArrayList<ForbiddenClass>();
        Map<GraphClass,Set<GraphClass> > scc = GAlg.calcSCCMap(d.getGraph());
        HashSet hasEqForb = new HashSet();
        
        // Collect interGC, interHS and forbid
        ArrayList<GraphClass> nodes =
                new ArrayList<GraphClass>(d.getGraph().vertexSet());
        d.sortByID(nodes);
allvertices:
        for (GraphClass gc : nodes) {
            if (gc instanceof IntersectClass) {
                interGC.add((IntersectClass) gc);
                interHS.add(((IntersectClass)gc).getSet());
            } else if (gc instanceof ForbiddenClass) {
                if (hasEqForb.contains(gc))
                    continue;           // Nodes in this SCC already added
                sccVec = scc.get(gc);
                if (sccVec.size() <= 1)
                    continue;
                // Only store the nicest definitions for handling
                // Since we go through the nodes by ID and every SCC is added
                // only once, this implies that we add the nicest class, with
                // the lowest id.
                for (GraphClass gc1 : sccVec) {
                    if ( gc1 instanceof ForbiddenClass  &&
                            ((ForbiddenClass) gc1).niceness() >
                            ((ForbiddenClass) gc).niceness() )
                        continue allvertices;
                }

                forbid.add((ForbiddenClass) gc);
                hasEqForb.addAll(sccVec);
            }
        }
        
        /* Replace classes with their forbidden-equivs
         * If there are several forbidden-equivs we use only one!
         */
        for (ForbiddenClass gc : forbid) {
            sccVec = scc.get(gc);
            for (GraphClass gc1 : sccVec) {
                if (gc1 == gc)
                    continue;
                if (gc1 instanceof IntersectClass) {
                    hs1 = ((IntersectClass)gc1).getSet();
                } else {
                    hs1 = new HashSet();
                    hs1.add(gc1);
                }
                for (k = 0; k < interHS.size(); k++) {
                    hs2 = interHS.get(k);
                    if (hs2.containsAll(hs1)) {
                        hs2 = new HashSet(hs2);
                        hs2.removeAll(hs1);
                        hs2.add(gc);
                        interHS.set(k,hs2);
                    }
                }
            }
        }

        //System.out.println("extend and equivs");
        // melt forbidden
        for(i = 0; i < interHS.size(); i++) {
            GraphClass gc = interGC.get(i);
            temp = d.isTempNode(gc)  ||  hasEqForb.contains(gc);
            eF(d, interHS.get(i), gc, temp);
        }
    }

    
    /** Extend the graph DAG by the equivalence between the class ic and the
     * intersection class of icSet. In particular are those members of icSet
     * that are characterized by forbidden subgraphs merged to produce a
     * single, new list of forbidden subgraphs for ic.
     * @param ic IntersectClass
     * @param icSet the GraphClasses that insersect to make up ic
     * @param temp should a possible new GraphClass be created temporary (true)
     * or trivial (false)?
     * @see IntersectClass GraphClass
     */
    private void eF(DeducerData d, Set<GraphClass> icSet, GraphClass ic,
            boolean temp){
        //System.out.println("hash: "+icSet);
        //System.out.println("class: "+ic);
        GraphClass v;
        int cntF = 0;
        HashSet fb = new HashSet();       // Set of forbidden subgraphs
        // Non-forbidden GraphClasses
        HashSet<GraphClass> gcs = new HashSet<GraphClass>();

        // Divide icSet into forbiddens (fb) and others (gcs)
        for (GraphClass gc : icSet) {
            if (gc instanceof ForbiddenClass) {
                fb.addAll(((ForbiddenClass)gc).getSet());
                cntF++;
            } else
                gcs.add(gc);
        }
        
        if (cntF<2)                      // nothing to melt with up to 1 class
            return;
        // found >=2 forbidden classes that are melted, so create new node
        if (temp)
            v = d.ensureTempNode(new ForbiddenClass(fb));
        else
            v = d.ensureTrivialNode(new ForbiddenClass(fb));
        
        if (!gcs.isEmpty()) {            // if other classes were found
            gcs.add(v);
            if (temp)
                v = d.ensureTempNode(new IntersectClass(gcs));
            else
                v = d.ensureTrivialNode(new IntersectClass(gcs));
        }
        d.addTrivialEdge(ic, v, d.newTraceData("extendForbidden"));
        d.addTrivialEdge(v, ic, d.newTraceData("extendForbidden"));
    }


}

/* EOF */
