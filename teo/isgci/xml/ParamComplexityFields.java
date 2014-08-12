/*
 * ParamComplexity fields to be written in the database.
 * @author vector
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.xml;

public class ParamComplexityFields {
    public static Integer parCompID = 0;

    public static final String OPEN = getNextID() + ", 'Open', 'Open', NULL";
    public static final String UNKNOWN = getNextID()
            + ", 'Unknown', '?', NULL";
    public static final String FPTLIN = getNextID()
            + ", 'FPT-Linear', 'FPT-lin', 1";
    public static final String FPT = getNextID()
            + ", 'Fixed-Parameter-Tractable', 'FPT', 2";
    public static final String XP = getNextID() + ", 'Exponential', 'XP', 3";
    public static final String WH = getNextID() + ", 'W-hard', 'Wh', 4";
    public static final String PARANPC = getNextID()
            + ", 'paraNP-complete', 'paraNPC', 5";
    public static final String PARANPH = getNextID()
            + ", 'paraNP-hard', 'paraNPH', 5";

    /*
     * Want to add a new Complexity ? Add a new public static final String.
     * Format: getNextID() +
     * ", 'COMPLEXITYNAME', 'COMPLEXITYALIAS', COMPLEXITYSORT" You replace
     * COMPLEXITYNAME, COMPLEXITYALIAS, COMPLEXITYSORT with your String.
     *
     * It automatically gets a new ID, which you can get by calling the static
     * method getID(name-of-the-static-field)
     */

    /**
     * @return the next ID-String.
     */
    private static String getNextID() {
        Integer p = parCompID;
        parCompID++;
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
