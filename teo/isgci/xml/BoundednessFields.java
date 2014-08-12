/*
 * Boundedness fields to be written in the database.
 * @author vector
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.xml;

public class BoundednessFields {
    public static Integer compID = 0;

    public static final String OPEN = getNextID() + ", 'Open', 'Open', NULL";

    public static final String UNKNOWN = getNextID()
            + ", 'UNKNOWN', '?', NULL";

    public static final String BOUNDED = getNextID()
            + ", 'Bounded', 'Bound', 1";

    public static final String UNBOUNDED = getNextID()
            + ", 'Unbounded', 'Unbound', 2";

    /*
     * Want to add a new Boundedness ? Add a new public static final String.
     * Format: getNextID() +
     * ", 'BOUNDEDNESSNAME', 'BOUNDEDNESSALIAS', BOUNDEDNESSSORT" You replace
     * BOUNDEDNESSNAME, BOUNDEDNESSALIAS, BOUNDEDNESSSORT with your String.
     *
     * It automatically gets a new ID, which you can get by calling the static
     * method getID(name-of-the-static-field)
     */

    /**
     * @return the next ID-String.
     */
    private static String getNextID() {
        Integer p = compID;
        compID++;
        return p.toString();

    }

    /**
     * Get the ID from a input-String.
     * @param s the String to get the id from
     * @return the int-ID
     */
    public static int getID(String s) {
        String[] array = s.split(",");
        return Integer.parseInt(array[0]);
    }
}

/* EOF */
