package teo.isgci.xml;

import teo.isgci.smallgraph.Configuration;
import teo.isgci.smallgraph.Family;
import teo.isgci.smallgraph.Graph;
import teo.isgci.smallgraph.HMTFamily;
import teo.isgci.smallgraph.HMTGrammar;
import teo.isgci.smallgraph.SimpleFamily;
import teo.isgci.smallgraph.SmallGraph;
import teo.isgci.smallgraph.UnionFamily;
import teo.isgci.smallgraph.HMTGrammar.HMTGraph;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class SmallGraphSQLExporter {

    private SQLWriter m_sql;

    private Integer m_sgID;
    private Integer m_aliasID;
    private Integer m_hmtGramID;
    private Integer m_sgsgID;
    private Integer m_hmtFamID;

    final static int EDGE = 1;
    final static int NONEDGE = -1;
    final static int OPTEDGE = 0;

    /**
     * Creates a Connection to a Database and fills this Database with
     * SmallGraphData.
     * 
     * @param sql
     */
    public SmallGraphSQLExporter(SQLWriter sql) {
        m_sql = sql;
        m_sgID = 0;
        m_aliasID = 0;
        m_hmtGramID = 0;
        m_sgsgID = 0;
        m_hmtFamID = 0;

        try {
            m_sql.executeSQLCommand("SET FOREIGN_KEY_CHECKS=0;");
            // Reset all tables that use our generated IDs
            m_sql.emptyTable("smallgraph");
            m_sql.emptyTable("sg_sg_derivation");
            m_sql.emptyTable("gc_sg_derivation");
            m_sql.emptyTable("alias");
            m_sql.emptyTable("hmt_grammar");
            m_sql.emptyTable("hmt_family");

            m_sql.executeSQLCommand("SET FOREIGN_KEY_CHECKS=1;");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * writes smallgraph related data to a given database.
     * 
     * @param smallGraphs
     * @param gram
     * @param famil
     * @param config
     * @param incls
     */
    public void writeSmallGraphs(Vector smallGraphs, Vector gram,
            Vector famil, Vector config,
            DirectedGraph<Graph, DefaultEdge> incls) {

        Calendar cal = Calendar.getInstance();
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL,
                DateFormat.MEDIUM);

        System.out.println("Time when started filling the DB "
                + "with SmallGraph DATA: " + df.format(cal.getTime()));

        System.out.println("Fill SmallGraphs");
        writeGraphs(smallGraphs);
        System.out.println("Fill HMT-Grammar");
        writeGrammars(gram);
        System.out.println("Fill Families");
        writeFamilies(famil);
        System.out.println("Fill configs");
        writeConfigurations(config);
        System.out.println("Fill inclusions");
        if (incls != null)
            writeEdges(incls);

        cal = Calendar.getInstance();

        System.out.println("Time when stopped filling "
                + "DB with SmallGraph Data: " + df.format(cal.getTime()));
    }

    /**
     * Writes the Edges to the Database
     * 
     * @param inclusions
     *            to write to the Database
     */
    protected void writeEdges(DirectedGraph<Graph, DefaultEdge> incls) {

        for (DefaultEdge e : incls.edgeSet()) {
            try {
                m_sql.insertNewSgSgDerivation(m_sgsgID, incls.getEdgeSource(e)
                        .getName(), incls.getEdgeTarget(e).getName(),
                        "inclusion");
                m_sgsgID++;
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Writes Configurations to the database
     * 
     * @param configs
     *            Configurations to write
     */
    private void writeConfigurations(Collection<Configuration> configs) {
        String name = null;
        Integer nodes;
        String edges = null;
        String link = null;

        for (Configuration c : configs) {
            if (!c.isPrimary())
                continue;

            name = c.getName();
            link = c.getLink() == null ? "null" : c.getLink();
            nodes = c.countNodes();

            if (c.getName().startsWith("XC"))
                edges = writeConfEdges(c, SmallGraphTags.NONEDGES, NONEDGE);
            else if (c.getName().startsWith("XZ"))
                edges = writeConfEdges(c, SmallGraphTags.OPTEDGES, OPTEDGE);
            else {
                edges = writeConfEdges(c, SmallGraphTags.OPTEDGES, OPTEDGE);
                System.out.println("Strange name of configuration: "
                        + c.getName());
            }
            try {
                m_sql.insertUpdateNewSmallgraph(name, m_sgID, link, "configuration",
                        nodes, edges); 
                m_sgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }

            writeContains(c.getContains(), name);
            writeInduced(c.getInduced(), name);
            writeConfigurationComplement(c, name);
            writeAliases(c.getNames(), name);
        }
    }

    /**
     * Writes the Complement of a Configuration to the Database
     * 
     * @param c
     *            Configuration
     * @param name
     *            Name of the Configuration
     */
    protected void writeConfigurationComplement(Configuration c, String name) {

        if (c.getComplement() == null)
            return;

        Configuration co = (Configuration) c.getComplement();

        String coName = co.getName();

        if (co.getLink() == null || co.getLink().equals(c.getLink())) {

            try {
                m_sql.insertNewSmallgraph(coName, m_sgID, "null",
                        "ConfComplement", null, "null");
                m_sgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } else {
            try {
                m_sql.insertNewSmallgraph(coName, m_sgID,
                        co.getLink() == null ? "null" : co.getLink(),
                        "ConfComplement", null, "null");
                m_sgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                m_sql.insertNewSgSgDerivation(m_sgsgID, name, coName,
                        "complement");
                m_sgsgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Writes the Edges of a Configuration as String
     * 
     * @param conf
     * @param tag
     * @param edgeType
     * @return
     */
    protected String writeConfEdges(Configuration conf, String tag,
            int edgeType) {
        String edge = new String(), otherEdge = new String();
        int i, j;

        edge += ("<" + SmallGraphTags.EDGES + ">\n");
        otherEdge += tag + "\n";
        for (i = 0; i < conf.countNodes(); i++)
            for (j = i + 1; j < conf.countNodes(); j++)
                if (conf.getEdge(i, j) == EDGE)
                    edge += ("      " + i + " - " + j + ";\n");
                else if (conf.getEdge(i, j) == edgeType)
                    otherEdge += ("      " + i + " - " + j + ";\n");
        edge += ("</" + SmallGraphTags.EDGES + ">\n");

        otherEdge += "</" + tag + ">\n";

        String concat = edge.concat(otherEdge);

        return concat;
    }

    /**
     * Writes SmallGraph Families to the database
     * 
     * @param families
     *            Families to write
     */
    private void writeFamilies(Collection<Family> families) {
        String type = null;
        String name = null;
        String grammar = null;
        String index = null;
        String definition = "";
        Vector<SmallGraph> smallmembers = null;
        boolean writehmtfamily = false;

        for (Family f : families) {
            definition = "null";
            type = null;
            grammar = null;
            index = null;
            writehmtfamily = false;
            smallmembers = null;
            if (!f.isPrimary())
                continue;

            if (f instanceof SimpleFamily)
                type = "simple";
            else if (f instanceof UnionFamily)
                type = "union";
            else if (f instanceof HMTFamily)
                type = "hmt";
            name = f.getName();
            String link = f.getLink() == null ? "null" : f.getLink();

            if (f instanceof HMTFamily) {
                HMTFamily fhmt = (HMTFamily) f;
                if (fhmt.getGrammar() != null) {
                    writehmtfamily = true;
                    if (fhmt.getGrammar().getName() == null) {
                        definition = definition.concat(writeGrammar(
                                fhmt.getGrammar(), "   ", false));
                    } else {
                        grammar = fhmt.getGrammar().getName();
                        index = null;
                        if (fhmt.getIndex() == null)
                            System.err.println("HMTFamily " + fhmt.getName()
                                    + " without index");
                        else
                            index = fhmt.getIndex();

                        definition = definition.concat("<"
                                + SmallGraphTags.USEGRAMMAR + " "
                                + SmallGraphTags.NAME + "=\"" + grammar + "\""
                                + "/>\n");
                    }

                }
            }
            try {
                m_sql.insertUpdateNewSmallgraph(name, m_sgID, link, type, null,
                        definition); 
                m_sgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (writehmtfamily)
                writeHmtFamily(grammar, name, index);

            if (f instanceof SimpleFamily) {
                SimpleFamily fs = (SimpleFamily) f;
                writeContains(fs.getContains(), name);
                writeInducedRest(fs.getInducedRest(), name);
            } else if (f instanceof HMTFamily) {
                HMTFamily fhmt = (HMTFamily) f;
                if (fhmt.getSmallmembers() != null) {
                    smallmembers = fhmt.getSmallmembers();
                    writeSmallmembers(smallmembers, name);
                }
            } else if (f instanceof UnionFamily) {
                UnionFamily fu = (UnionFamily) f;
                writeSubfamilies(fu.getSubfamilies(), name);
            }
            writeInduced(f.getInduced(), name);

            writeAliases(f.getNames(), name);
            writeFamilyComplement(f);
        }

    }

    /**
     * Writes Hmt Familys to the database
     * 
     * @param grammar
     * @param name
     * @param index
     */
    private void writeHmtFamily(String grammar, String name, String index) {
        try {
            m_sql.insertNewHmtFamily(m_hmtFamID, name, grammar, index); 
            m_hmtFamID++;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the Complement of a Family to the Database
     * 
     * @param f
     */
    protected void writeFamilyComplement(Family f) {
        if (f.getComplement() == null)
            return;

        Family co = (Family) f.getComplement();

        String coname = co.getName();

        if (co.getLink() == null || co.getLink().equals(f.getLink())) {
            try {
                m_sql.insertNewSmallgraph(coname, m_sgID, "null",
                        "familyComplement", null, "null");
                m_sgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } else {
            try {
                m_sql.insertNewSmallgraph(coname, m_sgID,
                        co.getLink() == null ? "null" : co.getLink(),
                        "familyComplement", null, "null");
                m_sgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        try {
            m_sql.insertNewSgSgDerivation(m_sgsgID, f.getName(), coname,
                    "familyComplement");
            m_sgsgID++;
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Writes the induced SmallGraphs to the database
     * 
     * @param list
     * @param fname
     */
    protected void writeInduced(Vector<Vector<SmallGraph>> list, String fname) {
        if (list == null)
            return;

        for (Vector<SmallGraph> vecInd : list) {
            if (vecInd.size() == 1) {
                try {
                    m_sql.insertNewSmallgraph(vecInd.firstElement().getName(),
                            m_sgID, "null", "induced", null, "null");
                    m_sgID++;
                    m_sql.insertNewSgSgDerivation(m_sgsgID, fname, vecInd
                            .firstElement().getName(), SmallGraphTags.INDUCED1);
                    m_sgsgID++;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                for (SmallGraph g : vecInd)
                    try {
                        m_sql.insertNewSmallgraph(g.getName(), m_sgID, "null",
                                "induced", null, "null");
                        m_sgID++;
                        m_sql.insertNewSgSgDerivation(m_sgsgID, fname,
                                g.getName(), SmallGraphTags.INDUCED);
                        m_sgsgID++;
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
            }
        }
    }

    /**
     * Writes Subfamilies to database
     * 
     * @param list
     * @param fname
     */
    protected void writeSubfamilies(Collection<SmallGraph> list, String fname) {
        if (list == null)
            return;

        for (SmallGraph g : list) {
            try {
                m_sql.insertNewSmallgraph(g.getName(), m_sgID, "null",
                        "subfamily", null, "null");
                m_sgID++;
                m_sql.insertNewSgSgDerivation(m_sgsgID, fname, g.getName(),
                        SmallGraphTags.SUBFAMILY);
                m_sgsgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * writes inducedRest to Database
     * 
     * @param list
     * @param fname
     */
    protected void writeInducedRest(Vector<Vector<SmallGraph>> list,
            String fname) {
        if (list == null)
            return;

        for (Vector<SmallGraph> vecInd : list) {

            if (vecInd.size() == 1) {
                try {
                    m_sql.insertNewSmallgraph(vecInd.firstElement().getName(),
                            m_sgID, "null", "inducedRest", null, "null");
                    m_sgID++;
                    m_sql.insertNewSgSgDerivation(m_sgsgID, fname, vecInd
                            .firstElement().getName(),
                            SmallGraphTags.INDUCEDREST1);
                    m_sgsgID++;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                for (SmallGraph g : vecInd)
                    try {
                        m_sql.insertNewSmallgraph(g.getName(), m_sgID, "null",
                                "inducedRest", null, "null");
                        m_sgID++;
                        m_sql.insertNewSgSgDerivation(m_sgsgID, fname,
                                g.getName(), SmallGraphTags.INDUCEDREST);
                        m_sgsgID++;
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
            }
        }
    }

    /**
     * writes Contains to the database
     * 
     * @param list
     * @param cname
     */
    protected void writeContains(Collection<SmallGraph> list, String cname) {
        if (list == null)
            return;

        for (SmallGraph g : list) {
            try {
                m_sql.insertNewSmallgraph(g.getName(), m_sgID, "null",
                        "contains", null, "null");
                m_sgID++;
                m_sql.insertNewSgSgDerivation(m_sgsgID, cname, g.getName(),
                        "contains");
                m_sgsgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Writes Smallmembers to database
     * 
     * @param small
     * @param familyName
     */
    protected void writeSmallmembers(Collection<SmallGraph> small,
            String familyName) {
        for (SmallGraph g : small) {
            try {
                m_sql.insertNewSmallgraph(g.getName(), m_sgID, "null",
                        "smallmember", null, "null");
                m_sgID++;
                m_sql.insertNewSgSgDerivation(m_sgsgID, familyName,
                        g.getName(), "smallmember");
                m_sgsgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Convenience Method for parsing through all grammars in the Collection
     * 
     * @param grammars
     */
    private void writeGrammars(Collection<HMTGrammar> grammars) {
        for (HMTGrammar hmtg : grammars) {
            writeGrammar(hmtg, "", true);
        }
    }

    /**
     * Writes either HMTGrammar to database or as String for definition
     * 
     * @param gram
     * @param indent
     * @param hmtwrite
     * @return String HMTGrammar
     */
    private String writeGrammar(HMTGrammar gram, String indent,
            boolean hmtwrite) {
        int type = gram.getType();

        String name = gram.getName();

        int[] nodes = new int[3];
        String[] edges = new String[3];
        String[] extensions = new String[2];
        String[] attachments = new String[2];

        nodes[0] = gram.getHead().countNodes();
        edges[0] = writeEdges(gram.getHead(), "   " + indent);
        attachments[0] = writeAttachment(gram.getHead(), indent);

        nodes[1] = gram.getMid().countNodes();
        edges[1] = writeEdges(gram.getMid(), "   " + indent);
        extensions[0] = writeExtension(gram.getMid(), indent);
        attachments[1] = writeAttachment(gram.getMid(), indent);

        nodes[2] = gram.getTail().countNodes();
        edges[2] = writeEdges(gram.getTail(), "   " + indent);
        extensions[1] = writeExtension(gram.getTail(), indent);

        if (hmtwrite) {
            try {
                m_sql.insertNewHmtGrammar(m_hmtGramID, name, type, nodes[0],
                        edges[0], attachments[0], nodes[1], edges[1],
                        extensions[0], attachments[1], nodes[2], edges[2],
                        extensions[1]);
                m_hmtGramID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        String writer = createHMTGrammarStyle(type, name, nodes, edges,
                extensions, attachments);

        return writer;

    }

    /**
     * Method for Creating HMT Grammar like in the smallgraphs.xml.
     * 
     * @param type
     * @param name
     * @param nodes
     * @param edges
     * @param extensions
     * @param attachments
     * @return String Hmt-Grammar
     */
    private String createHMTGrammarStyle(Integer type, String name,
            int[] nodes, String[] edges, String[] extensions,
            String[] attachments) {

        String grammarString = "";
        grammarString += "<" + SmallGraphTags.HMTGRAMMAR + " "
                + SmallGraphTags.TYPE + "=" + type.toString();
        if (name != null) {
            grammarString += " " + SmallGraphTags.NAME + "=" + name;
        }

        grammarString += ">\n";
        grammarString += "<" + SmallGraphTags.HEAD + ">\n";
        grammarString += "<" + SmallGraphTags.NODES + " "
                + SmallGraphTags.COUNT + "=\"" + nodes[0] + "\"/> \n";
        grammarString += "<" + SmallGraphTags.EDGES + ">" + edges[0] + "<"
                + SmallGraphTags.EDGES + "/>\n";
        grammarString += "<" + SmallGraphTags.ATTACHMENT + ">"
                + attachments[0] + "<" + SmallGraphTags.ATTACHMENT + "/>\n";
        grammarString += "<" + SmallGraphTags.HEAD + "/>\n";

        grammarString += "<" + SmallGraphTags.MID + ">\n";
        grammarString += "<" + SmallGraphTags.NODES + " "
                + SmallGraphTags.COUNT + "=\"" + nodes[1] + "\"/> \n";
        grammarString += "<" + SmallGraphTags.EDGES + ">" + edges[1] + "<"
                + SmallGraphTags.EDGES + "/>\n";
        grammarString += "<" + SmallGraphTags.EXTENSION + ">" + extensions[0]
                + "<" + SmallGraphTags.EXTENSION + "/>\n";
        grammarString += "<" + SmallGraphTags.ATTACHMENT + ">"
                + attachments[0] + "<" + SmallGraphTags.ATTACHMENT + "/>\n";
        grammarString += "<" + SmallGraphTags.MID + "/>\n";

        grammarString += "<" + SmallGraphTags.TAIL + ">\n";
        grammarString += "<" + SmallGraphTags.NODES + " "
                + SmallGraphTags.COUNT + "=\"" + nodes[2] + "\"/> \n";
        grammarString += "<" + SmallGraphTags.EDGES + ">" + edges[2] + "<"
                + SmallGraphTags.EDGES + "/>\n";
        grammarString += "<" + SmallGraphTags.EXTENSION + ">" + extensions[1]
                + "<" + SmallGraphTags.EXTENSION + "/>\n";
        grammarString += "<" + SmallGraphTags.TAIL + "/>\n";

        grammarString += "<" + SmallGraphTags.HMTGRAMMAR + "/>\n";

        return grammarString;
    }

    /**
     * Creates an extension String for Grammars
     * 
     * @param hmtgr
     * @param indent
     * @return String extension
     */
    protected String writeExtension(HMTGraph hmtgr, String indent) {
        String ext = new String();
        for (int i = 0; i < hmtgr.getExt().length; i++)
            ext += (hmtgr.getExt()[i] + ",");
        return ext;
    }

    /**
     * Writes an Attachment for the grammarfunction
     * 
     * @param hmtgr
     * @param indent
     * @return String Attachment
     */
    protected String writeAttachment(HMTGraph hmtgr, String indent) {
        String att = new String();
        for (int i = 0; i < hmtgr.getAtt().length; i++)
            att += (hmtgr.getAtt()[i] + ",");
        return att;
    }

    /**
     * writes all defined SmallGraphs to the Database
     * 
     * @param smallGraphs
     */
    public void writeGraphs(Collection<Graph> graphs) {

        for (Graph g : graphs) {
            if (!g.isPrimary())
                continue;

            if (g.getName().startsWith("USG"))
                continue;

            String smallgraphName = g.getName();
            String SmallGraphType = "SimpleGraph";

            int nodes = g.countNodes();

            String definition = writeEdges(g, "");

            String link = g.getLink() == null ? "null" : g.getLink();

                try {
                    m_sql.insertUpdateNewSmallgraph(smallgraphName, m_sgID, link,
                            SmallGraphType, nodes, definition);

                m_sgID++;

                writeAliases(g, smallgraphName);

                writeGraphComplement(g, smallgraphName);
                } catch (SQLException e) {
                    e.printStackTrace();
                } 
        }

    }

    /**
     * Writes Aliases of currently selected Configuration or Family to Database
     * 
     * @param aliases
     *            Alias
     * @param sgName
     *            BaseName
     */
    private void writeAliases(List<String> aliases, String sgName) {

        for (int i = 1; i < aliases.size(); i++) {
            try {
                m_sql.insertNewAlias(m_aliasID, sgName, aliases.get(i));
                m_aliasID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Writes Aliases of currently selected SmallGraph to Database
     * 
     * @param aliases
     *            Alias
     * @param sgName
     *            BaseName
     */
    private void writeAliases(Graph g, String sgName) {

        List<String> aliasnames = g.getNames();
        for (int i = 1; i < aliasnames.size(); i++) {
            try {
                m_sql.insertNewAlias(m_aliasID, sgName, aliasnames.get(i));
                m_aliasID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * writes complement of a given SmallGraph
     * 
     * @param g
     *            BaseGraph
     * @param gname
     *            BaseName
     */
    protected void writeGraphComplement(Graph g, String gname) {
        if (g.getComplement() == null)
            return;

        Graph co = (Graph) g.getComplement();
        List<String> names = co.getNames();
        String complname = null;

        if ((co.getLink() == null || co.getLink().equals(g.getLink()))
                && (names.size() == 1 || co.getName() == g.getName())) {
            complname = co.getName();
            try {
                m_sql.insertNewSmallgraph(complname, m_sgID, "null",
                        "complement", null, "null");
                m_sgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String link = "null";
            complname = co.getName();
            if (co.getLink() != null)
                link = (co.getLink());

            try {
                m_sql.insertNewSmallgraph(complname, m_sgID, link,
                        "complement", null, "null");
                m_sgID++;
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (names.size() > 1) {
                writeAliases(co, complname);
            }

        }
        try {
            m_sql.insertNewSgSgDerivation(m_sgsgID, gname, complname,
                    "complement");
            m_sgsgID++;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates String Edges for Graphs and Grammar
     * 
     * @param gr
     * @param indent
     * @return String Edges
     */
    private String writeEdges(Graph gr, String indent) {
        String edge = "<" + SmallGraphTags.EDGES + ">";
        for (int i = 0; i < gr.countNodes(); i++)
            for (int j = i + 1; j < gr.countNodes(); j++)
                if (gr.getEdge(i, j))
                    edge += (indent + "      " + i + " - " + j + ";\n");
        edge += "</" + SmallGraphTags.EDGES + ">";
        return edge;
    }

}
