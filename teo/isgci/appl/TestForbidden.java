/*
 * Commandline tool to test whether one set of smallgraphs forbids another.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl;

import java.io.*;
import java.util.*;
import org.xml.sax.*;
import teo.isgci.xml.*;
import teo.isgci.gc.*;

public class TestForbidden {

    public static void main(String args[]) throws IOException {

        SmallGraphReader handler = new SmallGraphReader();

        Resolver loader = new ISGCIResolver(args[0]);
        XMLParser xr = new XMLParser(
                loader.openInputSource("data/smallgraphs.xml"),
                handler, loader.getEntityResolver());
        xr.parse();
        ForbiddenClass.initRules(handler.getGraphs(), handler.getInclusions());

        boolean again = true;
        while (again) {
            System.out.println(
                    "Insert ;-separated forbidden HashSet of supposed SUB");
            String a = (new LineNumberReader(
                new InputStreamReader(System.in))).readLine();
            StringTokenizer strTok1 = new StringTokenizer(a, ";");
            System.out.println(
                    "Insert ;-separated forbidden HashSet of supposed SUPER");
            a = (new LineNumberReader(new InputStreamReader(System.in))).
                readLine();
            StringTokenizer strTok2 = new StringTokenizer(a, ";");
            HashSet set1 = new HashSet(), set2 = new HashSet();
            while (strTok1.hasMoreElements())
                set1.add(strTok1.nextToken().trim());
            while (strTok2.hasMoreElements())
                set2.add(strTok2.nextToken().trim());

            ForbiddenClass gc1 = new ForbiddenClass(set1);
            ForbiddenClass gc2 = new ForbiddenClass(set2);

            System.out.print("sub: "+ gc1.subClassOf(gc2));

            StringBuilder witness = new StringBuilder();
            boolean res = gc1.notSubClassOf(gc2, witness);
            System.out.println(" not sub: "+ res +" "+ witness);
            /*System.out.print("Continue? (y/n): ");
            System.out.flush();
            a = (new LineNumberReader(new InputStreamReader(System.in))).
                readLine();
            if (!a.equals("y"))
                again = false;*/
        }
    }
}

/* EOF */
