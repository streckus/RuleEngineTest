/*
 * Deduces trivial inclusions.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */


package teo.isgci.appl.deducer;

import java.io.PrintWriter;
import java.util.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import teo.isgci.gc.*;
import teo.isgci.ref.*;
import teo.isgci.relation.*;
import teo.isgci.util.*;
import teo.isgci.grapht.*;

public class Deducer implements DeducerData {
    
    /** Where we're deducing */
    CacheGraph<GraphClass,Inclusion> graph;
    /** Contains the temporary nodes */
    HashSet<GraphClass> temporaries;
    /** Trace where the deductions come from? */
    boolean trace;
    /** Perform consistency checks? */
    boolean checking;
    /** Tracedata why the inclusion holds */
    Annotation<GraphClass,Inclusion,TraceData> traceAnn;
    /** Tracedata why the inclusion is of this type (properness) */
    Annotation<GraphClass,Inclusion,TraceData> traceRelAnn;
    /** Classes added in the last run of findTrivialInclusions */
    private ArrayList<GraphClass> newclasses;
    /** Set by setProper(), reset by findTrivialPropers() */
    private boolean newproper;
    /** Confidence level at which we're currently deducing */
    private int confidence;
    /** Input edges with less than certain confidence levels */
    private HashSet<Inclusion> uncertains;
    /** Generates the ids for automatically deduces classes (AUTO_*) */
    private IDGenerator idgenerator;
    /** Iteration number */
    private int iteration;
    
    
    public Deducer(DirectedGraph<GraphClass,Inclusion> g, boolean trace,
            boolean checking) {
        graph = new CacheGraph<GraphClass,Inclusion>(g,
                1*1000*1000, 5*1000*1000);
        graph.setChecking(checking);
        temporaries = new HashSet<GraphClass>(g.vertexSet().size());
        idgenerator = null;
        this.trace = trace;
        this.checking = checking;
        if (trace) {
            traceAnn = new Annotation<GraphClass,Inclusion,TraceData>(graph);
            traceRelAnn= new Annotation<GraphClass,Inclusion,TraceData>(graph);
        }
        newclasses = null;
    }


    /**
     * Create a new IDGenerator using the given cachefile.
     * Must be called before start of deductions.
     */
    public void setGeneratorCache(String filename) {
        idgenerator = new IDGenerator(1, filename);
    }


    //------------------------ Trivial inclusions ---------------------------

    /**
     * Deduce all trivial inclusions. The resulting graph is transitively
     * closed.
     */
    public void findTrivialInclusions() {
        RCheck checks[] = new RCheck[]{
            new RCheckSCC(),
            new RCheckForbidden(),
            new RCheckForbiddenNonEdge(),
            new RCheckType()
        };

        for (RCheck check : checks)
            check.before(this);

        separateUncertains();
        if (checking)
            graph.check();
        GAlg.transitiveClosure(graph);

        confidence = Inclusion.CONFIDENCE_HIGHEST;
        iteration = 0;
        do {
            System.out.print("confidence level: ");
            System.out.println(confidence);

            iteration++;
            findTrivialInclusionsOneLevel();

            if (checking)
                graph.check();

            if (uncertains.isEmpty())
                break;

            do {
                confidence--;
            } while (confidence >= Inclusion.CONFIDENCE_LOWEST  &&
                    addUncertains() == 0);
        } while (confidence >= Inclusion.CONFIDENCE_LOWEST);

        tempify();

        // Force error for checking the checks
        /*{
            GraphClass chordal = null, interval = null;

            for (GraphClass gc : graph.vertexSet())
                if ("chordal".equals(gc.toString()))
                    chordal = gc;
                else if ("interval".equals(gc.toString()))
                    interval = gc;
            addTrivialEdge(interval, chordal, newTraceData("Test"));
        }*/

        for (RCheck check : checks)
            check.after(this);
    }


    /**
     * Deduce all trivial inclusions on the current confidence level.
     * The resulting graph is transitively closed.
     */
    private void findTrivialInclusionsOneLevel() {
        int oldnodes, oldedges;
        RClass[] classrules = new RClass[]{
            // (A,B)-free ==> A-free \cap B-free
            new RClassForbidden2Intersect(),
            // A-free \cap B-free ==> (A,B)-free
            new RClassExtendForbidden(),   // Uses inclusions!
            // X ==> clique X for clique fixed
            new RClassCliqueFixed(),
            // X ==> co-X for forbidden, self-compl, union, intersect
            new RClassComplements()
        };
        ArrayList<GraphClass> lastnewclasses = new ArrayList<GraphClass>();

        newclasses = new ArrayList<GraphClass>(graph.vertexSet());
 
        do {
            oldnodes = graph.vertexSet().size();
            oldedges = graph.edgeSet().size();
            System.out.print("Nodes: "+oldnodes);
            System.out.println("     Edges: "+oldedges);
            lastnewclasses.clear();

            do {
                ArrayList<GraphClass> added = newclasses;
                newclasses = new ArrayList<GraphClass>();
                sortByID(added);

                for (RClass r : classrules)
                    r.run(this, added);
                
                lastnewclasses.addAll(added);
            } while (newclasses.size() > 0); 
            findTrivialOnce(lastnewclasses);
        } while (graph.edgeSet().size() > oldedges  ||
                graph.vertexSet().size() > oldnodes);
        System.out.print("Nodes: "+ graph.vertexSet().size());
        System.out.println("     Edges: "+ graph.edgeSet().size());
    }


    private void findTrivialOnce(ArrayList<GraphClass> classes) {
        RSub[] rules = new RSub[]{
            new RSubComplement(),
            new RSubUnion(),
            new RSubIntersect(),
            new RSubHereditary(),
            new RSubProbe(),
            new RSubClique()
        };

        // Maps a GraphClass type to all instances of that type in classes
        HashMap<Class,ArrayList<GraphClass> > typedClasses = new HashMap<>();

        //---- Create the lists of typedClasses...
        for (RSub rule : rules) {
            RSubTyping type = rule.getClass().getAnnotation(RSubTyping.class);
            if (type == null)
                throw new RuntimeException("Rule without annotation: "+
                        rule.getClass().getName());

            if (!typedClasses.containsKey(type.superType()))
                typedClasses.put(type.superType(),new ArrayList<GraphClass>());
            if (!typedClasses.containsKey(type.subType()))
                typedClasses.put(type.subType(), new ArrayList<GraphClass>());
        }

        //---- ...and fill them
        for (GraphClass gc : graph.vertexSet()) {
            for (Class c : typedClasses.keySet())
                if (c.isInstance(gc))
                    typedClasses.get(c).add(gc);
        }

        //---- Direct inclusions deserve special treatment
        System.out.println("direct");
        if (graph.vertexSet().size() > classes.size()) {
            // Some classes pre-existing, some new: Check all-new and new-all
            new RSubDirect().run(this, graph.vertexSet(), classes);
            new RSubDirect().run(this, classes, graph.vertexSet());
        } else {
            new RSubDirect().run(this, graph.vertexSet(), graph.vertexSet());
        }

        //---- Now do all the other rules
        for (RSub rule : rules) {
            RSubTyping type = rule.getClass().getAnnotation(RSubTyping.class);
            String name = rule.getClass().getName();
            System.out.println(name.substring(name.lastIndexOf('.')+1));
            rule.run(this, typedClasses.get(type.superType()),
                    typedClasses.get(type.subType()));
        }
    }



    //------------------------ Proper inclusions ---------------------------

    /**
     * Mark (already found) inclusions as proper.
     */
    public void findTrivialPropers() {
        RProper[] rules = new RProper[]{
            new RProperComplement(),
            new RProperProbe(),
            new RProperForbiddenSub()
        };

        // Maps a GraphClass type to all instances of that type in classes
        HashMap<Class,ArrayList<GraphClass> > typedClasses = new HashMap<>();

        //---- Create the lists of typedClasses...
        for (RProper rule : rules) {
            RProperTyping type =
                    rule.getClass().getAnnotation(RProperTyping.class);
            if (type == null)
                throw new RuntimeException("Rule without annotation: "+
                        rule.getClass().getName());

            if (!typedClasses.containsKey(type.type()))
                typedClasses.put(type.type(),new ArrayList<GraphClass>());
        }

        //---- ...and fill them
        for (GraphClass gc : graph.vertexSet()) {
            for (Class c : typedClasses.keySet())
                if (c.isInstance(gc))
                    typedClasses.get(c).add(gc);
        }

        transitiveClosePropers(graph.edgeSet());
        new RProperDirect().run(this, graph.vertexSet());

        //---- Repeatedly deduce properness of inclusions
        do {
            newproper = false;
            for (RProper rule : rules) {
                RProperTyping type =
                        rule.getClass().getAnnotation(RProperTyping.class);
                String name = rule.getClass().getName();
                System.out.println(name.substring(name.lastIndexOf('.')+1));
                rule.run(this, typedClasses.get(type.type()));
            }
        } while (newproper);

        new RCheckProper().after(this);
    }


    /**
     * Ensure properness transitivity for the given edges
     * The graph must already be transitively closed.
     */
    private void transitiveClosePropers(Iterable<Inclusion> edges) {
        ArrayList<Inclusion> propers = new ArrayList<Inclusion>();
        
        for (Inclusion e : edges) {
            if (e.isProper())
                propers.add(e);
        }
        for (Inclusion e : propers) {
            e.setProper(false);
            setProper(e, null);
        }
    }

    
    //------------------------ "Transitive reduction" -----------------------
    
    /**
     * Reduction that removes edges between SCCs for which a path exists.
     * Sort of a transitive reduction except that edges inside an SCC are
     * never removed.
     * This ensures that the web pages contain only minimal/maximal
     * super/subclasses.
     * Returns the deleted edges.
     */
    public Collection<Inclusion> deleteSuperfluousEdgesFull(){
        int i;
        final List<Inclusion> deleted = new ArrayList<Inclusion>();
        Map<GraphClass,Set<GraphClass> > scc = GAlg.calcSCCMap(graph);

        for (Inclusion edge : graph.edgeSet()) {
            final GraphClass src = graph.getEdgeSource(edge);
            final GraphClass dest = graph.getEdgeTarget(edge);

            if (scc.get(src) == scc.get(dest))
                continue;

            final Inclusion theedge = edge;
            final ArrayList<GraphClass> reached = new ArrayList<>();

            new BFSWalker<GraphClass,Inclusion>(graph, src, null,
                    GraphWalker.InitCode.DYNAMIC) {
                public void explore(Inclusion e, GraphClass from,
                        GraphClass to) {
                    if (e != theedge  &&  !deleted.contains(e))
                        super.explore(e, from, to);
                }
                public void visit(GraphClass v) {
                    if (v == dest) {
                        reached.add(v);
                        q.clear();
                    }
                    else
                        super.visit(v);
                }

            }.run();
            if (!reached.isEmpty())
                deleted.add(edge);
        }

        for (Inclusion e : deleted)
            removeEdge(e);
        return deleted;
    }


    /**
     * Transitive reduction that prefers deleting edges with low keeperPrio().
     * Returns the deleted edges as list of Pairs(from, to).
     */
    public Collection<Inclusion> deleteSuperfluousEdges() {
        Collection<Inclusion> deleted = new ArrayList<Inclusion>();

        // First delete the bulk using weighted shortest paths
        WeightedGraph<GraphClass,Inclusion> wg = keeperWeightedGraph();
        FloydWarshallShortestPaths<GraphClass,Inclusion> fw =
            new FloydWarshallShortestPaths<GraphClass,Inclusion>(wg);
        for (Inclusion e : graph.edgeSet())
            if (keeperWeight(e) > fw.shortestDistance(
                    graph.getEdgeSource(e), graph.getEdgeTarget(e)))
                deleted.add(e);
        for (Inclusion e : deleted)
            removeEdge(e);

        // Then go through the remaining edges one by one
        deleted.addAll(deleteSuperfluousEdges(0));
        deleted.addAll(deleteSuperfluousEdges(1));
        return deleted;
    }


    /**
     * Removes edges with the given prio, provided a path from src to dest
     * remains in the graph. Like a restricted transitive reduction.
     * Returns the deleted edges as list of Inclusions.
     */
    private Collection<Inclusion> deleteSuperfluousEdges(int prio){
        final HashSet<Inclusion> deleted = new HashSet<Inclusion>();

        for (Inclusion edge : graph.edgeSet()) {
            if (keeperPrio(edge) != prio)
                continue;

            final GraphClass src = graph.getEdgeSource(edge);
            final GraphClass dest = graph.getEdgeTarget(edge);
            final Inclusion theedge = edge;
            final ArrayList<GraphClass> reached = new ArrayList<>();

            new BFSWalker<GraphClass,Inclusion>(graph, src, null,
                    GraphWalker.InitCode.DYNAMIC) {
                public void explore(Inclusion e, GraphClass from,
                        GraphClass to) {
                    if (e != theedge  &&  !deleted.contains(e))
                        super.explore(e, graph.getEdgeSource(e),
                                graph.getEdgeTarget(e));
                }
                public void visit(GraphClass v) {
                    if (v == dest) {
                        reached.add(v);
                        q.clear();
                    }
                    else
                        super.visit(v);
                }

            }.run();
            if (!reached.isEmpty())
                deleted.add(edge);
        }

        for (Inclusion e : deleted)
            removeEdge(e);
        return deleted;
    }


    /**
     * Return a weighted version of the deductions graph, designed for easily
     * recognizing edges that can deleted (see keeperWeight).
     */
    private WeightedGraph<GraphClass,Inclusion> keeperWeightedGraph() {
        Map<Inclusion,Double> map = new HashMap<Inclusion,Double>();
        for (Inclusion e : graph.edgeSet())
            map.put(e, new Double(keeperWeight(e)));
        return new AsWeightedDirectedGraph<>(graph, map);
    }


    /**
     * Return the priority for keeping e when deleting superfluous edges.
     * 0 = no tracedata or transitive
     * 1 = other cases
     * 2 = with refs
     */
    private int keeperPrio(Inclusion e) {
        if (e.getRefs() != null  &&  !e.getRefs().isEmpty())
            return 2;

        TraceData td = traceAnn.getEdge(e);

        if (td == null  ||  "Transitivity".equals(td.getDesc()))
            return 0;

        return 1;
    }


    /*
     * Return weights for the edges of the deductions graph, designed for
     * easily recognizing edges that can deleted. Every edge has weight
     * depending on keeperPrio: 2->0.0001, 1->1, 0->10000. If the shortest path
     * a->b is shorter than the edge weight, this means the edge can surely be
     * deleted.
     */
    private double keeperWeight(Inclusion e) {
        final double[] weights = {10000.0, 1.0, 0.0001};
        return weights[keeperPrio(e)];
    }




    //====================== Various public methods =======================

    /**
     * Adds reference strings for trivially deduced inclusions.
     */
    public void addRefs() {
        TraceData tr;

        for (Inclusion e : graph.edgeSet()) {
            tr = traceAnn.getEdge(e);
            if (tr == null)
                continue;

            if ("direct".equals(tr.getDesc())) {
                e.addRef(new Ref( graph.getEdgeTarget(e).whySubClassOf() ));
            } else if ("addForbiddenSuper".equals(tr.getDesc())) {
                e.addRef(new Ref("forbidden"));
            } else if ("addForbiddenSuperConfig".equals(tr.getDesc())) {
                e.addRef(new Ref("forbidden"));
            } else if ("extendForbidden".equals(tr.getDesc())) {
                e.addRef(new Ref("forbidden"));
            } else if ("complement".equals(tr.getDesc())) {
                e.addRef(new Ref("complement"));
            } else if ("probeclass".equals(tr.getDesc())) {
                e.addRef(new Ref("basederived"));
            } else if ("cliqueclass".equals(tr.getDesc())) {
                e.addRef(new Ref("basederived"));
            }
        }
    }


    /**
     * Remove nodes marked as temporary.
     */
    public void removeTemp() {
        for (GraphClass temp : temporaries)
            graph.removeVertex(temp);
    }

    /**
     * Print a trace for the given edge to writer.
     */
    public void printTrace(PrintWriter writer, Inclusion e) {
        if (trace)
            TraceData.print(writer, e, traceAnn);
    }


    /**
     * Print a trace for the given edge to writer.
     */
    public void printRelationTrace(PrintWriter writer, Inclusion e) {
        if (trace  &&  e.isProper())
            TraceData.print(writer, e, traceRelAnn);
    }


    //====================== DeducerData methods ==========================

    /**
     * Create new tracedata, if the trace flag is set.
     */
    public TraceData newTraceData(String desc, Inclusion... is) {
        if (trace)
            return new TraceData(desc, is);
        else
            return null;
    }


    /**
     * Return the graph on which we're deducing.
     */
    public DirectedGraph<GraphClass,Inclusion> getGraph() {
        return graph;
    }


    /**
     * Sort the given list of nodes by id.
     */
    public void sortByID(List<GraphClass> list) {
        Collections.sort(list, new Comparator<GraphClass>() {
                public int compare(GraphClass o1, GraphClass o2) {
                    Integer s1 = ( o1).getID();
                    Integer s2 = ( o2).getID();
                    boolean orig1 = (~(s1^0)&7) == 7; //intID
                    boolean orig2 = (~(s2^0)&7) == 7; //intID
                    if (orig1  &&  !orig2)
                        return -1;
                    if (orig2  &&  !orig1)
                        return 1;
                    return s1.compareTo(s2);
                }
        });
    }


    /**
     * Return true iff graph contains an edge between from and to.
     */
    public boolean containsEdge(GraphClass from, GraphClass to) {
        return graph.containsEdge(from, to);
    }


    /**
     * Look for an edge that starts at node <code>from</code> and ends at node
     * <code>to</code>.
     *
     * @param from the start node
     * @param to   the target node
     * return an appropriate edge or <tt>null</tt> if no such edge was found
     */
    public Inclusion getEdge(GraphClass from, GraphClass to) {
        if (from==null || to==null) {
            throw new NullPointerException("node can't be null");
        }
        return graph.findEdge(from, to);
    }


    /**
     * Add a trivially deduced inclusion.
     */
    public Inclusion addTrivialEdge(GraphClass from, GraphClass to,
            TraceData tr) {
        if (from == to  ||  containsEdge(from, to))
            return null;
        //System.out.println(from.getGraphClass() +" -> "+ to.getGraphClass());
        Inclusion e = addEdge(from, to);
        if (trace)
            traceAnn.setEdge(e, tr);

        //---- Maintain transitivity
        Inclusion trans;
        // Collect out-neighbours of to in tos
        GraphClass[] tos = new GraphClass[graph.outDegreeOf(to)];
        int i = 0;
        for (GraphClass g : GAlg.outNeighboursOf(graph, to))
            tos[i++] = g;
        
        // Add from -> tos
        for (i = 0; i < tos.length; i++)
            if (from != tos[i]  &&  !containsEdge(from, tos[i])) {
                if (from.equals(tos[i]))
                    System.err.println("Equals: "+ from.getID() +" "+ from +
                    " = "+ tos[i].getID() +" "+ tos[i]);
                trans = addEdge(from, tos[i]);
                if (trace) {
                    traceAnn.setEdge(trans, new TraceData("Transitivity", e,
                            getEdge(to, tos[i])));
                }
            }

        for (GraphClass v : GAlg.inNeighboursOf(graph, from)) {
            // add super(from) -> to
            if (v != to  &&  !containsEdge(v, to)) {
                if (v.equals(to))
                    System.err.println("Equals: "+
                        v.getID() +" "+ v +" = "+ to.getID() +" "+ to);
                trans = addEdge(v, to);
                if (trace)
                    traceAnn.setEdge(trans,
                           new TraceData("Transitivity", getEdge(v, from), e));
            }
            // add super(from) -> tos
            for (i = 0; i < tos.length; i++)
                if (v != tos[i]  &&  !containsEdge(v, tos[i])) {
                    if (v.equals(tos[i]))
                        System.err.println("Equals: "+ v.getID() +" "+ v +
                        " = "+ tos[i].getID() +" "+ tos[i]);
                    trans = addEdge(v, tos[i]);
                    if (trace) {
                        traceAnn.setEdge(trans,
                               new TraceData("Transitivity", getEdge(v, from),
                                       e, getEdge(to, tos[i])));
                    }
                }
        }

        return e;
    }


    /**
     * Set the properflag for an edge to true and do this for the transitive
     * inclusions as well.
     */
    public void setProper(Inclusion e, TraceData t) {
        if (e == null  ||  e.isProper())
            return;

        newproper = true;
        e.setProper(true);
        if (trace)
            traceRelAnn.setEdge(e, t);

        GraphClass from = graph.getEdgeSource(e);
        GraphClass to = graph.getEdgeTarget(e);
 
        // Maintain transitivity
        Inclusion trans;
        // Collect out-neighbours of to in tos
        GraphClass[] tos = new GraphClass[graph.outDegreeOf(to)];
        int i = 0;
        for (GraphClass g : GAlg.outNeighboursOf(graph, to))
            tos[i++] = g;
        
        for (i = 0; i < tos.length; i++)
            if (from != tos[i]  &&  containsEdge(from, tos[i])  &&
                    (trans = getEdge(from, tos[i])) != null &&
                    !trans.isProper()) {
                trans.setProper(true);
                if (trace)
                    traceRelAnn.setEdge(trans, newTraceData("Transitivity",e));
            }

        for (GraphClass v : GAlg.inNeighboursOf(graph, from)) {
            // add super(from) -> to
            if (v != to  &&  containsEdge(v, to)  &&
                    (trans = getEdge(v, to)) != null &&
                    !trans.isProper()) {
                trans.setProper(true);
                if (trace)
                    traceRelAnn.setEdge(trans, newTraceData("Transitivity",e));
            }
            // add super(from) -> tos
            for (i = 0; i < tos.length; i++)
                if (v != tos[i]  &&  containsEdge(v, tos[i])  &&
                        (trans = getEdge(v, tos[i])) != null &&
                        !trans.isProper()) {
                    trans.setProper(true);
                    if (trace)
                        traceRelAnn.setEdge(trans,
                                newTraceData("Transitivity",e));
                }
        }
    }


    /**
     * Return true iff gc is a temporary node.
     */
    public boolean isTempNode(GraphClass gc) {
        return temporaries.contains(gc);
    }


    /**
     * If a node for graphclass gc exists, return it, otherwise add it as a
     * temporary node.
     * @param gc GraphClass to ensure a node for
     */
    public GraphClass ensureTempNode(GraphClass gc) {
        GraphClass n = graph.findVertex(gc);
        return n == null ? addTempNode(gc) : n;
    }


    /**
     * If a node for graphclass gc exists, return it, otherwise add it as a
     * trivial node.
     * @param gc GraphClass to ensure a node for
     */
    public GraphClass ensureTrivialNode(GraphClass gc) {
        GraphClass n = graph.findVertex(gc);
        if (n == null)
            return addTrivialNode(gc);

        temporaries.remove(n);
        return n;
    }


    //====================== Various private methods ========================


    //---------------------- Private (un)certains methods --------------------
    
    /**
     * Remove the edges with confidence levels below the highest one and store
     * them in uncertains.
     */
    private void separateUncertains() {
        int i;

        uncertains = new HashSet<Inclusion>();
        for (Inclusion e : graph.edgeSet()) {
            if (e.getConfidence() < Inclusion.CONFIDENCE_HIGHEST)
                uncertains.add(e);
        }
        for (Inclusion e : uncertains)
            graph.removeEdge(e);
        /*System.out.print("unreliable: ");
        System.out.println(uncertains.size());*/
    }


    /**
     * Add the uncertains of the current confidence level back into the graph.
     * They are removed from uncertains.
     * Returns the number of edges added.
     */
    private int addUncertains() {
        Inclusion e;
        int i = 0;

        Iterator<Inclusion> iter = uncertains.iterator();
        while (iter.hasNext()) {
            e =  iter.next();
            if (e.getConfidence() == confidence) {
                if (!containsEdge(e.getSuper(), e.getSub())) {
                    Inclusion e2 =
                            addTrivialEdge(e.getSuper(), e.getSub(), null);
                    e2.setConfidence(e.getConfidence());
                    e2.setProper(e.isProper());
                    e2.setRefs(e.getRefs());
                }
                iter.remove();
                i++;
            }
        }
        return i;
    }


    //---------------------- Private tempifying methods ------------------


    /**
     * Go through all nodes and if several Forbidden classes are equivalent,
     * mark as many of them as possible as temporary.
     * After this, go through all SetClasses and mark classes that contain
     * temporary classes as temporary themselves.
     */
    private void tempify() {
        ArrayList<ForbiddenClass> forb = new ArrayList<ForbiddenClass>();
        List<Set<GraphClass> > sccs = GAlg.calcSCCList(graph);

        for (Set<GraphClass> eqs : sccs) {
            if (eqs.size() <= 1)
                continue;
            forb.clear();
            for (GraphClass nj : eqs) {
                if (nj instanceof ForbiddenClass)
                    forb.add((ForbiddenClass) nj);
            }
            if (forb.size() <= 1)
                continue;
            tempEqForbidden(forb);
        }

        for (GraphClass gc : graph.vertexSet()) {
            if (!(gc instanceof SetClass)  ||  temporaries.contains(gc))
                continue;
            for (GraphClass gc2 : ((SetClass) gc).getSet()) {
                if (temporaries.contains(gc2)) {
                    temporaries.add(gc);
                    System.out.println("Tempified "+ gc.getID() +" "+ gc);
                    break;
                }
            }
        }

    }


    /**
     * Given a set of equivalent Forbidden classes, mark as many of them as
     * possible as temporary.
     */
    private void tempEqForbidden(Collection<ForbiddenClass> v) {
        if (v.size() < 2)
            return;

        // original classes
        ArrayList<ForbiddenClass> orig = new ArrayList<ForbiddenClass>();
        // new, not temporary classes
        ArrayList<ForbiddenClass> auto = new ArrayList<ForbiddenClass>();
        // new, temporary classes;
        ArrayList<ForbiddenClass> temp = new ArrayList<ForbiddenClass>();

        //---- Categorize the classes
        for (ForbiddenClass ni : v) {
            if (temporaries.contains(ni))
                temp.add(ni);
            else if ((~(ni.getID()^1)&7) == 7) //intID AUTO 
                auto.add(ni);
            else
                orig.add(ni);
        }

        if (auto.size() == 0  ||  auto.size() + orig.size() <= 1)
            return;

        //---- Have orig -> temporize all new
        if (orig.size() > 0) {
            temporaries.addAll(auto);
            return;
        }

        //---- Keep only classes with minimal number of families,configurations
        int best = Integer.MIN_VALUE;
        for (ForbiddenClass ni : auto) {
            if (ni.niceness() > best)
                best = ni.niceness();
        }

        for (ForbiddenClass ni : auto) {
            if (ni.niceness() < best)
                temporaries.add(ni);
        }
    }


    //----------------------- Private node/edge methods -------------------

    /**
     * Remove an edge.
     */
    private void removeEdge(Inclusion e) {
        graph.removeEdge(e);
    }


    /**
     * Add an edge.
     */
    private Inclusion addEdge(GraphClass src, GraphClass dest) {
        Inclusion e = graph.addEdge(src, dest);
        e.setConfidence(confidence);
        return e;
    }


    /**
     * Add a new trivially deduced node.
     * The new node is printed in XML to stderr.
     */
    private GraphClass addTrivialNode(GraphClass gc) {

        // Intersections of temp classes are temp themselves
        if (gc instanceof IntersectClass) {
            for (Object o : ((IntersectClass) gc).getSet())
                if (temporaries.contains((GraphClass) o))
                    return addTempNode(gc);

        }

        GraphClass v = doAddTrivialNode(gc);
        //System.out.println("new: "+gc);
        return v;
    }


    /**
     * Add a temporary trivially deduced node.
     */
    private GraphClass addTempNode(GraphClass gc) {
        GraphClass v = doAddTrivialNode(gc);
        temporaries.add(v);
        //System.out.println("new (temp): "+gc);
        return v;
    }


    private GraphClass doAddTrivialNode(GraphClass gc) {
        graph.addVertex(gc);
        gc.setID(idgenerator.getID(gc.toString()));
        temporaries.remove(gc);
        newclasses.add(gc);
        /*System.out.print(iteration);
        System.out.print("\t");
        System.out.print(nodedebugprefix);
        System.out.print("\t");
        System.out.print(gc.getID());
        System.out.print("\t");
        System.out.println(gc);*/
        return gc;
    }


}


/* EOF */
