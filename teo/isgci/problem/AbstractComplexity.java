/*
 * An abstract complexity class interface. Used to share Methods between
 * Complexity, Boundedness and ParamComplexity.
 * @author vector
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.problem;

/**
 * An interface to share common methods among different complexity classes like
 * Complexity, ParamComplexity and Boundedness.
 */
public interface AbstractComplexity {

    /**
     * Get the short string representing the complexity.
     * @return the abbreviation for the complexity name.
     */
    String getShortString();

    /**
     * Get the string representing the name of the complexity.
     * @return the name of the complexity.
     */
    String getComplexityString();

    /**
     * Is this complexity unknown or open?
     * @return true iff this complexity is unknown or open.
     */
    boolean isUnknown();

    /**
     * Is this complexity open?
     * @return true iff this complexity is open.
     */
    boolean isOpen();

    /**
     * Does this complexity also hold for subclasses?
     * @return true iff this complexity also holds for subclasses.
     */
    boolean distributesDown();

    /**
     * Does this complexity also hold for superclasses?
     * @return true iff this complexity also holds for superclasses.
     */
    boolean distributesUp();

    /**
     * Assuming this complexity is assigned to some graphclasses, does it also
     * hold for equivalent classes? (Not necessarily for super/subclasses.)
     * @return true iff this complexity also holds for equivalent classes, but
     *         not necessarily for super/sub classes.
     */
    boolean distributesEqual();

    /**
     * Get a string representation of the complexity.
     * @return the name of the complexity.
     */
    String toString();
}
