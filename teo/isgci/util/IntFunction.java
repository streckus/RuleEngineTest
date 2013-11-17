/*
 * Unary function object that returns an int.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.util;


/**
 * A unary int-function.
 */
public interface IntFunction<I> {
    public abstract int execute(I o);
}

/* EOF */
