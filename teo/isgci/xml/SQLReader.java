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
import teo.isgci.parameter.Boundedness;
import teo.isgci.parameter.GraphParameter;
import teo.isgci.parameter.PseudoClass;
import teo.isgci.problem.Complexity;
import teo.isgci.problem.ParamAlgorithm;
import teo.isgci.problem.ParamComplexity;
import teo.isgci.problem.Problem;
import teo.isgci.ref.Note;
import teo.isgci.ref.Ref;
import teo.isgci.relation.AbstractRelation;
import teo.isgci.relation.Disjointness;
import teo.isgci.relation.Inclusion;
import teo.isgci.relation.Incomparability;
import teo.isgci.relation.NotBounds;
import teo.isgci.relation.Open;

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

    /* Parameters (added by vector)*/
    private HashMap<String, GraphParameter> parameterNames;
    private HashMap<Integer, GraphParameter> parameterIDs;
    private List<GraphParameter> parameters;
    private List<GraphParameterWrapper> parTodo;
    private GraphParameterWrapper curGraphPar;
    private List<BoundednessProofWrapper> proofs;
    private BoundednessProofWrapper curProof;

    // --------------------------------------------------------------------------------------

    /* Problems */
    private Hashtable problemNames;
    private List<Problem> problems;
    private List<AlgoWrapper> algos; // All read algo elements
    private AlgoWrapper curAlgo;
    private Problem curProblem;
    private List<ReductionWrapper> reductionsTodo;
    private List<ParamAlgoWrapper> paramAlgos;
    private ParamAlgoWrapper curParAlgo;

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
            List<Problem> problems, List<GraphParameter> parameters)
            throws SQLException {

        // get transfered graph g
        this.graph = g;

        // get transfered problems
        this.problems = problems;

        // get transfered parameters
        this.parameters = parameters;

        parameterNames = new HashMap<String, GraphParameter>();
        parameterIDs = new HashMap<Integer, GraphParameter>();
        problemNames = new Hashtable();
        classes = new HashMap<Integer, GraphClass>();
        todo = new ArrayList<GraphClassWrapper>();
        parTodo = new ArrayList<GraphParameterWrapper>();
        paramAlgos = new ArrayList<ParamAlgoWrapper>();
        algos = new ArrayList<AlgoWrapper>();
        reductionsTodo = new ArrayList<ReductionWrapper>();
        relations = new ArrayList<AbstractRelation>();
        parsingDone = false;
        proofs = new ArrayList<BoundednessProofWrapper>();

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

    /**
     * Method for returning Parameters
     * 
     * @return List<GraphParameter> parameters
     * @author vector
     */
    public List<GraphParameter> getParameters() {
        return parameters;
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
        startParameters();
        endParameters();
        startParameterRelations();
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
     * Sets GraphParameter References for currently selected GraphParameter
     * 
     * @throws SQLException
     * @author vector
     */
    private void handleGraphParameterRefs() throws SQLException {
        tempresult = tempconn.executeQuery("SELECT ref_id, type FROM param_ref"
                + " WHERE param_id=" + mainresult.getInt("param_id"));

        while (tempresult.next()) {
            String type = tempresult.getString("type");
            if (type.equals("ref"))
                refs.add(new Ref(new String("ref_"
                        + tempresult.getString("ref_id"))));
            else
                refs.add(new Ref(new String(type)));
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
     * Sets GraphParameter Notes for currently selected GraphParameter
     * 
     * @throws SQLException
     * @author vector
     */
    private void handleGraphParameterNotes() throws SQLException {
        tempresult = tempconn
                .executeQuery("SELECT title, note FROM param_note WHERE"
                        + " param_id=" + mainresult.getInt("param_id"));
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
                        + mainresult.getInt("gc_id") + " AND algorithm.gc_id = "
                        + mainresult.getInt("gc_id"));
        // read only the algorithms defined in materdata.xml

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
     * sets Algorithms for currently selected GraphParameter
     * 
     * @throws SQLException
     * @author vector
     */
    private void handleGraphParameterAlgo() throws SQLException {
        tempresult = tempconn
                .executeQuery("SELECT param_algorithm.complexity_alias, "
                        + "param_algorithm.param_algorithm_id, bounds, "
                        + "param_problem.problem_name, param_algorithm.computed"
                        + " FROM param_algorithm LEFT JOIN param_problem ON "
                        + "param_algorithm.param_problem_id = "
                        + "param_problem.param_problem_id WHERE "
                        + "param_problem.param_id = "
                        + mainresult.getInt("param_id")
                        + " AND param_algorithm.param_id = "
                        + mainresult.getInt("param_id"));
        // read only the algorithms defined in materdata.xml

        while (tempresult.next()) {

            // TODO boolean computed for Rule Engine Group
            boolean computed = tempresult
                    .getBoolean("param_algorithm.computed");

            try {
                curParAlgo = new ParamAlgoWrapper(
                        curGraphPar.id,
                        tempresult.getString("param_problem.problem_name"),
                        tempresult
                                .getString("param_algorithm.complexity_alias"),
                        tempresult.getString("bounds") == null ? "NULL"
                                : tempresult.getString("bounds"));
                handleParamAlgoReferences();
                handleParamAlgoNotes();
            } catch (SAXException e) {
                e.printStackTrace();
            }

            curParAlgo.end();
            paramAlgos.add(curParAlgo);

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
     * sets the Notes of Algorithms of selected Graphparameter
     * @author vector
     */
    private void handleParamAlgoNotes() throws SQLException {
        Statement temp2conn;

        temp2conn = m_connection.createStatement();
        ResultSet temp2result = temp2conn
                .executeQuery("SELECT note, title FROM param_algo_note "
                        + "WHERE param_algorithm_id ="
                        + tempresult
                             .getString("param_algorithm.param_algorithm_id"));
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
     * sets the References for Algorithms of selected Graphparameter
     * @author vector
     */
    private void handleParamAlgoReferences() throws SQLException {

        Statement temp2conn = m_connection.createStatement();
        ResultSet temp2result = temp2conn
                .executeQuery("SELECT ref_id, type from param_algo_ref "
                        + "WHERE type = 'ref' AND param_algo_id = "
                        + tempresult
                             .getString("param_algorithm.param_algorithm_id"));
        while (temp2result.next()) {
            String type = temp2result.getString("type");
            if (type.equals("ref"))
                refs.add(new Ref(new String("ref_"
                        + temp2result.getString("ref_id"))));
            else
                refs.add(new Ref(new String(type)));
        }
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
                    .executeQuery("SELECT problem_name, dir, sparse, forparams, computed from problem");

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
                 * =========handle forparams (same as </forparams>)====
                 */
                if (mainresult.getBoolean("forparams")) {
                    // set current problem for parameters = true
                    curProblem.setParameters(true);
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
     * Handle the complement parameters. Has to be called after all Parameters
     * are read.
     * @throws SQLException
     * @throws SAXException
     * @author vector
     */
    private void handleParameterComplements() throws SQLException,
            SAXException {

        mainresult = mainconn
                .executeQuery("SELECT param_id, complement FROM parameter WHERE"
                        + " complement IS NOT NULL");

        while (mainresult.next()) {
            GraphParameter par = parameterIDs.get(mainresult
                    .getInt("param_id"));
            GraphParameter compl = parameterIDs.get(mainresult
                    .getInt("complement"));
            if (compl == null)
                throw new SAXException("Complement parameter "
                        + mainresult.getInt("complement") + " not found.");
            if (par.getComplement() != null
                    && !par.getComplement().equals(compl))
                throw new SAXException(
                        "Inconsistent complement data for parameters "
                                + compl.getID() + " and " + par.getID());
            par.setComplement(compl);
        }
        mainresult.close();
    }

    /**
     * sets Boundedness Proofs for currently selected GraphClass
     *
     * @throws SQLException
     * @author vector
     */
    private void handleGraphClassBoundednessProof() throws SQLException {

        tempresult = tempconn
                .executeQuery("SELECT gc_parameter_boundedness.boundedness, "
                      + "gc_parameter_boundedness.gc_parameter_boundedness_id,"
                      + " gc_param.param_id, gc_parameter_boundedness.computed"
                      + " FROM gc_parameter_boundedness LEFT JOIN gc_param ON"
                      + " gc_parameter_boundedness.gc_param_id = "
                      + "gc_param.gc_param_id WHERE gc_param.gc_id = "
                      + mainresult.getInt("gc_id")
                      + " AND gc_parameter_boundedness.gc_id = "
                      + mainresult.getInt("gc_id"));
        // read only the boundedness proofs defined in materdata.xml

        while (tempresult.next()) {

            // TODO boolean computed for Rule Engine Group
            boolean computed = tempresult
                    .getBoolean("gc_parameter_boundedness.computed");

            try {
                curProof = new BoundednessProofWrapper(curClass.id,
                        tempresult.getInt("gc_param.param_id"),
                        tempresult
                            .getString("gc_parameter_boundedness.boundedness"));
                handleProofReferences();
                handleProofNotes();
            } catch (SAXException e) {
                e.printStackTrace();
            }

            curProof.end();
            proofs.add(curProof);

        }
        tempresult.close();
    }
    
    /**
     * sets the Notes of boundedness proofs of selected Graphclass
     * @author vector
     */
    private void handleProofNotes() throws SQLException {
        Statement temp2conn;

        temp2conn = m_connection.createStatement();
        ResultSet temp2result = temp2conn
                .executeQuery("SELECT note, title FROM gc_param_note WHERE "
                        + "gc_parameter_boundedness_id ="
                        + tempresult.getString(
                      "gc_parameter_boundedness.gc_parameter_boundedness_id"));
        while (temp2result.next())
            refs.add(new Note(new String(temp2result.getString("note")),
                    temp2result.getString("title")));
        temp2result.close();

    }

    /**
     * sets the References for boundedness proofs of selected Graphclass
     * @author vector
     */
    private void handleProofReferences() throws SQLException {

        Statement temp2conn = m_connection.createStatement();
        ResultSet temp2result = temp2conn
                .executeQuery("SELECT ref_id, type from gc_param_ref "
                        + "WHERE type = 'ref' AND gc_param_boundedness_id = "
                        + tempresult.getString(
                      "gc_parameter_boundedness.gc_parameter_boundedness_id"));
        while (temp2result.next()) {
            String type = temp2result.getString("type");
            if (type.equals("ref"))
                refs.add(new Ref(new String("ref_"
                        + temp2result.getString("ref_id"))));
            else
                refs.add(new Ref(new String(type)));
        }
        temp2result.close();

    }

    /**
     * Reads in all GraphParameters and saves them.
     * @author vector
     */
    private void startParameters() {
        System.out.println("=======start reading GraphParameters=======");
        try {

            mainresult = mainconn.executeQuery("SELECT param_id, name, dir,"
                    + "decomposition_time, decomposition_problem"
                    + " FROM parameter");

            while (mainresult.next()) {
                curGraphPar = new GraphParameterWrapper(
                        mainresult.getInt("param_id"),
                        mainresult.getString("name"),
                        mainresult.getString("dir"),
                        mainresult.getString("decomposition_time"));

                handleGraphParameterAlgo();

                handleGraphParameterRefs();

                handleGraphParameterNotes();

                endparameter();
            }
            mainresult.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("=======finished reading GraphParameters=======");
    }

    /**
     * Reads all Relations between GraphParameters.
     * @author vector
     */
    private void startParameterRelations() {
        System.out.println("=======start reading paramrelations=======");
        try {

            mainresult = mainconn
                    .executeQuery("SELECT param_relation_id, param1, type, "
                            + "param2, confidence, functiontype "
                            + "FROM param_relation");

            while (mainresult.next()) {
                GraphParameter gp1 = parameterIDs.get(mainresult
                        .getInt("param1"));
                GraphParameter gp2 = parameterIDs.get(mainresult
                        .getInt("param2"));
                PseudoClass pgc1 = gp1.getPseudoClass();
                PseudoClass pgc2 = gp2.getPseudoClass();
                String rel = mainresult.getString("type");
                String tmp_rel_id = mainresult.getString("param_relation_id");

                boolean confidence = mainresult.getBoolean("confidence");

                try {
                    if (pgc1 == pgc2)
                        throw new SAXException("param1 = param2 = "
                                + pgc1.getID());
                    if (rel.equals(Tags.PAR_BOUNDS) || rel.equals(Tags.PAR_EQU)
                            || rel.equals(Tags.PAR_STRICT_BOUNDS)) {
                        if (graph.containsEdge(pgc1, pgc2))
                            throw new SAXException("Edge " + pgc1.getID()
                                    + " -> " + pgc2.getID()
                                    + " already exists");
                        if (pgc1.getDirected() != pgc2.getDirected())
                            throw new SAXException(
                                    "Edge with unmatched directedness "
                                            + pgc1.getID() + " -> "
                                            + pgc2.getID());
                        curIncl = graph.addEdge(pgc1, pgc2);
                        if (rel.equals(Tags.PAR_STRICT_BOUNDS))
                            curIncl.setProper(true);
                        for (AbstractRelation r : relations)
                            if (r.get1() == pgc2 && r.get2() == pgc1)
                                curIncl.setProper(true);
                        curIncl.setConfidence(Tags
                                .boolean2confidence(confidence));
                        curIncl.setFunctiontype(mainresult
                                .getString("functiontype"));
                    } else

                    // Case: Relation "not >=" (not bounds) or open
                    if (rel.equals(Tags.PAR_NOT_BOUNDS)
                            || rel.equals(Tags.OPEN)) {

                        if (rel.equals(Tags.PAR_NOT_BOUNDS))
                            curRel = new NotBounds(pgc1, pgc2);
                        else
                            curRel = new Open(pgc1, pgc2);

                        curRel.setConfidence(
                                Tags.boolean2confidence(confidence));
                        if (graph.containsEdge(pgc2, pgc1)
                                && curRel instanceof NotBounds)
                            graph.getEdge(pgc2, pgc1).setProper(true);
                        for (AbstractRelation r : relations)
                            if (r.get1() == curRel.get1()
                                    && r.get2() == curRel.get2())
                                throw new SAXException(
                                        "A not >= or open between "
                                                + curRel.get1() + " and "
                                                + curRel.get2()
                                                + " already exists.");
                        relations.add(curRel);
                    } else {
                        throw new IllegalArgumentException(
                                "Unknown relation type for graphparameters: "
                                        + rel);
                    }
                    refs = new ArrayList();
                    endParameterRelation(rel, tmp_rel_id);
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
            mainresult.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("=======finished reading paramrelations=======");
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

                handleGraphClassBoundednessProof();

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
                else if (Tags.INCOMPARABLE
                        .equals(mainresult.getString("type")))
                    curRel = new Incomparability(gc1, gc2);
                else
                    curRel = new Open(gc1, gc2);

                curRel.setConfidence(mainresult.getInt("confidence") - 1);

                for (AbstractRelation r : relations)
                    if (r.get1() == curRel.get1() && r.get2() == curRel.get2())
                        throw new Exception(
                                "An incomparability or disjointness or open"
                                        + " between " + curRel.get1().getID()
                                        + " and " + curRel.get2().getID()
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
        // Boundedness Proofs added by vector
        for (BoundednessProofWrapper pw : proofs)
            pw.generate();

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
     * Endfunction for parameters, works like endGraphClasses().
     * @author vector
     */
    private void endParameters() {
        // First generate the outstanding graphparameters.
        int i, size, oldsize = parTodo.size();
        while ((size = parTodo.size()) != 0) {
            for (i = size - 1; i >= 0; i--) {
                try {
                    if (parTodo.get(i).generate())
                        parTodo.remove(i);
                } catch (SAXException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (parTodo.size() == oldsize) {
                System.err.println(size + " parameters not resolved");
                System.err.println(parTodo);
                return;
            }
            oldsize = size;
        }

        // handle complements
        try {
            handleParameterComplements();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            System.out.println("Some SAX ERROR occured during endParameters");
            e.printStackTrace();
        }

        // Then create the Complexities.
        for (ParamAlgoWrapper aw : paramAlgos)
            aw.generate();
    }

    /**
     * endfunction for parameter works like endgraphclass().
     * @author vector
     */
    private void endparameter() {
        curGraphPar.end();
        try {
            if (!curGraphPar.generate())
                parTodo.add(curGraphPar);
        } catch (SAXException e) {
            System.out.println("Some SAX ERROR occured during endparameter");
            e.printStackTrace();
        }
    }

    /**
     * Endfunction for parameter Relations. Called when creating Relations for
     * adding refs and notes.
     * @param rel the relation type
     * @param tmp_rel_id the id of the current read relation
     * @author vector
     */
    private void endParameterRelation(String rel, String tmp_rel_id) {

        try {
            //Build references
            tempresult = tempconn.executeQuery(""
                    + "SELECT ref_id, type FROM param_relation_ref"
                    + " LEFT JOIN param_relation"
                    + " ON param_relation_ref.param_relation_id = "
                    + "param_relation.param_relation_id"
                    + " WHERE param_relation.param_relation_id = "
                    + tmp_rel_id);
            while (tempresult.next()) {
                String type = tempresult.getString("type");
                if (type.equals("ref"))
                    refs.add(new Ref(new String("ref_"
                            + tempresult.getString("ref_id"))));
                else
                    refs.add(new Ref(new String(type)));
            }
            //Build notes
            tempresult = tempconn.executeQuery(""
                    + "SELECT title, text FROM param_relation_note"
                    + " LEFT JOIN param_relation"
                    + " ON param_relation_note.param_relation_id = "
                    + "param_relation.param_relation_id"
                    + " WHERE param_relation.param_relation_id = "
                    + tmp_rel_id);
            while (tempresult.next()) {
                String noteName = tempresult.getString("title");
                refs.add(new Note(tempresult.getString("text"), noteName));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Add references and push into system.
        if (rel.equals(Tags.PAR_BOUNDS)
                || rel.equals(Tags.PAR_EQU)
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

    // ------------------------ GraphParameterWrapper -----------------------
    /**
     * Serves as an all-in-one package for creating GraphParameters. More
     * information can be found in the constructor.
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
         * for creating GraphParameters in the system. Important: Only
         * information, no actual function (like deduction). By invoking
         * {@link #end()}, the parameter will receive the corresponding
         * references. Eventually, the wrapper can be used to create an
         * {@link GraphParameter}-object in the system by invoking
         * {@link #generate()}.
         * 
         * @param id
         *            A unique ID-String.
         * @param name
         *            The parameter-name
         * @param dirtype
         *            The corresponding {@link Directed}-Enum-value
         * @param decomp
         *            A decomposition time.
         * @param compl
         *            Corresponding complementary {@link GraphParameter} (if
         *            available)
         */
        public GraphParameterWrapper(Integer id, String name, String dirtype,
                String decomp) {
            complexities = new ArrayList<ParamProblemWrapper>();
            this.id = id;
            this.name = name;
            this.dirtype = dirtype;
            this.decomp = decomp;
            prevrefs = SQLReader.this.refs;
            SQLReader.this.refs = refs = new ArrayList();
        }

        /**
         * Adds references.
         */
        public void end() {
            SQLReader.this.refs = prevrefs;
        }

        /**
         * Creates a parameter with information provided by the wrapper and
         * eventually adds it into the system.
         * 
         * @return True if creation was successfully.
         * @throws SAXException
         *             Thrown if duplicate parameters exist.
         */
        public boolean generate() throws SAXException {
            GraphParameter par = GraphParameter.createParameter(id, name,
                    graph);

            // ---- Check if class already exists
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
                            + "non-linear decompisible parameter "
                            + par.getName());
                par.setDecompositionProblem(decomposition);
            }
            parameterIDs.put(id, par);
            parameterNames.put(name, par);
            par.setRefs(refs);
            for (ParamProblemWrapper w : complexities) {
                w.problem.setComplexity(par.getPseudoClass(), w.complexity);
            }
            parameters.add(par);
            return true;
        }

        @Override
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

    // -------------------------- ParamAlgoWrapper -------------------------
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
            prevrefs = SQLReader.this.refs;
            SQLReader.this.refs = refs = new ArrayList();
        }

        /**
         * Finishes the wrapper by adding references.
         */
        public void end() {
            SQLReader.this.refs = prevrefs;
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

    // ---------------------- BoundednessProofWrapper --------------
    /**
     * Serves as all-in-one help for creating/setting boundedness proofs values
     * on graphclasses with associated parameters.
     * 
     * Similar to {@link BoundednessWrapper}, but with references.
     * 
     * More info can be found in the constructor.
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
         * @param id
         *            graphparameter, whose boundedness is set.
         * @param id2
         *            graphclass, the parameter is (un-)bounded on.
         * @param boundedness
         *            the boundedness enumeration value (More info:
         *            {@link Boundedness})
         */
        public BoundednessProofWrapper(Integer id2, Integer id,
                String boundedness) throws SAXException {
            this.id = id2;
            this.parameter = parameterIDs.get(id);
            if (this.parameter == null)
                throw new SAXException("parameter not found: " + id);
            this.boundedness = Boundedness.getBoundedness(boundedness);
            prevrefs = SQLReader.this.refs;
            SQLReader.this.refs = refs = new ArrayList();
        }

        /**
         * Adds references.
         */
        public void end() {
            SQLReader.this.refs = prevrefs;
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

    //-------------------- ParamProblemWrapper --------------
    /**
     * Serves as all-in-one help for creating/setting parameterized complexity values on
     * pseudoclasses with associated problems.
     *
     * More info can be found in the constructor.
     * @author vector
     */
    private class ParamProblemWrapper {
        Problem problem;
        ParamComplexity complexity;

        /**
         * Constructs an all-in-one set containing the necessary information
         * for setting parametrized complexity on graphclasses. Important: Only
         * information, no actual function.
         * 
         * 
         * @param p
         *            Problem for which the complexity is set
         * @param c
         *            the parameterized complexity enumeration value (More
         *            info: {@link ParamComplexity})
         */
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

    /**
     * For debugging Purposes. Prints out read-in graphclasses, problems and relations.
     */
    private void printOutStuff() {
        System.out.println("GRAPHCLASSES");
        System.out.println("------------------------");
        System.out.println(graph.toString());
        System.out.println("PARAMETERS");
        System.out.println("------------------------");
        System.out.println("PARAMETERSIZE: " + parameters.size());
        for (int i = 0; i < parameters.size(); i++) {
            System.out.println(parameters.get(i).getName());
        }
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
