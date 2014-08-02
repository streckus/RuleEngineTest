/*
 * Find trivial inclusions, generate node info.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import teo.isgci.grapht.*;
import teo.isgci.xml.*;
import teo.isgci.gc.*;
import teo.isgci.relation.*;
import teo.isgci.problem.*;
import teo.isgci.appl.*;

import gnu.getopt.Getopt;
import java.io.*;
import java.util.*;
import java.net.URL;
import org.xml.sax.InputSource;
import org.jgrapht.Graph;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;
import java.sql.SQLException;

public class Generate {

    static final String XMLDECL =
        "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
        "<!DOCTYPE ISGCI SYSTEM \"isgci.dtd\">\n";

    /**
     * Main
     */
    public static void is(String args[]) throws Exception {
        int i;

        Deducer deducer;
        DirectedGraph<GraphClass,Inclusion> graph;
        List<Problem> problems;
        RCheck checkReachability = new RCheckReachability();

        boolean notrivial = false;
        boolean extrachecks = false;
        String debugout = null;
        String debugrelout = null;
        String autocache = null;
        String sageout = null;
        PrintWriter writer;
        Map<GraphClass,Set<GraphClass> > compls;
        List<AbstractRelation> relations = new ArrayList<AbstractRelation>();

        Getopt opts = new Getopt("Generate", args, "Cxa:l:r:s:h");
        opts.setOpterr(false);
        while ((i = opts.getopt()) != -1) {
            switch (i) {
                case 'C':
                    extrachecks = true;
                    break;
                case 'x':
                    notrivial = true;
                    break;
                case 'a':
                    autocache = opts.getOptarg();
                    break;
                case 'l':
                    debugout = opts.getOptarg();
                    break;
                case 'r':
                    debugrelout = opts.getOptarg();
                    break;
                case 's':
                    sageout = opts.getOptarg();
                    break;
                case '?':
                case 'h':
                    usage();
                    System.exit(1);
            }
        }
        if (args.length - opts.getOptind() < 5) {
            usage();
            System.exit(1);
        }

        //---- Load everything
        // Performance optimization: We only add edges that do not exist yet,
        // so the underlying graph does not need to check this.
        graph = new DirectedMultigraph<GraphClass,Inclusion>(Inclusion.class);
        problems = new ArrayList<Problem>();

        Problem.setDeducing();

        load(args[opts.getOptind()], args[opts.getOptind()+1],
                graph, problems, relations);
        deducer = new Deducer(graph,true, extrachecks);
        deducer.setGeneratorCache(autocache);
        showNodeStats(graph);

        ArrayList<Inclusion> originals =
                new ArrayList<Inclusion>(graph.edgeSet());

        //---- Deduce relations
        if (notrivial)
            GAlg.transitiveClosure(graph);
        else {
            deducer.findTrivialInclusions();
            deducer.findTrivialPropers();
            new RCheckAbstractRelations().after(deducer, relations);
            showRelationStats(deducer);
        }

        //---- Export debug info
        if (debugout != null) {
            writer = new PrintWriter(
                    new BufferedWriter(new FileWriter(debugout), 64*1024));
            exportNames(graph, writer);
            exportDebug(deducer, writer);
        }
            
        if (debugrelout != null) {
            writer = new PrintWriter(
                    new BufferedWriter(new FileWriter(debugrelout), 64*1024));
            exportRelDebug(deducer, originals, writer);
        }

        //---- Deduce complexities
        System.out.println("Distributing complexities");
        Problem.distributeComplexities();
        showProblemStats(graph, problems);

        compls = gatherComplements(graph);

        //---- Remove temporaries and some edges
        System.out.println("Cleaning up");
        deducer.removeTemp();
        showNodeStats(graph);
        showProblemStats(graph, problems);
        
        checkReachability.before(deducer);
        int nc = graph.vertexSet().size();               // For Safety check
        int ec = graph.edgeSet().size();

        deducer.deleteSuperfluousEdges();
        if (extrachecks) {
            checkReachability.after(deducer);
        }

        showNodeStats(graph);

        //---- Export data
        deducer.addRefs();
        exportApp(graph, problems, relations, compls,args[opts.getOptind()+3]);
        exportSage(graph, problems, relations, compls, sageout);

        deducer.deleteSuperfluousEdgesFull();
        if (extrachecks) {
            checkReachability.after(deducer);
        }

        exportWeb(graph,problems, relations, compls, args[opts.getOptind()+2]);
        exportNames(graph, args[opts.getOptind()+4]);

        //---- Final safety check
        GAlg.transitiveClosure(graph);
        if (graph.vertexSet().size() != nc  ||  graph.edgeSet().size() != ec)
            System.err.println("Error in deleteSuperfluousEdges?!");
    }


    private static void usage() {
        System.out.println("Usage: java Generate [options] "+
                "input.xml smallgraphsin.xml "+
                "fullout.xml shortout.xml outnames.txt\n"+
                " -x : Only generate XML, no deductions done\n"+
                " -C : Perform extra checks on code (not data) correctness\n"+
                " -s filename: write out for sage to filename\n" +
                " -a filename: AUTO_* cache filename\n" +
                " -l filename: Log debug output to filename\n" +
                " -r filename: Log relations debug output to filename");
    }


    /**
     * Load the ISGCI databases.
     */
    private static void load(String file,
            String smallgraphfile,
            DirectedGraph<GraphClass,Inclusion> graph,
            List<Problem> problems,
            List<AbstractRelation> relations)
            throws java.net.MalformedURLException {
        XMLParser xml;
        Resolver loader = new ISGCIResolver(
                "file:"+System.getProperty("user.dir")+"/");

        // Smallgraphs first, because we need to know them for properly
        // resolving aliases.
        SmallGraphReader handler = new SmallGraphReader();
        xml = new XMLParser(loader.openInputSource(smallgraphfile),
                handler, loader.getEntityResolver());
        xml.parse();
        ForbiddenClass.initRules(handler.getGraphs(), handler.getInclusions());

        ISGCIReader gcr = new ISGCIReader(graph, problems);
        xml = new XMLParser(loader.openInputSource(file),
                gcr, loader.getEntityResolver(), new NoteFilter());
        xml.parse();
        relations.addAll(gcr.getRelations());
		
		//TODO right now, still using ISGCIReader
        /*
        SQLReader sqr = null;
        try{
                sqr = new SQLReader("jdbc:mySQL://localhost/Spectre", "root", 
                                   "", "", graph, problems);
                sqr.readDatabase();
        }catch(SQLException e){
                e.printStackTrace();
        }
        relations.addAll(sqr.getRelations());
        */
		
    }



    //---------------------- On-screen output -----------------------------

    /**
     * Print node/edge count statistics of the given graph.
     */
    private static void showNodeStats(Graph dg){
        System.err.print("Nodes: "+ dg.vertexSet().size());
        System.err.println("     Edges: "+ dg.edgeSet().size());
    }


    /**
     * Print statistics on the given problem.
     */
    private static void showProblemStats(
            DirectedGraph<GraphClass,Inclusion> dg, Problem p) {
        int i;
        String cs;
        int[] count = new int[Complexity.values().length];

        Arrays.fill(count, 0);

        for (GraphClass gc : dg.vertexSet())
            count[p.getDerivedComplexity(gc).ordinal()]++;

        System.out.print(String.format("%1$-15.15s", p.getName()));
        System.out.print("\t");
        for (i = 0; i < count.length; i++) {
            System.out.print(count[i]);
            System.out.print("\t");
        }
        System.out.println();
    }


    /**
     * Print statistics on the given problems.
     */
    private static void showProblemStats(
            DirectedGraph<GraphClass,Inclusion> dg, List<Problem> problems) {
        System.out.print("Problem:        ");
        for (Complexity c : Complexity.values()) {
            System.out.print("\t");
            System.out.print(c.getShortString());
        }
        System.out.println();
        for (Problem p : problems)
            showProblemStats(dg, p);
    }


    /**
     * Print statistics on the deduced relations.
     */
    private static void showRelationStats(DeducerData d) {
        DirectedGraph<GraphClass,Inclusion> graph = d.getGraph();
        int nodecount = graph.vertexSet().size();
        int edgecount = graph.edgeSet().size();
        int total = nodecount * (nodecount-1) / 2;
        int numproper = 0, numequal = 0;
        int numpropereq = 0;

        for (Inclusion e : graph.edgeSet()) {
            if (e.isProper())
                numproper++;
            else if (d.containsEdge(graph.getEdgeTarget(e),
                    graph.getEdgeSource(e)))
                numequal++;
        }
        numpropereq = edgecount - numproper - numequal;

        System.out.print("Total relations: ");
        System.out.println(total);

        System.out.print("Inclusions: ");
        System.out.print(edgecount);
        System.out.print(" (");
        System.out.print((100.0 * edgecount)/total);
        System.out.println("%)");

        System.out.println("of these");
        
        System.out.print("   Proper inclusions: ");
        System.out.print(numproper);
        System.out.print(" (");
        System.out.print((100.0 * numproper)/edgecount);
        System.out.println("%)");

        System.out.print("   Equalities: ");
        System.out.print(numequal);
        System.out.print(" (");
        System.out.print((100.0 * numequal)/edgecount);
        System.out.println("%)");

        System.out.print("   Equal or proper: ");
        System.out.print(numpropereq);
        System.out.print(" (");
        System.out.print((100.0 * numpropereq)/edgecount);
        System.out.println("%)");

        System.out.println("with confidence");

        for (int level = Inclusion.CONFIDENCE_HIGHEST;
                level >= Inclusion.CONFIDENCE_LOWEST;
                level--) {
            int count = countEdges(graph, level);

            System.out.print("   ");
            System.out.print(level);
            System.out.print(": ");
            System.out.print(count);
            System.out.print(" (");
            System.out.print((100.0 * count)/edgecount);
            System.out.println("%)");
        }
    }


    /**
     * Return the number of edges with the given confidence level.
     */
    private static int countEdges(Graph<GraphClass,Inclusion> graph,int level){
        int res = 0;

        for (Inclusion e : graph.edgeSet()) {
            if (e.getConfidence() == level)
                res++;
        }
        return res;
    }


    //-------------------------- Output to file ------------------------------
    
    /**
     * Write a list of gc-numbers and names to file.
     */
    private static void exportNames(
            DirectedGraph<GraphClass,Inclusion> dg, String file) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(file));
        } catch (IOException e) {
            System.out.println(e);
        }

        exportNames(dg, out);
        out.close();
    }


    /**
     * Write a list of gc-numbers and names to out.
     */
    private static void exportNames(
            DirectedGraph<GraphClass,Inclusion> dg, PrintWriter out) {
        for (GraphClass v : dg.vertexSet())
            out.println(v.getID() +"\t"+ v);
    }


    /**
     * Write a MODE_SAGE document of g to file.
     */
    private static void exportSage(
            DirectedGraph<GraphClass,Inclusion> g,
            List<Problem> problems,
            List<AbstractRelation> relations,
            Map<GraphClass,Set<GraphClass> > complements,
            String file) {
        writeISGCI(g, problems, relations, complements, file,
                ISGCIWriter.MODE_SAGE);
    }


    /**
     * Write a MODE_WEB document of g to file.
     */
    private static void exportWeb(
            DirectedGraph<GraphClass,Inclusion> g,
            List<Problem> problems,
            List<AbstractRelation> relations,
            Map<GraphClass,Set<GraphClass> > complements,
            String file) {
        writeISGCI(g, problems, relations, complements, file,
                ISGCIWriter.MODE_WEB);
    }


    /**
     * Write a MODE_ONLINE document of g to file.
     */
    private static void exportApp(
            DirectedGraph<GraphClass,Inclusion> g,
            List<Problem> problems,
            List<AbstractRelation> relations,
            Map<GraphClass,Set<GraphClass> > complements,
            String file){
        writeISGCI(g, problems, relations, complements, file,
                ISGCIWriter.MODE_ONLINE);
    }


    /**
     * Write the data in xml format in the given ISGCIWriter.MODE_* mode.
     */
    private static void writeISGCI(
            DirectedGraph<GraphClass,Inclusion> g,
            List<Problem> problems,
            List<AbstractRelation> relations,
            Map<GraphClass,Set<GraphClass> > complements,
            String file,
            int format) {
        try {
            FileWriter out = new FileWriter(file);
            ISGCIWriter w = new ISGCIWriter(out, format);
            w.writeISGCIDocument(g, problems, relations, complements, XMLDECL);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Prints e to the given writer as gc_ -> gc_\t name -> name
     */
    private static void println(PrintWriter w, Inclusion e) {
        println(w, e.getSuper(), e.getSub());
    }


    /**
     * Prints from->to to the given writer as gc_ -> gc_\t name -> name
     */
    private static void println(PrintWriter w, GraphClass from, GraphClass to){
        w.print(from.getID());
        w.print(" -> ");
        w.print(to.getID());
        w.print("\t");
        w.print(from);
        w.print(" -> ");
        w.println(to);
        w.flush();
    }


    /**
     * Print debug info gathered in the deducer d to the given writer.
     */
    private static void exportDebug(Deducer d, PrintWriter w) {
        w.println("\n==== Start debug ==================================");
        for (Inclusion e : d.getGraph().edgeSet())
            d.printTrace(w, e);
        w.println("==== End debug =====================================");
        w.flush();
    }


    /**
     * Print relation debug info gathered in the deducer d to the given writer.
     */
    private static void exportRelDebug(Deducer d,
            Iterable<Inclusion> originals, PrintWriter w) {
        w.println("\n==== Start relations ==============================");
        for (Inclusion e : d.getGraph().edgeSet())
            d.printRelationTrace(w, e);
        w.println("==== End relations ==================================");
        w.flush();
        w.println("==== Proper or equal ================================");
        for (Inclusion e : originals) {
            if (!e.isProper()  &&  d.getEdge(e.getSub(),e.getSuper()) == null){
                w.print("! ");
                println(w, e);
            }
        }
        w.println("==== End proper or equal ============================");
        w.flush();
    }


    //------------------------------- Misc ---------------------------------

    /**
     * Give every class a list of its complements and return it.
     */
    private static Map<GraphClass,Set<GraphClass> >
            gatherComplements(DirectedGraph<GraphClass,Inclusion> dg) {
        GraphClass w;

        Map<GraphClass,Set<GraphClass> > scc = GAlg.calcSCCMap(dg);
        Map<GraphClass,Set<GraphClass> > compls =
            new HashMap<GraphClass,Set<GraphClass> >();

        for (GraphClass v : dg.vertexSet()) {
            if (!(v instanceof ComplementClass))
                continue;
            if (compls.containsKey(v))          // Already handled.
                continue;

            w = ((ComplementClass) v).getBase();
            Set<GraphClass> compo = scc.get(v);
            Set<GraphClass> cocompo = scc.get(w);
            for (GraphClass x : compo)
                compls.put(x, cocompo);
            for (GraphClass x : cocompo)
                compls.put(x, compo);
        }
        return compls;
    }


}
