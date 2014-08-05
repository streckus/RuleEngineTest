package teo.isgci.xml;

public class ComplexityFields {
	public static Integer compID = 0;

	public static final String OPEN     = getNextID() + ", 'Open', 'Open', NULL" ;
	
	public static final String UNKNOWN  = getNextID() + ", 'UNKNOWN', '?', NULL" ;
	
	public static final String LINEAR   = getNextID() + ", 'Linear', 'Lin', 1" ;

	public static final String POLY     = getNextID() + ", 'Polynomial', 'P', 2" ;
	
	public static final String GICOMP   = getNextID() + ", 'GI-complete', 'GIC', 3" ;	
	
	public static final String NPCOMP   = getNextID() + ", 'NP-complete', 'NPC', 4" ;	
	
	public static final String NPHARD   = getNextID() + ", 'NP-hard', 'NPH', 4" ;
	
	public static final String CONPCOMP = getNextID() + ", 'coNP-complete', 'coNPC', 4" ;
	
	public static final String LINEARS   = getNextID() + ", 'LiN', 'Lin', 1" ;

	public static final String POLYS     = getNextID() + ", 'P', 'P', 2" ;
	
	public static final String GICOMPS   = getNextID() + ", 'GIC', 'GIC', 3" ;	
	
	public static final String NPCOMPS   = getNextID() + ", 'NPC', 'NPC', 4" ;	
	
	public static final String NPHARDS   = getNextID() + ", 'NPH', 'NPH', 4" ;
	
	public static final String CONPCOMPS = getNextID() + ", 'coNPC', 'coNPC', 4" ;

	public static final String BOUNDED = getNextID() + ", 'Bounded', 'B', 1" ;
	
	public static final String UNBOUNDED = getNextID() + ", 'Unbounded', 'unB', 4" ;

	/*
	 * Want to add a new Complexity ? Add a new public static final String. Format: getNextID() + ", 'COMPLEXITYNAME', 'COMPLEXITYALIAS', COMPLEXITYSORT"
	 * You replace COMPLEXITYNAME, COMPLEXITYALIAS, COMPLEXITYSORT with your String.
	 * 
	 * It automatically gets a new ID, which you can get by calling the static method getID(name-of-the-static-field)
	 *
	 */

	
	private static String getNextID()
	{
		Integer p = compID;
		compID++;
		return p.toString();
		
	}
	
	public static int getID(String s)
	{
		String[] array = s.split(",");
		return Integer.parseInt(array[0]);
	}
}
