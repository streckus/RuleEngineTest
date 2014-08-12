/*
 * A complexity class.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.problem;

import java.awt.Color;

/**
 * Represents complexity classes like Linear, P, NPC, coNPC and Unknown.
 * Use only the defined comparison methods, the enum compareTo does not give a
 * complexity comparison.
 * The betterThan and betterOrEqual compare as LIN < P < GIC < NPC,NPH,CONPC.
 * betterThan with other complexities is undefined.
 */
public enum Complexity implements AbstractComplexity {
    /** Higher complexity, higher number */
    LINEAR ("Linear",        "Lin", Color.green),
    P      ("Polynomial",    "P", Color.green.darker()),
    GIC    ("GI-complete",   "GIC", new Color(255, 100, 100)),
    NPC    ("NP-complete",   "NPC", Color.red),
    NPH    ("NP-hard",       "NPh", Color.red),
    CONPC  ("coNP-complete", "coNPC", Color.red),
    OPEN   ("Open",          "Open", Color.white),
    UNKNOWN("Unknown",       "?", Color.white);


    /** Complexity class */
    protected String name;
    protected String abbrev;
    protected Color defaultColor;

    /**
     * Creates a new complexity with the given value and names
     */
    private Complexity(String name, String abbrev, Color color) {
        this.name = name;
        this.abbrev = abbrev;
        this.defaultColor = color;
    }


    public String getShortString() {
        return abbrev;
    }

    public String getComplexityString() {
        return name;
    }

    public boolean betterThan(Complexity c) {
        return compareTo(c) < 0;
    }

    public boolean betterOrEqual(Complexity c) {
        return compareTo(c) <= 0;
    }
    
    /**
     * Gets the default color of the complexity.
     * @return
     *          The default color.
     */
    public Color getDefaultColor() {
        return defaultColor;
    }

    public boolean isUnknown() {
        return this == UNKNOWN  ||  this == OPEN;
    }

    public boolean isOpen() {
        return this == OPEN;
    }

    public boolean isNPC() {
        return this == NPC;
    }

    public boolean isCONPC() {
        return this == CONPC;
    }

    public boolean likelyNotP() {
        return this == CONPC  || this == NPC  ||  this == NPH  ||  this == GIC;
    }

    public String toString() {
        return name;
    }

    /**
     * Can a problem at the same time have this complexity and c's?
     */
    public boolean isCompatible(Complexity c) {
        return this == UNKNOWN  ||  c == UNKNOWN  ||
                (betterOrEqual(P)  &&  c.betterOrEqual(P))  ||
                equals(c);
    }

    /**
     * Can a problem at the same time have this complexity and c's
     * parameterized complexity?
     * @param c
     *            the parameterized complexity to check compatibility with.
     * @return true, iff c and this are compatible.
     * @author vector
     */
    public boolean isCompatible(ParamComplexity c) {
        // To prevent errors in later changes of compatibilities, this function
        // is implemented only once.
        return c.isCompatible(this);
    }

    // changes in visibility needed for abstraction (changed by vector)
    @Override
    public boolean distributesDown() {
        return betterOrEqual(P);
    }

    @Override
    public boolean distributesUp() {
        return likelyNotP();
    }

    @Override
    public boolean distributesEqual() {
        return isOpen();
    }


    /*
     * If a problem has both complexity this and c, return the resulting
     * complexity.
     */
    public Complexity distil(Complexity c) throws ComplexityClashException {
        if (!isCompatible(c))
            throw new ComplexityClashException(this, c);
        if (c.isUnknown())
            return this;
        if (c.betterThan(this)  ||  this.isUnknown())
            return c;
        return this;
    }


    /**
     * Return the complexity class represented by s.
     */
    public static Complexity getComplexity(String s) {
        for (Complexity c : Complexity.values())
            if (c.name.equals(s)  ||  c.abbrev.equals(s))
                return c;
        // vector removed Bounded and Unbounded since Parameters are now
        // imported as GraphParameters.
        // if (s.equals("Bounded"))
        //      return Complexity.LINEAR;
        // if (s.equals("Unbounded"))
        //      return Complexity.NPC;
        throw new IllegalArgumentException(s);
    }
}

/* EOF */
