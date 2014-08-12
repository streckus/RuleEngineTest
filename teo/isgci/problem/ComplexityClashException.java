/*
 * An exception for problems that seem to be both P and NPC
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.problem;

public class ComplexityClashException extends RuntimeException {
    public ComplexityClashException(String s) {
        super(s);
    }
    // changed to AbstractComplexity by vector
    public ComplexityClashException(AbstractComplexity a, AbstractComplexity b) {
        super(a.toString() +" /\\ "+ b.toString());
    }
}

/* EOF */
