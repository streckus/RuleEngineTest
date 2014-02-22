/*
 * Defines a properness determining rule.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import teo.isgci.gc.*;

/**
 * A properness rule. Rules are stateless and the run method may be
 * called multiple times with the same or different arguments.
 * T is the type of the graphclass this rule needs and
 * it should be specified in an RProperTyping annotation.
 */
public abstract class RProper<T extends GraphClass> {

    /**
     * Run this rule on d examining only classes in classes, which contains
     * only classes of type T.
     */
    public void run(DeducerData d, Iterable<GraphClass> classes) {}
}

/* EOF */

