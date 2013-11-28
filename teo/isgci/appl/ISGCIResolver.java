/*
 * A Resolver specific to the ISGCI executables.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl;

import java.io.*;
import java.net.*;
import org.xml.sax.*;
import teo.isgci.xml.Resolver;

/**
 * XML resolving and loading methods specific to ISGCI programs.
 */
public class ISGCIResolver implements Resolver {

    /** Where we are and from where we try loading if the class loader can't.
     * If null, only the classloader is used.
     */
    private URL locationURL;


    /**
     * Create new new ISGCIResolver that only loads using the classloader.
     */
    public ISGCIResolver() {
        this.locationURL = null;
    }


    /**
     * Create a new ISGCIResolver that loads first using the classloader and
     * if this fails, from the given location.
     */
    public ISGCIResolver(String location) throws MalformedURLException {
        this.locationURL = new URL(location);
    }


    /**
     * Return a SAX InputSource for the given file
     */
    public InputSource openInputSource(String filename) {
        InputStream is = null;
        InputSource i = null;

        //System.err.println(filename);
        // Try to load from jar
        try {
            is = getClass().getClassLoader().getResourceAsStream(filename);
            if (is != null) {
                i = new InputSource(is);
                i.setSystemId(filename);
            }
        } catch (Exception e) {
            System.err.println(e);
            i = null;
        }

        // Backup plan: load from server
        if (i == null  &&  locationURL != null) {
            try {
                System.err.println("Trying loading "+filename+" from server");
                URL url = new URL(locationURL, filename);
                i = new InputSource(url.openStream());
                i.setSystemId(url.toString());
            } catch (Exception e) {
                System.err.println(e);
                i = null;
            }
        }
        return i;
    }


    /**
     * Resolve XML public ids that refer to the data directory.
     */
    public EntityResolver getEntityResolver() {
        return new EntityResolver() {
            public InputSource resolveEntity(String systemId,String publicId) {
                if (publicId.endsWith("isgci.dtd"))
                    publicId = "data/isgci.dtd"; 
                else if (publicId.endsWith("smallgraphs.dtd"))
                    publicId = "data/smallgraphs.dtd"; 
                return openInputSource(publicId);
            }
        };
    }
}

/* EOF */
