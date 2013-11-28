/*
 * Annotations (inline references). These are copied as-is to the output file.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.ref;

/**
 * An annotation.
 */
public class Note {
    String text;
    String name;
    
    public Note(String text, String name) {
        this.text = text != null ? text : "";
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return text;
    }
}

/* EOF */
