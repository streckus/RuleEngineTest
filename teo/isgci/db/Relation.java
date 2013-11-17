/*
 * Specifies standard stuff for relations.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.db;

import java.util.List;

public interface Relation {
    /** The confidence levels for relations */
    public static final int CONFIDENCE_HIGHEST = 0;
    public static final int CONFIDENCE_REVIEWED = 0;
    //public static final int CONFIDENCE_UNREVIEWED = -1;
    public static final int CONFIDENCE_UNPUBLISHED = -1;
    public static final int CONFIDENCE_LOWEST = -1;

    public int getConfidence();
    public void setConfidence(int c);
    public void setRefs(List v);
    public void addRef(Object ref);
    public List getRefs();
}

/* EOF */
