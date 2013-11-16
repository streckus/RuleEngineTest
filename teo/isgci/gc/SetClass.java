/*
 * A GraphClass defined by intersection/union of two or more other
 * GraphClasses.
 *
 * $Header: /home/ux/CVSROOT/teo/teo/isgci/gc/SetClass.java,v 1.4 2013/09/12 14:31:13 ux Exp $
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */


package teo.isgci.gc;

import java.util.Arrays;
import java.util.Set;
import teo.isgci.util.LessLatex;


/**
 * A GraphClass defined by intersection/union of two or more other
 * GraphClasses.
 */
public abstract class SetClass extends GraphClass {
    
    /** Contains the base classes. */
    private Set<GraphClass> gcSet;

    public SetClass(Directed d) {
        super(d);
    }


    /**
     * Sets gcSet to an unmodifiable view of the given set.
     */
    protected final void setSet(Set<GraphClass> set){
        if (gcSet != null)
            throw new UnsupportedOperationException("Attempt to modify set");
        for (GraphClass gc : set)
            if (gc.getDirected() != getDirected())
                throw new IllegalArgumentException(gc.getID() +
                        " wrongly directed for "+ this.getID());

        gcSet = java.util.Collections.unmodifiableSet(set);
    }


    /**
     * Return the set of GraphClasses this is defined upon.
     * We don't need to copy since gcSet is already unmodifiable.
     */
    public final Set<GraphClass> getSet() {
        return gcSet;
    }


    /**
     * Constructs the name of this graphclass by connecting the names of
     * its base-classes with infix (e.g. \cap or \cup).
     */
    protected void buildName(String infix) {
        StringBuilder nm = new StringBuilder();
        String[] gcNames = new String[gcSet.size()];
        int i = 0;

        for (GraphClass gc : gcSet)
            gcNames[i++] = gc.toString();  // forces gc.setName() if necessary

        Arrays.sort(gcNames, new LessLatex());
       
        for (i = 0; i < gcNames.length; i++) {
            nm.append(gcNames[i]);
            if (i < gcNames.length-1)
                nm.append(infix);
        }

        name = nm.toString();
        nameExplicit = false;
    }
        
    
    /**
     * Returns <tt>true</tt> if <tt>obj</tt> is of the same class as this
     * and constructed from equal classes.
     */
    public boolean equals(Object obj){
        if (obj == this)
            return true;
        if(obj != null  &&  obj.getClass() == getClass()) {
            return hashCode() == obj.hashCode()  &&
                    gcSet.equals(((SetClass) obj).gcSet);
        }
        return false;
    }

    public int calcHash() {
        return gcSet.hashCode();
    }
    
}
/* EOF */
