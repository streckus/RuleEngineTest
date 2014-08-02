package teo.isgci.xml;

import java.sql.SQLException;


public class PropertyFields {
	public static Integer propID = 0;
	
	public static final String UNKNOWN   = getNextID() + ", '" + Tags.HERED + "', 'Unknown', ''" ;

	public static final String ISO       = getNextID() + ", '" + Tags.HERED + "', '" + Tags.ISOHERED + "', 'Property holds for every isometric connected induced subgraph'" ;
	
	public static final String CON       = getNextID() + ", '" + Tags.HERED + "', '" + Tags.CONHERED + "', 'Property holds for every connected induced subgraph'" ;
	
	public static final String IND       = getNextID() + ", '" + Tags.HERED + "', '" + Tags.INDHERED + "', 'Property holds for every incuded subgraph'" ;
	
	public static final String STRICT    = getNextID() + ", '" + Tags.HERED + "', 'STRICTEST', 'For having a max'" ;

	public static final String DIRTRUE   = getNextID() + ", '" + Tags.DIRTYPE + "', '" + Tags.DIRECTED + "', 'This is a directed class'" ;

	public static final String DIRFALSE  = getNextID() + ", '" + Tags.DIRTYPE + "', '" + Tags.UNDIRECTED + "', 'This is an undirected class (default)'" ;
	
	public static final String SELFTRUE  = getNextID() + ", 'self-complementary', 'TRUE', 'This class is marked as self complementary'" ;

	public static final String SELFFALSE = getNextID() + ", 'self-complementary', 'FALSE', 'This class is not marked as self complementary'" ;

	public static final String CLIQTRUE  = getNextID() + ", '" + Tags.CLIQUEFIXED + "', 'TRUE', 'This class is marked as clique fixed (K(C)=C)'" ;

	public static final String CLIQFALSE = getNextID() + ", '" + Tags.CLIQUEFIXED + "', 'FALSE', 'This class is not marked as clique fixed'" ;
	
	/*
	 * Want to add a new Property ? Add a new public static final String. Format: getNextID() + ", 'PROPERTYNAME', 'PROPERTYVALUE', 'PROPERTYEXPLENATION'"
	 * You replace  PROPERTYNAME, PROPERTYVALUE and PROPERTYEXPLENATION with your String.
	 * 
	 * It automatically gets a new ID, which you can get by calling the static method getID(name-of-the-static-field)
	 *
	 */

	
	private static String getNextID()
	{
		Integer p = propID;
		propID++;
		return p.toString();
		
	}
	
	public static int getID(String s)
	{
		String[] array = s.split(",");
		return Integer.parseInt(array[0]);
	}
	
}
