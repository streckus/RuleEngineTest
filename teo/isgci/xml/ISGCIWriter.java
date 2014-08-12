/*
 * Write an ISGCIGraph using SAX events.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.xml;

import java.io.Writer;
import java.util.*;
import java.text.SimpleDateFormat;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.Graphs;
import teo.isgci.grapht.*;
import teo.isgci.ref.*;
import teo.isgci.relation.*;
import teo.isgci.gc.*;
import teo.isgci.parameter.*;
import teo.isgci.problem.*;
import teo.isgci.util.LessLatex;
import java.sql.SQLException;


public class ISGCIWriter {
    /** Where to write to */
    private XMLWriter writer;
    /** What should be written? */
    private int mode;

    /** Write only online needed information */
    public static final int MODE_ONLINE = 0;
    /** Write information for sage. */
    public static final int MODE_SAGE = 1;
    /** Write information for the web pages */
    public static final int MODE_WEB = 2;


    /**
     * Create a new ISGCIWriter
     * @param writer where to write to
     * @param mode what should be written
     */
    public ISGCIWriter(Writer writer, int mode) {
        this.writer = new XMLWriter(writer);
        this.mode = mode;
    }


    /**
     * Write a full ISGCI dataset as an XML document.
     * @param g the graph whose data to write
     * @param problems the problems to write
     * @param parameters the parameters to write (added by vector)
     * @param complementAnn a Set of complement nodes per node
     * @param xmldecl XML declaration (may be null)
     */
    public void writeISGCIDocument(DirectedGraph<GraphClass,Inclusion> g,
            Collection<Problem> problems,
            Collection<GraphParameter> parameters,
            Collection<AbstractRelation> relations,
            Map<GraphClass,Set<GraphClass> > complementAnn,
            String xmldecl) throws SAXException {
			
		/*Write to the database only if its webmode and if DB credentials are set
		  For example for an XAMPP version with no password set and db name "Spectre":
		  String databaseAdress = "jdbc:mySQL://localhost/Spectre";
		  String databaseAccountName = "root";
		  String databaseAccountPassword = ""; 
		*/
		if(mode == MODE_WEB){
			String databaseAdress = null;
			String databaseAccountName = null;
			String databaseAccountPassword = null;
			try {
				if((databaseAdress != null) && (databaseAccountName != null) && (databaseAccountPassword != null))
				{
					SQLWriter s = new SQLWriter(databaseAdress, databaseAccountName, databaseAccountPassword, "", false);
					SQLExporter exp = new SQLExporter(s, SQLExporter.MODE_WEB);
					exp.writeISGCIDocument(g, problems, relations, complementAnn);				
				}
			} catch (SQLException e) {
				System.out.println("Problems with sql");
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				System.out.println("Problems with property/complexity tables");
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				System.out.println("Problems with property/complexity tables");
				e.printStackTrace();
			}
    	}
			
			
        TreeMap<String,GraphClass> names = null;
        boolean sortbyname =  mode == MODE_WEB || mode == MODE_SAGE; 

         if (sortbyname) {
            names = new TreeMap<String,GraphClass>(new LessLatex());
            GraphClass w;
            for (GraphClass v : g.vertexSet()) {
                if ((w = names.put(v.toString(), v)) != null)
                    System.err.println("Duplicate classname! "+
                        v.getID() +" "+ w.getID() +" "+ v +" "+w);
            }
        }

        if (xmldecl == null)
            writer.startDocument();
        else
            writer.startDocument(xmldecl);
        writer.startElement(Tags.ROOT_ISGCI);
        writer.characters("\n");
            writeStatistics(g);
            writeProblemDefs(problems);
            // Parameters added by vector
            writeParameters(parameters, problems, g);
            writer.startElement(Tags.PARAM_RELATIONS);
            writer.characters("\n");
            writeParamEdges(g);
            writeParamRelations(relations);
            writer.endElement(Tags.PARAM_RELATIONS);
            writer.characters("\n");
            writeNodes(sortbyname ?  names.values() : g.vertexSet(),
                    problems, parameters, complementAnn, g);

            writer.startElement(Tags.INCLUSIONS);
            writer.characters("\n");
                writeEdges(g);
                writeRelations(relations);
            writer.endElement(Tags.INCLUSIONS);
            writer.characters("\n");
        writer.endElement(Tags.ROOT_ISGCI);
        writer.endDocument();
    }


    private void writeStatistics(DirectedGraph<GraphClass,Inclusion> g)
            throws SAXException {
        SimpleAttributes atts = new SimpleAttributes();

        DirectedGraph<GraphClass,Inclusion> closedGraph =
                new SimpleDirectedGraph<GraphClass,Inclusion>(Inclusion.class);
        Graphs.addGraph(closedGraph, g);
        GAlg.transitiveClosure(closedGraph);

        atts.addAttribute(Tags.DATE,
                new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        atts.addAttribute(Tags.NODECOUNT,
                Integer.toString(closedGraph.vertexSet().size()));
        atts.addAttribute(Tags.EDGECOUNT,
                Integer.toString(closedGraph.edgeSet().size()));
        writer.emptyElement("", Tags.STATS, "", atts);
        writer.characters("\n");
    }


    /**
     * Write the GraphParameters.
     * @param parameters the parameters to write
     * @param problems the problems that can occur for parameters
     * @throws SAXException
     * @author vector
     */
    private void writeParameters(Collection<GraphParameter> parameters,
            Collection<Problem> problems,
            DirectedGraph<GraphClass, Inclusion> g) throws SAXException {
        SimpleAttributes atts = new SimpleAttributes();
        Map<GraphClass,Set<GraphClass> > scc = GAlg.calcSCCMap(g);

        writer.startElement(Tags.PARAMETERS);
        writer.characters("\n");

        for (GraphParameter par : parameters) {
            // Header
            atts.addAttribute(Tags.ID, par.getID().toString());
            atts.addAttribute(Tags.NAME, par.getName());
            if (par.forDirected() && !par.forUndirected())
                atts.addAttribute(Tags.DIRTYPE, Tags.DIRECTED);
            else if (par.forUndirected() && !par.forDirected())
                atts.addAttribute(Tags.DIRTYPE, Tags.UNDIRECTED);
            if (!par.isLinDecomp())
                atts.addAttribute(Tags.PARAMETER_DECOMP, par
                        .getDecomposition().getComplexityString());
            writer.startElement("", Tags.PARAMETER_DEF, "", atts);
            writer.characters("\n");
            atts.clear();
            // References and notes
            if (mode == MODE_WEB) {
                writeEquivParams( scc.get(par.getPseudoClass()) );
                writer.characters("\n");
                writeRefs(par.getRefs());
                writer.characters("\n");
            }
            // Problems
            writeComplexities(par.getPseudoClass(), problems);
            writer.endElement(Tags.PARAMETER_DEF);
            writer.characters("\n\n");
            atts.clear();
        }
        writer.endElement(Tags.PARAMETERS);
        writer.characters("\n");
    }

    /**
     * Write the GraphClasses.
     * @param nodes the nodes to write
     * @param problems the problems that can occur for nodes
     */
    private void writeNodes(Iterable<GraphClass> nodes,
            Collection<Problem> problems,
            Collection<GraphParameter> parameters,
            Map<GraphClass,Set<GraphClass> > complementAnn,
            DirectedGraph<GraphClass,Inclusion> g) throws SAXException {
        SimpleAttributes atts = new SimpleAttributes();
        Map<GraphClass,Set<GraphClass> > scc = GAlg.calcSCCMap(g);

        writer.startElement(Tags.GRAPHCLASSES);
        writer.characters("\n");

        for (GraphClass gc : nodes) {
            if (!gc.isPseudoClass()) { // no Parameter-PseudoClasses
                // Header
                atts.addAttribute(Tags.ID, gc.getID().toString()); //intID -> toString
                atts.addAttribute(Tags.TYPE, Tags.graphClassType(gc));
                if (gc.isDirected())
                    atts.addAttribute(Tags.DIRTYPE, Tags.DIRECTED);
                writer.startElement("", Tags.GRAPHCLASS, "", atts);
                writer.characters("\n");
                    // Name
                    if (mode == MODE_WEB  ||  mode == MODE_SAGE  ||
                                gc.namedExplicitly()) {
                        writer.dataElement(Tags.NAME, gc.toString());
                        writer.characters("\n");
                    }
                    // Set
                    if (gc.getClass() != BaseClass.class) {
                        if (gc instanceof ForbiddenClass)
                            writeForbiddenSet(((ForbiddenClass) gc).getSet());
                        else if (gc instanceof IntersectClass)
                            writeClassesSet(((IntersectClass) gc).getSet());
                        else if (gc instanceof UnionClass)
                            writeClassesSet(((UnionClass) gc).getSet());
                        else if (gc instanceof ComplementClass)
                            writeClassesSet( ((ComplementClass) gc).getBase() );
                        else if (gc instanceof HereditaryClass)
                            writeClassesSet( ((HereditaryClass) gc).getBase() );
                        else if (gc instanceof DerivedClass)
                            writeClassesSet( ((DerivedClass) gc).getBase() );
                        else
                            throw new RuntimeException(
                                    "Unknown class for node "+gc.getID().toString());//intID
                        writer.characters("\n");
                    }
                    // Hereditariness, Complements, references and notes
                    if (mode == MODE_WEB) {
                        writeHereditariness(gc);
                        writeCliqueFixed(gc);
                        writer.characters("\n");
                        writeEquivs( scc.get(gc) );
                        writer.characters("\n");
                        writeComplements(complementAnn.get(gc));
                        writer.characters("\n");
                        writeRefs(gc.getRefs());
                        writer.characters("\n");
                    }
                    // Problems
                    writeComplexities(gc, problems);
                    // Parameters
                    writeBoundednesses(gc, parameters);
                writer.endElement(Tags.GRAPHCLASS);
                writer.characters("\n\n");
                atts.clear();
            }
        }
        writer.endElement(Tags.GRAPHCLASSES);
        writer.characters("\n");
    }


    /**
     * Write the edges.
     */
    private void writeRelations(Collection<AbstractRelation> relations)
            throws SAXException {
        int confidence;
        SimpleAttributes atts = new SimpleAttributes();

        for (AbstractRelation r : relations) {
            if (!(r instanceof NotBounds) // no PseudoClass-Relations
                    && !(r instanceof Open && r.get1().isPseudoClass())) {
                String tag = r instanceof Disjointness ? Tags.DISJOINT :
                        Tags.INCOMPARABLE;
                if (r instanceof Open)
                    tag = Tags.OPEN;

                atts.addAttribute(Tags.GC1, r.get1().getID().toString()); //intID -> toString();
                atts.addAttribute(Tags.GC2, r.get2().getID().toString());
                confidence = r.getConfidence();
                if (confidence < Inclusion.CONFIDENCE_HIGHEST) {
                    atts.addAttribute(Tags.CONFIDENCE,
                            Tags.confidence2string(confidence));
                }
                writer.startElement("", tag, "", atts);
                if (mode != MODE_SAGE)
                    writeRefs(r.getRefs());
                writer.endElement(tag);
                writer.characters("\n");
                atts.clear();
            }
        }
    }

    /**
     * Write the not bounds relation between parameters.
     * @author vector
     */
    private void writeParamRelations(Collection<AbstractRelation> relations)
            throws SAXException {
        int confidence;
        SimpleAttributes atts = new SimpleAttributes();

        for (AbstractRelation r : relations) {
            if (r instanceof NotBounds
                    || (r instanceof Open && r.get1().isPseudoClass())) {
                atts.addAttribute(Tags.PARAM1, r.get1().getID().toString());
                atts.addAttribute(Tags.PARAM2, r.get2().getID().toString());

                String rel = r instanceof NotBounds ? Tags.PAR_NOT_BOUNDS
                        : Tags.OPEN;
                atts.addAttribute(Tags.PAR_ATT_REL, rel);
                confidence = r.getConfidence();
                if (confidence < Inclusion.CONFIDENCE_HIGHEST) {
                    atts.addAttribute(Tags.CONFIDENCE,
                            Tags.confidence2string(confidence));
                }
                writer.startElement("", Tags.PARAM_RELATION, "", atts);
                if (mode != MODE_SAGE)
                    writeRefs(r.getRefs());
                writer.endElement(Tags.PARAM_RELATION);
                writer.characters("\n");
                atts.clear();
            }
        }
    }

    /**
     * Write the edges.
     */
    private void writeEdges(DirectedGraph<GraphClass,Inclusion> g)
            throws SAXException {
        int confidence;
        SimpleAttributes atts = new SimpleAttributes();

        for (Inclusion e : g.edgeSet()) {
            if (!g.getEdgeSource(e).isPseudoClass()) { // no PseudoClass-Edges
                atts.addAttribute(Tags.SUPER, g.getEdgeSource(e).getID().toString());//intID
                atts.addAttribute(Tags.SUB, g.getEdgeTarget(e).getID().toString());
                if (e.isProper()) {
                    atts.addAttribute(Tags.PROPER, "y");
                }
                confidence = e.getConfidence();
                if (confidence < Inclusion.CONFIDENCE_HIGHEST) {
                    atts.addAttribute(Tags.CONFIDENCE,
                            Tags.confidence2string(confidence));
                }
                writer.startElement("", Tags.INCLUSION, "", atts);
                if (mode != MODE_SAGE)
                    writeRefs(e.getRefs());
                writer.endElement(Tags.INCLUSION);
                writer.characters("\n");
                atts.clear();
            }
        }
    }

    /**
     * Write the parameter relation edges.
     * @author vector
     */
    private void writeParamEdges(DirectedGraph<GraphClass, Inclusion> g)
            throws SAXException {
        int confidence;
        SimpleAttributes atts = new SimpleAttributes();

        for (Inclusion e : g.edgeSet()) {
            if (g.getEdgeSource(e).isPseudoClass()) {
                atts.addAttribute(Tags.PARAM1, g.getEdgeSource(e).getID()
                        .toString());
                atts.addAttribute(Tags.PARAM2, g.getEdgeTarget(e).getID()
                        .toString());
                atts.addAttribute(Tags.PAR_ATT_REL, Tags.PAR_BOUNDS);
                confidence = e.getConfidence();
                if (confidence < Inclusion.CONFIDENCE_HIGHEST) {
                    atts.addAttribute(Tags.CONFIDENCE,
                            Tags.confidence2string(confidence));
                }
                atts.addAttribute(Tags.FUNCTIONTYPE, e.getFunctiontype()
                        .toString());
                writer.startElement("", Tags.PARAM_RELATION, "", atts);
                if (mode != MODE_SAGE)
                    writeRefs(e.getRefs());
                writer.endElement(Tags.PARAM_RELATION);
                writer.characters("\n");
                atts.clear();
            }
        }
    }

    /**
     * Write the forbidden subgraphs in set.
     */
    private void writeForbiddenSet(Iterable set) throws SAXException{
        for (Object elem : set)
            writer.dataElement(Tags.SMALLGRAPH, elem.toString());
    }


    /**
     * Write the graphclasses in set.
     * @param set the graphclasses to write
     */
    private void writeClassesSet(Iterable<GraphClass> set) throws SAXException{
        for (GraphClass gc : set){
        	if(gc.getID()==null){
        		System.out.println("missing ID at: " +gc);//intID
        		gc.setID(Integer.MAX_VALUE);//intID
        		//IDGenerator temp = new IDGenerator(1, cachefile) //is AUTO !
        		//wie soll ich fehlende ID fixen? ... IDGenerator übergeben?
        	}
            writer.dataElement(Tags.GCREF, gc.getID().toString());//intID
        }
    }

    /**
     * Write the single graphclass gc as a set.
     * @param gc the graphclass to write
     */
    private void writeClassesSet(GraphClass gc) throws SAXException {
        writer.dataElement(Tags.GCREF, gc.getID().toString());//intID
    }

    /**
     * Write the single graphparameter for gc as a set.
     * @param gc the pseudoclass of the parameter to write
     * @author vector
     */
    private void writeParameterSet(PseudoClass gc) throws SAXException {
        writer.dataElement(Tags.PARREF, gc.getID().toString());
    }


    /**
     * Write the hereditary element (if needed) for gc.
     */
    private void writeHereditariness(GraphClass gc) throws SAXException {
        if (!gc.hereditarinessExplicitly())
            return;

        SimpleAttributes atts = new SimpleAttributes();
        atts.addAttribute(Tags.TYPE,
            Tags.hereditariness2string(gc.getHereditariness()));
        writer.emptyElement("", Tags.HERED, "", atts);
    }


    /**
     * Write the clique-fixed element (if needed) for gc.
     */
    private void writeCliqueFixed(GraphClass gc) throws SAXException {
        if (!gc.isCliqueFixed())
            return;

        SimpleAttributes atts = new SimpleAttributes();
        writer.emptyElement("", Tags.CLIQUEFIXED, "", atts);
    }



    /**
     * Write a note containing the given equivalent classes.
     */
    private void writeEquivs(Set<GraphClass> eqs) throws SAXException {
        if (eqs == null)
            return;
        SimpleAttributes atts = new SimpleAttributes();
        atts.addAttribute(Tags.NAME, Tags.EQUIVALENTS);
        writer.startElement("", Tags.NOTE, "", atts);
            for (GraphClass eq : eqs) {
                writeClassesSet(eq);
            }
        writer.endElement(Tags.NOTE);
    }

    /**
     * Write a note containing the given equivalent parameters.
     * @author vector
     */
    private void writeEquivParams(Set<GraphClass> eqs) throws SAXException {
        if (eqs == null)
            return;
        SimpleAttributes atts = new SimpleAttributes();
        atts.addAttribute(Tags.NAME, Tags.EQUIVALENTS);
        writer.startElement("", Tags.NOTE, "", atts);
            for (GraphClass eq : eqs) {
                writeParameterSet((PseudoClass) eq);
            }
        writer.endElement(Tags.NOTE);
    }

    /**
     * Write a note containing the given complementclasses.
     */
    private void writeComplements(Set<GraphClass> cos)
            throws SAXException {
        if (cos == null)
            return;
        SimpleAttributes atts = new SimpleAttributes();
        atts.addAttribute(Tags.NAME, Tags.COMPLEMENTS);
        writer.startElement("", Tags.NOTE, "", atts);
            for (GraphClass co : cos) {
                writeClassesSet(co);
            }
        writer.endElement(Tags.NOTE);
    }

    /**
     * Write all boundedness values for GraphClass n.
     * @author vector
     */
    private void writeBoundednesses(GraphClass n,
            Collection<GraphParameter> parameters) throws SAXException {
        for (GraphParameter par : parameters) {
            if (mode != MODE_WEB || par.validFor(n))
                writeBoundedness(par, par.getDerivedBoundedness(n),
                        par.getProofs(n));
        }
    }

    /**
     * Write a boundednes value for GraphParameter parameter.
     * @author vector
     */
    private void writeBoundedness(GraphParameter parameter, Boundedness b,
            Iterator<BoundednessProof> proofs) throws SAXException {
        if (b == null)
            return;
        SimpleAttributes atts = new SimpleAttributes();
        atts.addAttribute(Tags.NAME, parameter.getName());
        atts.addAttribute(Tags.BOUNDEDNESS, parameter.getComplexityString(b));
        if (mode == MODE_ONLINE || mode == MODE_SAGE) {
            writer.emptyElement("", Tags.PARAMETER, "", atts);
        } else {
            writer.startElement("", Tags.PARAMETER, "", atts);
            writer.characters("\n");
            writeProofs(parameter, proofs);
            writer.endElement(Tags.PARAMETER);
        }
        writer.characters("\n");
    }

    /**
     * Write the boundedness proofs for parameter.
     * @author vector
     */
    private void writeProofs(GraphParameter parameter,
            Iterator<BoundednessProof> proofs) throws SAXException {
        if (mode == MODE_ONLINE || mode == MODE_SAGE || proofs == null)
            return;
        SimpleAttributes atts = new SimpleAttributes();
        while (proofs.hasNext()) {
            BoundednessProof b = proofs.next();
            atts.addAttribute(Tags.NAME, parameter.getName());
            atts.addAttribute(Tags.BOUNDEDNESS,
                    parameter.getComplexityString(b.getBoundedness()));
            writer.startElement("", Tags.BOUNDED, "", atts);
            if (b.getGraphClass() != null)
                writer.dataElement(Tags.GCREF, b.getGraphClass().getID()
                        .toString());
            writeRefs(b.getRefs());
            writer.endElement(Tags.BOUNDED);
            writer.characters("\n");
            atts.clear();
        }
    }


    /**
     * Write all Complexities for GraphClass n.
     */
    private void writeComplexities(GraphClass n, Collection<Problem> problems)
            throws SAXException {
        for (Problem p : problems) {
            if (mode != MODE_WEB  ||  p.validFor(n))
                writeComplexity(p, p.getDerivedComplexity(n),
                        p.getAlgos(n));
        }
    }

    /**
     * Write all Complexities for PseudoClass n.
     * @author vector
     */
    private void writeComplexities(PseudoClass n, Collection<Problem> problems)
            throws SAXException {
        for (Problem p : problems) {
            if (mode != MODE_WEB || p.validFor(n))
                writeComplexity(p, p.getDerivedComplexity(n), p.getAlgos(n));
        }
    }

    /**
     * Write a Complexity for Problem problem.
     */
    private void writeComplexity(Problem problem, Complexity c,
            Iterator algos) throws SAXException {
        if (c == null)
            return;
        SimpleAttributes atts = new SimpleAttributes();
        atts.addAttribute(Tags.NAME, problem.getName());
        atts.addAttribute(Tags.COMPLEXITY, problem.getComplexityString(c));
        if (mode == MODE_ONLINE  ||  mode == MODE_SAGE) {
            writer.emptyElement("", Tags.PROBLEM, "", atts);
        } else {
            writer.startElement("", Tags.PROBLEM, "", atts);
            writer.characters("\n");
                writeAlgorithms(problem, algos);
            writer.endElement(Tags.PROBLEM);
        }
        writer.characters("\n");
    }

    /**
     * Write a parameterized Complexity for Problem problem.
     * @author vector
     */
    private void writeComplexity(Problem problem, ParamComplexity c,
            Iterator<ParamAlgorithm> algos) throws SAXException {
        if (c == null)
            return;
        SimpleAttributes atts = new SimpleAttributes();
        atts.addAttribute(Tags.NAME, problem.getName());
        atts.addAttribute(Tags.COMPLEXITY, problem.getComplexityString(c));
        if (mode == MODE_ONLINE || mode == MODE_SAGE) {
            writer.emptyElement("", Tags.PAR_PROBLEM, "", atts);
        } else {
            writer.startElement("", Tags.PAR_PROBLEM, "", atts);
            writer.characters("\n");
            writeParamAlgorithms(problem, algos);
            writer.endElement(Tags.PAR_PROBLEM);
        }
        writer.characters("\n");
    }

    /**
     * Write the algorithms for problem.
     */
    private void writeAlgorithms(Problem problem, Iterator algos)
            throws SAXException {
        if (mode == MODE_ONLINE  ||  mode == MODE_SAGE  ||  algos == null)
            return;
        SimpleAttributes atts = new SimpleAttributes();
        while (algos.hasNext()) {
            Algorithm a = (Algorithm) algos.next();
            atts.addAttribute(Tags.NAME, problem.getName());
            atts.addAttribute(Tags.COMPLEXITY,
                    problem.getComplexityString(a.getComplexity()));
            if (a.getTimeBounds() != null)
                atts.addAttribute(Tags.BOUNDS, a.getTimeBounds());
            writer.startElement("", Tags.ALGO, "", atts);
                if (a.getGraphClass() != null)
                    writer.dataElement(Tags.GCREF, a.getGraphClass().getID().toString());//intID
                writeRefs(a.getRefs());
            writer.endElement(Tags.ALGO);
            writer.characters("\n");
            atts.clear();
        }
    }

    /**
     * Write the parameterized algorithms for problem.
     * @author vector
     */
    private void writeParamAlgorithms(Problem problem,
            Iterator<ParamAlgorithm> algos) throws SAXException {
        if (mode == MODE_ONLINE || mode == MODE_SAGE || algos == null)
            return;
        SimpleAttributes atts = new SimpleAttributes();
        while (algos.hasNext()) {
            ParamAlgorithm a = algos.next();
            atts.addAttribute(Tags.NAME, problem.getName());
            atts.addAttribute(Tags.COMPLEXITY,
                    problem.getComplexityString(a.getComplexity()));
            if (a.getTimeBounds() != null)
                atts.addAttribute(Tags.BOUNDS, a.getTimeBounds());
            writer.startElement("", Tags.PAR_ALGO, "", atts);
            if (a.getGraphClass() != null)
                writer.dataElement(Tags.PARREF, a.getGraphClass().getID()
                        .toString());
            writeRefs(a.getRefs());
            writer.endElement(Tags.PAR_ALGO);
            writer.characters("\n");
            atts.clear();
        }
    }

    /**
     * Write the references in refs.
     */
    private void writeRefs(Collection refs)
            throws SAXException {
        if (refs == null)
            return;

        SimpleAttributes atts = new SimpleAttributes();
        for (Object o : refs) {
            if (o instanceof Note) {
                Note n = (Note) o;
                if (n.getName() != null)
                    atts.addAttribute(Tags.NAME, n.getName());
                writer.startElement("", Tags.NOTE, "", atts);
                    writer.charactersRaw(n.toString());
                writer.endElement(Tags.NOTE);
                atts.clear();
            } else if (o instanceof Ref) {
                writer.dataElement(Tags.REF, ((Ref) o).getLabel());
            } else
                throw new RuntimeException("Not a note/ref"+ o);
        }
    }


    /**
     * Write Problem definitions.
     */
    private void writeProblemDefs(Collection<Problem> problems)
            throws SAXException {
        SimpleAttributes atts = new SimpleAttributes();
        for (Problem p : problems) {
            atts.addAttribute(Tags.NAME, p.getName());
            if (p.forDirected()  &&  !p.forUndirected())
                atts.addAttribute(Tags.DIRTYPE, Tags.DIRECTED);
            else if (p.forUndirected()  &&  !p.forDirected())
                atts.addAttribute(Tags.DIRTYPE, Tags.UNDIRECTED);
            writer.startElement("", Tags.PROBLEM_DEF, "", atts);
            atts.clear();
            if (p.isSparse())
                writer.emptyElement(Tags.PROBLEM_SPARSE);
            if (p.forParameters())
                writer.emptyElement(Tags.PROBLEM_FORPARAMS);
            writer.characters("\n");
            if (mode == MODE_WEB) {
                writeReductions(p.getReductions());
                writeRefs(p.getRefs());
            }
            writer.endElement(Tags.PROBLEM_DEF);
            writer.characters("\n");
        }
    }

    private void writeReductions(Iterator<Reduction> reds) throws SAXException{
        Reduction red;
        SimpleAttributes atts = new SimpleAttributes();

        while (reds.hasNext()) {
            red = reds.next();
            atts.addAttribute(Tags.NAME, red.getParent().getName());
            atts.addAttribute(Tags.COMPLEXITY,
                    red.getComplexity().getComplexityString());
            writer.emptyElement("", Tags.PROBLEM_FROM, "", atts);
            writer.characters("\n");
            atts.clear();
        }
    }
}

/* EOF */
