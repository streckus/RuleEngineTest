/*
 * A parameterized complexity class.
 * @author vector
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
 * Represents parameterized complexity classes like FPT, XP, W-hard, paraNPC
 * and paraNPH. Use only the defined comparison methods, the enum compareTo
 * does not give a complexity comparison. The betterThan and betterOrEqual
 * compare as FTP-Linear,FPT < XP < W-hard < paraNPC,paraNPH. betterThan with
 * other complexities is undefined.
 */
public enum ParamComplexity implements AbstractComplexity {
    /** Higher complexity, higher number. */
    FPTLIN("FPT-Linear", "FPT-lin", Color.green),
    FPT("Fixed-Parameter-Tractable", "FPT", Color.green.darker()),
    XP("Exponential", "XP", Color.yellow),
    WH("W-hard", "Wh", Color.red),
    PARANPC("paraNP-complete", "paraNPC", Color.red.darker()),
    PARANPH("paraNP-hard", "paraNPh", Color.red.darker()),
    OPEN("Open", "Open", Color.white),
    UNKNOWN("Unknown", "?", Color.white);

    /* Complexity class. */
    protected String name;
    protected String abbrev;
    protected Color defaultColor;

    /**
     * Creates a new complexity with the given value and names.
     * @param name
     *            the name of the complexity.
     * @param abbrev
     *            an abbreviation for name.
     * @param color
     *            the default color for this complexity
     */
    private ParamComplexity(final String name, final String abbrev,
            final Color color) {
        this.name = name;
        this.abbrev = abbrev;
        this.defaultColor = color;
    }

    @Override
    public String getShortString() {
        return abbrev;
    }

    @Override
    public String getComplexityString() {
        return name;
    }

    /**
     * Is this complexity better than c?
     * @param c
     *            the complexity to compare with.
     * @return true, iff this is better.
     */
    public boolean betterThan(final ParamComplexity c) {
        return compareTo(c) < 0;
    }

    /**
     * Is this complexity better of equal to c?
     * @param c
     *            the complexity to compare with.
     * @return true, iff this is better or equal to c.
     */
    public boolean betterOrEqual(final ParamComplexity c) {
        return compareTo(c) <= 0;
    }

    @Override
    public boolean isUnknown() {
        return this == UNKNOWN || this == OPEN;
    }

    @Override
    public boolean isOpen() {
        return this == OPEN;
    }

    /**
     * Is this complexity worse than XP?
     * @return true iff this is one of the worse complexity classes.
     */
    public boolean likelyNotXP() {
        return this == WH || this == PARANPC || this == PARANPH;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Get the corresponding P/NP complexity for this parameterized complexity
     * class.
     * @return the P/NP complexity class.
     */
    public Complexity toComplexity() {
        if (this == FPTLIN)
            return Complexity.LINEAR;
        if (this == FPT || this == XP)
            return Complexity.P;
        if (this == PARANPH)
            return Complexity.NPH;
        if (this == PARANPC)
            return Complexity.NPC;
        return Complexity.UNKNOWN;
    }

    /**
     * Can a problem at the same time have this parameterized complexity and
     * c's P/NP complexity?
     * @param c
     *            the P/NP complexity to check compatibility with.
     * @return true, iff c and this are compatible.
     */
    public boolean isCompatible(final Complexity c) {
        return this == UNKNOWN || c == Complexity.UNKNOWN || this == WH
                || (betterOrEqual(XP) && c.betterOrEqual(Complexity.P))
                || (!betterThan(PARANPC) && !c.betterThan(Complexity.GIC));
    }

    /**
     * Can a problem at the same time have this parameterized complexity and
     * c's?
     * @param c
     *            the parameterized complexity to check compatibility with
     * @return true, iff c and this are compatible.
     */
    public boolean isCompatible(final ParamComplexity c) {
        return this == UNKNOWN || c == UNKNOWN
                || (betterOrEqual(XP) && c.betterOrEqual(XP))
                || (!betterThan(WH) && !c.betterThan(WH))
                || (this == XP && c == WH) || (this == WH && c == XP)
                || equals(c);
    }

    /**
     * Assuming this complexity is assigned to some parameter, does it also
     * hold for "subparameters"? (Rule 7 parameters.pdf)
     * @return true iff this complexity also holds for "subparameters"
     */
    public boolean distributesDown() {
        return likelyNotXP();
    }

    /**
     * Assuming this complexity is assigned to some parameter, does it also
     * hold for "superparameters"? (Rule 6 parameters.pdf)
     * @return true iff this complexity also holds for "superparameters"
     */
    public boolean distributesUp() {
        return betterOrEqual(XP);
    }

    @Override
    public boolean distributesEqual() {
        return isOpen();
    }

    /**
     * If a problem has both complexity this and c, what is the resulting
     * complexity?
     * @param c
     *            the other complexity.
     * @return the resulting complexity.
     */
    public ParamComplexity distil(final ParamComplexity c)
            throws ComplexityClashException {
        if (!isCompatible(c))
            throw new ComplexityClashException(this, c);
        if (c.isUnknown())
            return this;
        if (c.betterThan(this) || this.isUnknown())
            return c;
        return this;
    }

    /**
     * Get the complexity class represented by s.
     * @param s
     *            the string to get the complexity class for.
     * @return the complexity class represented by s.
     */
    public static ParamComplexity getComplexity(String s) {
        for (ParamComplexity c : ParamComplexity.values())
            if (c.name.equals(s) || c.abbrev.equals(s))
                return c;
        throw new IllegalArgumentException(s);
    }

    /**
     * Gets the default color of the complexity.
     * @return
     *          The default color.
     * @author mater
     */
    public Color getDefaultColor() {
        return defaultColor;
    }
}

/* EOF */
