package teo.isgci.xml;

import teo.isgci.gc.BaseClass;
import teo.isgci.gc.CliqueClass;
import teo.isgci.gc.ComplementClass;
import teo.isgci.gc.ConnectedHereditaryClass;
import teo.isgci.gc.ForbiddenClass;
import teo.isgci.gc.GraphClass;
import teo.isgci.gc.InducedHereditaryClass;
import teo.isgci.gc.IntersectClass;
import teo.isgci.gc.IsometricHereditaryClass;
import teo.isgci.gc.ProbeClass;
import teo.isgci.gc.UnionClass;
import teo.isgci.problem.Complexity;
import teo.isgci.problem.Problem;
import teo.isgci.ref.Note;
import teo.isgci.ref.Ref;
import teo.isgci.relation.AbstractRelation;
import teo.isgci.relation.Disjointness;
import teo.isgci.relation.Inclusion;
import teo.isgci.relation.Incomparability;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.xml.sax.SAXException;

//rz225.wwwdns.rz.uni-konstanz.de

public class SQLReader {
    /* Database Connection */
    private Connection m_connection;
    Statement mainconn;
    Statement tempconn;
    ResultSet mainresult;
    ResultSet tempresult;

    /* ISGCI */
    DirectedGraph<GraphClass, Inclusion> graph;

    // --------------------------------------------------------------------------------------

    /* Graphclasses */
    private HashMap<Integer, GraphClass> classes; // key = id, obj = gc
    private List<GraphClassWrapper> todo;// Save Wrappers that are not yet done
    private GraphClassWrapper curClass;

    // --------------------------------------------------------------------------------------

    /* Inclusions */
    // set new inclusion with refs
    private Inclusion curIncl;

    // get relation between graphclass
    private AbstractRelation curRel;
    private List<AbstractRelation> relations;

    // --------------------------------------------------------------------------------------

    /* Problems */
    private Hashtable problemNames;
    private List<Problem> problems;
    private List<AlgoWrapper> algos; // All read algo elements
    private AlgoWrapper curAlgo;
    private Problem curProblem;
    private List<ReductionWrapper> reductionsTodo;

    // --------------------------------------------------------------------------------------

    /* References */
    private List refs; // Refs for the current element

    // --------------------------------------------------------------------------------------

    private boolean parsingDone;

    /**
     * Create a new SQLReader with a connection to the database. The Reader
     * closely imitates the ISGCIReader and reuses most of its code.
     * 
     * @param databaseAddress
     *            address to database for example: jdbc:myDriver:myDatabase
     * @param username
     * @param password
     * @param dbName
     *            the name of the database we are using, for example the ones we
     *            used early on: SpectreSoft or newspec. If none used/required,
     *            just pass an empty String.
     * @param g
     *            transfered graph
     * @param problems
     *            transefered problems
     * @throws SQLException
     */
    public SQLReader(String databaseAddress, String username, String password,
            String dbName, DirectedGraph<GraphClass, Inclusion> g,
            List<Problem> problems) throws SQLException {

        // get transfered graph g
        this.graph = g;

        // get transfered problems
        this.problems = problems;

        problemNames = new Hashtable();
        classes = new HashMap<Integer, GraphClass>();
        todo = new ArrayList<GraphClassWrapper>();
        algos = new ArrayList<AlgoWrapper>();
        reductionsTodo = new ArrayList<ReductionWrapper>();
        relations = new ArrayList<AbstractRelation>();
        parsingDone = false;

        /*
         * =======================Database Connection===================
         */
        String driver = "org.gjt.mm.mysql.Driver";

        try {
            Class.forName(driver);

            m_connection = DriverManager.getConnection(databaseAddress,
                    username, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**********************************************
     * 
     * Gettermethods
     * 
     **********************************************/

    /**
     * Method for returning Graphs
     * 
     * @return DirectedGraph<GraphClass,Inclusion> graph
     */

    public DirectedGraph<GraphClass, Inclusion> getGraph() {
        return graph;
    }

    /**
     * Method for returning Graphs
     * 
     * @return List<AbstractRelation> getRelations();
     */

    public List<AbstractRelation> getRelations() {
        return relations;
    }

    /**
     * Method for returning Problems
     * 
     * @return List<Problem> problems
     */
    public List<Problem> getProblems() {
        return problems;
    }

    /**********************************************
     * 
     * Mainmethod
     * 
     **********************************************/

    /**
     * This is the main method, which reads all necessary information from the
     * database in a specific order (like they would appear in the xml).
     * 
     * @return boolean parsingDone, if finished successfully
     */

    public boolean readDatabase() {

        try {
            mainconn = m_connection.createStatement();

            tempconn = m_connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("=======start reading Database=======");
        startProblems();
        startGraphclass();
        startgraphclasses();
        endGraphClasses();
        startInclEqu();
        startDisjointIncmp();
        printOutStuff();

        parsingDone = true;

        System.out.println("=======finished reading Database=======");
        return parsingDone;
    }

    /**********************************************
     * 
     * reading the database startmethods for big elements, like GraphClasses,
     * handlemethods for attributes of them
     * 
     **********************************************/

    /**
     * Method that returns a String holding the Name of the Complement of a
     * Problem that is selected from the Database
     * 
     * @return Complement of Problem
     * @throws Exception
     * @throws SQLException
     */
    private String handleProblemComplement() throws Exception, SQLException {

        tempresult = tempconn
                .executeQuery("SELECT upper_name from problem_reduction WHERE "
                        + "lower_name = '"
                        + mainresult.getString("problem_name")
                        + "' AND complement=1");

        String compl = null;

        // Check whether there is a complement. Error if more than one

        if (tempresult.isBeforeFirst()) {
            compl = tempresult.getString("problem_name");
            if (tempresult.next()) {
                throw new Exception("More than one complement");
            }
        }

        tempresult.close();

        return compl;

    }

    /**
     * handles Problem Reductions, which is equivalent to the <from> attribute
     * in the masterdata.xml
     * 
     * @throws SQLException
     */
    private void handleProblemFrom() throws SQLException {

        tempresult = tempconn
                .executeQuery("SELECT upper_name, complexity_name FROM problem_reduction WHERE problem_reduction.lower_name= \""
                        + mainresult.getString("problem_name") + "\"");

        String from = null;

        while (tempresult.next()) {

            from = tempresult.getString("upper_name");
            Complexity c = Complexity.getComplexity(tempresult
                    .getString("complexity_name"));

            reductionsTodo.add(new ReductionWrapper(curProblem, from, c));
        }

        tempresult.close();
    }

    /**
     * Handles Notes for currently selected Problem.
     * 
     * @throws SQLException
     */
    private void handleProblemNote() throws SQLException {
        tempresult = tempconn
                .executeQuery("SELECT note,title FROM problem_note "
                        + "WHERE problem_name= \""
                        + mainresult.getString("problem_name") + "\"");

        while (tempresult.next()) {
            String noteName = tempresult.getString("title");

            refs.add(new Note(tempresult.getString("note"), noteName));
        }
        tempresult.close();
    }

    /**
     * Sets Properties of currently selected Graphclass, e.g. hereditary
     * 
     * @throws SQLException
     */
    private void handleGraphClassProps() throws SQLException {
        tempresult = tempconn
                .executeQuery("SELECT property.key, value FROM property LEFT JOIN gc_property ON"
                        + " gc_property.property_id = property.property_id "
                        + "WHERE gc_id=" + mainresult.getInt("gc_id"));

        while (tempresult.next()) {
            if (Tags.HERED.equals(tempresult.getString("key"))) {
                curClass.hered = tempresult.getString("value");
            } else if (Tags.SELFCO.equals(tempresult.getString("key"))) {
                if(tempresult.getString("value").equals("TRUE"))
                curClass.selfComplementary = true;
            } else if (Tags.CLIQUEFIXED.equals(tempresult.getString("key"))) {
                if(tempresult.getString("value").equals("TRUE"))
                curClass.cliqueFixed = true;
            }
        }
        tempresult.close();
    }

    /**
     * Sets Graphclass References for currently selected GraphClass
     * 
     * @throws SQLException
     */
    private void handleGraphClassRefs() throws SQLException {
        tempresult = tempconn.executeQuery("SELECT ref_id FROM gc_ref"
                + " WHERE gc_id=" + mainresult.getInt("gc_id"));

        while (tempresult.next()) {
            refs.add(new Ref(new String(tempresult.getString("ref_id"))));
        }
        tempresult.close();
    }

    /**
     * Sets GraphClass Notes for currently selected GraphClass
     * 
     * @throws SQLException
     */
    private void handleGraphClassNote() throws SQLException {
        tempresult = tempconn
                .executeQuery("SELECT title, note FROM gc_note WHERE"
                        + " gc_id=" + mainresult.getInt("gc_id"));
        while (tempresult.next()) {
            String noteName = tempresult.getString("title");
            refs.add(new Note(new String(tempresult.getString("note")),
                    noteName));
        }
        tempresult.close();
    }

    /**
     * Sets the GraphClass Derivations for a given GraphClass
     * 
     * @throws SQLException
     */
    private void handleGraphClassDerivations() throws SQLException {
        tempresult = tempconn
                .executeQuery("Select derived_id FROM gc_gc_derivation"
                        + " WHERE base_id =" + mainresult.getInt("gc_id")
                        + " AND type='graphclass'");
        while (tempresult.next()) {
            if (Tags.INTER.equals(curClass.type)
                    || Tags.FORBID.equals(curClass.type)
                    || Tags.UNION.equals(curClass.type)) {
                    curClass.set.add(new String(
                        String.valueOf(tempresult.getInt("derived_id"))));
            } else if (Tags.INDHERED.equals(curClass.type)
                    || Tags.CONHERED.equals(curClass.type)
                    || Tags.ISOHERED.equals(curClass.type)
                    || Tags.PROBE.equals(curClass.type)
                    || Tags.CLIQUE.equals(curClass.type)
                    || Tags.COMPL.equals(curClass.type)) {
                if (curClass.base == null)
                    curClass.base = new String(String.valueOf(tempresult.getInt("derived_id")));
            }

        }
        tempresult.close();
    }

    /**
     * Sets SmallGraph Derivations for selected Graphclass.
     * 
     * @throws SQLException
     */
    private void handleGraphClassSmallGraph() throws SQLException {
        tempresult = tempconn
                .executeQuery("SELECT sg_name FROM gc_sg_derivation WHERE gc_id = "
                        + mainresult.getInt("gc_id"));

        while (tempresult.next()) {
            if (Tags.INTER.equals(curClass.type)
                    || Tags.FORBID.equals(curClass.type)
                    || Tags.UNION.equals(curClass.type)) {
                curClass.set.add(new String(String.valueOf(tempresult.getString("sg_name"))));
            } else if (Tags.INDHERED.equals(curClass.type)
                    || Tags.CONHERED.equals(curClass.type)
                    || Tags.ISOHERED.equals(curClass.type)
                    || Tags.PROBE.equals(curClass.type)
                    || Tags.CLIQUE.equals(curClass.type)
                    || Tags.COMPL.equals(curClass.type)) {
                if (curClass.base == null){
                    curClass.base = new String(String.valueOf(tempresult.getString("sg_name")));
                }
            }
        }
        tempresult.close();
    }

    /**
     * sets References for Inclusions
     * 
     * @throws SQLException
     */
    private void handleInclEquDisjointImcpRefs() throws SQLException {
        tempresult = tempconn.executeQuery("SELECT ref_id, type FROM relation_ref"
                + " WHERE relation_id=" + mainresult.getString("relation_id"));

        while (tempresult.next()) {
            if(!tempresult.getString("type").equals("trivial"))
                refs.add(new Ref(new String(String.valueOf(tempresult.getString("ref_id")))));
            else{
                refs.add(new Ref("trivial"));
            }
        }

        tempresult.close();
    }

    /**
     * sets Algorithms for currently selected GraphClass
     * 
     * @throws SQLException
     */
    private void handleGraphClassAlgo() throws SQLException {

        tempresult = tempconn
                .executeQuery("SELECT algorithm.complexity_name, algorithm.algorithm_id, bounds, gc_problem.problem_name, "
                        + "algorithm.computed FROM algorithm LEFT JOIN gc_problem ON algorithm.gc_problem_id = "
                        + "gc_problem.gc_problem_id WHERE gc_problem.gc_id = "
                        + mainresult.getInt("gc_id"));

        while (tempresult.next()) {

            // TODO boolean computed for Rule Engine Group
            boolean computed = tempresult.getBoolean("algorithm.computed");

            try {
                curAlgo = new AlgoWrapper(curClass.id,
                        tempresult.getString("gc_problem.problem_name"),
                        tempresult.getString("algorithm.complexity_name"),
                        tempresult.getString("bounds") == null ? "NULL"
                                : tempresult.getString("bounds"));
                handleAlgoReferences();
                handleAlgoNotes();
            } catch (SAXException e) {
                e.printStackTrace();
            }

            curAlgo.end();
            algos.add(curAlgo);

        }
        tempresult.close();
    }

    /**
     * sets the Notes of Algorithms of selected Graphclass
     */
    private void handleAlgoNotes() throws SQLException {
        Statement temp2conn;

        temp2conn = m_connection.createStatement();
        ResultSet temp2result = temp2conn
                .executeQuery("SELECT note, title FROM algorithm_note WHERE algorithm_id ="
                        + tempresult.getString("algorithm.algorithm_id"));
        while (temp2result.next())
            refs.add(new Note(new String(temp2result.getString("note")),
                    temp2result.getString("title")));
        temp2result.close();

    }

    /**
     * sets the References for Algorithms of selected Graphclass
     */
    private void handleAlgoReferences() throws SQLException {

        Statement temp2conn = m_connection.createStatement();
        ResultSet temp2result = temp2conn
                .executeQuery("SELECT ref_id from algorithm_ref "
                        + "WHERE type = 'ref' AND algorithm_id = "
                        + tempresult.getString("algorithm.algorithm_id"));
        while (temp2result.next())
            refs.add(new Ref(new String(temp2result.getString("ref_id"))));
        temp2result.close();

    }

    /**
     * Reads in all ProblemDefinitions and saves them.
     */
    private void startProblems() {
        // create new problem p

        System.out.println("=======start reading problems=======");

        try {
            Problem p;
            mainresult = mainconn
                    .executeQuery("SELECT problem_name, dir, sparse, computed from problem");

            while (mainresult.next()) {
                // get the complement

                String compl = handleProblemComplement();

                // TODO computed for Rule Engine
                boolean computed = mainresult.getBoolean("computed");

                // create new problem as cliquewidth, recognition or normal
                // problem
                p = Problem.createProblem(
                        mainresult.getString("problem_name"), graph);

                p.setDirected(Tags.problemString2directed(mainresult
                        .getString("dir")));

                // add p to list of all problems
                problems.add(p);

                // add problem to hashtable with key = name and value = problem
                problemNames.put(p.getName(), p);

                // set complement to problem if not null
                if (compl != null) {
                    Problem c = (Problem) problemNames.get(compl);
                    if (c == null)
                        throw new SAXException("Complement problem " + compl
                                + "not found.");
                    p.setComplement((Problem) c);
                }

                // set problem to current problem
                curProblem = p;
                refs = new ArrayList();

                /*
                 * =========handle sparse (same as </sparse>)=========
                 */

                if (mainresult.getBoolean("sparse")) {
                    // set current problem sparse = true
                    curProblem.setSparse();
                }

                /*
                 * =========handle from (same as <from>)=========
                 */

                handleProblemFrom();

                /*
                 * =========handle note (same as <note>)=========
                 */
                handleProblemNote();

                curProblem.setRefs(new ArrayList(refs));

            }

            mainresult.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("=======finished reading problems=======");

    }

    /**
     * First method that is called by ISGCIReader. Here used to emulate the xml
     * Structure
     */
    private void startGraphclass() {

        System.out.println("=======start reading GraphClasses=======");
        // Add the problem reductions
        int i, size, oldsize;

        // number of elements in list (child,parent,complexity)
        oldsize = reductionsTodo.size();
        while ((size = reductionsTodo.size()) != 0) {
            for (i = size - 1; i >= 0; i--) {
                // if element of list complete reduct the problem
                if (reductionsTodo.get(i).generate())
                    reductionsTodo.remove(i);
            }
            /*
             * Iterator<ReductionWrapper> iter = reductionsTodo.iterator();
             * while (iter.hasNext()) if (iter.next().generate()) iter.remove();
             */

            // if no problems reduct
            if (reductionsTodo.size() == oldsize) {
                System.err.println(size + " problems not resolved");
                System.err.println(reductionsTodo);
                return;
            }
            oldsize = size;
        }
    }

    /**
     * Reads in all GraphClasses and saves them. Since database is very nested,
     * it takes a lot of time to do that. Could probably be improved with better
     * Joins.
     */
    private void startgraphclasses() {

        try {

            mainresult = mainconn
                    .executeQuery("SELECT gc_id, name, type,computed FROM graphclass"
                            + " WHERE gc_id mod 8 = 0");

            while (mainresult.next()) {

                tempresult = tempconn
                        .executeQuery("SELECT value FROM property "
                                + "LEFT JOIN gc_property ON gc_property.property_id ="
                                + "property.property_id" + " WHERE gc_id="
                                + mainresult.getInt("gc_id")
                                + " AND property.key='dir'");

                // TODO boolean for Rule Engine
                boolean computed = mainresult.getBoolean("computed");

                String dirtype = null;

                while (tempresult.next())
                    dirtype = tempresult.getString("value");

                tempresult.close();

                // TODO Right now, adding gc_ in front of id
                curClass = new GraphClassWrapper(mainresult.getInt("gc_id"),
                        mainresult.getString("type"), dirtype);

                curClass.name = mainresult.getString("name");

                if (curClass.type.equals(Tags.FORBID))
                    handleGraphClassSmallGraph();

                handleGraphClassProps();

                if (!curClass.type.equals(Tags.FORBID))
                    handleGraphClassDerivations();

                handleGraphClassRefs();

                handleGraphClassAlgo();

                handleGraphClassNote();

                endgraphclass();
            }
            mainresult.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("=======finished reading GraphClasses=======");
    }

    /**
     * Reads in all Inclusions. Has to be called after creating all GraphClasses
     * because it depends on these.
     */
    private void startInclEqu() {
        System.out.println("=======start reading Inclusions=======");
        try {
            mainresult = mainconn
                    .executeQuery("SELECT gc1_id, gc2_id, confidence, relation_id, type, computed "
                            + "FROM relation WHERE gc1_id mod 8 = 0 AND gc2_id mod 8 = 0"
                            + " AND NOT type = 'disjoint' AND NOT type "
                            + "= 'incmp'");
            while (mainresult.next()) {
                // TODO boolean computed for Rule Engine Group
                boolean computed = mainresult.getBoolean("computed");

                GraphClass gcsuper = classes.get(mainresult.getInt("gc1_id"));
                GraphClass gcsub = classes.get(mainresult.getInt("gc2_id"));

                if (gcsuper == gcsub)
                    throw new Exception("super = sub = " + gcsuper.getID());

                if (graph.containsEdge(gcsuper, gcsub))
                    throw new Exception("Edge " + gcsuper.getID() + " -> "
                            + gcsub.getID() + " already exists");

                if (gcsuper.getDirected() != gcsub.getDirected())
                    throw new Exception("Edge with unmatched directedness "
                            + gcsuper.getID() + " -> " + gcsub.getID());

                curIncl = graph.addEdge(gcsuper, gcsub);

                curIncl.setConfidence(mainresult.getInt("confidence") - 1);

                refs = new ArrayList();

                handleInclEquDisjointImcpRefs();

                curIncl.setRefs(refs);
            }

            mainresult.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("=======finished reading Inclusions=======");
    }

    /**
     * Reads in all Disjoints and Incomparables. Must be called after creating
     * the GraphClass Objects because it depends on these.
     */
    private void startDisjointIncmp() {
        System.out
                .println("=======start reading Disjoints and Incomparables=======");
        try {
            mainresult = mainconn
                    .executeQuery("SELECT gc1_id, gc2_id, type, confidence, relation_id"
                            + " FROM relation WHERE gc1_id mod 8 = 0 AND gc2_id mod 8 = 0 "
                            + "AND (type ='disjoint' OR type ='incmp')");

            while (mainresult.next()) {
                if (mainresult.getString("gc1_id").equals(
                        mainresult.getString("gc2_id")))
                    throw new Exception("gc1 = gc2 = "
                            + mainresult.getString("gc1_id"));

                GraphClass gc1 = classes.get(mainresult.getInt("gc1_id"));
                GraphClass gc2 = classes.get(mainresult.getInt("gc2_id"));

                if (gc1.getDirected() != gc2.getDirected())
                    throw new Exception(
                            "Relation with unmatched directedness "
                                    + gc1.getID() + " -> " + gc2.getID());

                if (Tags.DISJOINT.equals(mainresult.getString("type")))
                    curRel = new Disjointness(gc1, gc2);
                else
                    curRel = new Incomparability(gc1, gc2);

                curRel.setConfidence(mainresult.getInt("confidence") - 1);

                for (AbstractRelation r : relations)
                    if (r.get1() == curRel.get1() && r.get2() == curRel.get2())
                        throw new Exception(
                                "An incomparability or disjointness between "
                                        + curRel.get1().getID() + " and "
                                        + curRel.get2().getID()
                                        + " already exists.");

                relations.add(curRel);

                refs = new ArrayList();

                handleInclEquDisjointImcpRefs();

                curRel.setRefs(refs);
            }
            mainresult.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out
                .println("=======finished reading Disjoints and Incomparables=======");
    }

    /**********************************************
     * 
     * Endfunctions
     * 
     **********************************************/

    private void endGraphClasses() {

        int i, size, oldsize = todo.size();
        while ((size = todo.size()) != 0) {
            for (i = size - 1; i >= 0; i--) {
                try {
                    if (todo.get(i).generate())
                        todo.remove(i);
                } catch (SAXException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (todo.size() == oldsize) {
                System.err.println(size + " classes not resolved");
                System.err.println(todo);
                return;
            }
            oldsize = size;
        }
        // System.out.println(classes.size()+" classes successfully read");

        // Then create the Complexities.
        for (AlgoWrapper aw : algos)
            aw.generate();

        System.out.println("=======finished reading GraphClasses=========");

    }

    private void endgraphclass() {
        curClass.end();
        try {
            if (!curClass.generate())
                todo.add(curClass);
        } catch (SAXException e) {
            System.out.println("Some SAX ERROR occured during endgraphclass");
            e.printStackTrace();
        }
    }

    /**
     * Returned format is: "YYYY-(M)M-(D)D HH:MM:SS"
     * 
     * @return current date and time
     */
    public static String getDateTime() {
        Calendar cal = Calendar.getInstance();
        return (cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-"
                + cal.get(Calendar.DATE) + " " + cal.get(Calendar.HOUR_OF_DAY)
                + ":" + cal.get(Calendar.MINUTE) + ":" + cal
                    .get(Calendar.SECOND));
    }

    // ===================================Wrapper============================================

    // -------------------------- GraphClassWrapper -------------------------
    //-------------------------- GraphClassWrapper -------------------------
    private class GraphClassWrapper {
        String type, name, dirtype;
        Integer id;                             //intID
        String base;            // base class id for complement/hereditary
        HashSet<String> set;    // set for union/intersect/forbidden
        String hered;
        boolean selfComplementary;
        boolean cliqueFixed;
        List<ProblemWrapper> complexities;
        List refs, prevrefs;
        
        public GraphClassWrapper(Integer id, String type, String dirtype) { //intID
            name = null;
            base = null;
            set = null;
            hered = null;
            selfComplementary = false;
            cliqueFixed = false;
            complexities = new ArrayList<ProblemWrapper>();
            this.id = id;
            this.type = type;
            this.dirtype = dirtype;
            if (Tags.INTER.equals(type) ||
                    Tags.FORBID.equals(type) ||
                    Tags.UNION.equals(type)) {
                set = new HashSet<String>();
            }
            prevrefs = SQLReader.this.refs;
            SQLReader.this.refs = refs = new ArrayList();
        }

        public void end() {
            SQLReader.this.refs = prevrefs;
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
            classes.put(id, gc);
            return true;
        }

        public String toString() {
            return "<GraphClass: "+id+" "+name+">";
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
            prevrefs = SQLReader.this.refs;
            SQLReader.this.refs = refs = new ArrayList();
        }

        public void end() {
            SQLReader.this.refs = prevrefs;
        }

        public boolean generate() {
            problem.createAlgo(classes.get(id), complexity, bounds, refs);
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

    /**
     * For debugging Purposes. Prints out read-in graphclasses, problems and relations.
     */
    private void printOutStuff() {
        System.out.println("GRAPHCLASSES");
        System.out.println("------------------------");
        System.out.println(graph.toString());
        System.out.println("PROBLEMS");
        System.out.println("------------------------");
        System.out.println("PROBLEMSIZE: " + problems.size());
        for (int i = 0; i < problems.size(); i++) {
            System.out.println(problems.get(i).getName());
        }
        System.out.println("RELATIONS");
        System.out.println("------------------------");
        System.out.println("RELATIONSIZE: " + relations.size());
        System.out.println("------------------------");
        for (int i = 0; i < relations.size(); i++) {
            System.out.println(relations.get(i).toString());
        }
    }

}
