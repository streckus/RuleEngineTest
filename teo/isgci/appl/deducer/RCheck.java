/*
 * A check on the derivation results
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.io.PrintWriter;

/**
 * A check on the deduction results. Errors found are printed to System.out.
 * before/after need not both be implemented.
 */
public abstract class RCheck {
    /** Run at the beginning of the deductions process */
    public void before(DeducerData d, PrintWriter w) {}
    /** Run at the end of the deductions process */
    public void after(DeducerData d, PrintWriter w) {}
}

/* EOF */

