/*
 * Directly derived properness.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import teo.isgci.gc.*;
import teo.isgci.relation.*;

/* No typing annotation because it is called separately */
public class RProperDirect extends RProper<GraphClass> {

    /**
     * Run this rule on d examining only classes in classes, which contains
     * only classes of type T.
     */
    public void run(DeducerData d, Iterable<GraphClass> classes) {
        ArrayList<ForbiddenClass> forbids = new ArrayList<ForbiddenClass>();
        ArrayList<ProbeClass> probes = new ArrayList<ProbeClass>();
        ArrayList<CliqueClass> cliques = new ArrayList<CliqueClass>();
        ArrayList<HereditaryClass> hereds = new ArrayList<HereditaryClass>();

        //---- Gather classes according to type
        for (GraphClass gi : classes) {
            if (gi instanceof ForbiddenClass)
                forbids.add((ForbiddenClass) gi);
            else if (gi instanceof CliqueClass)
                cliques.add((CliqueClass) gi);
            else if (gi instanceof ProbeClass)
                probes.add((ProbeClass) gi);
            else if (gi instanceof HereditaryClass)
                hereds.add((HereditaryClass) gi);

        }

        properForbiddenDirect(d, forbids);
        properCliqueDirect(d, cliques);
        properProbeDirect(d, probes);
        properHereditaryDirect(d, hereds);
    }


    /**
     * Mark inclusions between forbidden classes as proper if their definition
     * warrants so.
     */
    private void properForbiddenDirect(DeducerData d,
            Iterable<ForbiddenClass> classes) {
        for (ForbiddenClass from : classes) {
            for (ForbiddenClass to : classes) {
                if (from == to)
                    continue;
                Inclusion e = d.getEdge(from, to);
                if (e != null  &&  !d.containsEdge(to, from)) {
                    d.setProper(e, d.newTraceData("properForbiddenDirect"));
                }
            }
        }
    }


    /**
     * Mark clique X < clique unless we already know otherwise.
     */
    private void properCliqueDirect(DeducerData d,
            Iterable<CliqueClass> cliques) {
        //---- Find the "clique" class
        GraphClass clique = null;
        for (GraphClass gc : d.getGraph().vertexSet()) {
            if (1128==gc.getID()) { //intID
                if (!"clique graphs".equals(gc.toString())) // Safety check
                    throw new RuntimeException(
                            "gc_141 expected to be clique graphs");
                clique = gc;
                break;
            }
        }

        for (CliqueClass vi : cliques) {
            if (!d.containsEdge(vi, clique))
                d.setProper(d.getEdge(clique, vi),
                        d.newTraceData("properCliqueDirect"));
        }
    }


    /**
     * Mark X < probe X unless we already know otherwise.
     */
    private void properProbeDirect(DeducerData d, Iterable<ProbeClass> probes){
        GraphClass basei;

        for (ProbeClass vi : probes) {
            basei = vi.getBase();
            if (!d.containsEdge(basei, vi))
                d.setProper(d.getEdge(vi, basei),
                        d.newTraceData("properProbeDirect"));
        }
    }


    /**
     * Mark hereditary X < X unless we already know otherwise.
     */
    private void properHereditaryDirect(DeducerData d,
            Iterable<HereditaryClass> hereds) {
        GraphClass basei;

        for (HereditaryClass vi : hereds) {
            basei = vi.getBase();
            if (!d.containsEdge(vi, basei))
                d.setProper(d.getEdge(basei, vi),
                        d.newTraceData("properHereditaryDirect"));
        }
    }
}

/* EOF */

