/*
 * GraphParameter. Contains all information about a graphparameter and the
 * needed methods for deduction.
 * @author vector
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;

import teo.isgci.gc.ComplementClass;
import teo.isgci.gc.GraphClass;
import teo.isgci.gc.UnionClass;
import teo.isgci.gc.GraphClass.Directed;
import teo.isgci.grapht.Annotation;
import teo.isgci.grapht.GAlg;
import teo.isgci.problem.AbstractProblem;
import teo.isgci.problem.Algorithm;
import teo.isgci.problem.Complexity;
import teo.isgci.problem.ParamAlgorithm;
import teo.isgci.problem.ParamComplexity;
import teo.isgci.problem.Problem;
import teo.isgci.ref.Note;
import teo.isgci.relation.Inclusion;
import teo.isgci.util.Itera;

/**
 * Stores Information about a graph parameter similar to the Problem type.
 */
public class GraphParameter extends AbstractProblem {
    /* The ID for this parameter. */
    protected Integer paramId;
    /* In which time can a decomposition for this parameter be found? */
    protected Complexity decompositionTime;
    /*
     * If a decomposition cannot always be found in linear time, a
     * GraphParameter needs a decomposition problem.
     */
    protected Problem decomposition;
    /*
     * A GraphParameter has its own PseudoClass to be stored and drawn in the
     * hierarchy.
     */
    private PseudoClass pseudoclass;
    /* Stores boundedness information on a graph class. */
    protected Annotation<GraphClass, Inclusion, Boundedness> boundedAnn;
    /* Stores boundedness proofs on a graph class. */
    protected Annotation<GraphClass, Inclusion, ParameterOnNode> proofAnn;

    /* This bounded on G is equivalent complement bounded on co-G. */
    protected GraphParameter complement;
    /* The proofs (node independent) for boundedness of depending on co-G. */
    protected List<BoundednessProof> coProofs;

    /*
     * The proofs of boundedness for the super-/subparameters in the hierarchy.
     */
    protected Map<GraphParameter, Set<BoundednessProof>> superProofs,
            subProofs;
    /*
     * The proofs (node independent) from problem complexity incompatibilities.
     */
    protected Map<Problem, Set<BoundednessProof>> probProofs;

    /**
     * Create a new GraphParameter with given data.
     * @param id the ID of the parameter
     * @param name the name of the parameter
     * @param g the graph on which this parameter is defined
     */
    protected GraphParameter(Integer id, String name,
            DirectedGraph<GraphClass, Inclusion> g) {
        this(id, name, g, null, null, null, null);
    }

    /**
     * Create a new GraphParameter with its corresponding PseudoClass.
     * @param name
     *            the name of the Parameter
     * @param g
     *            the graph of GraphClasses for which the parameter exists.
     * @param directed
     *            to which graphs the problem/parameter applies (null for all)
     */
    protected GraphParameter(Integer id, String name,
            DirectedGraph<GraphClass, Inclusion> g, GraphParameter complement,
            Directed directed, Complexity dec, Problem decprob) {
        super(name, g, directed);
        this.paramId = id;
        if (directed == Directed.PARAMETER)
            throw new IllegalArgumentException("Parameters may not be defined"
                    + " on parameter PseudoClasses.");
        setComplement(complement);
        // Create an new PseudoClass for this parameter.
        this.pseudoclass = new PseudoClass(this);
        this.decompositionTime = dec == null ? Complexity.LINEAR : dec;
        this.decomposition = decprob;
        this.boundedAnn = new Annotation<>(g);
        this.proofAnn = deducing ?
                new Annotation<GraphClass, Inclusion, ParameterOnNode>(g)
                    : null;
        this.superProofs = new HashMap<GraphParameter, Set<BoundednessProof>>();
        this.subProofs = new HashMap<GraphParameter, Set<BoundednessProof>>();
        this.probProofs = new HashMap<Problem, Set<BoundednessProof>>();
    }

    /**
     * @return the ID of this parameter.
     */
    public Integer getID() {
        return paramId;
    }

    /**
     * Get the decomposition time for this parameter.
     * @return the decomposition time.
     */
    public Complexity getDecomposition() {
        return decompositionTime;
    }

    /**
     * Get the decomposition problem for this parameter.
     * @return the decomposition problem for this parameter. If a decomposition
     *         can always be found in linear time, it will be null.
     */
    public Problem getDecompositionProblem() {
        return decomposition;
    }

    /**
     * Set the decomposition time for this parameter to d.
     * @param d
     *            the new decomposition time.
     */
    public void setDecomposition(Complexity d) {
        this.decompositionTime = d;
    }

    /**
     * Set the decomposition problem for this parameter.
     * @param p
     *            the decomposition problem for this parameter.
     */
    public void setDecompositionProblem(Problem p) {
        this.decomposition = p;
    }

    /**
     * Does this parameter always have linear time decomposition?
     * @return true iff a decomposition for this parameter can always be found
     *         in linear time.
     */
    public boolean isLinDecomp() {
        return decompositionTime.betterOrEqual(Complexity.LINEAR);
    }

    /**
     * Does this parameter always have polytime decomposition?
     * @return true iff a decomposition for this parameter can always be found
     *         in polynomial time.
     */
    public boolean isPolyDecomp() {
        return decompositionTime.betterOrEqual(Complexity.P);
    }

    /**
     * A decomposition for this parameter can't always be found in linear or
     * polytime.
     * @return true iff a decomposition for this parameter can't always be
     *         found in linear or polytime.
     */
    public boolean isNPDecomp() {
        return decompositionTime.likelyNotP();
    }

    /**
     * Get the pseudoclass that represents this parameter.
     * @return this parameter's pseudoclass.
     */
    public PseudoClass getPseudoClass() {
        return pseudoclass;
    }

    @Override
    public void setDirected(Directed d) {
        if (d == Directed.PARAMETER)
            throw new IllegalArgumentException("Parameters may not be defined"
                    + " on parameter PseudoClasses.");
        directed = d;
    }

    @Override
    public boolean validFor(GraphClass gc) {
        if (gc.isPseudoClass())
            return false;
        return super.validFor(gc);
    }

    /**
     * Set the complementary parameter and this as a complement of
     * thecomplement.
     * @param thecomplement
     *            this parameters' complement
     */
    public void setComplement(GraphParameter thecomplement) {
        this.complement = thecomplement;
        if (thecomplement != null)
            thecomplement.complement = this;
    }

    /**
     * @return the complement to this parameter
     */
    public GraphParameter getComplement() {
        return complement;
    }

    /**
     * Create a new parameter with the given name and graph and adds its
     * pseudoclass as node to the given graph.
     * @param id
     *            the id of the parameter.
     * @param name
     *            the name of the parameter.
     * @param g
     *            the graph in which the inclusion-hierarchy is stored.
     * @return the created GraphParameter.
     */
    public static GraphParameter createParameter(Integer id, String name,
            DirectedGraph<GraphClass, Inclusion> g) {
        GraphParameter p = new GraphParameter(id, name, g);
        g.addVertex(p.getPseudoClass());
        if (deducing)
            parameters.add(p);
        return p;
    }

    // ===================== Boundedness on a node ===========================

    /**
     * Return the stored boundedness of this parameter on n. Return unknown if
     * nothing is stored.
     * @param n the GraphClass to get the Boundedness for
     * @return the boundedness of this on n
     */
    public Boundedness getBoundedness(GraphClass n) {
        Boundedness b = boundedAnn.getNode(n);
        return b == null ? Boundedness.UNKNOWN : b;
    }

    /**
     * Set the boundedness of this on n to b.
     * @param n the GraphClass to set the boundedness for
     * @param b the Boundedness to set
     */
    public void setBoundedness(GraphClass n, Boundedness b) {
        if (n.isPseudoClass())
            throw new UnsupportedOperationException(
                    "Parameter complexity on a pseudoclass.");
        boundedAnn.setNode(n, b);
    }

    /**
     * Return the boundedness of this parameter on n, as derived in the last
     * completed step. Meant to be used internally and for XML writing.
     * @param n the GraphClass to get the derived boundedness for
     * @return the derived boundedness of this on n
     */
    public Boundedness getDerivedBoundedness(GraphClass n) {
        ParameterOnNode pon = proofAnn.getNode(n);

        return pon == null ? Boundedness.UNKNOWN
                : pon.getBoundedness(GraphParameter.currentStep > 0 ?
                        GraphParameter.currentStep - 1
                        : 0);
    }

    /**
     * Get the boundedness of n, consulting the superparameter, too. (Rule 4
     * parameters.pdf)
     * @param n the GraphClass for which we want to get data
     * @return the deduced boundedness by parameters which bound this parameter
     */
    protected Boundedness getSuperDerivedBoundedness(GraphClass n) {
        if (n.isPseudoClass())
            throw new UnsupportedOperationException(
                    "Boundedness for parameter pseudoclasses is not defined.");

        Boundedness b = getDerivedBoundedness(n);
        if (!GAlg.inNeighboursOf(graph, pseudoclass).hasNext())
            return b;

        Boundedness sb = Boundedness.UNKNOWN;
        for (GraphClass par : GAlg.inNeighboursOf(graph, pseudoclass)) {
            sb = fromSuper(((PseudoClass) par).getParameter()
                    .getDerivedBoundedness(n));
            if (!b.isCompatible(sb))
                throw new Error("Inconsistent data for " + n + " " + name);
            if (sb.betterThan(b))
                b = sb;
        }
        return b;
    }

    /**
     * If the superparameter has boundedness b on a graph class, return the
     * boundedness for this class. (Rule 4 parameters.pdf)
     * @param b
     *            the boundedness of the superparameter.
     * @return the resulting boundedness.
     */
    private Boundedness fromSuper(Boundedness b) {
        if (b.betterOrEqual(Boundedness.BOUNDED))
            return b;
        return Boundedness.UNKNOWN;
    }

    /**
     * Get the boundedness of n, consulting the subparameter, too (Rule 5
     * parameters.pdf)
     * @param n the GraphClass for which we want to get data
     * @return the deduced boundedness by parameters which are bounded by this
     *         parameter
     */
    protected Boundedness getSubDerivedBoundedness(GraphClass n) {
        if (n.isPseudoClass())
            throw new UnsupportedOperationException(
                    "Boundedness for parameter pseudoclasses is not defined.");

        Boundedness b = getDerivedBoundedness(n);
        if (!GAlg.outNeighboursOf(graph, pseudoclass).hasNext())
            return b;

        Boundedness sb = Boundedness.UNKNOWN;
        for (GraphClass par : GAlg.outNeighboursOf(graph, pseudoclass)) {
            sb = fromSub(((PseudoClass) par).getParameter()
                    .getDerivedBoundedness(n));
            if (!b.isCompatible(sb))
                throw new Error("Inconsistent data for " + n + " " + name);
            if (sb.betterThan(b))
                b = sb;
        }
        return b;
    }

    /**
     * If the subparameter has boundedness b on a graph class, return the
     * boundedness for this class. (Rule 5 parameters.pdf)
     * @param b
     *            the boundedness of the subparameter.
     * @return the resulting boundedness.
     */
    private Boundedness fromSub(Boundedness b) {
        if (b.isUnbounded())
            return b;
        return Boundedness.UNKNOWN;
    }

    /**
     * Get the boundedness of n, consulting problems, too (Rule 10,
     * parameters.pdf).
     * @param n the GraphClass for which we want to get data
     * @param p the Problem that is consulted
     * @return unbounded, if there are incompatible complexities for p and the
     *         derived boundedness in the current step else
     */
    protected Boundedness getProblemDerivedBoundedness(GraphClass n, Problem p) {
        if (n.isPseudoClass())
            throw new UnsupportedOperationException(
                    "Boundedness for parameter pseudoclasses is not defined.");

        Boundedness b = getDerivedBoundedness(n);
        if (!b.isUnknown())
            return b;

        Complexity c = p.getDerivedComplexity(n);
        ParamComplexity parc = p.getDerivedComplexity(pseudoclass);
        if (!parc.isCompatible(c))
            b = Boundedness.UNBOUNDED;

        return b;
    }

    /**
     * If complement has boundedness b on co-G, return the boundedness for this
     * parameter.
     * @param b the boundedness for the complement on co-G
     * @return the boundedness for this parameter on G (at the current state it
     *         is the same)
     */
    public Boundedness complementBoundedness(Boundedness b) {
        return b.betterOrEqual(Boundedness.BOUNDED) ? Boundedness.BOUNDED : b;
    }

    // ======================= Boundedness Proofs ============================

    /**
     * Add a boundedness proof for this parameter on a graphclass n and update
     * boundedness on n.
     * @param n the GraphClass to add a proof for
     * @param b the BoundednessProof to add
     */
    protected void addProof(GraphClass n, BoundednessProof b) {
        if (!graph.containsVertex(n) || n.isPseudoClass())
            throw new IllegalArgumentException("Invalid node");
        if (b.getParameter() != this)
            throw new IllegalArgumentException("Invalid proof " + b);

        ParameterOnNode pon = proofAnn.getNode(n);
        if (pon == null) {
            pon = new ParameterOnNode(this, n);
            proofAnn.setNode(n, pon);
        }

        pon.addProof(b, GraphParameter.currentStep);
    }

    /**
     * Add all proofs in iter to n.
     * @param n the GraphClass to add proofs for
     * @param iter the collection of proofs to add
     */
    protected void addProofs(GraphClass n, Iterator iter) {
        while (iter.hasNext())
            addProof(n, (BoundednessProof) iter.next());
    }

    /**
     * Create a new boundedness proof for this parameter on a node n, add it to
     * node n and return it. n may be null.
     * @param n the GraphClass to create a proof for
     * @param boundedness the Boundedness the created proof proves
     * @param refs references for the proof
     * @return the created proof 
     */
    public BoundednessProof createProof(GraphClass n, Boundedness boundedness,
            List refs) {
        BoundednessProof res = new BoundednessProof(this, n, boundedness, refs);
        if (n != null)
            addProof(n, res);
        return res;
    }

    /**
     * Create a new algorithm for this parameter on a node n with a simple
     * explanation (Note text), add it to node n and return it. n may be null.
     * @param n the GraphClass to create a proof for
     * @param boundedness the Boundedness the created proof proves
     * @param why a note that proves the boundedness
     * @return the created proof 
     */
    public BoundednessProof createProof(GraphClass n, Boundedness boundedness,
            String why) {
        List refs = new ArrayList();
        refs.add(new Note(why, null));
        return createProof(n, boundedness, refs);
    }

    /**
     * Get the proof for this parameter that work on node n or null if there
     * are none.
     * @param n the GraphClass to get proofs for
     * @return the proof-set for n
     */
    protected HashSet<BoundednessProof> getProofSet(GraphClass n) {
        if (proofAnn == null)
            return null;

        ParameterOnNode pon = proofAnn.getNode(n);
        return pon == null ? null : pon.getProofSet();
    }

    /**
     * Return an iterator over the proofs for this parameter on the given node.
     * Never returns null.
     * @param n the GraphClass to get proofs for
     * @return the proof-iterator for n
     */
    public Iterator<BoundednessProof> getProofs(GraphClass n) {
        HashSet<BoundednessProof> hash = getProofSet(n);
        if (hash == null)
            hash = new HashSet<BoundednessProof>();
        return hash.iterator();
    }

    // ================= Distribution of boundedness proofs ===================
    /**
     * Distribute the boundedness proofs for this parameter over all nodes.
     * initProof/addProof must have been called for all parameters. Assumes
     * graph is transitively closed!
     */
    protected void distributeProofs() {
        Boundedness b;
        Map<GraphClass, Set<GraphClass>> scc = GAlg.calcSCCMap(graph);

        // ---- Add every set of algorithms to the super/subnodes' set. ----
        for (GraphClass n : graph.vertexSet()) {
            if (!n.isPseudoClass()) {
                HashSet<BoundednessProof> proofs = getProofSet(n);
                if (proofs != null) {
                    b = getDerivedBoundedness(n);
                    if (b.distributesUp())
                        distribute(proofs, GAlg.inNeighboursOf(graph, n));
                    else if (b.distributesDown())
                        distribute(proofs, GAlg.outNeighboursOf(graph, n));
                    else if (b.distributesEqual())
                        distribute(proofs, scc.get(n));
                }
            }
        }
    }

    /**
     * Distribute the proofs for the parameters that bound this parameter to
     * this parameter.
     */
    protected void distributeSuper() {
        Boundedness b;

        for (GraphClass n : graph.vertexSet()) {
            if (!n.isPseudoClass()) {
                for (GraphClass par : GAlg.inNeighboursOf(graph, pseudoclass)) {
                    b = fromSuper(((PseudoClass) par).getParameter()
                            .getSuperDerivedBoundedness(n));
                    if (!b.isUnknown())
                        addProof(
                                n,
                                getSubProof(b,
                                        ((PseudoClass) par).getParameter()));
                }
            }
        }
    }

    /**
     * Return a boundedness proof for boundedness b on a class, assuming the
     * superparameter is bounded.
     * @param b
     *            the boundedness to get a proof for.
     * @param sup
     *            the superparameter of this (sup >= this).
     * @return the resulting proof.
     */
    public BoundednessProof getSubProof(Boundedness b, GraphParameter sup) {
        final String why = "from " + sup.getName();
        if (subProofs.get(sup) == null)
            subProofs.put(sup, new HashSet<BoundednessProof>());
        for (BoundednessProof p : subProofs.get(sup)) {
            if (p.getBoundedness().equals(b))
                return p;
        }

        BoundednessProof p = createProof(null, b, why);
        subProofs.get(sup).add(p);
        return p;
    }

    /**
     * Distribute the proofs for the parameters that are bounded by this
     * parameter to this parameter.
     */
    protected void distributeSub() {
        Boundedness b;

        for (GraphClass n : graph.vertexSet()) {
            if (!n.isPseudoClass()) {
                for (GraphClass par : GAlg.outNeighboursOf(graph, pseudoclass)) {
                    b = fromSub(((PseudoClass) par).getParameter()
                            .getSubDerivedBoundedness(n));
                    if (!b.isUnknown())
                        addProof(
                                n,
                                getSuperProof(b,
                                        ((PseudoClass) par).getParameter()));
                }
            }
        }
    }

    /**
     * Return a boundedness proof for boundedness b on a class, assuming the
     * subparameter is bounded.
     * @param b
     *            the boundedness to get a proof for.
     * @param sub
     *            the subparameter of this (sub <= this).
     * @return the resulting proof.
     */
    public BoundednessProof getSuperProof(Boundedness b, GraphParameter sub) {
        final String why = "from " + sub.getName();
        if (superProofs.get(sub) == null)
            superProofs.put(sub, new HashSet<BoundednessProof>());
        for (BoundednessProof p : superProofs.get(sub)) {
            if (p.getBoundedness().equals(b))
                return p;
        }

        BoundednessProof p = createProof(null, b, why);
        superProofs.get(sub).add(p);
        return p;
    }

    /**
     * Distribute the proofs for unboundedness by incompatibilities for
     * complexities.
     */
    protected void distributeProblems() {
        Boundedness b;

        for (GraphClass n : graph.vertexSet()) {
            if (!n.isPseudoClass()) {
                b = getDerivedBoundedness(n);
                if (b.isUnknown()) {
                    for (Problem p : problems) {
                        b = getProblemDerivedBoundedness(n, p);
                        if (!b.isUnknown())
                            addProof(n, getProblemProof(b, p));
                    }
                }
            }
        }
    }

    /**
     * Return a boundedness proof on a class for boundedness b, assuming there
     * are incompatibilities in complexities for problem.
     * @param b the resulting (un-)boundedness to get a proof for
     * @param p the problem with which there are incompatible complexities
     * @return a proof from the node-independent collection or a new one that
     *         is added to the collection
     */
    protected BoundednessProof getProblemProof(Boundedness b, Problem p) {
        final String why = "from incompatible complexities for " + p.getName();
        if (probProofs.get(p) == null)
            probProofs.put(p, new HashSet<BoundednessProof>());
        for (BoundednessProof proof : probProofs.get(p)) {
            if (proof.getBoundedness().equals(b))
                return proof;
        }

        BoundednessProof proof = createProof(null, b, why);
        probProofs.get(p).add(proof);
        return proof;
    }

    /**
     * Distribute the proofs for this parameter over all nodes via the
     * complement. initProof/addProof must have been called for all parameters.
     * distributeProofs must have been called for this parameter, and all
     * super/sub parameters. Assumes the graph is transitively closed and the
     * complement index is set!
     */
    public void distributeComplement() {
        if (complement == null)
            return;

        GraphClass con;
        Boundedness nc, conc;

        for (GraphClass n : graph.vertexSet()) {
            if (!(n instanceof ComplementClass))
                continue;
            con = ((ComplementClass) n).getBase();
            nc = getDerivedBoundedness(n);
            conc = complement.getDerivedBoundedness(con);
            if (!nc.isCompatible(complementBoundedness(conc))) {
                System.err.println("ComplexityClash: " + n + " " + this.name
                        + "=" + nc + " but " + con + " "
                        + this.complement.name + "=" + conc);
            } else if (nc.isUnknown() && !conc.isUnknown()) {
                addProof(n, getComplementProof(complementBoundedness(conc)));
            } else if (conc.isUnknown() && !nc.isUnknown()) {
                complement.addProof(con, complement
                        .getComplementProof(complement
                                .complementBoundedness(nc)));
            }
        }
    }

    /**
     * Try moving boundedness information UP to union nodes. We only change the
     * boundedness for Union nodes, and do not generate new references. The
     * reasoning is: If the parameter is bounded on every part of the union,
     * then we can apply all part boundedness proofs. So the parameter is
     * solvable bounded on the union.
     */
    protected void distributeUpUnion() {
        int i;
        boolean ok;
        Boundedness b;

        for (GraphClass n : graph.vertexSet()) {
            if (!(n instanceof UnionClass)
                    || getDerivedBoundedness(n).betterOrEqual(
                            Boundedness.BOUNDED))
                continue;

            // ---- Check whether all parts are bounded ----
            ok = true;
            for (GraphClass part : ((UnionClass) n).getSet()) {
                if (!getDerivedBoundedness(part).betterOrEqual(
                        Boundedness.BOUNDED)) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                // System.err.println("NOTE: distributeUpUnion invoked on "+
                // n.getName()+" "+toString());
                createProof(n, Boundedness.BOUNDED,
                        "From the constituent classes.");
            }
        }
    }

    /**
     * Do special deductions for a particular parameter.
     * Default implementation does nothing.
     */
    protected void distributeSpecial() {}

    /**
     * Adds the proofs in proofs to the classes in nodes.
     * @param proofs
     *            the proofs to add to the classes
     * @param nodes
     *            the nodes the proofs are added to
     */
    protected void distribute(HashSet<BoundednessProof> proofs,
            Iterable<GraphClass> nodes) {
        for (GraphClass n : nodes) {
            addProofs(n, proofs.iterator());
        }
    }

    // -------------------- Derived Proofs -----------------------------------

    /**
     * Find in the given list a proof of the requested boundedness. If it
     * doesn't exist yet, create it node-independently with the given
     * boundedness and text.
     * @param l the List to find a proof in
     * @param b the requested boundedness
     * @param why a text to prove boundedness if the proof doesn't exist yet
     * @return the resulting BoundednessProof
     */
    protected BoundednessProof getDerivedProof(List<BoundednessProof> l,
            Boundedness b, String why) {
        for (BoundednessProof a : l)
            if (a.getBoundedness().equals(b))
                return a;

        BoundednessProof a = createProof(null, b, why);
        l.add(a);
        return a;
    }

    /**
     * Return an proof for boundedness b of this on a class, assuming the
     * complement is bounded on co-G.
     * @param b the boundedness to get a proof for
     * @return either a proof from the list of coProofs or a new one
     */
    protected BoundednessProof getComplementProof(Boundedness b) {
        final String why = "from " + complement + " on the complement";

        if (coProofs == null)
            coProofs = new ArrayList<BoundednessProof>();
        return getDerivedProof(coProofs, b, why);
    }

    // ================= Controlling the deduction process ====================
    /**
     * Boundedness values are deduced in multiple steps, as follows:
     * Repeat twice:
     * - Proofs for parameter on node or on a super/subclass of node.
     * - Derived from previous step by super/sub parameters
     * - Derived from previous step by union/intersect/special
     * Derived from previous step by complement parameters.
     * Repeat twice:
     * - Proofs for parameter on node or on a super/subclass of node.
     * - Derived from previous step by super/sub parameters and problems
     * - Derived from previous step by union/special
     */
    static final int STEPS = 4 * 3 + 1;
    /** The current step. */
    private static int currentStep;

    /**
     * Call this before reading the graph when you're going to deduce.
     */
    public static void setDeducing() {
        deducing = true;
        currentStep = 0;
        parameters = new ArrayList<GraphParameter>();
    }

    /**
     * Perform a single sequence of boundedness deductions (3 steps), without
     * complement.
     */
    public static void distributeBoundednessBasic() {
        for (GraphParameter p : parameters)
            p.distributeProofs();
        currentStep++;

        for (GraphParameter p : parameters) {
            p.distributeSuper();
            p.distributeSub();
            p.distributeProblems();
        }
        currentStep++;

        for (GraphParameter p : parameters) {
            p.distributeUpUnion();
            p.distributeSpecial();
        }
        currentStep++;
    }

    /**
     * Distribute/deduce the boundedness proofs and values.
     */
    public static void distributeBoundednesses() {
        // distributeBoundednessBasic();
        // distributeBoundednessBasic();

        for (GraphParameter p : parameters)
            p.distributeComplement();
        currentStep++;

        // distributeBoundednessBasic();
        // distributeBoundednessBasic();
    }
}

/* EOF */
