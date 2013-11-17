/*
 * Unary function object (for compatibility with JGL).
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.util;


/**
 * A unary function.
 */
public interface UnaryFunction<I,O> {
    public abstract O execute(I o);
}

/* EOF */
