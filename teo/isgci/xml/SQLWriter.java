package teo.isgci.xml;

import java.sql.*;
import java.util.Calendar;

import teo.isgci.parameter.GraphParameter;
import teo.isgci.problem.Complexity;
import teo.isgci.problem.Problem;


//rz225.wwwdns.rz.uni-konstanz.de

public class SQLWriter {
    private Connection m_connection;
    private String m_dbName;
    private String m_insertOption;
    private String m_insertIgnoreOrReplace;
    private String[] m_pooledValues;
    private Integer m_pooledCounter;
    private String m_pooledTable;

    /**
     * Create a new SQLWriter with a connection to the database.
     * 
     * @param databaseAddress
     *            address to database for example: jdbc:myDriver:myDatabase
     * @param username
     * @param password
     * @param dbName
     *            the name of the database we are using, for example the ones we
     *            used early on: SpectreSoft or newspec. If none used/required,
     *            just pass an empty String.
     * @param writeAll
     *            true: writes all rows with REPLACE INTO. false: if primary key
     *            already existing, do nothing (INSERT IGNORE used).
     * @throws SQLException
     */
    public SQLWriter(String databaseAddress, String username, String password,
            String dbName, Boolean writeAll) throws SQLException {
        m_connection = DriverManager.getConnection(databaseAddress, username,
                password);
        m_dbName = (dbName.length() == 0) ? "" : dbName + ".";
        m_insertOption = writeAll ? "" : "";
        m_insertIgnoreOrReplace = writeAll ? "REPLACE " : "INSERT IGNORE ";
        m_pooledValues = new String[1000];
        m_pooledCounter = 0;

    }

    /**********************************************
     * 
     * Insert functions for problem related tables
     * 
     **********************************************/

    /**
     * Insert new line into table problem, if you want to leave any field empty,
     * call with "NULL" for String or null for Boolean. Note that name is
     * primary id and cant be "NULL" or null.
     * 
     * @param name
     *            String with the name
     * @param dir
     *            String with its direction
     * @param sparse
     *            boolean wether its sparse or not
     * @param forparams
     *            boolean whether it's applicable for parameters or not
     * @throws SQLException
     */
    public void insertNewProblem(Integer problemID, String name, String dir,
            Boolean sparse, Boolean forparams, Boolean computed) throws SQLException {
        String sparseString = (sparse == null) ? "NULL" : (sparse ? "1" : "0");
        String computedString = computed ? "1" : "0";
        String forParamsString = (forparams == null) ? "NULL" : (forparams ? "1" : "0");

        String[] values = { problemID.toString(), "'" + name + "'", 
                dir == null ? "NULL" : getCharOfString(dir), getCharOfString(sparseString), 
                getCharOfString(forParamsString), "1", // TODO  autoapproved ?
                "NULL", "NULL", "'system'", getDateTime(), computedString };

        executeSingleSQLINSERT("problem", values);

    }

    /**
     * Insert new line into table problem_reduction, if you want to leave any
     * field empty, call with "NULL" as String or null for Boolean. Note that
     * reductionID is primary id and cant be null.
     * 
     * @param reductionID
     *            int ID for reductions
     * @param lowerProblem
     *            String name of problem that is the lower part of the reduction
     * @param upperProblem
     *            String name of problem that is the upper part of the reduction
     * @param complexityAlias
     *            String that describes the complexity
     * @param complement
     *            Boolean whether or not its a complement
     * @throws SQLException
     */
    public void insertNewProblemReduction(Integer reductionID,
            String lowerProblem, String upperProblem, String complexityAlias,
            Boolean complement) throws SQLException {
        String complementString = (complement == null) ? "NULL"
                : (complement ? "1" : "0");

        String[] values = { reductionID.toString(), "'" + lowerProblem + "'",
                "'" + upperProblem + "'", "'" + complexityAlias + "'",
                getCharOfString(complementString) };

        executeSingleSQLINSERT("problem_reduction", values);

    }

    /**
     * Insert new line into table problem_note, if you want to leave any field
     * empty, call with "NULL" as String. Note that noteID is primary id and
     * cant be null.
     * 
     * @param noteID
     *            int ID for problem notes
     * @param parentProblem
     *            name of the related problem
     * @param title
     *            title of the problem note
     * @param text
     *            the note itself
     * @throws SQLException
     */
    public void insertNewProblemNote(Integer noteID, String parentProblem,
            String title, String text) throws SQLException {
        String[] values = { noteID.toString(), "'" + parentProblem + "'",
                getCharOfString(title), "'" + correctQuotations(text) + "'" };

        executeSingleSQLINSERT("problem_note", values);
    }

    /**
     * Insert new gc problem relation. gcProblemID is primary key and can't be
     * null.
     * 
     * @param gcProblemID
     * @param gc
     *            graphclass where the problem is defined on
     * @param problem
     *            the problem that is defined on a graphclass
     * @param complexity_alias
     *            the complexity of this problem on that graphclass
     * @throws SQLException
     */
    public void insertNewGCProblemRelation(Integer gcProblemID, Integer gc,
            String problem, String complexity_alias) throws SQLException {
        String[] values = { gcProblemID.toString(), gc.toString(),
                "'" + problem + "'", "'" + complexity_alias + "'" };

        prepareBatchSQLINSERT("gc_problem", values);
    }

    /**
     * Insert new param problem relation. paramProblemID is primary key and
     * can't be null.
     * 
     * @param parProblemID
     * @param par
     *            parameter where the problem is defined on
     * @param problem
     *            the problem that is defined on a parameter
     * @param complexity_alias
     *            the complexity of this problem on that parameter
     * @throws SQLException
     * @author vector
     */
    public void insertNewParamProblemRelation(Integer parProblemID,
            Integer par, String problem, String complexity_alias)
            throws SQLException {
        String[] values = { parProblemID.toString(), par.toString(),
                "'" + problem + "'", "'" + complexity_alias + "'" };

        prepareBatchSQLINSERT("param_problem", values);
    }

    /**********************************************
     * 
     * Insert functions for algorithm/complexity related tables
     * 
     **********************************************/

    /**
     * Insert new algorithm. algorithmID primary key and can't be null. gcID or
     * paramID may be null (just use null as parameter there).
     * 
     * 
     * 
     * @param algorithmID
     *            primaryID
     * @param gcProblemID
     *            the graphclass-problem relation its defined on
     * @param complexity_alias
     *            name for its complexity
     * @param bounds
     * @param gcID
     * @param paramID
     * @param confidence
     *            Bool about the confidence
     * @throws SQLException
     */
    public void insertNewAlgorithm(Integer algorithmID, Integer gcProblemID,
            String complexity_alias, String bounds, Integer gcID,
            Integer paramID, Boolean confidence, Boolean computed)
            throws SQLException {

        String gcIDString = (gcID == null) ? "NULL" : gcID.toString();
        String paramIDString = (paramID == null) ? "NULL" : paramID.toString();
        String confidenceString = (confidence == null) ? "NULL"
                : (confidence ? "1" : "0");
        String nullCheckedbounds = (bounds == null) ? "NULL" : bounds;
        String computedString = computed ? "1" : "0";

        String[] values = { algorithmID.toString(), gcProblemID.toString(),
                "'" + complexity_alias + "'",
                getCharOfString(nullCheckedbounds), gcIDString, paramIDString,
                confidenceString, "1", // TODO autoapproved ?
                "NULL", "NULL", "'system'", getDateTime(), computedString };
        executeSingleSQLINSERT("algorithm", values);
    }

    /**
     * Insert new parameter algorithm. algorithmID primary key and can't be
     * null.
     * 
     * 
     * 
     * @param algorithmID
     *            primaryID
     * @param parProblemID
     *            the parameter-problem relation its defined on
     * @param complexity_alias
     *            name for its complexity
     * @param bounds
     * @param paramID
     * @param confidence
     *            Bool about the confidence
     * @throws SQLException
     * @author vector
     */
    public void insertNewParamAlgorithm(Integer algorithmID,
            Integer parProblemID, String complexity_alias, String bounds,
            Integer paramID, Boolean confidence, Boolean computed) {
        String paramIDString = (paramID == null) ? "NULL" : paramID.toString();
        String confidenceString = (confidence == null) ? "NULL"
                : (confidence ? "1" : "0");
        String nullCheckedbounds = (bounds == null) ? "NULL" : bounds;
        String computedString = computed ? "1" : "0";

        String[] values = { algorithmID.toString(), parProblemID.toString(),
                "'" + complexity_alias + "'",
                getCharOfString(nullCheckedbounds), paramIDString,
                confidenceString, "1", // TODO autoapproved ?
                "NULL", "NULL", "'system'", getDateTime(), computedString };
        executeSingleSQLINSERT("param_algorithm", values);
    }

    /**
     * Insert new line into table algorithm_note, if you want to leave any field
     * empty, call with "NULL" as String. noteID is primary key and can't be
     * null.
     * 
     * 
     * @param noteID
     * @param parentAlgo
     * @param title
     * @param text
     * @throws SQLException
     */
    public void insertNewAlgorithmNote(Integer noteID, Integer parentAlgo,
            String title, String text) throws SQLException {
        String[] values = { noteID.toString(), parentAlgo.toString(),
                getCharOfString(title), "'" + correctQuotations(text) + "'" };

        executeSingleSQLINSERT("algorithm_note", values);
    }

    /**
     * Insert new line into table param_algo_note, if you want to leave any
     * field empty, call with "NULL" as String. noteID is primary key and can't
     * be null.
     *
     *
     * @param noteID
     * @param parentAlgo
     * @param title
     * @param text
     * @throws SQLException
     * @author vector
     */
    public void insertNewParamAlgorithmNote(Integer noteID,
            Integer parentAlgo, String title, String text) throws SQLException {
        String[] values = { noteID.toString(), parentAlgo.toString(),
                getCharOfString(title), 
                "'" + correctQuotations(text) + "'" };

        executeSingleSQLINSERT("param_algo_note", values);
    }

    public void insertNewAlgorithmRef(Integer algorefID, Integer parentAlgo,
            String type, Integer refID) throws SQLException {
        String refIDString = (refID == null) ? "NULL" : refID.toString();

        String[] values = { algorefID.toString(), parentAlgo.toString(),
                getCharOfString(type), refIDString };

        if (refID != null) {
            insertNewReference(refID);
        }
        executeSingleSQLINSERT("algorithm_ref", values);
    }
    /**
     * Writes a new reference for a
     * parametrized algorithm into the database.
     * @param paralgorefID general ID. May not be null.
     * @param parentAlgo Corresponding parametrized algorithm.
     * @param type
     * @param refID Global reference ID.
     * @throws SQLException
     *          Thrown, if insertion was not possible or successful.
     * @author vector
     */
    public void insertNewParamAlgorithmRef(Integer paralgorefID, Integer parentAlgo,
            String type, Integer refID) throws SQLException {
        String refIDString = (refID == null) ? "NULL" : refID.toString();

        String[] values = { paralgorefID.toString(), parentAlgo.toString(),
                getCharOfString(type), refIDString };

        if (refID != null) {
            insertNewReference(refID);
        }
        executeSingleSQLINSERT("param_algo_ref", values);
    }

    public void insertNewGraphclass(Integer gcID, String type, String name,
            Boolean computed) throws SQLException {
        String computedString = computed ? "1" : "0";

        String[] values = { gcID.toString(), "'" + type + "'",
                getCharOfString(correctBackslashes(name)), "1", // TODO autoapproved ?
                "NULL", "NULL", "'system'", getDateTime(), computedString };

        executeSingleSQLINSERT("graphclass", values);

    }

    public void insertNewGraphclassNote(Integer noteID,
            Integer parentGraphclass, String title, String text)
            throws SQLException {
        String[] values = { noteID.toString(), parentGraphclass.toString(),
                getCharOfString(title), "'" + correctQuotations(correctBackslashes(text)) + "'" };

        executeSingleSQLINSERT("gc_note", values);
    }

    public void insertNewGraphclassProperty(Integer gcPropertyID,
            Integer parentGraphclass, Integer propertyID) throws SQLException {
        String[] values = { gcPropertyID.toString(),
                parentGraphclass.toString(), propertyID.toString() };

        executeSingleSQLINSERT("gc_property", values);
    }

    public void insertNewGraphclassGraphclassDerivation(
            Integer gcgcDerivationID, Integer parentGraphclassID, String type,
            Integer derivedID) throws SQLException {
        String[] values = { gcgcDerivationID.toString(),
                parentGraphclassID.toString(), "'" + type + "'",
                derivedID.toString() };

        executeSingleSQLINSERT("gc_gc_derivation", values);

    }

    public void insertNewGraphclassSmallgraphDerivation(
            Integer gcsgDerivationID, Integer graphclassID, String smallgraphID)
            throws SQLException {
        String[] values = { gcsgDerivationID.toString(),
                graphclassID.toString(), getCharOfString(correctBackslashes(smallgraphID)) };
        
        executeSingleSQLINSERT("gc_sg_derivation", values);
    }

    public void insertNewGraphclassRef(Integer gcRefID,
            Integer parentGraphclass, String type, Integer refID)
            throws SQLException {
        String refIDString = (refID == null) ? "NULL" : refID.toString();

        String[] values = { gcRefID.toString(), parentGraphclass.toString(),
                refIDString, getCharOfString(type) };

        if (refID != null) {
            insertNewReference(refID);
        }
        executeSingleSQLINSERT("gc_ref", values);
    }

    public void insertNewRelation(Integer relationID, Integer gcID1,
            String type, Integer gcID2, Boolean confidence,
            Boolean shownOnWebsite, Boolean computed) throws SQLException {
        String confidenceString = confidence ? "1" : "0";
        String shownOnWebsiteString = (shownOnWebsite == null) ? "NULL"
                : (shownOnWebsite ? "1" : "0");
        String computedString = computed ? "1" : "0";

        String[] values = { relationID.toString(), gcID1.toString(),
                "'" + type + "'", gcID2.toString(), confidenceString,
                shownOnWebsiteString, "1", // TODO autoapproved ?
                "NULL", "NULL", "'system'", getDateTime(), computedString };

        prepareBatchSQLINSERT("relation", values);

    }

    public void insertNewRelationRef(Integer relationRefID,
            Integer parentRelation, String type, Integer refID)
            throws SQLException {
        String refIDString = (refID == null) ? "NULL" : refID.toString();

        String[] values = { relationRefID.toString(),
                parentRelation.toString(), refIDString, getCharOfString(type) };

        if (refID != null) {
            insertNewReference(refID);
        }
        executeSingleSQLINSERT("relation_ref", values);
    }

    public void insertNewReference(Integer RefID) throws SQLException {

        executeSQLCommand("INSERT IGNORE INTO `reference` (`ref_id`) VALUES ("
                + RefID.toString() + ")");
    }

    public void insertNewProperty(String values) throws SQLException {

        String sql = m_insertIgnoreOrReplace + "into " + m_dbName + "property"
                + " values("; // Header of SQL call

        sql = sql + values;
        sql = sql + ")" + m_insertOption; // End SQL call

        Statement stmt = m_connection.createStatement();
        stmt.executeUpdate(sql);

    }

    public void insertNewComplexity(String values) throws SQLException {

        String sql = m_insertIgnoreOrReplace + "into " + m_dbName
                + "complexity" + " values("; // Header of SQL call

        sql = sql + values;
        sql = sql + ")" + m_insertOption; // End SQL call

        Statement stmt = m_connection.createStatement();
        stmt.executeUpdate(sql);

    }

    /**
     * @author vector
     */
    public void insertNewParamComplexity(String values) throws SQLException {

        String sql = m_insertIgnoreOrReplace + "into " + m_dbName
                + "param_complexity" + " values("; // Header of SQL call

        sql = sql + values;
        sql = sql + ")" + m_insertOption; // End SQL call

        Statement stmt = m_connection.createStatement();
        stmt.executeUpdate(sql);

    }

    /**
     * @author vector
     */
    public void insertNewBoundedness(String values) throws SQLException {

        String sql = m_insertIgnoreOrReplace + "into " + m_dbName
                + "boundedness" + " values("; // Header of SQL call

        sql = sql + values;
        sql = sql + ")" + m_insertOption; // End SQL call

        Statement stmt = m_connection.createStatement();
        stmt.executeUpdate(sql);

    }

    /**********************************************
     * 
     * Smallgraph functions
     * 
     **********************************************/

    /**
     * New smallgraphs. Img blob is auto Null for now, everything else is not
     * Null. This is referenced by forbidden, alias, hmt_family, smallmembers,
     * gc_sg_derivation and by itself(complement).
     * 
     * @param sgID
     * @param name
     * @param type
     * @param expl
     * @param definition
     * @param complementSgID
     * @throws SQLException
     */
    public void insertNewSmallgraph(String name, Integer sgID, String link,
            String type, Integer nodes, String definition)
            throws SQLException {

        String[] values = { getCharOfString(correctBackslashes(name)), sgID.toString(),
                getCharOfString(link),
                (nodes == null) ? "Null" : nodes.toString(), 
                getCharOfString(type),
                "NULL", // TODO IMG blob null right now
                "NULL",
                getCharOfString(definition), "1", // TODO
                                                                            // autoapproved
                                                                            // ?
                "NULL", "NULL", "'system'", getDateTime() };
        executeSingleSQLINSERT("smallgraph", values);
    }
    
    
    
    /**
         * Inserts new or updates already existing Smallgraph. This is needed because some
         * Graphs are written as Complement before they are defined themselves.
         * Img blob is auto Null for now, everything else is not
         * Null. This is referenced by forbidden, alias, hmt_family, smallmembers,
         * gc_sg_derivation and by itself(complement). 
         * 
         * @param sgID
         * @param name
         * @param type
         * @param expl
         * @param definition
         * @param complementSgID
         * @throws SQLException
         */
    public void insertUpdateNewSmallgraph(String name,
          Integer sgID, String link, String type, Integer nodes,
          String definition) 
          throws SQLException{
        
            String[] values = { getCharOfString(correctBackslashes(name)), sgID.toString(),
                    getCharOfString(link),
                    (nodes == null) ? "Null" : nodes.toString(), 
                    getCharOfString(type),
                    "NULL", // TODO IMG blob null right now
                    "NULL",
                    getCharOfString(definition), "1",                                                                                                               
                    "NULL", "NULL", "'system'", getDateTime() };
            
            executeSingleSmallGraphSQLINSERTUPDATE("smallgraph", values);
            
    }


    /**
     * New sgsgderivation (belongs to smallgraph). Everything not null for now.
     * 
     * @param forbiddenID
     * @param forbidderSgID
     * @param hostSgID
     * @param induced_rest
     * @throws SQLException
     */
    public void insertNewSgSgDerivation(Integer sgsgID, String baseSgID,
            String derivedSgID, String type) throws SQLException {

        String[] values = { sgsgID.toString(), getCharOfString(correctBackslashes(baseSgID)),
                getCharOfString(type),
                getCharOfString(correctBackslashes(derivedSgID)) };
        executeSingleSQLINSERT("sg_sg_derivation", values);
    }

    /**
     * New alias (belongs to smallgraphs), everything not null for now.
     * 
     * @param aliasID
     * @param sgID
     * @param alias
     * @throws SQLException
     */
    public void insertNewAlias(Integer aliasID, String sgName, String alias)
            throws SQLException {

        String[] values = { aliasID.toString(), getCharOfString(correctBackslashes(sgName)),
                getCharOfString(correctBackslashes(alias)) };
        executeSingleSQLINSERT("alias", values);
    }

    /**
     * New HMT Family. References HMT Grammar. Everything not Null for now.
     * 
     * @param hmtFamilyID
     * @param sgID
     * @param hmtGrammarID
     * @param index
     * @throws SQLException
     */
    public void insertNewHmtFamily(Integer hmtFamilyID, String sgID,
            String hmtGrammarID, String index) throws SQLException {

        String[] values = { hmtFamilyID.toString(), getCharOfString(correctBackslashes(sgID)), 
                hmtGrammarID == null ? "NULL" : 
                getCharOfString(correctBackslashes(hmtGrammarID)),
                index == null ? "NULL" : getCharOfString(index)};
        executeSingleSQLINSERT("hmt_family", values);
    }

    /**
     * New HMT Grammar. HMT Family references this so in case of truncating
     * delete that other one first. Everything not Null for now.
     * 
     * @param hmtGrammarID
     * @param name
     * @param hNodes
     * @param hEdges
     * @param hAttatchment
     * @param mNodes
     * @param mEdges
     * @param mExtension
     * @param mAttatchment
     * @param tNodes
     * @param tEdges
     * @param tExtension
     * @throws SQLException
     */
    public void insertNewHmtGrammar(Integer hmtGrammarID, String name,
            Integer type, Integer hNodes, String hEdges, String hAttatchment,
            Integer mNodes, String mEdges, String mExtension,
            String mAttatchment, Integer tNodes, String tEdges,
            String tExtension) throws SQLException {

        String[] values = { hmtGrammarID.toString(), getCharOfString(correctBackslashes(name)),
                String.valueOf(type), String.valueOf(hNodes),
                getCharOfString(hEdges), getCharOfString(hAttatchment),
                String.valueOf(mNodes), getCharOfString(mEdges),
                getCharOfString(mExtension), getCharOfString(mAttatchment),
                String.valueOf(tNodes), getCharOfString(tEdges),
                getCharOfString(tExtension) };
        executeSingleSQLINSERT("hmt_grammar", values);
    }

    /**********************************************
     *
     * Insert functions for GraphParameters (added by vector)
     *
     **********************************************/
    /**
     * Inserts a new Graph Parameter into the database.
     * @param parameterID Corresponding ID. Must be unique and not null.
     * @param name Corresponding name.
     * @param dir Corresponding directedness.
     * @param decompTime Corrresponding decomposition-time.
     * @param decompProblem Corresponding decomposition problem.
     * @param complement Corresponding complement.
     * @author vector
     */
    public void insertNewParameter(final Integer parameterID,
            final String name, final String dir,
            final Complexity decompTime,
            final Problem decompProblem,
            final GraphParameter complement) {

        String tmpDecompTime;
        String tmpDecompProblem;
        String tmpComplement;

        // null checks
        if (decompTime == null) tmpDecompTime = "NULL";
            else tmpDecompTime = decompTime.toString();
        if (decompProblem == null) tmpDecompProblem = "NULL";
            else tmpDecompProblem = decompProblem.getName();
        if (complement == null) tmpComplement = "NULL";
            else tmpComplement = complement.getID().toString();

        String[] values = { String.valueOf(parameterID),
                getCharOfString(name),
                getCharOfString(dir),
                getCharOfString(tmpDecompTime),
                // TODO decompProblem should not be needed here since it is
                // automatically linked through its name.
                getCharOfString(tmpDecompProblem), tmpComplement, "0", "NULL",
                "NULL", "'system'", getDateTime() };
        executeSingleSQLINSERT("parameter", values);
    }

    /**
     * Inserts a new parameter note into the database.
     * The function also replaces all "` and "' with their LaTeX-equivalent
     * to prevent errors from happening.
     * @param noteID Corresponding ID. Must be unique and not null.
     * @param parentParamater Corresponding parameter whose information is used.
     * @param title Title of the note.
     * @param text Content of the note.
     * @throws SQLException Thrown, if insertion was not possible.
     * @author vector
     */
    public void insertNewParameterNote(Integer noteID,
            Integer parentParamater, String title, String text)
            throws SQLException {
        String[] values = {noteID.toString(), parentParamater.toString(),
                getCharOfString(title), "'" + correctQuotations(text) + "'" };

        executeSingleSQLINSERT("param_note", values);
    }

    /**
     * Inserts a new parameter-relation into the database.
     * @param paramRelID Corresponding relation-ID. Must not be null.
     * @param p1 First parameter.
     * @param rel Type of the relation.
     * @param p2 Second parameter.
     * @param confidence Corresponding confidence-level.
     * @param functionType
     * @param shownOnWebsite Whether the relation is shown on the website.
     * @param computed
     * @throws SQLException Thrown, if the insertion was not successful.
     * @author vector
     */
    public void insertNewParamRelation(Integer paramRelID, Integer p1,
            String rel, Integer p2, Boolean confidence,
            String functionType, Boolean shownOnWebsite, Boolean computed)
            throws SQLException {
        String tmpString = "NULL";
        if (functionType != null)
            tmpString = functionType;

        String confidenceString = confidence ? "1" : "0";
        String shownOnWebsiteString = (shownOnWebsite == null) ? "NULL"
                : (shownOnWebsite ? "1" : "0");
        String computedString = computed ? "1" : "0";
        String[] values = { paramRelID.toString(), p1.toString(),
                getCharOfString(rel), p2.toString(), confidenceString,
                getCharOfString(tmpString), shownOnWebsiteString, "1", "NULL",
                "NULL", "'system'", getDateTime(), computedString };
        prepareBatchSQLINSERT("param_relation", values);
    }

    /**
     * Insert new gc parameter relation. gcParameterID is primary key and can't
     * be null.
     * 
     * @param gcParameterID
     * @param gc
     *            graphclass where the parameter is defined on
     * @param parameter
     *            the parameter that is defined on a graphclass
     * @param complexity_alias
     *            the complexity of this problem on that graphclass
     * @throws SQLException
     * @author vector
     */
    public void insertNewGCParamRelation(Integer gcParameterID, Integer gc,
            Integer parameter, String boundedness_alias) throws SQLException {
        String[] values = { gcParameterID.toString(), gc.toString(),
                parameter.toString(), "'" + boundedness_alias + "'" };

        prepareBatchSQLINSERT("gc_param", values);
    }

    /**
     * Insert new boundedness proof. proofID primary key and can't be null.
     *
     *
     *
     * @param proofID
     *            primaryID
     * @param gcParameterID
     *            the graphclass-parameter relation its defined on
     * @param boundedness_alias
     *            name for its complexity
     * @param gcID
     * @param confidence
     *            Bool about the confidence
     * @throws SQLException
     * @author vector
     */
    public void insertNewBoundednessProof(Integer proofID,
            Integer gcParameterID, String boundedness_alias, Integer gcID,
            Boolean confidence, Boolean computed) throws SQLException {

        String gcIDString = (gcID == null) ? "NULL" : gcID.toString();
        String confidenceString = (confidence == null) ? "NULL"
                : (confidence ? "1" : "0");
        String computedString = computed ? "1" : "0";

        String[] values = { proofID.toString(), gcParameterID.toString(),
                "'" + boundedness_alias + "'", gcIDString,
                confidenceString, "1", // TODO autoapproved ?
                "NULL", "NULL", "'system'", getDateTime(), computedString };
        executeSingleSQLINSERT("gc_parameter_boundedness", values);
    }

    /**
     * Inserts a new parameter-reference into the database.
     *
     * @param paramRefID Corresponding reference-ID.
     * @param parentParameter Corresponding Parameter
     * @param type Corresponding reference-type.
     * @param refID Global references-ID.
     * @throws SQLException Thrown, if Insertion was not successful.
     * @author vector
     */
    public void insertNewParameterRef(Integer paramRefID,
            Integer parentParameter, String type, Integer refID)
            throws SQLException {
        String refIDString = (refID == null) ? "NULL" : refID.toString();

        String[] values = {paramRefID.toString(), parentParameter.toString(),
                refIDString, getCharOfString(type)};

        if (refID != null) {
            insertNewReference(refID);
        }
        executeSingleSQLINSERT("param_ref", values);
    }

    /**
     * Insert new line into table algorithm_note, if you want to leave any
     * field empty, call with "NULL" as String. noteID is primary key and can't
     * be null.
     * 
     * 
     * @param noteID
     * @param parentProof
     * @param title
     * @param text
     * @throws SQLException
     * @author vector
     */
    public void insertNewBoundednessProofNote(Integer noteID,
            Integer parentProof, String title, String text)
            throws SQLException {
        String[] values = { noteID.toString(), parentProof.toString(),
                getCharOfString(title), "'" + correctQuotations(text) + "'" };

        executeSingleSQLINSERT("gc_param_note", values);
    }
    /**
     * Insert a new boundedness proof
     * ({@link BoundednessProof}) into the databse.
     *
     * @param parProofRefID
     *          Corresponding database-ID. Must be unique and not null.
     * @param parentProof Corresponding BoundednessProof.
     * @param type Corresponding reference-type.
     * @param refID Global references-ID.
     * @throws SQLException Thrown, if insertion was unsuccessful.
     * @author vector
     */
    public void insertNewBoundednessProofRef(Integer parProofRefID,
            Integer parentProof, String type, Integer refID)
            throws SQLException {
        String refIDString = (refID == null) ? "NULL" : refID.toString();

        String[] values = { parProofRefID.toString(), parentProof.toString(),
                refIDString, getCharOfString(type)};

        if (refID != null) {
            insertNewReference(refID);
        }
        executeSingleSQLINSERT("gc_param_ref", values);
    }

    /**
     * Insert a new reference for a relation between parameters.
     *
     * @param parRelationRefID Corresponding ID. Must be unique and not null.
     * @param parentRelation Corresponding relation.
     * @param type Corresponding reference-type.
     * @param refID Global references-ID.
     * @throws SQLException Thrown if insertion was unsuccessful.
     * @author vector
     */
    public void insertNewParamRelationRef(Integer parRelationRefID,
            Integer parentRelation, String type, Integer refID)
            throws SQLException {
        String refIDString = (refID == null) ? "NULL" : refID.toString();

        String[] values = { parRelationRefID.toString(),
                parentRelation.toString(), refIDString, getCharOfString(type) };

        if (refID != null) {
            insertNewReference(refID);
        }
        executeSingleSQLINSERT("param_relation_ref", values);
    }

    /**
     * @author vector
     */
    public void insertNewParamRelationNote(Integer parRelationNoteID,
            Integer parentRelation, String title, String text)
            throws SQLException {
        String[] values = { parRelationNoteID.toString(),
                parentRelation.toString(), getCharOfString(title),
                "'" + correctQuotations(text) + "'" };

        executeSingleSQLINSERT("param_relation_note", values);
    }


    /**********************************************
     * 
     * Helper functions
     * 
     **********************************************/

    /**
     * Convenience function for Notes and generally Text with Quotation Marks.
     * Adds backslashes in front of quotation marks to prevent mySQL from
     * misunderstanding those backslashes as end of value.
     * 
     * @param text
     *            String to be corrected
     * @return String corrected text
     */
    public String correctQuotations(String text) {
        text = text.replace("\'", "\\'");
        text = text.replace("\"", "\\\"");
        return text;
    }
    
    /**
     * Convenience function for Smallgraphs to keep backslashes of Smallgraphnames.
     * @param text
     * String to be corrected
     * @return String corrected text
     */
    
    public String correctBackslashes(String text) {
        text = text.replace("\\", "\\\\");
        return text;
    }

    /**
     * Checks if the String s says Null, if yes, return s, if not then return
     * "'" + s + "'" which makes it a char in mysql.
     * 
     * @param s
     * @return
     */
    public String getCharOfString(String s) {
        if (s.equalsIgnoreCase("Null")) {
            return s;
        } else {
            return "'" + s + "'";
        }
    }

    /**
     * Get the current Connection object
     * 
     * @return
     */
    public Connection getConnection() {
        return m_connection;
    }

    /**
     * Execute all current pooled inserts for 1 table
     */
    public void executeSQLInsert() {

        if (m_pooledCounter == 0) {
            return; // Do nothing in case theres nothing pooled
        }
        String sql = m_insertIgnoreOrReplace + "into " + m_dbName
                + m_pooledTable + " values "; // Header of SQL call
        String values = "";
        for (String string : m_pooledValues) { // insert all pooled values into
                                                // SQL call
            if (string != null) {
                values = values + "(" + string + ")" + ", ";
            }

        }

        values = values.substring(0, values.length() - 2); // cut last "," out

        sql = sql + values + m_insertOption; // End SQL call
        m_pooledValues = new String[1000];
        m_pooledTable = "";
        m_pooledCounter = 0;

        try {

            Statement stmt = m_connection.createStatement();
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println(sql);
            e.printStackTrace();
        }
    }

    /**
     * All another value String to the pool. Don't mix tables into the same pool
     * ! If you want to pool the inserts for a new table make sure the pool is
     * empty by calling {@link #executeSQLInsert() executeSQLInsert()}
     * 
     * @param table
     * @param values
     * @throws SQLException
     */
    public void prepareBatchSQLINSERT(String table, String[] values)
            throws SQLException {
        String sql = "";
        for (String string : values) { // insert all values into SQL call
            sql = sql + string + ", ";
        }
        sql = sql.substring(0, sql.length() - 2); // cut last ", " out
        m_pooledValues[m_pooledCounter] = sql;
        m_pooledTable = table;
        m_pooledCounter++;
        if (m_pooledCounter == 1000) {
            executeSQLInsert();
        }
    }

    /**
     * Executes a single INSERT sql statement into the given table name with the
     * given values
     * 
     * @param table
     *            which table to write to.
     * @param values
     *            String array of all columns. Remember to write chars with '
     *            for example: String parameter = "'i am a char'";
     * @throws SQLException
     */
    public void executeSingleSQLINSERT(String table, String[] values) {
        String sql = m_insertIgnoreOrReplace + "into " + m_dbName + table
                + " values("; // Header of SQL call

        try {

            for (String string : values) { // insert all values into SQL call
                sql = sql + string + ", ";
            }

            sql = sql.substring(0, sql.length() - 2); // cut last ", " out
            sql = sql + ")" + m_insertOption; // End SQL call

            Statement stmt = m_connection.createStatement();
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println(sql);
            e.printStackTrace();
        }
    }
    
    
           /**
         * Executes a single INSERT OR UPDATE sql statement into the given table name with the
         * given values
         * 
         * @param table
         *            which table to write to.
         * @param values
         *            String array of all columns. Remember to write chars with '
         *            for example: String parameter = "'i am a char'";
         * @throws SQLException
         */
        public void executeSingleSmallGraphSQLINSERTUPDATE(String table, String[] values) {
                String sql = "INSERT INTO " + m_dbName + table
                                + " (sg_name, sg_id, link, nodes, type, image,"
                                + " expl, definition, approved, approved_at, "
                                + "approved_by, entered_by, entered_at) "
                                + "VALUES("; // Header of SQL call

                try {

                        for (String string : values) { // insert all values into SQL call
                                sql = sql + string + ", ";
                        }

                        sql = sql.substring(0, sql.length() - 2); // cut last ", " out
                        sql = sql + ")" + m_insertOption; // End SQL call
                        sql = sql + " ON DUPLICATE KEY UPDATE sg_name=VALUES(sg_name), "
                                + "sg_id=VALUES(sg_id), link=VALUES(link), nodes=VALUES(nodes),"
                                + " type=VALUES(type), image=VALUES(image), expl=VALUES(expl),"
                                + " definition=VALUES(definition)";
                                
                        Statement stmt = m_connection.createStatement();
                        stmt.executeUpdate(sql);
                } catch (Exception e) {
                        System.out.println(sql);
                        e.printStackTrace();
                }
        }

    /**
     * Convenience function to directly execute a custom sql command
     * 
     * @param sql
     * @throws SQLException
     */
    public void executeSQLCommand(String sql) throws SQLException {
        Statement stmt = m_connection.createStatement();
        stmt.executeUpdate(sql);
    }

    /**
     * Uses built in mysql function for datetime
     * 
     * @return current date and time
     */
    public static String getDateTime() {
        return "NOW()";
    }

    /**
     * Truncates table which means if there are no foreign references to it,
     * delete table and recreate it (very fast). If there are foreign references
     * to it, there will be an sqlexception. If you are sure there wont be
     * problems with the integrity, you can execute SET FOREIGN_KEY_CHECKS=0
     * before and SET FOREIGN_KEY_CHECKS=1 after emptyTable(tableName)
     * 
     * @param args
     * @throws SQLException
     */
    public void emptyTable(String tableName) throws SQLException {
        String sql = "Truncate table " + tableName;

        Statement stmt = m_connection.createStatement();
        stmt.executeUpdate(sql);
    }


}
