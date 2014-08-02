package teo.isgci.xml;

import teo.isgci.gc.*;
import teo.isgci.grapht.*;
import teo.isgci.problem.*;
import teo.isgci.ref.*;
import teo.isgci.relation.*;
import teo.isgci.util.LessLatex;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.*;
import java.sql.SQLException;

import org.jgrapht.DirectedGraph;

public class SQLExporter {
	/** What should be written? */
	private int mode;

	/** Write only online needed information */
	public static final int MODE_ONLINE = 0;
	/** Write information for sage. */
	public static final int MODE_SAGE = 1;
	/** Write information for the web pages */
	public static final int MODE_WEB = 2;

	/** Which database to write to */
	private SQLWriter m_sql;

	private Integer m_reductionID;
	private Integer m_problemNoteID;
	private Integer m_gcPropertyID;
	private Integer m_gcsgID;
	private Integer m_gcgcID;
	private Integer m_gcNoteID;
	private Integer m_gcProblemID;
	private Integer m_algoID;
	private Integer m_relationID;
	private Integer m_gcRefID;
	private Integer m_relationRefID;
	private Integer m_algoNoteID;
	private Integer m_algoRefID;
	private Integer m_problemID;
	// TODO initialize all as 0

	ArrayList<Integer> m_gcIDs;
	ArrayList<String> m_gcNames; 	

	/**
	 * Create a new ISGCIWriter
	 * 
	 * @param writer
	 *            where to write to
	 * @param mode
	 *            what should be written
	 * @throws SQLException
	 */
	public SQLExporter(SQLWriter sql, int mode) throws SQLException {
		this.mode = mode;
		m_sql = sql;

		// initialize the ID counters
		m_reductionID = 0;
		m_gcPropertyID = 0;
		m_problemNoteID = 0;
		m_gcProblemID = 0;
		m_gcsgID = 0;
		m_gcgcID = 0;
		m_gcNoteID = 0;
		m_algoRefID = 0;
		m_algoNoteID = 0;
		m_algoID = 0;
		m_relationRefID = 0;
		m_relationID = 0;
		m_gcRefID = 0;
		m_problemID = 0;
		m_gcIDs = new ArrayList<>(4000);
		m_gcNames = new ArrayList<>(4000); 	

		m_sql.executeSQLCommand("SET FOREIGN_KEY_CHECKS=0;");
		// Reset all tables that use our generated IDs
		m_sql.emptyTable("problem_reduction");
		m_sql.emptyTable("gc_property");
		m_sql.emptyTable("property");
		m_sql.emptyTable("problem_note");
		m_sql.emptyTable("gc_sg_derivation");
		m_sql.emptyTable("gc_gc_derivation");
		m_sql.emptyTable("graphclass");
		m_sql.emptyTable("gc_note");
		m_sql.emptyTable("algorithm_note");
		m_sql.emptyTable("algorithm_ref");
		m_sql.emptyTable("algorithm");
		m_sql.emptyTable("gc_problem");
		m_sql.emptyTable("relation_ref");
		m_sql.emptyTable("relation");
		m_sql.emptyTable("gc_ref");
		m_sql.emptyTable("complexity");
		m_sql.emptyTable("problem");
		m_sql.executeSQLCommand("SET FOREIGN_KEY_CHECKS=1;");

	}

	/**
	 * Write a full ISGCI dataset as an XML document.
	 * 
	 * @param g
	 *            the graph whose data to write
	 * @param problems
	 *            the problems to write
	 * @param complementAnn
	 *            a Set of complement nodes per node
	 * @param xmldecl
	 *            XML declaration (may be null)
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@SuppressWarnings("unchecked")
	public void writeISGCIDocument(DirectedGraph<GraphClass, Inclusion> g,
			Collection<Problem> problems,
			Collection<AbstractRelation> relations,
			Map<GraphClass, Set<GraphClass>> complementAnn)
			throws SQLException, IllegalArgumentException,
			IllegalAccessException {
		TreeMap<String, GraphClass> names = null;
		// true, if write for SAGE or WEB
		boolean sortbyname = mode == MODE_WEB || mode == MODE_SAGE;

		if (sortbyname) {
			names = new TreeMap<String, GraphClass>(new LessLatex());
			GraphClass w;
			for (GraphClass v : g.vertexSet()) {
				if ((w = names.put(v.toString(), v)) != null)
					System.err.println("Duplicate classname! " + v.getID()
							+ " " + w.getID() + " " + v + " " + w);
			}
		}

		Calendar cal = Calendar.getInstance();
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL,
				DateFormat.MEDIUM);

		System.out.println("Time when starting to filling the DB:"
				+ df.format(cal.getTime()));

		System.out.println("Fill complexities");
		fillComplexities();

		System.out.println("Fill properties");
		fillProperties();

		System.out.println("Fill problems related tables");
		writeProblemDefs(problems);

		System.out.println("Fill GC related tables");
		writeNodes(sortbyname ? names.values() : g.vertexSet(), problems,
				complementAnn, g);

		System.out.println("Fill relations related tables (abstract)");
		writeEdges(g);
		System.out.println("Fill relations related tables (incmp, disjoint)");
		writeRelations(relations);
		m_sql.executeSQLInsert();
		m_relationID = 0; // reset because we iterate twice in order to batch
							// insert edges and relations
		writeEdgesRef(g);
		writeRelationsRef(relations);

		Calendar cale = Calendar.getInstance();
		DateFormat dfe = DateFormat.getDateTimeInstance(DateFormat.FULL,
				DateFormat.MEDIUM);

		System.out.println("Time when filling DB ended: "
				+ dfe.format(cale.getTime()));
	}

	/**
	 * Write the GraphClasses.
	 * 
	 * @param nodes
	 *            the nodes to write
	 * @param problems
	 *            the problems that can occur for nodes
	 * @throws SQLException
	 */
	private void writeNodes(Iterable<GraphClass> nodes,
			Collection<Problem> problems,
			Map<GraphClass, Set<GraphClass>> complementAnn,
			DirectedGraph<GraphClass, Inclusion> g) throws SQLException {
		Map<GraphClass, Set<GraphClass>> scc = GAlg.calcSCCMap(g);

		// for every Graphclass in Iterable nodes
		for (GraphClass gc : nodes) {
			int gcIntID = gc.getID();
			String name;
			// Name
			// for web, sage and if name is explicitly written
			if (mode == MODE_WEB || mode == MODE_SAGE || gc.namedExplicitly()) {
				name = gc.toString();
			} else {
				name = "NULL";
			}
			m_sql.insertNewGraphclass(gcIntID, Tags.graphClassType(gc), name,
					false); // TODO computed set on true for now
			m_gcIDs.add(gcIntID);
			m_gcNames.add(name); 	
		}

		for (GraphClass gc : nodes) {
			int gcIntID = gc.getID();

			// if directed
			if (gc.isDirected()) {
				m_sql.insertNewGraphclassProperty(m_gcPropertyID, gcIntID,
						PropertyFields.getID(PropertyFields.DIRTRUE));
				m_gcPropertyID++;
			} else {
				m_sql.insertNewGraphclassProperty(m_gcPropertyID, gcIntID,
						PropertyFields.getID(PropertyFields.DIRFALSE));
				m_gcPropertyID++;
			}

			if (gc.getClass() != BaseClass.class) {
				if (gc instanceof ForbiddenClass) {
					writeForbiddenSet(((ForbiddenClass) gc).getSet(), gcIntID);
				} else if (gc instanceof IntersectClass) {
					writeClassesSet(((IntersectClass) gc).getSet(), gcIntID);
				} else if (gc instanceof UnionClass) {
					writeClassesSet(((UnionClass) gc).getSet(), gcIntID);
				} else if (gc instanceof ComplementClass) {
					writeClassesSet(((ComplementClass) gc).getBase(), gcIntID);
				} else if (gc instanceof HereditaryClass) {
					writeClassesSet(((HereditaryClass) gc).getBase(), gcIntID);
				} else if (gc instanceof DerivedClass) {
					writeClassesSet(((DerivedClass) gc).getBase(), gcIntID);
				} else {
					throw new RuntimeException("Unknown class for node "
							+ gc.getID().toString());
				}
			}

			if (mode == MODE_WEB) {
				// Hereditariness, Complements, references and notes
				writeHereditariness(gc, gcIntID);
				writeCliqueFixed(gc, gcIntID);
				writeEquivs(scc.get(gc), gcIntID);
				writeComplements(complementAnn.get(gc), gcIntID);
				writeGCRefs(gc.getRefs(), gcIntID);
			}
		}
		// We split writing here so we can batch write all gc_problem (30min ->
		// 2min)
		for (GraphClass gc : nodes) {
			// Problems
			writeComplexities(gc, problems, false);
		}
		m_sql.executeSQLInsert();
		m_gcProblemID = 0; // need to reset here since we start over but only
							// write the algos for the gc_problem
		for (GraphClass gc : nodes) {
			// Problems
			writeComplexities(gc, problems, true);
		}

	}

	/**
	 * Write the references in refs.
	 * 
	 * @throws SQLException
	 */
	@SuppressWarnings("rawtypes")
	private void writeGCRefs(Collection refs, int gcIntID) throws SQLException {

		// stop if there are no References
		if (refs == null)
			return;

		for (Object o : refs) {
			if (o instanceof Note) {
				Note n = (Note) o;
				String title;
				if (n.getName() == null) {
					title = "NULL";
				} else {
					title = n.getName();
				}
				String tempNote = n.toString(); 					
				String refactoredNote = refactorGCNote(tempNote); 	
				m_sql.insertNewGraphclassNote(m_gcNoteID, gcIntID, title,
						refactoredNote);
				m_gcNoteID++;

			} else if (o instanceof Ref) {
				Ref r = (Ref) o;
				String type;
				Integer refID;
				if (r.isTrivial()) {
					type = r.getLabel();
					refID = null;
				} else {
					type = "ref";
					refID = Integer.parseInt(r.getLabel().substring(4));
				}
				m_sql.insertNewGraphclassRef(m_gcRefID, gcIntID, type, refID);
				m_gcRefID++;

			} else
				throw new RuntimeException("Not a graphclass note/ref" + o);
		}
	}

	/**
	 * Write the edges.
	 * 
	 * @throws SQLException
	 */
	private void writeRelations(Collection<AbstractRelation> relations)
			throws SQLException {
		Integer confidence;
		Boolean confidenceBool;
		for (AbstractRelation r : relations) {
			String tag = r instanceof Disjointness ? Tags.DISJOINT
					: Tags.INCOMPARABLE;
			confidence = r.getConfidence();
			if (confidence < Inclusion.CONFIDENCE_HIGHEST) {
				confidenceBool = false;
			} else {
				confidenceBool = true;
			}
			m_sql.insertNewRelation(m_relationID, r.get1().getID(),
					tag, r.get2().getID(), confidenceBool, false, false); // TODO
																					// computed
																					// set
																					// on
																					// true
																					// for
																					// now

			m_relationID++;
		}
	}

	/**
	 * Write the ref edges.
	 * 
	 * @throws SQLException
	 */
	private void writeRelationsRef(Collection<AbstractRelation> relations)
			throws SQLException {
		for (AbstractRelation r : relations) {

			if (mode != MODE_SAGE) {
				// if not SAGE then write References
				writeRelationRefs(r.getRefs(), m_relationRefID);
			}
			m_relationID++;
		}
	}

	/**
	 * Write the edges.
	 * 
	 * @throws SQLException
	 */
	private void writeEdges(DirectedGraph<GraphClass, Inclusion> g)
			throws SQLException {
		int confidence;
		Boolean confidenceBool;
		for (Inclusion e : g.edgeSet()) {
			String type = "not" + Tags.PROPER;
			if (e.isProper()) {
				type = Tags.PROPER;
			}
			confidence = e.getConfidence();
			if (confidence < Inclusion.CONFIDENCE_HIGHEST) {
				confidenceBool = false;
			} else {
				confidenceBool = true;
			}
			m_sql.insertNewRelation(m_relationID,
					g.getEdgeSource(e).getID(), type,
					g.getEdgeTarget(e).getID(), confidenceBool, false,
					false); // TODO computed set on true for now

			m_relationID++;

		}
	}

	/**
	 * Write the edges.
	 * 
	 * @throws SQLException
	 */
	private void writeEdgesRef(DirectedGraph<GraphClass, Inclusion> g)
			throws SQLException {
		for (Inclusion e : g.edgeSet()) {

			if (mode != MODE_SAGE) {
				// write References
				writeRelationRefs(e.getRefs(), m_relationID);
			}
			m_relationID++;

		}
	}

	/**
	 * Write the references in refs.
	 * 
	 * @throws SQLException
	 */
	@SuppressWarnings("rawtypes")
	private void writeRelationRefs(Collection refs, Integer parentRelationID)
			throws SQLException {

		// stop if there are no References
		if (refs == null)
			return;

		for (Object o : refs) {
			if (o instanceof Ref) {
				Ref r = (Ref) o;
				String type;
				Integer refID;
				if (r.isTrivial()) {
					type = r.getLabel();
					refID = null;
				} else {
					type = "ref";
					refID = Integer.parseInt(r.getLabel().substring(4));
				}
				m_sql.insertNewRelationRef(m_relationRefID, parentRelationID,
						type, refID);
				m_relationRefID++;
			} else
				throw new RuntimeException("Not a relation ref" + o);
		}
	}

	/**
	 * Write the forbidden subgraphs in set.
	 * 
	 * @throws SQLException
	 */
	@SuppressWarnings("rawtypes")
	private void writeForbiddenSet(Iterable set, Integer gcIntID)
			throws SQLException {
		for (Object elem : set) {
			m_sql.insertNewGraphclassSmallgraphDerivation(m_gcsgID, gcIntID,
					elem.toString());
			m_gcsgID++;
		}
	}

	/**
	 * Write the graphclasses in set.
	 * 
	 * @param set
	 *            the graphclasses to write
	 * @throws SQLException
	 */
	private void writeClassesSet(Iterable<GraphClass> set, Integer gcIntID)
			throws SQLException {
		for (GraphClass gc : set) {
			m_sql.insertNewGraphclassGraphclassDerivation(m_gcgcID, gcIntID,
					Tags.GCREF, gc.getID());
			m_gcgcID++;

		}
	}

	/**
	 * Write the single graphclass gc as a set.
	 * 
	 * @param gc
	 *            the graphclass to write
	 * @throws SQLException
	 */
	private void writeClassesSet(GraphClass gc, Integer gcIntID)
			throws SQLException {
		m_sql.insertNewGraphclassGraphclassDerivation(m_gcgcID, gcIntID,
				Tags.GCREF, gc.getID());
		m_gcgcID++;
	}

	/**
	 * Write the hereditary element (if needed) for gc.
	 * 
	 * @throws SQLException
	 */
	private void writeHereditariness(GraphClass gc, Integer gcIntID)
			throws SQLException {
		// not explicitly written return
		if (!gc.hereditarinessExplicitly())
			return;

		Integer propID = PropertyFields.getID(PropertyFields.UNKNOWN); // Initialize
																		// with
																		// Hered
																		// unknown
		String hered = Tags.hereditariness2string(gc.getHereditariness());
		if (hered.equals(Tags.ISOHERED)) {
			propID = PropertyFields.getID(PropertyFields.ISO);
		} else if (hered.equals(Tags.CONHERED)) {
			propID = PropertyFields.getID(PropertyFields.CON);
		} else if (hered.equals(Tags.INDHERED)) {
			propID = PropertyFields.getID(PropertyFields.IND);
		}

		m_sql.insertNewGraphclassProperty(m_gcPropertyID, gcIntID, propID);
		m_gcPropertyID++;
	}

	/**
	 * Write the clique-fixed element (if needed) for gc.
	 * 
	 * @throws SQLException
	 */
	private void writeCliqueFixed(GraphClass gc, Integer gcIntID)
			throws SQLException {
		
		if (gc.isCliqueFixed()) {
			m_sql.insertNewGraphclassProperty(m_gcPropertyID, gcIntID,
					PropertyFields.getID(PropertyFields.CLIQTRUE));
			m_gcPropertyID++;
		} else {
			m_sql.insertNewGraphclassProperty(m_gcPropertyID, gcIntID,
					PropertyFields.getID(PropertyFields.CLIQFALSE));
			m_gcPropertyID++;
		}

	}

	/**
	 * Write a note containing the given equivalent classes.
	 * 
	 * @throws SQLException
	 */
	private void writeEquivs(Set<GraphClass> eqs, Integer gcIntID)
			throws SQLException {
		// return if eqs is empty
		if (eqs == null)
			return;
		for (GraphClass eq : eqs) {
			m_sql.insertNewGraphclassGraphclassDerivation(m_gcgcID, gcIntID,
					Tags.EQUIVALENTS, eq.getID());
			m_gcgcID++;
		}
	}

	/**
	 * Write a note containing the given complementclasses.
	 * 
	 * @throws SQLException
	 */
	private void writeComplements(Set<GraphClass> cos, Integer gcIntID)
			throws SQLException {
		// if cos is empty return
		if (cos == null)
			return;
		// write <note name="complements">
		for (GraphClass co : cos) {
			if (m_gcIDs.contains(co.getID())) // We don't write the
														// complement if it has
														// been deleted by
														// deducer !
			{
				m_sql.insertNewGraphclassGraphclassDerivation(m_gcgcID,
						gcIntID, Tags.COMPLEMENTS, co.getID());
				m_gcgcID++;
			}

		}
	}

	/**
	 * Write all Complexities for GraphClass n.
	 * 
	 * @throws SQLException
	 */
	private void writeComplexities(GraphClass n, Collection<Problem> problems,
			Boolean onlyWriteAlgos) throws SQLException {
		for (Problem p : problems) {
			if (mode != MODE_WEB || p.validFor(n))
				// call writeComplexity for every Problem p, that is
				// either valid for GraphClass n
				// or if mode is not WEB
				writeComplexity(p, p.getDerivedComplexity(n), p.getAlgos(n),
						n.getID(), onlyWriteAlgos);
		}
	}

	/**
	 * Write a Complexity for Problem problem.
	 * 
	 * @throws SQLException
	 */
	@SuppressWarnings("rawtypes")
	private void writeComplexity(Problem problem, Complexity c, Iterator algos,
			Integer gcIntID, Boolean onlyWriteAlgos) throws SQLException {
		// if complexity is not defined, return
		if (c == null)
			return;

		// if online or sage MODE
		if (mode == MODE_ONLINE || mode == MODE_SAGE) {
			if (!onlyWriteAlgos) {
				m_sql.insertNewGCProblemRelation(m_gcProblemID, gcIntID,
						problem.getName(), problem.getComplexityString(c));
				m_gcProblemID++;
			}
		} else {
			if (!onlyWriteAlgos) {
				m_sql.insertNewGCProblemRelation(m_gcProblemID, gcIntID,
						problem.getName(), problem.getComplexityString(c));
				m_gcProblemID++;
			} else {
				// writes algorithms
				writeAlgorithms(problem, algos, m_gcProblemID);
				m_gcProblemID++;
			}
		}
	}

	/**
	 * Write the algorithms for problem.
	 * 
	 * @throws SQLException
	 */
	@SuppressWarnings("rawtypes")
	private void writeAlgorithms(Problem problem, Iterator algos,
			Integer gcProbID) throws SQLException {
		// return for online, sage or algo == null
		if (mode == MODE_ONLINE || mode == MODE_SAGE || algos == null)
			return;

		while (algos.hasNext()) {
			Algorithm a = (Algorithm) algos.next();
			GraphClass gc = a.getGraphClass();
			Integer tempGCID = null;
			if (gc != null) {
				tempGCID = m_gcIDs.contains(gc.getID()) ? gc.getID()
						: null;
			}

			m_sql.insertNewAlgorithm(m_algoID, gcProbID,
					problem.getComplexityString(a.getComplexity()),
					a.getTimeBounds(), tempGCID, null, false, true);
			// TODO computed set on true for now

			// writes refs as seen before
			writeAlgoRefs(a.getRefs(), m_algoID);
			m_algoID++;
		}
	}

	/**
	 * Write the references in Algo.
	 * 
	 * @throws SQLException
	 */
	@SuppressWarnings("rawtypes")
	private void writeAlgoRefs(Collection refs, Integer algoID)
			throws SQLException {

		// stop if there are no References
		if (refs == null) {
			return;
		}

		for (Object o : refs) {
			if (o instanceof Note) {
				Note n = (Note) o;
				String title;
				if (n.getName() == null) {
					title = "NULL";
				} else {
					title = n.getName();
				}
				m_sql.insertNewAlgorithmNote(m_algoNoteID, algoID, title,
						n.toString());
				m_algoNoteID++;

			} else if (o instanceof Ref) {
				Ref r = (Ref) o;
				String type;
				Integer refID;
				if (r.isTrivial()) {
					type = r.getLabel();
					refID = null;
				} else {
					type = "ref";
					refID = Integer.parseInt(r.getLabel().substring(4));
				}
				m_sql.insertNewAlgorithmRef(m_algoRefID, algoID, type, refID);
				m_algoRefID++;

			} else
				throw new RuntimeException("Not an algo note/ref" + o);
		}
	}

	/**
	 * Write Problem definitions.
	 * 
	 * @throws SQLException
	 */
	@SuppressWarnings("rawtypes")
	private void writeProblemDefs(Collection<Problem> problems)
			throws SQLException {
		for (Problem p : problems) {
			String dir;
			if (p.forDirected() && !p.forUndirected()) {
				dir = "directed";
			} 
			else if (p.forUndirected()  &&  !p.forDirected())
		                dir = "undirected";
			else{
			        dir = null;
			}
			m_sql.insertNewProblem(m_problemID, p.getName(), dir, p.isSparse(),
					true); // TODO computed set on true for now
			m_problemID++;
		}
		// for webmode
		if (mode == MODE_WEB) {
			for (Problem p : problems) {
				writeReductions(p.getReductions(), p.getName());

				// We write the notes/refs for problems directly without another
				// function here

				Collection refs = p.getRefs();
				// write Problem Notes
				if (refs != null) {
					for (Object o : refs) {
						if (o instanceof Note) {
							Note n = (Note) o;
							String title;
							if (n.getName() != null) {
								title = n.getName();
							} else {
								title = "Null";
							}
							String text = n.toString();
							m_sql.insertNewProblemNote(m_problemNoteID,
									p.getName(), title, text);
							m_problemNoteID++;
						} else {
							throw new RuntimeException("Not a problem note" + o);
						}
					}
				}
			}
		}
	}

	private void writeReductions(Iterator<Reduction> reds, String problemLower)
			throws SQLException {
		Reduction red;
		while (reds.hasNext()) {
			red = reds.next();
			m_sql.insertNewProblemReduction(m_reductionID, problemLower, red
					.getParent().getName(), red.getComplexity()
					.getComplexityString(), null);
			m_reductionID++;
		}
	}

	/**
	 * Iterate all static String fields of PropertyFields and write them to the
	 * property table in the database.
	 * 
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private void fillProperties() throws SQLException,
			IllegalArgumentException, IllegalAccessException {
		Field[] fields = PropertyFields.class.getDeclaredFields();
		for (Field f : fields) {
			Object o = f.get(null); // get the object of this field
			if (o instanceof String) // we insert all objects of the field that
										// are Strings
			{
				String value = (String) o;
				m_sql.insertNewProperty(value);
			}
		}
	}

	/**
	 * Iterate all static String fields of ComplexityFields and write them to
	 * the complexity table in the database.
	 * 
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private void fillComplexities() throws SQLException,
			IllegalArgumentException, IllegalAccessException {
		Field[] fields = ComplexityFields.class.getDeclaredFields();
		for (Field f : fields) {
			Object o = f.get(null); // get the object of this field
			if (o instanceof String) // we insert all objects of the field that
										// are Strings
			{
				String value = (String) o;
				m_sql.insertNewComplexity(value);
			}
		}
	}

	
	/**
     * Removes all <graphclass>, </graphclass>, <smallgraph> and </smallgraph> from the String. 
     * Also replaces gcIDs with their names, if we have one for them.
     * @param input
     * @return
     */
    private String refactorGCNote(String input)
    {
    	
		String output = input;

    	if(input.contains("<graphclass>"))
    	{
        	ArrayList<Integer> begin = new ArrayList<Integer>();
        	ArrayList<Integer> end = new ArrayList<Integer>();
        	// find all occurrences forward
        	for (int i = -1; (i = input.indexOf("<graphclass>", i + 1)) != -1; ) 
        	{
        		begin.add(i);
        	} 
        	for (int i = -1; (i = input.indexOf("</graphclass>", i + 1)) != -1; ) 
        	{
        		end.add(i);
        	}
        	for (int i = begin.size() - 1; i >= 0 ; i--) //we iterate backwards because otherwise we mess up the values of begin and end arrayLists 
        	{	
        		String frontTemp = output.substring(0, begin.get(i));  //get the front part without <graphclass>
        		String backTemp = output.substring(end.get(i) + "</graphclass>".length(), output.length()  );  //get the back part without </graphclass>
    			String gcIDTemp =  output.substring(begin.get(i) + "<graphclass>".length() , end.get(i));  //get the gcID
    			String gcNameTemp = gcIDTemp;
    			
				if(m_gcIDs.indexOf(Integer.parseInt(gcNameTemp)) == -1)
				{
					gcNameTemp = gcIDTemp;  //We have no name for this ID, so lets just write the ID out again.
				}else
				{
					gcNameTemp = m_gcNames.get( m_gcIDs.indexOf(Integer.parseInt(gcNameTemp)) );
					gcNameTemp = "[[GraphClass:" + gcNameTemp + " |]]";
				}
				
				output = frontTemp + gcNameTemp + backTemp;
			
        	}
    	}
    	
    	//simply replace all smallgraph brackets since it references the smallgraphs name already
    	output = output.replace("<smallgraph>", "[[SmallGraph:");
    	output = output.replace("</smallgraph>", " |]]");
    	//remove unused <br> </br>
    	output = output.replace("<br>", "");
    	output = output.replace("</br>", "");
    	
    	return output;
    }
						
		
	}

