/*
 * A boundedness value. Works like complexity for Problems to realize
 * boundedness values for GraphParameters on GraphClasses.
 * @author vector
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.parameter;

import java.awt.Color;

import teo.isgci.problem.AbstractComplexity;
import teo.isgci.problem.ComplexityClashException;

/**
 * Represents the boundedness of a graph parameter on a graphclass. At the
 * current stage there are only two values (bounded/unbounded) but there will
 * be future extensions like "log-bounded".
 */
public enum Boundedness implements AbstractComplexity {
    BOUNDED("Bounded", "Bound", Color.green),
    UNBOUNDED("Unbounded", "Unbound", Color.red),
    OPEN("Open", "Open", Color.white),
    UNKNOWN("Unknown", "?", Color.white);

    /** Boundedness value. */
    protected String name;
    protected String abbrev;
    protected Color defaultColor;

    /**
     * Creates a new boundedness with the given value and name.
     * @param name
     *            the name of the boundedness.
     * @param abbrev
     *            an abbreviation for name.
     * @param color
     *            the default color for this boundedness
     */
    private Boundedness(final String name, final String abbrev,
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
     * Is this Boundedness better than b?
     * @param b the Boundedness to compare with
     * @return true, iff this is better
     */
    public boolean betterThan(Boundedness b) {
        return compareTo(b) < 0;
    }

    /**
     * Is this Boundedness better than or equal to b?
     * @param b the Boundedness to compare with
     * @return true, iff this is better or equal
     */
    public boolean betterOrEqual(Boundedness b) {
        return compareTo(b) <= 0;
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
     * Does this Boundedness mean a parameter is bounded?
     * @return true, iff this represents boundedness
     */
    public boolean isBounded() {
        return this == BOUNDED;
    }

    /**
     * Does this Boundedness mean a parameter is unbounded?
     * @return true, iff this represents unboundedness
     */
    public boolean isUnbounded() {
        return this == UNBOUNDED;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Can a parameter at the same time have this boundedness and b's? (Rule 3
     * parameters.pdf)
     * @param b
     *            the boundedness to check compatibility with.
     * @return true iff b and this are compatible.
     */
    public boolean isCompatible(Boundedness b) {
        return this == UNKNOWN || b == UNKNOWN || equals(b);
    }

    /**
     * Assuming this boundedness is assigned to some graphclasses, does it also
     * hold for subclasses? (Rule 1 paramters.pdf)
     * @return true iff this boundedness also holds for subclasses.
     */
    public boolean distributesDown() {
        return betterOrEqual(BOUNDED);
    }

    /**
     * Assuming this boundedness is assigned to some graphclasses, does it also
     * hold for superclasses? (Rule 2 paramters.pdf)
     * @return true iff this boundedness also holds for superclasses.
     */
    public boolean distributesUp() {
        return this == UNBOUNDED;
    }

    @Override
    public boolean distributesEqual() {
        return isOpen();
    }

    /**
     * Get the default color for this Boundedness.
     * @return the default color
     * @author mater
     */
    public Color getDefaultColor(){
    	return defaultColor;
    }

    /**
     * If a parameter has both boundedness this and b, what is the resulting
     * boundedness?
     * @param b
     *            the other complexity.
     * @return the resulting complexity.
     */
    public Boundedness distil(final Boundedness b)
            throws ComplexityClashException {
        if (!isCompatible(b))
            throw new ComplexityClashException(this, b);
        if (b.isUnknown())
            return this;
        if (b.betterThan(this) || this.isUnknown())
            return b;
        return this;
    }

    /**
     * Get the boundedness represented by s.
     * @param s
     *            the string to get the boundedness for.
     * @return the boundedness represented by s.
     */
    public static Boundedness getBoundedness(String s) {
        for (Boundedness b : Boundedness.values())
            if (b.name.equals(s) || b.abbrev.equals(s))
                return b;
        throw new IllegalArgumentException(s);
    }
}

/* EOF */
