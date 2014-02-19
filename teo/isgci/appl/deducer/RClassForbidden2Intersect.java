/*
 * (A,B)-free ==> A-free \cap B-free
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import teo.isgci.gc.*;

class RClassForbidden2Intersect implements RClass {

    /**
     * Create an intersection of forbidden subgraph-classes for every
     * forbidden subgraph-class that allows so.
     */
    public void run(DeducerData d, Collection<GraphClass> classes) {
        for (GraphClass gc : classes) {
            if (/*!temporaries.contains(gc) &&*/
                    (gc instanceof ForbiddenClass)) {
                if (((ForbiddenClass) gc).getSet().size() >= 2)
                    addForbiddenSuper(d, (ForbiddenClass) gc);
                else
                    addForbiddenSuperConfig(d, (ForbiddenClass) gc);
            }
        }
    }

    /**
     * For a node (A,B,..)-free add nodes A-free, B-free, ... and A-free \cap
     * B-free \cap ...
     * @param node ForbiddenClass node
     */
    private void addForbiddenSuper(DeducerData d, ForbiddenClass node) {
        HashSet one;
        GraphClass sup;
        HashSet supers = new HashSet();
        TraceData tr = d.newTraceData("addForbiddenSuper");

        for (Object forb : node.getSet()) {
            one = new HashSet();
            one.add(forb);
            sup = d.ensureTempNode(new ForbiddenClass(one));
            addForbiddenSuperConfig(d, (ForbiddenClass) sup);
            supers.add(sup);
            d.addTrivialEdge(sup, node, tr);
        }

        sup = d.ensureTempNode(new IntersectClass(supers));
        d.addTrivialEdge(sup, node, tr);
        d.addTrivialEdge(node, sup, tr);
    }


    /**
     * For a node config-free add nodes config-1-free, config-2-free, ...
     * and config-1-free \cap config-2-free \cap ...
     * @param node ForbiddenClass node
     */
    private void addForbiddenSuperConfig(DeducerData d, ForbiddenClass node) {
        HashSet<String> one;
        GraphClass sup;
        HashSet<GraphClass> supers = new HashSet<GraphClass>();
        TraceData tr = d.newTraceData("addForbiddenSuperConfig");
        Set<String> isgSet = node.getConfigContains();

        if (isgSet.size() == 0) 
            return;

        if (isgSet.size() == 1) {
            one = new HashSet<String>();
            one.add(isgSet.iterator().next());
            sup = d.ensureTempNode(new ForbiddenClass(one));
        } else {
            for (String s : isgSet) {
                one = new HashSet<String>();
                one.add(s);
                sup = d.ensureTempNode(new ForbiddenClass(one));
                supers.add(sup);
                d.addTrivialEdge(sup, node, tr);
            }
            sup = d.ensureTempNode(new IntersectClass(supers));
        }
        d.addTrivialEdge(sup, node, tr);
        d.addTrivialEdge(node, sup, tr);
    }

}

/* EOF */
