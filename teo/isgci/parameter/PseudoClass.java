/*
 * An Pseudo-GraphClass to store and draw the hierarchy between parameters.
 * @author vector
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */
package teo.isgci.parameter;

import teo.isgci.gc.GraphClass;

/**
 * A PseudoClass can be stored and drawn in a hierarchy like a GraphClass, but
 * represents a GraphParameter.
 */
public class PseudoClass extends GraphClass {

    /* The GraphParameter this Class represents. */
    private GraphParameter parameter;

    /**
     * Create a new PseudoClass for a given Parameter. Should only be called
     * when creating a new parameter.
     * @param parameter
     *            the parameter this Class represents.
     */
    PseudoClass(GraphParameter parameter) {
        super(Directed.PARAMETER);
        this.parameter = parameter;
        name = parameter.getName();
        id = parameter.getID();
    }

    /**
     * The name of this class is set to the name of the Parameter in the
     * constructor.
     */
    public void setName() {
    }

    @Override
    public void setName(String s) {
        throw new UnsupportedOperationException(
                "Attempt to change name of PseudoClass");
    }

    @Override
    public void setID(Integer id) {
        throw new UnsupportedOperationException(
                "Attempt to change ID of PseudoClass");
    }

    /**
     * Get the corresponding Parameter of this class.
     * @return the parameter this class represents.
     */
    public GraphParameter getParameter() {
        return parameter;
    }

    /**
     * Two Pseudoclasses are equal iff their corresponding GraphParameters are
     * equal.
     * @param obj the obj to compare this with
     */
    public boolean equals(Object obj) {
        if (obj instanceof PseudoClass)
            return this.parameter.equals(((PseudoClass) obj).parameter);
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see teo.isgci.gc.GraphClass#calcHash()
     */
    @Override
    protected int calcHash() {
        return name.hashCode();
    }

}

/* EOF */
