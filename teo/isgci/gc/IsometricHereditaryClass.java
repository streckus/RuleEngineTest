/*
 * A GraphClass based on another class and adding the isometric-hereditary
 * property.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */


package teo.isgci.gc;


/**
 * A GraphClass based on another class and adding the isometric-hereditary
 * property.
 */
public class IsometricHereditaryClass extends HereditaryClass {
    
    /** Creates a new graph class based on <tt>gc</tt>. */
    public IsometricHereditaryClass(GraphClass gc){
        super(gc);
        hereditariness = Hered.ISOMETRIC;
    }

}
/* EOF */
