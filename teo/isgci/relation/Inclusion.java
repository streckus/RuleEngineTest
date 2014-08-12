/*
 * Stores the information on inclusions.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.relation;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import teo.isgci.gc.GraphClass;
import teo.isgci.parameter.PseudoClass;

/**
 * Represents an inclusion in the system. For efficiency of the graph
 * structure, equals and hashCode should NOT be overridden!
 */
public class Inclusion extends org.jgrapht.graph.DefaultEdge
        implements Relation {
    /**
     * Relations between parameter PseudoClasses can have one of these
     * complexities.
     * @author vector
     */
    public enum Functiontype {
        LOGARITHMIC("logarithmic", Color.green.darker()),
        LINEAR("linear", Color.green),
        POLYNOMIAL("polynomial", Color.yellow),
        EXPONENTIAL("exponential", Color.red),
        ANY("any", Color.black);

        /* Functiontype */
        private String name;
        private Color defaultColor;

        /**
         * Create a new Functiontype.
         * @param n the name of the functiontype
         * @param color the default color for the functiontype
         */
        private Functiontype(String n, Color color) {
            this.name = n;
            this.defaultColor = color;
        }

        /**
         * Get the functiontype represented by s.
         * @param s the String to get a functiontype for
         * @return the resulting functiontype
         */
        public static Functiontype getFunctiontype(String s) {
            // Handle null and SQL-"NULL" the same.
            if (s == null || s.equals("NULL"))
                return ANY;
            for (Functiontype f : Functiontype.values())
                if (f.name.equals(s))
                    return f;
            throw new IllegalArgumentException(s);
        }

        /**
         * Is this better than f?
         * @param f the functiontype to compare with
         * @return true, iff this is better
         */
        public boolean betterThan(Functiontype f) {
            return compareTo(f) < 0;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * @return the default color for this functiontype (for drawing colored
         *         edges)
         */
        public Color getDefaultColor() {
            return defaultColor;
        }
    };

    private boolean isProper;       // True if this incl is proper
    private RelationData rel;
    private Functiontype functype;

    public Inclusion() {
        isProper = false;
        rel = new RelationData();
    }

    /*public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof Inclusion))
            return false;

        Inclusion o = (Inclusion) other;
        return superClass.equals(o.superClass) && subClass.equals(o.subClass);
    }

    public int hashCode() {
        return superClass.hashCode() + subClass.hashCode();
    }*/

    public String toString() {
        if (getSource() instanceof PseudoClass)
            return getSuper() +" >= "+ getSub();

        if (getSource() instanceof GraphClass)
            return getSuper().getID() +" -> "+ getSub().getID();
        return super.toString();
    }

    public GraphClass getSuper() {
        return (GraphClass) getSource();
    }

    public GraphClass getSub() {
        return (GraphClass) getTarget();
    }

    public boolean isProper() {
        return isProper;
    }
    
    public void setProper(boolean b) {
        isProper = b;
    }

    public int getConfidence() {
        return rel.getConfidence();
    }

    public void setConfidence(int c) {
        rel.setConfidence(c);
    }

    /**
     * Set the functiontype of this to s.
     * @param s a String representing the functiontype
     * @author vector
     */
    public void setFunctiontype(String s) {
        if (!getSuper().isPseudoClass())
            throw new UnsupportedOperationException(
                    "Functiontype may only be defined for "
                            + "Inclusions between parameter PseudoClasses.");
        this.functype = Functiontype.getFunctiontype(s);
    }

    /**
     * Set the functiontype of this to s.
     * @param f the functiontype to set.
     * @author vector
     */
    public void setFunctiontype(Functiontype f) {
        if (!getSuper().isPseudoClass())
            throw new UnsupportedOperationException(
                    "Functiontype may only be defined for "
                            + "Inclusions between parameter PseudoClasses.");
        this.functype = f;
    }

    /**
     * @return the functiontype of this parameter-inclusion
     */
    public Functiontype getFunctiontype() {
        if (this.functype != null)
            return this.functype;
        else if (getSuper().isPseudoClass())
            return Functiontype.ANY;
        else
            throw new UnsupportedOperationException(
                    "Functiontype is only defined for "
                            + "Inclusions between parameter PseudoClasses.");
    }

    public void setRefs(List v) {
        rel.setRefs(v);
    }

    public void addRef(Object ref) {
        rel.addRef(ref);
    }

    public List getRefs() {
        return rel.getRefs();
    }

}

/* EOF */
