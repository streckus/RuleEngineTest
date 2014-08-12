/*
 * Problems on graphs.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.problem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import org.jgrapht.DirectedGraph;
import teo.isgci.grapht.*;
import teo.isgci.gc.*;
import teo.isgci.parameter.*;
import teo.isgci.relation.*;
import teo.isgci.ref.*;


/**
 * Stores the information about a graph problem.
 */
public class Problem extends AbstractProblem {

    /** Sparse problems have data for only a few classes */
    protected boolean sparse;
    /** Is this Problem applicable to parameters? (added by vector) */
    protected boolean forparams;
    /** Stores complexity information on a graph class */
    protected Annotation<GraphClass,Inclusion,Complexity> complexAnn;
    /** Stores algorithms on a graph class */
    protected Annotation<GraphClass,Inclusion,ProblemOnNode> algoAnn;
    /** Stores complexity information on a graph parameter. (added by vector) */
    protected Annotation<GraphClass, Inclusion, ParamComplexity> parComplexAnn;
    /** Stores algorithms on a graph parameter. (added by vector) */
    protected Annotation<GraphClass, Inclusion, ProblemOnPseudoNode> parAlgoAnn;
    /** More/less general problems */
    protected List<Reduction> parents;
    protected List<Reduction> children;
    /** Solving this on G is polytime equivalent to solving complement on co-G
     */
    protected Problem complement;
    /** The algorithms (node independent) that solve depending on co-G. */
    protected List<Algorithm> coAlgos;
    /**
     * The algorithms (node independent) that solve this decomposition problem
     * depending on the corresponding parameter. (added by vector)
     */
    protected Map<GraphParameter, Set<Algorithm>> paramDecompAlgos;
    /**
     * The algorithms (node independent) that solve this problem depending on
     * parameters and decomposition complexities. (added by vector)
     */
    protected Map<GraphParameter, Map<Complexity, Set<Algorithm>>> paramAlgos;

    /**
     * The FTP-Algorithms that were resolved from an FPT-lin algo on a
     * parameter. (added by vector)
     */
    protected Map<PseudoClass, Set<ParamAlgorithm>> FPTAlgos;

    protected Problem(String name, DirectedGraph<GraphClass,Inclusion> g) {
        this(name, g, null, null, false);
    }

    /**
     * Create a problem.
     * @param name the name of the problem ("Independent set")
     * @param g the graph of classes for which the problem exists
     * @param complement the complement of the problem (Clique)
     * @param directed to which graphs the problem applies (null for all)
     */
    protected Problem(String name, DirectedGraph<GraphClass,Inclusion> g,
            Problem complement, GraphClass.Directed directed, boolean forparams) {
        super(name, g, directed);
        if (directed == GraphClass.Directed.PARAMETER)
            this.forparams = true;
        else
            this.forparams = forparams;
        this.sparse = false;
        setComplement(complement);
        this.parents = new ArrayList<Reduction>();
        this.children = new ArrayList<Reduction>();
        this.complexAnn = new Annotation<GraphClass,Inclusion,Complexity>(g);
        this.algoAnn = deducing ?
                new Annotation<GraphClass,Inclusion,ProblemOnNode>(g) : null;
        this.parComplexAnn =
                new Annotation<GraphClass, Inclusion, ParamComplexity>(g);
        this.parAlgoAnn = deducing ?
                new Annotation<GraphClass, Inclusion, ProblemOnPseudoNode>(g)
                : null;
        this.coAlgos = null;
        this.paramAlgos =
                new HashMap<GraphParameter, Map<Complexity, Set<Algorithm>>>();
        this.paramDecompAlgos = new HashMap<GraphParameter, Set<Algorithm>>();
        this.FPTAlgos = new HashMap<PseudoClass, Set<ParamAlgorithm>>();
    }

    public void setSparse() {
        sparse = true;
    }

    public boolean isSparse() {
        return sparse;
    }

    @Override
    public void setDirected(GraphClass.Directed d) {
        this.directed = d;
        if (d == GraphClass.Directed.PARAMETER)
            forparams = true; // added by vector
    }

    /**
     * Set this problem to be applicable for parameters.
     * @param params a boolean to set the forparams-attribute to
     * @author vector
     */
    public void setParameters(boolean params) {
        forparams = params;
    }

    /**
     * @return true iff this problem is applicable to parameters.
     * @author vector
     */
    public boolean forParameters() {
        return directed == GraphClass.Directed.PARAMETER || forparams;
    }

    /**
     * Return true iff this problem is applicable to the given class.
     */
    public boolean validFor(GraphClass gc) {
        if (gc.isPseudoClass()) // added by vector
            return forParameters()
                    && (((PseudoClass) gc).getParameter().forDirected() == this
                            .forDirected() || ((PseudoClass) gc)
                            .getParameter().forUndirected() == this
                            .forUndirected());
        return super.validFor(gc);
    }


    /**
     * Adds a new reduction parent->this with cost c.
     */
    public void addReduction(Problem parent, Complexity c) {
        Reduction r = new Reduction(this, parent, c);
        this.parents.add(r);
        parent.children.add(r);
    }

    /**
     * Return the reductions from other problems to this.
     */
    public Iterator<Reduction> getReductions() {
        return parents.iterator();
    }


    public void setComplement(Problem thecomplement) {
        this.complement = thecomplement;
        if (thecomplement != null)
            thecomplement.complement = this;
    }

    public Problem getComplement() {
        return complement;
    }

    /**
     * Create a new problem with the given name and graph;
     */
    public static Problem createProblem(
            String name, DirectedGraph<GraphClass,Inclusion> g) {
        Problem p = null;
        // Cliquewidth removed from here since it is a GraphParameter.
        // if (name.equals("Cliquewidth"))
        //      p = new Cliquewidth(name, g);
        if (name.equals("Recognition"))
            p = new Recognition(name, g);
        else
            p = new Problem(name, g);
        if (deducing)
            problems.add(p);
        return p;
    }

    //====================== Complexity on a node ===========================

    /**
     * Return the stored complexity of this problem on n. Return UNKNOWN if
     * nothing is stored.
     */
    public Complexity getComplexity(GraphClass n) {
        Complexity c = complexAnn.getNode(n);
        return c == null ? Complexity.UNKNOWN : c;
    }


    /**
     * Return the stored complexity of this problem on the PseudoClass n.
     * Return UNKNOWN if nothing is stored.
     * @param n the pseudoclass to get the Complexity for
     * @return a ParamComplexity
     * @author vector
     */
    public ParamComplexity getComplexity(PseudoClass n) {
        ParamComplexity c = parComplexAnn.getNode(n);
        return c == null ? ParamComplexity.UNKNOWN : c;
    }


    /**
     * Set the complexity of this on n to c.
     */
    public void setComplexity(GraphClass n, Complexity c) {
        if (n.isPseudoClass())
            throw new UnsupportedOperationException(
                    "P/NP complexity on a parameter.");
        complexAnn.setNode(n, c);
    }

    /**
     * Set the complexity of this on pseudoclass n to c.
     * @param n the PseudoClass to set the complexity for
     * @param c the ParamComplexity for this on n
     * @author vector
     */
    public void setComplexity(PseudoClass n, ParamComplexity c) {
        parComplexAnn.setNode(n, c);
    }

    /**
     * Return the complexity of this problem on n, as derived in the last
     * completed step.
     * Meant to be used internally and for XML writing.
     */
    public Complexity getDerivedComplexity(GraphClass n) {
        if (n.isPseudoClass())
            throw new UnsupportedOperationException(
                    "Derived complexity for parameter pseudoclasses"
                            + " is not defined.");
        ProblemOnNode pon = algoAnn.getNode(n);

        return pon == null ? Complexity.UNKNOWN :
                pon.getComplexity(
                    Problem.currentStep > 0 ? Problem.currentStep-1 : 0);
    }

    /**
     * Return the complexity of this problem on pseudoclass n, as derived in
     * the last completed step. Meant to be used internally and for XML
     * writing.
     * @param n the PseudoClass to get the derived Complexity for
     * @return the ParamComplexity of this on n
     * @author vector
     */
    public ParamComplexity getDerivedComplexity(PseudoClass n) {
        ProblemOnPseudoNode pon = parAlgoAnn.getNode(n);

        return pon == null ? ParamComplexity.UNKNOWN
                : pon.getComplexity(Problem.currentStep > 0 ? Problem.currentStep - 1
                        : 0);
    }

    /**
     * Get the complexity of n, consulting the parent problem, too.
     */
    protected Complexity getParentallyDerivedComplexity(GraphClass n) {
        if (n.isPseudoClass())
            throw new UnsupportedOperationException(
                    "Derived complexity for parameter pseudoclasses"
                            + " is not defined.");

        Complexity c = getDerivedComplexity(n);
        if (parents.isEmpty())
            return c;

        Complexity pc = Complexity.UNKNOWN;
        for (Reduction r : parents) {
            pc = r.fromParent(r.getParent().getParentallyDerivedComplexity(n));
            if (!c.isCompatible(pc))
                throw new Error("Inconsistent data for "+n+" "+name);
            if (pc.betterThan(c))
                c = pc;
        }
        return c;
    }


    /**
     * Get the complexity of n, consulting the child problem, too.
     */
    protected Complexity getProgeniallyDerivedComplexity(GraphClass n) {
        if (n.isPseudoClass())
            throw new UnsupportedOperationException(
                    "Derived complexity for parameter pseudoclasses"
                            + " is not defined.");

        Complexity c = getDerivedComplexity(n);
        if (children.isEmpty())
            return c;

        Complexity cc = Complexity.UNKNOWN;
        for (Reduction r : children) {
            cc = r.fromChild(r.getChild().getProgeniallyDerivedComplexity(n));
            if (!cc.isCompatible(c))
                throw new Error("Inconsistent data for "+n+" "+name);
            if (cc.betterThan(c))
                c = cc;
        }
        return c;
    }

    /**
     * Get the complexity of n, consulting bounded parameters, too.
     * @param n the GraphClass to get the complexity for
     * @return the derived complexity from consulted GraphParameters
     * @author vector
     */
    protected Complexity getParameterDerivedComplexity(GraphClass n) {
        if (n.isPseudoClass())
            throw new UnsupportedOperationException(
                    "Derived complexity for parameter pseudoclasses"
                            + " is not defined.");

        Complexity c = getDerivedComplexity(n);
        for (GraphParameter par : parameters) {
            PseudoClass pc = par.getPseudoClass();
            if (par.getDerivedBoundedness(n).isBounded()) {
                ParamComplexity parcom = getDerivedComplexity(pc);
                if (!c.isCompatible(parcom))
                    throw new Error("Inconsistent data for " + n + " " + name
                            + " " + par);
                Complexity decomp = par.isLinDecomp() ? Complexity.LINEAR
                        : par.getDecompositionProblem()
                                .getDerivedComplexity(n);
                Complexity res = getParamResultComplexity(decomp, parcom);
                if (res.betterThan(c))
                    c = res;
            }
        }
        return c;
    }

    /**
     * Get the resulting complexity given, a decomposition time and a
     * parameterized complexity.
     * @param decomp
     *            the complexity for the decomposition problem.
     * @param parcom
     *            the parameterized complexity on a bounded parameter.
     * @return the resulting complexity
     * @author vector
     */
    protected Complexity getParamResultComplexity(Complexity decomp,
            ParamComplexity parcom) {
        Complexity complexity = parcom.toComplexity();
        return decomp.betterThan(complexity) ? complexity : decomp;
    }

    /**
     * If complement can be solved on co-G in time c, return the time in which
     * this can be solved on G.
     */
    public Complexity complementComplexity(Complexity c) {
        return c.betterThan(Complexity.P) ? Complexity.P : c;
    }


    //============================ Algorithms =============================


    /**
     * Add an algorithm for this problem on graphclass n and update the
     * complexity on n.
     */
    protected void addAlgo(GraphClass n, Algorithm a) {
        if (!graph.containsVertex(n) || n.isPseudoClass())
            throw new IllegalArgumentException("Invalid node");
        if (a.getProblem() != this  &&  a.getProblem() != complement)
            throw new IllegalArgumentException("Invalid algorithm "+ a);

        ProblemOnNode pon = algoAnn.getNode(n);
        if (pon == null) {
            pon = new ProblemOnNode(this, n);
            algoAnn.setNode(n, pon);
        }

        pon.addAlgo(a, Problem.currentStep);
    }

    /**
     * Add an algorithm for this problem on graphclass n and update the
     * complexity on pseudoclass n.
     * @param n the PseudoClass to add an algorithm for
     * @param a the algortithm for this on the PseudoClass
     * @author vector
     */
    protected void addAlgo(PseudoClass n, ParamAlgorithm a) {
        if (!graph.containsVertex(n))
            throw new IllegalArgumentException("Invalid node");
        if (a.getProblem() != this && a.getProblem() != complement)
            throw new IllegalArgumentException("Invalid algorithm " + a);

        ProblemOnPseudoNode pon = parAlgoAnn.getNode(n);
        if (pon == null) {
            pon = new ProblemOnPseudoNode(this, n);
            parAlgoAnn.setNode(n, pon);
        }

        pon.addAlgo(a, Problem.currentStep);
    }

    /**
     * Add all algorithms in iter to n.
     */
    protected void addAlgos(GraphClass n, Iterator<Algorithm> iter) {
        while (iter.hasNext())
            addAlgo(n, iter.next());
    }

    /**
     * Add all algorithms in iter to pseudoclass n.
     * @param n the PseudoClass to add algorithms for
     * @param iter the Algorithms to add
     * @author vector
     */
    protected void addAlgos(PseudoClass n, Iterator<ParamAlgorithm> iter) {
        while (iter.hasNext()) {
            ParamAlgorithm a = iter.next();
            if (a.getComplexity().equals(ParamComplexity.FPTLIN)) {
                a = getFPTAlgo(a.getComplexity(), a.getPseudoClass());
            }
            addAlgo(n, a);
        }
    }

    /**
     * Get a FPT-Algorithm for a FPT-lin-Algorithm.
     * @param c the ParamComplexity to get an algorithm for
     * @param from the PseudoClass on which the FPT-lin algorithm is defined
     * @return the FPT-Algortihm either from the FPTAlgos-List or a new one
     * @author vector
     */
    protected ParamAlgorithm getFPTAlgo(ParamComplexity c, PseudoClass from) {
        String why = "from " + c.getComplexityString() + " on " + from;
        if (FPTAlgos.get(from) == null)
            FPTAlgos.put(from, new HashSet<ParamAlgorithm>());
        for (ParamAlgorithm a : FPTAlgos.get(from)) {
            if (a.getComplexity().equals(ParamComplexity.FPT))
                return a;
        }

        ParamAlgorithm a = createAlgo(null, ParamComplexity.FPT, why);
        FPTAlgos.get(from).add(a);
        return a;
    }

    /**
     * Create a new algorithm for this problem on a node n, add it to node n
     * and return it.
     * n may be null.
     */
    public Algorithm createAlgo(GraphClass n, Complexity complexity,
            String bounds, List refs) {
        Algorithm res = new SimpleAlgorithm(this, n, complexity, bounds, refs);
        if (n != null)
            addAlgo(n, res);
        return res;
    }

    /**
     * Create a new algorithm for this problem on a pseudoclass n, add it to
     * pseudoclass n and return it. n may be null.
     * @param n the PseudoClass to create an algorithm for
     * @param complexity the complexity class of the algorithm
     * @param refs the references for the algorithm
     * @return the created algorithm
     * @author vector
     */
    public ParamAlgorithm createAlgo(PseudoClass n,
            ParamComplexity complexity, String bounds, List refs) {
        ParamAlgorithm res = new ParamAlgorithm(this, n, complexity, bounds,
                refs);
        if (n != null)
            addAlgo(n, res);
        return res;
    }

    /**
     * Create a new algorithm for this problem on a node n with a simple
     * explanation (Note text), add it to node n and return it.
     * n may be null.
     */
    public Algorithm createAlgo(GraphClass n, Complexity complexity,
            String why) {
        List refs = new ArrayList();
        refs.add(new Note(why, null));
        return createAlgo(n, complexity, null, refs);
    }

    /**
     * Create a new algorithm for this problem on a pseudoclass n with a simple
     * explanation (Note text), add it to pseudoclass n and return it. n may be
     * null.
     * @param n the PseudoClass to create an algorithm for
     * @param complexity the complexity class of the algorithm
     * @param why a text describing the algorithm
     * @return the created algorithm
     * @author vector
     */
    public ParamAlgorithm createAlgo(PseudoClass n,
            ParamComplexity complexity, String why) {
        List refs = new ArrayList();
        refs.add(new Note(why, null));
        return createAlgo(n, complexity, null, refs);
    }

    /**
     * Get the algorithms for this problem that work on node n or null if there
     * are none.
     */
    protected HashSet<Algorithm> getAlgoSet(GraphClass n) {
        if (algoAnn == null)
            return null;

        ProblemOnNode pon = algoAnn.getNode(n);
        return pon == null ? null : pon.getAlgoSet();
    }

    /**
     * Get the algorithms for this problem that work on pseudoclass n or null
     * if there are none.
     * @param n the PseudoClass to get the AlgoSet for
     * @return the set of Algorithms for n
     * @author vector
     */
    protected HashSet<ParamAlgorithm> getAlgoSet(PseudoClass n) {
        if (parAlgoAnn == null)
            return null;

        ProblemOnPseudoNode pon = parAlgoAnn.getNode(n);
        return pon == null ? null : pon.getAlgoSet();
    }

    /**
     * Return an iterator over the algorithms for this problem on the given
     * node. Never returns null.
     */
    public Iterator<Algorithm> getAlgos(GraphClass n) {
        HashSet<Algorithm> hash = getAlgoSet(n);
        if (hash == null)
            hash = new HashSet<Algorithm>();
        return hash.iterator();
    } 

    /**
     * Return an iterator over the algorithms for this problem on the given
     * pseudoclass. Never returns null.
     * @param n the PseudoClass to get the AlgoSet for
     * @return the iterator of Algorithms for n
     * @author vector
     */
    public Iterator<ParamAlgorithm> getAlgos(PseudoClass n) {
        HashSet<ParamAlgorithm> hash = getAlgoSet(n);
        if (hash == null)
            hash = new HashSet<ParamAlgorithm>();
        return hash.iterator();
    }


    //====================== Distribution of algorithms =====================


    /**
     * Distribute the Algorithms for this problem over all nodes.
     * initAlgo/addAlgo must have been called for all problems.
     * Assumes the graph is transitively closed!
     * @param gc2node maps GraphClass to Node in g
     */
    protected void distributeAlgorithms() {
        Complexity c;
        ParamComplexity pc;
        Map<GraphClass,Set<GraphClass> > scc = GAlg.calcSCCMap(graph);

        //---- Add every set of algorithms to the super/subnodes' set. ----
        for (GraphClass n : graph.vertexSet()) {
            if (n.isPseudoClass()) {// added by vector
                HashSet<ParamAlgorithm> algos = getAlgoSet((PseudoClass) n);
                if (algos != null) {
                    PseudoClass p = (PseudoClass) n;
                    pc = getDerivedComplexity(p);
                    if (pc.distributesUp()) {
                        distributeParams(algos, GAlg.inNeighboursOf(graph, p));
                    } else if (pc.distributesDown())
                        distributeParams(algos, GAlg.outNeighboursOf(graph, p));
                    else if (pc.distributesEqual())
                        distributeParams(algos, scc.get(p));
                }
            } else {
                HashSet<Algorithm> algos = getAlgoSet(n);
                if (algos != null) {
                    c = getDerivedComplexity(n);
                    if (c.distributesUp())
                        distribute(algos, GAlg.inNeighboursOf(graph, n));
                    else if (c.distributesDown())
                        distribute(algos, GAlg.outNeighboursOf(graph, n));
                    else if (c.distributesEqual())
                        distribute(algos, scc.get(n));
                }
            }
        }
    }


    /**
     * Distribute the algorithms for the parents to this problem.
     */
    protected void distributeParents() {
        Complexity c;

        for (GraphClass n : graph.vertexSet()) {
            if (!n.isPseudoClass()) {
                for (Reduction r : parents) {
                    c = r.fromParent(r.getParent()
                            .getParentallyDerivedComplexity(n));
                    if (!c.isUnknown())
                        addAlgo(n, r.getChildAlgo(c));
                }
            }
        }
    }


    /**
     * Distribute the algorithms for the children to this problem.
     */
    protected void distributeChildren() {
        Complexity c;

        for (GraphClass n : graph.vertexSet()) {
            if (!n.isPseudoClass()) {
                for (Reduction r : children) {
                    c = r.fromChild(r.getChild()
                            .getProgeniallyDerivedComplexity(n));
                    if (!c.isUnknown())
                        addAlgo(n, r.getParentAlgo(c));
                }
            }
        }
    }

    /**
     * Distribute the algorithms for the parameters to this problem.
     * @author vector
     */
    protected void distributeParameters() {
        Complexity c;

        for (GraphClass n : graph.vertexSet()) {
            if (!n.isPseudoClass()) {
                c = getParameterDerivedComplexity(n);
                Complexity compl = getDerivedComplexity(n);
                for (GraphParameter par : parameters) {
                    PseudoClass pc = par.getPseudoClass();
                    if (par.getDerivedBoundedness(n).isBounded()) {
                        if (!par.isLinDecomp()
                                && par.getDecompositionProblem().equals(this)) {
                            // Distribute the standard decomposition time for a
                            // bounded parameter.
                            addAlgo(n,
                                    getParamDecompAlgo(par.getDecomposition(),
                                            par, Boundedness.BOUNDED));
                        } else {
                            ParamComplexity parcom = getDerivedComplexity(pc);
                            Complexity decomp = par.isLinDecomp() ?
                                    Complexity.LINEAR
                                    : par.getDecompositionProblem()
                                            .getDerivedComplexity(n);
                            c = getParamResultComplexity(decomp, parcom);
                            if (!c.isUnknown())
                                addAlgo(n,
                                        getParamAlgo(c, par, parcom, decomp));
                        }
                    } else if (!par.isLinDecomp()
                            && par.getDerivedBoundedness(n).isUnbounded()
                            && par.getDecompositionProblem().equals(this)) {
                        // If the parameter is unbounded, the decomposition
                        // problem is NPC.
                        addAlgo(n,
                                getParamDecompAlgo(Complexity.NPC, par,
                                        Boundedness.UNBOUNDED));
                    }
                }
            }
        }
    }

    /**
     * Return an algorithm on a class in time c assuming the parameter is
     * bounded.
     * @param c the Complexity to get an Algotithm for
     * @param par the GraphParameter the Complexity is derived of
     * @param parcom the ParamComplexity c is derived from
     * @param decomp the decomposition time for par
     * @return an algorithm solving this in time c either from the list or a
     *         new one
     * @author vector
     */
    protected Algorithm getParamAlgo(Complexity c, GraphParameter par,
            ParamComplexity parcom, Complexity decomp) {
        final String why = "from " + parcom.getComplexityString() + " on "
                + par.getName() + " and " + decomp.getComplexityString()
                + " decomposition time";
        if (paramAlgos.get(par) == null)
            paramAlgos.put(par, new HashMap<Complexity, Set<Algorithm>>());
        if (paramAlgos.get(par).get(decomp) == null)
            paramAlgos.get(par).put(decomp, new HashSet<Algorithm>());
        for (Algorithm a : paramAlgos.get(par).get(decomp)) {
            if (a.getComplexity().equals(c))
                return a;
        }

        Algorithm a = createAlgo(null, c, why);
        paramAlgos.get(par).get(decomp).add(a);
        return a;
    }

    /**
     * Return an algorithm on a class in time c assuming the parameter is
     * bounded.
     * @param c the complexity to get an algorithm for
     * @param par the GraphParameter this is the decomposition problem for
     * @param b the boundedness of the parameter
     * @return an Algorithm solving this in time c
     * @author vector
     */
    protected Algorithm getParamDecompAlgo(Complexity c, GraphParameter par,
            Boundedness b) {
        final String why = "from " + b.getComplexityString() + " "
                + par.getName();
        if (paramDecompAlgos.get(par) == null)
            paramDecompAlgos.put(par, new HashSet<Algorithm>());
        for (Algorithm a : paramDecompAlgos.get(par)) {
            if (a.getComplexity().equals(c))
                return a;
        }

        Algorithm a = createAlgo(null, c, why);
        paramDecompAlgos.get(par).add(a);
        return a;
    }

    /**
     * Distribute the Algorithms for this problem over all nodes via the
     * complement.
     * initAlgo/addAlgo must have been called for all problems.
     * distributeAlgorithms must have been called for this problem, and all
     * parent/child problems.
     * Assumes the graph is transitively closed and the complement index is
     * set!
     */
    public void distributeComplement() {
        if (complement == null)
            return;

        GraphClass con;
        Complexity nc, conc;

        for (GraphClass n : graph.vertexSet()) {
            if (!(n instanceof ComplementClass))
                continue;
            con = ((ComplementClass) n).getBase();
            nc = getDerivedComplexity(n);
            conc = complement.getDerivedComplexity(con);
            if (!nc.isCompatible(complementComplexity(conc))) {
                System.err.println("ComplexityClash: "+
                        n +" "+ this.name +"="+ nc +" but "+
                        con +" "+ this.complement.name +"="+ conc);
            } else if (nc.isUnknown() && !conc.isUnknown()) {
                addAlgo(n, getComplementAlgo(complementComplexity(conc)));
            } else if (conc.isUnknown() && !nc.isUnknown()) {
                complement.addAlgo(con, complement.getComplementAlgo(
                        complement.complementComplexity(nc)) );
            }
        }
    }


    /**
     * Try moving complexity information UP to union nodes. We only change the
     * complexity class for Union nodes, and do not generate new references or
     * timebounds.
     * The reasoning is: If we can solve the problem for every part of the
     * union in polytime, then we can apply all part algorithms in polytime,
     * and check their solutions in polytime. So the problem is solvable in
     * polytime on the union.
     */
    protected void distributeUpUnion() {
        int i;
        boolean ok;
        Complexity c;

        for (GraphClass n: graph.vertexSet()) {
            if ( !(n instanceof UnionClass) ||
                    getDerivedComplexity(n).betterOrEqual(Complexity.P) )
                continue;

            //---- Check whether all parts are in P ----
            ok = true;
            for (GraphClass part : ((UnionClass) n).getSet()) {
                if (!getDerivedComplexity(part).betterOrEqual(Complexity.P)) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                //System.err.println("NOTE: distributeUpUnion invoked on "+
                        //n.getName()+" "+toString());
                createAlgo(n, Complexity.P, "From the constituent classes.");
            }
        }
    }


    /**
     * Try moving complexity information DOWN to intersection nodes.
     * Example: If we can recognize every part of the intersection in
     * polytime/lin, then we can take their conjunction in polytime/lin as
     * well.
     * @param gc2node translates a GraphClass into the corresponding ISGCINode
     */
    protected void distributeDownIntersect() {}

    /**
     * Do special deductions for a particular problem.
     * Default implementation does nothing.
     */
    protected void distributeSpecial() {}


    /**
     * Adds the algorithms in algos to the classes in nodes.
     */
    protected void distribute(HashSet<Algorithm> algos,
            Iterable<GraphClass> nodes) {
        for (GraphClass n : nodes) {
            addAlgos(n, algos.iterator());
        }
    }

    /**
     * Adds the algorithms in algos to the pseudoclasses in nodes.
     */
    protected void distributeParams(HashSet<ParamAlgorithm> algos,
            Iterable<GraphClass> nodes) {
        for (GraphClass n : nodes) {
            addAlgos((PseudoClass) n, algos.iterator());
        }
    }

    //--------------------- Derived algorithms ---------------------------

    /**
     * Find in the given list an algorithm of the requested complexity. If it
     * doesn't exist yet, create it node-independently with the given
     * complexity and text.
     */
    protected Algorithm getDerivedAlgo(List<Algorithm> l,
            Complexity c, String why) {
        for (Algorithm a : l)
            if (a.getComplexity().equals(c))
                return a;

        Algorithm a = createAlgo(null, c, why);
        l.add(a);
        return a;
    }
    

    /**
     * Find in the given list an algorithm of the requested complexity. If it
     * doesn't exist yet, create it node-independently with the given
     * complexity and text.
     * @param l a list of algorithms to search in
     * @param c the complexity to look for
     * @param why the string to describe a new algorithm
     * @return a ParamAlgorithm that solves this in time c
     * @author vector
     */
    protected ParamAlgorithm getDerivedAlgo(List<ParamAlgorithm> l,
            ParamComplexity c, String why) {
        for (ParamAlgorithm a : l)
            if (a.getComplexity().equals(c))
                return a;

        ParamAlgorithm a = createAlgo(null, c, why);
        l.add(a);
        return a;
    }

    /**
     * Return an algorithm that solves this on a class in time c, assuming the
     * complement can be solved on co-G.
     */
    protected Algorithm getComplementAlgo(Complexity c) {
        final String why = "from "+ complement +" on the complement";

        if (coAlgos ==  null)
            coAlgos = new ArrayList<Algorithm>();
        return getDerivedAlgo(coAlgos, c, why);
    }

    //================= Controlling the deduction process ====================
    /**
    * Complexities are deduced in multiple steps, as follows:
    * Repeat twice:
    * - Algorithms for problem on node or on a super/subclass of node.
    * - Derived from previous step by parent/child problems
    * - Derived from previous step by union/intersect/special
    * Derived from previous step by complement problems.
    * Repeat twice:
    * - Algorithms for problem on node or on a super/subclass of node.
    * - Derived from previous step by parent/child problems and parameters
    * - Derived from previous step by union/intersect/special
    */
    static final int STEPS = 4*3 + 1;
    /** The current step */
    private static int currentStep;

    /**
     * Call this before reading the graph when you're going to deduce.
     */
    public static void setDeducing() {
        deducing = true;
        currentStep = 0;
        problems = new ArrayList<Problem>();
    }


    /**
     * Perform a single sequence of complexity deductions (3 steps), without
     * complement.
     */
    public static void distributeComplexitiesBasic() {
        for (Problem p : problems)
            p.distributeAlgorithms();
        currentStep++;

        for (Problem p : problems) {
            p.distributeParents();
            p.distributeChildren();
            p.distributeParameters();// added by vector
        }
        currentStep++;

        for (Problem p : problems) {
            p.distributeUpUnion();
            p.distributeDownIntersect();
            p.distributeSpecial();
        }
        currentStep++;
    }


    /**
     * Distribute/deduce the algorithms and complexities.
     * @param gc2node maps GraphClass to Node in g
     */
    public static void distributeComplexities() {
        // distributeComplexitiesBasic(); in AbstractProblem.java (changed by
        // vector)

        for (Problem p : problems)
            p.distributeComplement();
        currentStep++;
    }
}


/* EOF */
