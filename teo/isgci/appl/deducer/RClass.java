/*
 * Defines a class creating rule.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.util.*;
import teo.isgci.gc.*;

/**
 * A class creating rule. Rules are stateless and the run method may be called
 * multiple times with teh same or different arguments.
 */
public interface RClass {
    /** Run this rule on d examining classes. */
    public void run(DeducerData d, Collection<GraphClass> classes);
}

/* EOF */

