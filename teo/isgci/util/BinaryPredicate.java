/*
 * Binary predicate object (for compatibility with JGL).
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.util;


/**
 * A Binary predicate.
 */
public interface BinaryPredicate {
    public abstract boolean execute(Object a, Object b);
}

/* EOF */
