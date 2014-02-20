/*
 * Defines a super sub relation determining rule.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import teo.isgci.gc.*;

/**
 * A subclass relation rule. Rules are stateless and the run method may be
 * called multiple times with the same or different arguments.
 * TP is the type of the superclass this rule needs, and TB the subclass. Their
 * classes should be specified in an RSubTyping annotation.
 */
public abstract class RSub<TP extends GraphClass, TB extends GraphClass> {

    /**
     * Run this rule on d examining every pair from supers, subs.
     * supers, sub are assumed to contain only TP and TB, resp.!
     * By default, run is only examined for unequal pairs that have no edge
     * super->sub already.
     */
    public void run(DeducerData d,
            Iterable<GraphClass> supers, Iterable<GraphClass> subs) {
        for (GraphClass gi : supers) {
            for (GraphClass gj : subs) {
                //System.err.println(gi.getID() +" >?> "+ gj.getID());
                if (gi == gj)
                    continue;
                if (!d.containsEdge(gi, gj))
                    run(d, (TP) gi, (TB) gj);
            }
        }
    }

    /**
     * Run this rule for super gc1 and sub gc2.
     * Note that a rule may add more edges than just gc1->gc2
     */
    protected void run(DeducerData d, TP gc1, TB gc2) {}
}

/* EOF */

