/*
 * Starts ISGCI and provides functinos for loading data.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */


package teo.isgci.gui;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.File;
import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import teo.isgci.xml.*;
import teo.isgci.appl.*;

public class Loader {
    private URL locationURL;
    private int registered;
    private Object finished;
    private boolean trylocal;
    private Resolver resolver;

    public Loader() {
        locationURL = null;
        registered = 0;
        finished = new Object();
        resolver = new ISGCIResolver();
    }

    public Loader(String location) throws MalformedURLException {
        this();
        locationURL = new URL(location);
    }

    public Loader(String location, boolean trylocal)
            throws MalformedURLException {
        this(location);
        this.trylocal = trylocal;
    }

    public synchronized void register() {
        registered++;
    }

    public synchronized void unregister() {
        if (--registered == 0) {
            synchronized(finished) {
                finished.notify();
            }
        }
    }


    //-------------------------- Input stuff -------------------------------

    /**
     * Display a document in a new browser window
     * @param url the document to display (relative to documentbase)
     * @param name the name of the window to display the document in.
     */
    public void showDocument(String url) {
        try {
            System.err.println(new URL(locationURL, url).toURI());
            Desktop.getDesktop().browse(new URL(locationURL, url).toURI());
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }


    /**
     * Open a stream to filename
     */
    public InputStream openStream(String filename) {
        InputStream is = null;

        // Try to load from jar
        try {
            is = getClass().getClassLoader().getResourceAsStream(filename);
        } catch (Exception e) {
            //System.err.println(e);
            is = null;
        }
 
        // Backup plan: load from server
        if (is == null  &&  trylocal) {
            try {
                System.err.println("Trying loading "+filename+" from server");
                is = (new URL(locationURL, filename)).openStream();
            } catch (Exception e) {
                System.err.println(e);
                is = null;
            }
        }
        return is;
    }


    /**
     * Get the image in the given file
     * WARNING: max image size 5k!
     */
    public Image getImage(String filename) {
        Toolkit kit = Toolkit.getDefaultToolkit();
        Image result = null;

        // Try to load from jar
        try {
            InputStream is = getClass().getClassLoader().
                    getResourceAsStream(filename);
            if (is != null) {
                byte[] data = new byte[1024*5];     // max image size 5k!
                int len = is.read(data);
                result = kit.createImage(data, 0, len);
            }
        } catch (Exception e) {
            System.err.println(e);
            result = null;
        }
        return result;
    }


    public Resolver getResolver() {
        return resolver;
    }

    //----------------------------- main ----------------------------------

    public static void main(String args[]) {
        if (args.length < 1) {
            System.err.println("Usage: ISGCI.jar url");
            System.exit(1);
        }

        try {
            final Loader loader = new Loader(args[0]);
            synchronized (loader.finished) {
                new Thread(new Runnable() {
                    public void run()  { new ISGCIMainFrame(loader); }
                }).start();
                loader.finished.wait();
            }
            System.exit(0);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }

}


/* EOF */
