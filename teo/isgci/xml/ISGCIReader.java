/*
 * SAX parser event handler for all ISGCI data.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */


package teo.isgci.xml;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.jgrapht.DirectedGraph;

import java.util.*;
import java.io.StringWriter;
import java.io.PrintWriter;

import teo.isgci.gc.*;
import teo.isgci.grapht.*;
import teo.isgci.parameter.*;
import teo.isgci.problem.*;
import teo.isgci.ref.*;
import teo.isgci.relation.*;

public class ISGCIReader extends DefaultHandler{
    
    private StringBuffer chunks;
    private Locator locator;
    
    /* ISGCI */
    DirectedGraph<GraphClass,Inclusion> graph;

    /* Graphclasses */
    private HashMap<Integer,GraphClass> classes;         // key = id, obj = gc   //intID
    private List<GraphClassWrapper> todo;// Save Wrappers that are not yet done
    private GraphClassWrapper curClass;

    /* Inclusions */
    private Inclusion curIncl;
    private AbstractRelation curRel;
    private List<AbstractRelation> relations;
    private String rel; // The string representing the parameter relation.

    /* Parameters (added by vector) */
    private HashMap<String, GraphParameter> parameterNames;
    private HashMap<Integer, GraphParameter> parameterIDs;
    private List<GraphParameter> parameters;
    private List<GraphParameterWrapper> parTodo;
    private GraphParameterWrapper curGraphPar;
    private List<BoundednessProofWrapper> proofs;
    private BoundednessProofWrapper curProof;

    /* Problems */
    private Hashtable problemNames;
    private List<Problem> problems;
    private List<AlgoWrapper> algos;               // All read algo elements
    private AlgoWrapper curAlgo;
    private Problem curProblem;
    private List<ReductionWrapper> reductionsTodo;
    private List<ParamAlgoWrapper> paramAlgos;
    private ParamAlgoWrapper curParAlgo;

    /* References */
    private List refs;                // Refs for the current element
    private String noteName;
    
    /* Statistics */
    private String date;
    private String nodecount;
    private String edgecount;

    private boolean parsingDone;
    

    /**
     * Creates a reader that uses g for storing data. Data indices will be set
     * by the reader.
     */
    public ISGCIReader(DirectedGraph<GraphClass,Inclusion> g,
            List<Problem> problems, List<GraphParameter> parameters) {
        parsingDone = false;
        chunks = new StringBuffer();
        this.graph = g;
        this.parameters = parameters;
        this.problems = problems;
        parameterNames = new HashMap<String, GraphParameter>();
        parameterIDs = new HashMap<Integer, GraphParameter>();
        problemNames = new Hashtable();
        classes = new HashMap<Integer,GraphClass>(); //intID
        todo = new ArrayList<GraphClassWrapper>();
        parTodo = new ArrayList<GraphParameterWrapper>();
        proofs = new ArrayList<BoundednessProofWrapper>();
        algos = new ArrayList<AlgoWrapper>();
        paramAlgos = new ArrayList<ParamAlgoWrapper>();
        reductionsTodo = new ArrayList<ReductionWrapper>();
        relations = new ArrayList<AbstractRelation>();
    }
    
    public DirectedGraph<GraphClass,Inclusion> getGraph() {
        return graph;
    }

    public List<AbstractRelation> getRelations() {
        return relations;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public List<GraphParameter> getGraphParameters() {
        return parameters;
    }

    /** ContentHandler interface */
    public void setDocumentLocator(Locator l) {
        locator = l;
    }

    /** ContentHandler Interface */
    public void startDocument() {
    }
    
    /** ContentHandler Interface */
    public void endDocument() {
    }
    
    public String getDate() {
        return date;
    }    
    
    public String getNodeCount() {
        return nodecount;
    }    
    
    public String getEdgeCount() {
        return edgecount;
    }    
    
    /** ContentHandler Interface */
    public void startElement(String uri, String locName, String qName, 
             Attributes atts) throws SAXException {
        try {
        
            
        //---- Statistics ----
        if (Tags.STATS.equals(qName)) {
            date = atts.getValue(Tags.DATE);
            nodecount = atts.getValue(Tags.NODECOUNT);
            edgecount = atts.getValue(Tags.EDGECOUNT);
        } else

        //---- GraphClasses ----
        if (Tags.GRAPHCLASSES.equals(qName)) {
            // Add the problem reductions
            int i, size, oldsize;
            oldsize = reductionsTodo.size();
            while ((size = reductionsTodo.size()) != 0) {
                for (i = size-1; i >= 0; i--) {
                    if (reductionsTodo.get(i).generate())
                        reductionsTodo.remove(i);
                }
                /*Iterator<ReductionWrapper> iter = reductionsTodo.iterator();
                while (iter.hasNext())
                    if (iter.next().generate())
                        iter.remove();*/
                if (reductionsTodo.size() == oldsize) {
                    System.err.println(size+" problems not resolved");
                    System.err.println(reductionsTodo);
                    return;
                }
                oldsize = size;
            }
        } else
        
        //---- GraphClass ----
        if (Tags.GRAPHCLASS.equals(qName)) {
            curClass = new GraphClassWrapper(Integer.parseInt(atts.getValue(Tags.ID)), //intID parseInt
                    atts.getValue(Tags.TYPE), atts.getValue(Tags.DIRTYPE));
        } else if (Tags.HERED.equals(qName)) {
            curClass.hered = atts.getValue(Tags.TYPE);
        } else if (Tags.SELFCO.equals(qName)) {
            curClass.selfComplementary = true;
        } else if (Tags.CLIQUEFIXED.equals(qName)) {
            curClass.cliqueFixed = true;
        } else

        //---- Inclusion/relation ----
        if (Tags.INCLUSION.equals(qName)  || Tags.EQU.equals(qName)) {
   
        	GraphClass gcsuper = classes.get(Integer.parseInt(
                    Tags.INCLUSION.equals(qName) ? atts.getValue(Tags.SUPER) : atts.getValue(Tags.GC1)));//intID
        	GraphClass gcsub = classes.get(Integer.parseInt(
                    Tags.INCLUSION.equals(qName) ? atts.getValue(Tags.SUB) : atts.getValue(Tags.GC2)));

            if (gcsuper == gcsub)
                throw new SAXException("super = sub = "+ gcsuper.getID());
            /*System.out.println(
                    atts.getValue(gcsuper) +" -> "+ atts.getValue(gcsub) +" "+
                    classes.get(atts.getValue(gcsuper)) +" ->"+
                    classes.get(atts.getValue(gcsub)) );*/
            if (graph.containsEdge(gcsuper, gcsub))
                throw new SAXException("Edge "+ gcsuper.getID() +" -> "+
                        gcsub.getID() +" already exists");
            if (gcsuper.getDirected() != gcsub.getDirected())
                throw new SAXException("Edge with unmatched directedness "+
                        gcsuper.getID() +" -> "+ gcsub.getID());
            curIncl = graph.addEdge(gcsuper, gcsub);
            curIncl.setProper(atts.getValue(Tags.PROPER) != null);
            curIncl.setConfidence(Tags.string2confidence(
                    atts.getValue(Tags.CONFIDENCE)));
            refs = new ArrayList();

        } else if (Tags.DISJOINT.equals(qName)  ||
                Tags.INCOMPARABLE.equals(qName)  ||
                Tags.OPEN.equals(qName)) {
            if (atts.getValue(Tags.GC1) == atts.getValue(Tags.GC2))
                throw new SAXException("gc1 = gc2 = "+
                        atts.getValue(Tags.GC1));
            GraphClass gc1 = classes.get(Integer.parseInt(atts.getValue(Tags.GC1))); //intID
            GraphClass gc2 = classes.get(Integer.parseInt(atts.getValue(Tags.GC2))); //intID
            if (gc1.getDirected() != gc2.getDirected())
                throw new SAXException("Relation with unmatched directedness "+
                        gc1.getID() +" -> "+ gc2.getID());
            if (Tags.DISJOINT.equals(qName))
                curRel = new Disjointness(gc1, gc2);
            else if (Tags.INCOMPARABLE.equals(qName))
                curRel = new Incomparability(gc1, gc2);
            else
                curRel = new Open(gc1, gc2);
            curRel.setConfidence(Tags.string2confidence(
                    atts.getValue(Tags.CONFIDENCE)));
            for (AbstractRelation r : relations)
                if (r.get1() == curRel.get1()  &&  r.get2() == curRel.get2())
                    throw new SAXException(
                        "An incomparability, disjointness or open between "+
                        curRel.get1().getID() +" and "+ curRel.get2().getID() +
                        " already exists.");
            relations.add(curRel);
            refs = new ArrayList();
        } else

        // ---- Parameter stuff (added by vector) ----
        if (Tags.PARAMETER_DEF.equals(qName)) {

            // System.out.println("ID: "+atts.getValue(Tags.ID)+
            // " Name: "+atts.getValue(Tags.NAME));
            curGraphPar = new GraphParameterWrapper(
                        Integer.parseInt(atts.getValue(Tags.ID)),
                        atts.getValue(Tags.NAME),
                        atts.getValue(Tags.DIRTYPE),
                        atts.getValue(Tags.PARAMETER_DECOMP),
                        atts.getValue(Tags.PARAMETER_COMPLEMENT));
        } else
        // ---- Parameter-Boundedness ----
        if (Tags.BOUNDED.equals(qName)) {
            curProof = new BoundednessProofWrapper(curClass.id,
                    atts.getValue(Tags.NAME),
                    atts.getValue(Tags.BOUNDEDNESS));
        } else
        // ---- "parameter"-Tag ----
        if (Tags.PARAMETER.equals(qName)) {
            curClass.boundednesses.add(new BoundednessWrapper(
                    parameterNames.get(atts.getValue(Tags.NAME)),
                    atts.getValue(Tags.BOUNDEDNESS) != null ? Boundedness
                            .getBoundedness(atts
                                    .getValue(Tags.BOUNDEDNESS))
                            : Boundedness.UNKNOWN));
        } else

        // ---- Relations ----
        if (Tags.PARAM_RELATION.equals(qName)) {
            PseudoClass pgc1;
            PseudoClass pgc2;
            rel = atts.getValue(Tags.PAR_ATT_REL);

                pgc1 = parameterIDs.get(
                        Integer.parseInt(atts.getValue(Tags.PARAM1)))
                        .getPseudoClass();
                pgc2 = parameterIDs.get(
                        Integer.parseInt(atts.getValue(Tags.PARAM2)))
                        .getPseudoClass();
            if (pgc1 == pgc2)
                throw new SAXException("param1 = param2 = " + pgc1.getID());
            // Case: Relation ">=" (bounds) or "=" (equ) or ">" (strictly
            // bounds)
            if (rel.equals(Tags.PAR_BOUNDS) || rel.equals(Tags.PAR_EQU)
                    || rel.equals(Tags.PAR_STRICT_BOUNDS)) {
                if (graph.containsEdge(pgc1, pgc2))
                    throw new SAXException("Edge " + pgc1.getID() + " -> "
                            + pgc2.getID() + " already exists");
                if (pgc1.getDirected() != pgc2.getDirected())
                    throw new SAXException(
                            "Edge with unmatched directedness " + pgc1.getID()
                                    + " -> " + pgc2.getID());
                curIncl = graph.addEdge(pgc1, pgc2);
                if (rel.equals(Tags.PAR_STRICT_BOUNDS))
                    curIncl.setProper(true);
                for (AbstractRelation r : relations)
                    if (r.get1() == pgc2 && r.get2() == pgc1)
                        curIncl.setProper(true);
                curIncl.setConfidence(Tags.string2confidence(atts
                        .getValue(Tags.CONFIDENCE)));
                curIncl.setFunctiontype(atts.getValue(Tags.FUNCTIONTYPE));
            } else
            // Case: Relation "not >=" (not bounds) or open
            if (rel.equals(Tags.PAR_NOT_BOUNDS)|| rel.equals(Tags.OPEN)) {
                if (pgc1.getDirected() != pgc2.getDirected())
                     throw new SAXException(
                            "Relation with unmatched directedness "
                                    + pgc1.getID() + " -> " + pgc2.getID());

                if (rel.equals(Tags.PAR_NOT_BOUNDS))
                    curRel = new NotBounds(pgc1, pgc2);
                else
                    curRel = new Open(pgc1, pgc2);

                curRel.setConfidence(Tags.string2confidence(atts
                        .getValue(Tags.CONFIDENCE)));
                if (graph.containsEdge(pgc2, pgc1)
                        && curRel instanceof NotBounds)
                    graph.getEdge(pgc2, pgc1).setProper(true);
                for (AbstractRelation r : relations)
                    if (r.get1() == curRel.get1()
                            && r.get2() == curRel.get2())
                        throw new SAXException("A not >= or open between "
                                + curRel.get1() + " and " + curRel.get2()
                                + " already exists.");
                relations.add(curRel);
            } else {
                    throw new IllegalArgumentException(
                            "Unknown relation type for graphparameters: "
                                    + rel);
            }
            refs = new ArrayList();
        }

        //---- Problem stuff ----
        if (Tags.PROBLEM_DEF.equals(qName)) {
            Problem p;
            String compl = atts.getValue(Tags.PROBLEM_COMPLEMENT);
            p = Problem.createProblem(atts.getValue(Tags.NAME), graph);
            p.setDirected(Tags.problemString2directed(
                    atts.getValue(Tags.DIRTYPE)));
            problems.add(p);
            problemNames.put(p.getName(), p);
            if (compl != null) {
                Problem c = (Problem) problemNames.get(compl);
                if (c == null)
                    throw new SAXException("Complement problem "+ compl +
                            "not found.");
                p.setComplement((Problem) c);
            }
            curProblem = p;
            refs = new ArrayList();

        } else if (Tags.PROBLEM_SPARSE.equals(qName)) {
            curProblem.setSparse();
        } else if (Tags.PROBLEM_FORPARAMS.equals(qName)) {
            curProblem.setParameters(true);
        } else if (Tags.PROBLEM_FROM.equals(qName)) {
            String from = atts.getValue(Tags.NAME);
            Complexity c = Complexity.getComplexity(
                    atts.getValue(Tags.COMPLEXITY));
            reductionsTodo.add(new ReductionWrapper(curProblem, from, c));

        } else if (Tags.ALGO.equals(qName)) {
            curAlgo = new AlgoWrapper(curClass.id, atts.getValue(Tags.NAME),
                atts.getValue(Tags.COMPLEXITY), atts.getValue(Tags.BOUNDS));

        } else if (Tags.PROBLEM.equals(qName)) {
            curClass.complexities.add(new ProblemWrapper(
                (Problem) problemNames.get(atts.getValue(Tags.NAME)),
                atts.getValue(Tags.COMPLEXITY) != null ?
                    Complexity.getComplexity(atts.getValue(Tags.COMPLEXITY)) :
                    Complexity.UNKNOWN));
            // ---- Parameter-Algorithm ----
        } else if (Tags.PAR_ALGO.equals(qName)) {
            curParAlgo = new ParamAlgoWrapper(curGraphPar.id,
                    atts.getValue(Tags.NAME),
                    atts.getValue(Tags.COMPLEXITY),
                    atts.getValue(Tags.BOUNDS));
            // ---- Parameter-Problem ----
        } else if (Tags.PAR_PROBLEM.equals(qName)) {
            curGraphPar.complexities
                    .add(new ParamProblemWrapper(
                          (Problem) problemNames.get(atts.getValue(Tags.NAME)),
                          atts.getValue(Tags.COMPLEXITY) != null ?
                              ParamComplexity.getComplexity(
                                      atts.getValue(Tags.COMPLEXITY))
                              : ParamComplexity.UNKNOWN));
        } else

        //---- References ----
        if (Tags.NOTE.equals(qName)) {
            chunks.setLength(0);
            noteName = atts.getValue(Tags.NAME);
        } else if (Tags.REF.equals(qName) ||  Tags.SMALLGRAPH.equals(qName) ||
                Tags.GCREF.equals(qName)  ||  Tags.NAME.equals(qName)) {
            chunks.setLength(0);
        }
        
        } catch (Exception e) {
            String s = "Line "+ Integer.toString(locator.getLineNumber()) +
                "\nColumn "+ Integer.toString(locator.getColumnNumber()) +
                "\nId "+ qName +
                e.toString();
            throw new SAXException(s);
        }
    }
    
    /** ContentHandler Interface */
    public void endElement(String uri, String locName, String qName)
            throws SAXException {
        try {

        //---- ISGCI ----
        if (Tags.ROOT_ISGCI.equals(qName)) {
            parsingDone = true;
        }

        //---- GraphClasses ----
        if (Tags.GRAPHCLASSES.equals(qName)) {
            // First generate the outstanding graphclasses.
            int i, size, oldsize = todo.size();
            while ((size = todo.size()) != 0) {
                for (i = size-1; i >= 0; i--) {
                    if (todo.get(i).generate())
                        todo.remove(i);
                }
                if (todo.size() == oldsize) {
                    System.err.println(size+" classes not resolved");
                    System.err.println(todo);
                    return;
                }
                oldsize = size;
            }
            //System.out.println(classes.size()+" classes successfully read");

            // Then create the Complexities.
            for (AlgoWrapper aw : algos)
                aw.generate();
            for (BoundednessProofWrapper pw : proofs)
                pw.generate();

        } else if (Tags.GRAPHCLASS.equals(qName)) {
            curClass.end();
            if (!curClass.generate())
                todo.add(curClass);
        } else if (Tags.NAME.equals(qName)) {
            curClass.name = new String(chunks.toString());
        } else if (Tags.SMALLGRAPH.equals(qName) || Tags.GCREF.equals(qName)) {
            if (Tags.INTER.equals(curClass.type) ||
                    Tags.FORBID.equals(curClass.type) ||
                    Tags.UNION.equals(curClass.type)) {
                curClass.set.add(new String(chunks.toString()));
            } else if (Tags.INDHERED.equals(curClass.type)  ||
                    Tags.CONHERED.equals(curClass.type) ||
                    Tags.ISOHERED.equals(curClass.type)  ||
                    Tags.PROBE.equals(curClass.type)  ||
                    Tags.CLIQUE.equals(curClass.type)  ||
                    Tags.COMPL.equals(curClass.type)) {
                if (curClass.base == null)
                    curClass.base = new String(chunks.toString());
                else
                    throw new SAXException("More than one "+qName+" in "+
                        curClass.type);
            } else
                throw new SAXException("Unexpected "+qName);
        } else

        //---- Inclusions ----
        if (Tags.INCLUSION.equals(qName)) {
            curIncl.setRefs(refs);
        } else if (Tags.DISJOINT.equals(qName)  ||
                Tags.INCOMPARABLE.equals(qName)) {
            curRel.setRefs(refs);
        } else

        if (Tags.EQU.equals(qName)) {
            curIncl.setRefs(refs);
            Inclusion revIncl = graph.addEdge(
                    graph.getEdgeTarget(curIncl),
                    graph.getEdgeSource(curIncl) );
            revIncl.setProper(false);
            revIncl.setConfidence(curIncl.getConfidence());
            revIncl.setRefs(new ArrayList(refs));
        } else

        //---- Parameters (added by vector) ----
        if (Tags.PARAMETERS.equals(qName)) {
            // First generate the outstanding graphparameters.
            int i, size, oldsize = parTodo.size();
            while ((size = parTodo.size()) != 0) {
                for (i = size - 1; i >= 0; i--) {
                    if (parTodo.get(i).generate())
                        parTodo.remove(i);
                }
                if (parTodo.size() == oldsize) {
                    System.err.println(size + " parameters not resolved");
                    System.err.println(parTodo);
                    return;
                }
                oldsize = size;
            }

            // Then create the Complexities.
            for (ParamAlgoWrapper aw : paramAlgos)
                aw.generate();
        } else
        // ---- Parameter ----
        if (Tags.PARAMETER_DEF.equals(qName)) {
            curGraphPar.end();
            if (!curGraphPar.generate())
                parTodo.add(curGraphPar);
        } else
        // ---- Parameter-Boundedness ----
        if (Tags.BOUNDED.equals(qName)) {
            curProof.end();
            proofs.add(curProof);
        } else
        // ---- Relations ----
        if (Tags.PARAM_RELATION.equals(qName)) {
           if (rel.equals(Tags.PAR_BOUNDS) || rel.equals(Tags.PAR_EQU)
                   || rel.equals(Tags.PAR_STRICT_BOUNDS)) {
                curIncl.setRefs(refs);
                if (rel.equals(Tags.PAR_EQU)) {
                    Inclusion revIncl = graph.addEdge(
                            graph.getEdgeTarget(curIncl),
                            graph.getEdgeSource(curIncl));
                    revIncl.setConfidence(curIncl.getConfidence());
                    revIncl.setRefs(new ArrayList(refs));
                } else if (rel.equals(Tags.PAR_STRICT_BOUNDS)) {
                    NotBounds revRel = new NotBounds(
                            (PseudoClass) graph.getEdgeTarget(curIncl),
                            (PseudoClass) graph.getEdgeSource(curIncl));
                    revRel.setConfidence(curIncl.getConfidence());
                    revRel.setRefs(new ArrayList(refs));
                    relations.add(revRel);
                }
            } else if (rel.equals(Tags.PAR_NOT_BOUNDS)) {
                curRel.setRefs(refs);
            }
        } else
        //---- Problems ----
        if (Tags.ALGO.equals(qName)) {
            curAlgo.end();
            algos.add(curAlgo);
        // ---- Parameter-Algorithm ----
        } else if (Tags.PAR_ALGO.equals(qName)) {
            curParAlgo.end();
            paramAlgos.add(curParAlgo);
        } else if (Tags.PROBLEM_DEF.equals(qName)) {
            curProblem.setRefs(new ArrayList(refs));
        } else

        //---- References ----
        if (Tags.NOTE.equals(qName)) {
            refs.add(new Note(new String(chunks.toString()), noteName));
        } else if (Tags.REF.equals(qName)) {
            refs.add(new Ref(new String(chunks.toString())));
        }

        } catch (Exception e) {
            e.printStackTrace();
            throw new SAXException(e.toString());
        }
    }
    
    /** ContentHandler Interface */
    public void characters(char[] ch, int start, int len) {
        chunks.append(ch, start, len);
    }

    
    //-------------------------- GraphClassWrapper -------------------------
    private class GraphClassWrapper {
        String type, name, dirtype;
        Integer id;				//intID
        String base;            // base class id for complement/hereditary
        HashSet<String> set;    // set for union/intersect/forbidden
        String hered;
        boolean selfComplementary;
        boolean cliqueFixed;
        List<ProblemWrapper> complexities;
        List<BoundednessWrapper> boundednesses;
        List refs, prevrefs;
        
        public GraphClassWrapper(Integer id, String type, String dirtype) { //intID
            name = null;
            base = null;
            set = null;
            hered = null;
            selfComplementary = false;
            cliqueFixed = false;
            complexities = new ArrayList<ProblemWrapper>();
            boundednesses = new ArrayList<BoundednessWrapper>();
            this.id = id;
            this.type = type;
            this.dirtype = dirtype;
            if (Tags.INTER.equals(type) ||
                    Tags.FORBID.equals(type) ||
                    Tags.UNION.equals(type)) {
                set = new HashSet<String>();
            }
            prevrefs = ISGCIReader.this.refs;
            ISGCIReader.this.refs = refs = new ArrayList();
        }

        public void end() {
            ISGCIReader.this.refs = prevrefs;
        }

        public boolean generate() throws SAXException { 
            GraphClass gc = null, base2;
            HashSet<GraphClass> set2;

            //---- Create class
            if (Tags.BASE.equals(type)) {
                gc = new BaseClass(name, Tags.graphString2directed(dirtype));
            } else if (Tags.FORBID.equals(type)) {
                gc = new ForbiddenClass(set); // still uses strings 
                gc.setName(name);
            } else if (Tags.COMPL.equals(type)) {
                if (base == null)
                    throw new SAXException(
                        "base class required for complement class "+id);
                base2 = classes.get(Integer.parseInt(base)); //intID
                if (base2 == null) {
                    return false;
                }
                gc = new ComplementClass(base2);
                gc.setName(name);
            } else if (Tags.ISOHERED.equals(type)) {
                if (base == null)
                    throw new SAXException(
                        "base class required for hereditary class "+id);
                base2 = classes.get(Integer.parseInt(base)); //intID
                if (base2 == null) {
                    return false;
                }
                gc = new IsometricHereditaryClass(base2);
                gc.setName(name);
            } else if (Tags.CONHERED.equals(type)) {
                if (base == null)
                    throw new SAXException(
                        "base class required for hereditary class "+id);
                base2 = classes.get(Integer.parseInt(base)); //intID
                if (base2 == null) {
                    return false;
                }
                gc = new ConnectedHereditaryClass(base2);
                gc.setName(name);
            } else if (Tags.INDHERED.equals(type)) {
                if (base == null)
                    throw new SAXException(
                        "base class required for hereditary class "+id);
                base2 = classes.get(Integer.parseInt(base)); //intID
                if (base2 == null) {
                    return false;
                }
                gc = new InducedHereditaryClass(base2);
                gc.setName(name);
            } else if (Tags.INTER.equals(type)) {
                set2 = new HashSet<GraphClass>();
                for (String s : set) {
                    base2 = classes.get(Integer.parseInt(s));  //Hier war der fehler intID
                    if (base2 == null) {
                        return false;    
                    }
                    set2.add(base2);
                }
                gc = new IntersectClass(set2,
                        Tags.graphString2directed(dirtype));
                gc.setName(name);
            } else if (Tags.UNION.equals(type)) {
                set2 = new HashSet<GraphClass>();
                for (String s : set) {
                    base2 = classes.get(Integer.parseInt(s)); //intID
                    if (base2 == null) {
                        return false;
                    }
                    set2.add(base2);
                }
                gc = new UnionClass(set2, Tags.graphString2directed(dirtype));
                gc.setName(name);
            } else if (Tags.PROBE.equals(type)) {
                if (base == null)
                    throw new SAXException(
                        "base class required for probe class "+id);
                base2 = classes.get(Integer.parseInt(base)); //intID
                if (base2 == null) {
                    return false;
                }
                gc = new ProbeClass(base2);
                gc.setName(name);
            } else if (Tags.CLIQUE.equals(type)) {
                if (base == null)
                    throw new SAXException(
                        "base class required for clique class "+id);
                base2 = classes.get(Integer.parseInt(base)); //intID
                if (base2 == null) {
                    return false;
                }
                gc = new CliqueClass(base2);
                gc.setName(name);
            }

            if (hered != null) {
                if (gc.getHereditariness() != GraphClass.Hered.UNKNOWN)
                    System.out.println(
                        "Warning: Changing hereditariness for " + id +
                        ": was "+gc.getHereditariness());
                gc.setHereditariness(Tags.string2hereditary(hered));
            }

            //---- Check if class already exists
            for (GraphClass other : classes.values()) {
                if (gc.equals(other))
                    throw new SAXException("Duplicate classses "+ id +" "+
                        other.getID());
            }
            
            //---- Complete it and add it
            gc.setSelfComplementary(selfComplementary);
            gc.setCliqueFixed(cliqueFixed);
            gc.setID(id);
            gc.setRefs(refs);
            graph.addVertex(gc);
            for (ProblemWrapper w : complexities) {
                w.problem.setComplexity(gc, w.complexity);
            }
            for (BoundednessWrapper w : boundednesses) {
                w.parameter.setBoundedness(gc, w.boundedness);
            }
            classes.put(id, gc);
            return true;
        }

        public String toString() {
            return "<GraphClass: "+id+" "+name+">";
        }
    }

    //------------------------ GraphParameterWrapper -----------------------
    /**
     * Serves as an all-in-one package for creating GraphParameters.
     * More information can be found in the constructor.
     * @author vector
     */
    private class GraphParameterWrapper {
        /**
         * Name of the Parameter.
         */
        private String name;
        /**
         * Its corresponding Parameter-ID.
         */
        private Integer id;
        /**
         * Corresponding Directedness ({@link Directed}) value.
         */
        private String dirtype;
        /**
         * Corresponding decomposition-time.
         */
        private String decomp;
        /**
         * It's complementary GraphParameter.
         */
        private String compl;
        /**
         * All complexities belonging to this parameter.
         */
        private List<ParamProblemWrapper> complexities;
        /**
         * Corresponding references ({@link Ref}).
         */
        private List refs;
        /**
         * Corresponding previous references.
         */
        private List prevrefs;

        /**
         * Constructs an all-in-one set containing the necessary information
         * for creating GraphParameters in the system.
         * Important: Only information, no actual function (like deduction).
         * By invoking {@link #end()}, the parameter will receive the
         * corresponding references.
         * Eventually, the wrapper can be used to create an
         * {@link GraphParameter}-object
         * in the system by invoking {@link #generate()}.
         *
         * @param id A unique ID-Integer.
         * @param name The parameter-name
         * @param dirtype The corresponding {@link Directed}-Enum-value
         * @param decomp A decomposition time.
         * @param compl Corresponding complementary
         *               {@link GraphParameter} (if available)
         */
        public GraphParameterWrapper(Integer id, String name, String dirtype,
                String decomp, String compl) {
            complexities = new ArrayList<ParamProblemWrapper>();
            this.id = id;
            this.name = name;
            this.dirtype = dirtype;
            this.decomp = decomp;
            this.compl = compl;
            prevrefs = ISGCIReader.this.refs;
            ISGCIReader.this.refs = refs = new ArrayList();
        }
        /**
         * Adds references.
         */
        public void end() {
            ISGCIReader.this.refs = prevrefs;
        }
        /**
         * Creates a parameter with information provided by the
         * wrapper and eventually adds it into the system.
         *
         * @return True if creation was successfully.
         * @throws SAXException Thrown if duplicate parameters exist.
         */
        public boolean generate() throws SAXException {
            GraphParameter par = GraphParameter.createParameter(id, name,
                    graph);

            // ---- Check if parameter already exists
            for (GraphParameter other : parameterIDs.values()) {
                if (par.equals(other))
                    throw new SAXException("Duplicate parameters " + id + " "
                            + other.getID());
            }

            // ---- Complete it and add it
            par.setDirected(Tags.problemString2directed(dirtype));
            par.setDecomposition(Complexity.getComplexity(this.decomp));
            if (!par.isLinDecomp()) {
                Problem decomposition = (Problem) problemNames.get(par
                        .getName() + " decomposition");
                if (decomposition == null)
                    throw new SAXException("No decomposition problem given "
                            + "non-linear decomposible parameter "
                            + par.getName());
                par.setDecompositionProblem(decomposition);
            }
            parameterNames.put(name, par);
            parameterIDs.put(id, par);
            if (compl != null) {
                GraphParameter c = parameterNames.get(compl);
                if (c == null)
                    throw new SAXException("Complement parameter " + compl
                            + " not found.");
                par.setComplement(c);
            }
            par.setRefs(refs);
            for (ParamProblemWrapper w : complexities) {
                w.problem.setComplexity(par.getPseudoClass(), w.complexity);
            }
            parameters.add(par);
            return true;
        }

        public String toString() {
            return "<GraphParameter: " + id + " " + name + ">";
        }
    }

    //-------------------------- AlgoWrapper -------------------------
    private class AlgoWrapper {
        Integer id;                      // graphclass id
        String bounds;
        Problem problem;
        Complexity complexity;
        List refs, prevrefs;

        public AlgoWrapper(Integer id, String name, String complexity,
                String bounds) throws SAXException {
            this.id = id;
            this.bounds = bounds;
            this.problem = (Problem) problemNames.get(name);
            if (this.problem == null)
                throw new SAXException("problem not found: "+name);
            this.complexity = Complexity.getComplexity(complexity);
            prevrefs = ISGCIReader.this.refs;
            ISGCIReader.this.refs = refs = new ArrayList();
        }

        public void end() {
            ISGCIReader.this.refs = prevrefs;
        }

        public boolean generate() {
            problem.createAlgo(classes.get(id), complexity, bounds, refs);
            return true;
        }
    }

    //-------------------------- ParamAlgoWrapper -------------------------
    /**
     * ParamAlgoWrapper serves as a help for handling algorithms (
     * {@link ParamAlgorithm}) defined on parameters ({@link GraphParameter}).
     * More information can be found in the constructor.
     * @author vector
     */
    private class ParamAlgoWrapper {
        /**
         * Corresponding Parameter to ParamAlgorithm.
         */
        private Integer id;
        /**
         * Boundedness of ParamAlgo.
         */
        private String bounds;
        /**
         * Correspnding problem.
         */
        private Problem problem;
        /**
         * Corresponding complexity. More info: {@link ParamComplexity}
         */
        private ParamComplexity complexity;
        /**
         * Corresponding references.
         */
        private List refs, prevrefs;

        /**
         * Constructs an all-in-one set containing the necessary information
         * for handling algorithms on parameters. Important: Only information,
         * no actual function.
         *
         * The wrapper can then be used to create an algorithm in the system by
         * invoking {@link #generate()}.
         *
         * @param id
         *            Parameter id
         * @param probname
         *            Problem name
         * @param complexity
         *            Parametrized complexity
         * @param bounds
         *            Specifies a boundedness.
         * @throws SAXException
         *             Thrown, if problem hasn't been found.
         */
        public ParamAlgoWrapper(Integer id, String probname,
                String complexity, String bounds) throws SAXException {
            this.id = id;
            this.bounds = bounds;
            this.problem = (Problem) problemNames.get(probname);
            if (this.problem == null)
                throw new SAXException("problem not found: " + probname);
            this.complexity = ParamComplexity.getComplexity(complexity);
            prevrefs = ISGCIReader.this.refs;
            ISGCIReader.this.refs = refs = new ArrayList();
        }

        /**
         * Finishes the wrapper by adding references.
         */
        public void end() {
            ISGCIReader.this.refs = prevrefs;
        }

        /**
         * Generates an algorithm on a parameter, with information provided by
         * the wrapper.
         * @return true if creation is finished.
         */
        public boolean generate() {
            problem.createAlgo(
                    parameterIDs.get(this.id).getPseudoClass(),
                    complexity, bounds, refs);
            return true;
        }
    }

    //---------------------- BoundednessProofWrapper --------------
    /**
     * Serves as all-in-one help for creating/setting boundedness proofs values
     * on graphclasses with associated parameters.
     *
     * Similar to {@link BoundednessWrapper}, but with references.
     *
     * More info can be found in the constructor.
     * @author vector
     */
    private class BoundednessProofWrapper {
        /**
         * Corresponding {@link GraphParameter}.
         */
        private GraphParameter parameter;
        /**
         * Corresponding {@link GraphClass}-ID.
         */
        private Integer id;
        /**
         * Corresponding {@link Boundedness}.
         */
        private Boundedness boundedness;
        /**
         * Corresponding local references (added to the object on generation
         * via {@link #generate()}).
         */
        private List refs, prevrefs;

        /**
         * Constructs an all-in-one set containing the necessary information
         * for setting boundedness proofs on parameters, similar to
         * {@link BoundednessWrapper}, but providing more information on the
         * actual proof (references, notes) Important: Only information, no
         * actual function. <br>
         * <br>
         * The wrapper can then be used to add the boundedness proof into the
         * system by invoking {@link #generate()}.<br>
         * <br>
         * References are never null (set to empty element on creation of
         * object).
         *
         * @param id2
         *            graphclass, the parameter is (un-)bounded on.
         * @param name
         *            graphparameter, whose boundedness is set.
         * @param boundedness
         *            the boundedness enumeration value (More info:
         *            {@link Boundedness})
         */
        public BoundednessProofWrapper(Integer id2, String name,
                String boundedness) throws SAXException {
            this.id = id2;
            this.parameter = parameterNames.get(name);
            if (this.parameter == null)
                throw new SAXException("parameter not found: " + name);
            this.boundedness = Boundedness.getBoundedness(boundedness);
            prevrefs = ISGCIReader.this.refs;
            ISGCIReader.this.refs = refs = new ArrayList();
        }

        /**
         * Adds references.
         */
        public void end() {
            ISGCIReader.this.refs = prevrefs;
        }

        /**
         * Generates/sets a boundedness proof on a graphclass, with information
         * provided by the wrapper.
         * @return true if creation is finished.
         */
        public boolean generate() {
            parameter.createProof(classes.get(id), boundedness, refs);
            return true;
        }
    }

    //---------------------- ProblemWrapper -----------------------
    private class ProblemWrapper {
        Problem problem;
        Complexity complexity;

        public ProblemWrapper(Problem p, Complexity c) {
            problem = p;
            complexity = c;
        }
    }

    // -------------------- ParamProblemWrapper (added by vector)-------------
    private class ParamProblemWrapper {
        Problem problem;
        ParamComplexity complexity;

        public ParamProblemWrapper(Problem p, ParamComplexity c) {
            problem = p;
            complexity = c;
        }
    }

    // ---------------------- BoundednessWrapper -------------------
    /**
     * Serves as all-in-one help for creating/setting boundedness values on
     * graphclasses with associated parameters.
     *
     * More info can be found in the constructor.
     * @author vector
     */
    private class BoundednessWrapper {
        /**
         * Corresponding {@link Boundedness}-Enum-Value.
         */
        private Boundedness boundedness;
        /**
         * Corresponding {@link GraphParameter}-Object.
         */
        private GraphParameter parameter;

        /**
         * Constructs an all-in-one set containing the necessary information
         * for setting parametrized boundednesses on graphclasses. Important:
         * Only information, no actual function.
         *
         *
         * @param parameter
         *            graphparameter, whose boundedness is set.
         * @param boundedness
         *            the boundedness enumeration value (More info:
         *            {@link Boundedness})
         */
        public BoundednessWrapper(final GraphParameter parameter,
                final Boundedness boundedness) {
            this.parameter = parameter;
            this.boundedness = boundedness;
        }
    }

    //---------------------- ReductionWrapper -------------------------
    private class ReductionWrapper {
        Problem child;
        String parent;
        Complexity complexity;

        public ReductionWrapper(Problem p, String from, Complexity c) {
            child = p;
            parent = from;
            complexity = c;
        }

        public boolean generate() {
            Problem f = (Problem) problemNames.get(parent);
            if (f == null)
                return false;
            child.addReduction(f, complexity);
            return true;
        }
    }
}

/* EOF */
