/*
 * A GraphClass based on another class and adding the connected-hereditary
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
 * A GraphClass based on another class and adding the connected-hereditary
 * property.
 */
public class ConnectedHereditaryClass extends IsometricHereditaryClass {
    
    /** Creates a new graph class based on <tt>gc</tt>. */
    public ConnectedHereditaryClass(GraphClass gc){
        super(gc);
        hereditariness = Hered.CONNECTED;
    }

}
/* EOF */
