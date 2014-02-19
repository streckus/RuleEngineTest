/*
 * Relations between complements and forbidden/union/intersect.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import teo.isgci.gc.*;
import teo.isgci.grapht.*;

class RClassComplements implements RClass {

    public void run(DeducerData d, Collection<GraphClass> classes) {
        // X ==> co-X for forbidden and self-compl
        addComplements1(d, classes);
        // co-(A-free) ==> (co-A)-free
        correspondCompAndForbidden(d, classes);
        // For A\c.p B add co-(A\c.p B) <==> co-A \c.p co-B
        addComplements2(d);
    }


    /**
     * For every node (A,B,..)-free add temporary node co-((A,B,..)-free)
     * For every self-complementary node X add temporary node co-X
     */
    private void addComplements1(DeducerData d,
            Collection<GraphClass> classes) {
        GraphClass con;

        for (GraphClass gc : classes) {
            if (gc instanceof ForbiddenClass) {
                d.ensureTempNode(new ComplementClass(gc));
            } else if (gc.isSelfComplementary()) {
                con = d.ensureTempNode(new ComplementClass(gc));
                d.addTrivialEdge(gc,con, d.newTraceData("self-complementary"));
                d.addTrivialEdge(con,gc, d.newTraceData("self-complementary"));
            }
        }
    }


    /**
     * For every node co-((A,B,..)-free) add trivial equal node
     * (co-A,co-B,..)-free
     */
    private void correspondCompAndForbidden(
            DeducerData d, Collection<GraphClass> classes) {
        TraceData tr = d.newTraceData("correspondCompAndForbidden");
        GraphClass gc2, gc3;
        for (GraphClass gc1 : classes) {
            if (gc1 instanceof ComplementClass) {
                gc2 = ((ComplementClass) gc1).getBase();
                if (gc2 instanceof ForbiddenClass) {
                    if ( d.isTempNode(gc2) )
                        gc3 = d.ensureTempNode(
                                ((ForbiddenClass)gc2).complement());
                    else
                        gc3 = d.ensureTrivialNode(
                                ((ForbiddenClass)gc2).complement());
                    d.addTrivialEdge(gc1, gc3, tr);
                    d.addTrivialEdge(gc3, gc1, tr);
                }
            }
        }
    }


    /**
     * For every node A\c.p B such that co-A and co-B exists, add temporary
     * node co-(A\c.p B) and co-A\c.p co-B.
     */
    private void addComplements2(DeducerData d) {
        ArrayList<GraphClass> classes =
                new ArrayList<GraphClass>(d.getGraph().vertexSet());
        d.sortByID(classes);
        for (GraphClass gc : classes)
            if (gc instanceof UnionClass  ||  gc instanceof IntersectClass)
                doAddComplements2(d, gc);
    }


    /**
     * For a node A\c.p B check whether co-A and co-B exists, and, if so,
     * add temporary nodes co-(A\c.p B) and co-A\c.p co-B.
     * Return true if the graph could be handled successfully.
     */
    private boolean doAddComplements2(DeducerData d, GraphClass gc) {
        GraphClass gcco, cogc;
        Set<GraphClass> parts = null;
        TraceData tr = d.newTraceData("addComplements2");

        //---- Try to create complement
        cogc = gc.complement();
        if (cogc instanceof SetClass)
            parts = ((SetClass) cogc).getSet();
        else
            throw new IllegalArgumentException("Wrong classtype "+ gc);
        for (GraphClass p : parts) {
            if (!d.getGraph().containsVertex(p))
                return false;
        }

        gcco = d.ensureTempNode(new ComplementClass(gc));
        cogc = d.ensureTempNode(cogc);
        d.addTrivialEdge(cogc, gcco, tr);
        d.addTrivialEdge(gcco, cogc, tr);
        return true;
    }
    

    /**
     * For all non-temporary nodes (co-A,...)-free create an equivalent class
     * co-X, if (A,...)-free has a non temporary basic equivalent class X.
     * 
     * We actually start at co-((A,..)-free) and from there find (co-A,..)-free
     * and X.
     *
     * N.B. This method is currently not used.
     * N.B.2. It also creates complements for self-complementary classes.
     */
    /*private void correspondCompAndForbidden2(Vector classes) {
        TraceData tr = trace ?
                new TraceData("correspondCompAndForbidden2") : null;
        ISGCINode node1, node;
        GraphClass gc1, gc2, basic;
        Vector equ1, equ2;
        boolean ok1, ok2;
        int i;
        int scc = findSCC();

        Enumeration eenum = classes.elements();
nextnode:
        while (eenum.hasMoreElements()) {

            //---- Find gc1 = co-(A-free), gc2 = A-free
            node1 = (ISGCINode) eenum.nextElement();
            gc1 = node1.getGraphClass();
            if (!(gc1 instanceof ComplementClass))
                continue;
            gc2 = ((ComplementClass) gc1).getBase();
            if (!(gc2 instanceof ForbiddenClass))
                continue;

            //System.out.println("found "+ gc1 +" co of: "+ gc2);
            
            //---- Check that gc1 has non-temp equivs
            equ1 = (Vector) node1.getData(scc);
            for (i = equ1.size()-1; i >= 0; i--) {
                if ( ((Node) equ1.elementAt(i)).getData(tempIx) != temporary)
                    break;
                if (i == 0)
                    continue nextnode;
            }

            //System.out.println("gc1 ok");
            //---- Go through basic equivs of gc2 (basic implies non-temp)
            equ2 = (Vector) ((Node) hash.get(gc2)).getData(scc);
            for (i = equ2.size()-1; i >= 0; i--) {
                basic = ((ISGCINode) equ2.elementAt(i)).getGraphClass();
                if ( basic.getClass() != BaseClass.class)
                    continue;
                //System.out.println("gc2 ok: "+ basic);
                node = ensureTrivialNode(basic.complement());
                addTrivialEdge(node1, node, tr);
                addTrivialEdge(node, node1, tr);
            }
        }
        releaseData(scc);
    }*/

}

/* EOF */
