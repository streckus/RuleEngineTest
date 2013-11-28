/*
 * Resolves entities and loads resources for XML parsers.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.xml;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Provides loading and resolving methods for XML parsers.
 */
public interface Resolver {

    /**
     * Return a SAX InputSource for the given file
     */
    public InputSource openInputSource(String filename);


    /**
     * Return a project specific EntityResolver, possibly creating it first.
     */
    public EntityResolver getEntityResolver();
}

/* EOF */
