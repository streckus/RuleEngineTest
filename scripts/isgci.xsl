<?xml version="1.0"?>
<!DOCTYPE xsl:stylesheet [
<!ENTITY nbsp "&#xA0;">
<!ENTITY envelope "&#x2709;">
]>
<xsl:stylesheet version="2.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xs="http://www.w3.org/2001/XMLSchema"
   xmlns:saxon="http://saxon.sf.net/"
   xmlns:teo="http://www.graphclasses.org"
   xmlns:XsltUtil="java:teo.XsltUtil"
   extension-element-prefixes="xs saxon teo XsltUtil">
<!-- $Header: /home/ux/CVSROOT/teo/isgci.xsl,v 1.85 2013/11/15 19:55:57 ux Exp $ -->

<xsl:output method="html" indent="no" encoding="utf-8"/>
<xsl:strip-space elements="*"/>
<xsl:key name="graphclass" match="GraphClass" use="@id"/>
<xsl:key name="problem" match="Problem" use="@name"/>
<xsl:key name="childproblem" match="Problem" use="from/@name"/>
<xsl:key name="outedges" match="incl" use="@super"/>
<xsl:key name="inedges" match="incl" use="@sub"/>
<xsl:key name="seenby" match="GraphClass" use="graphclass|(note//graphclass)"/>

<!--
  - Return the GraphClass node for the given id.
 -->
<xsl:function name="teo:graphclass">
   <xsl:param name="gc"/>
   <xsl:variable name="set" select="key('graphclass', $gc, $theroot)"/>
   <xsl:if test="not($set)">
      <xsl:message>Graphclass <xsl:value-of select="$gc"/> not found.</xsl:message>
   </xsl:if>
   <!-- <xsl:if test="not($set) and not(starts-with($gc, 'AUTO_'))">
      <xsl:message>Graphclass <xsl:value-of select="$gc"/> not found.</xsl:message>
   </xsl:if> -->
   <xsl:sequence select="$set"/>
</xsl:function>

<!--
  - Return a sort key for a graphclass given by id.
  - The key sorts graphclasses in document order.
 -->
<xsl:function name="teo:graphclass_sortkey" as="xs:integer">
   <xsl:param name="gc"/>
   <xsl:sequence select="count(
	 key('graphclass', $gc, $theroot)/preceding-sibling::GraphClass )"/>
</xsl:function>

<xsl:include href="lib.xsl"/>

<!--
  - Return the long complexity name for the given problem and full.xml
  - complexity name.
 -->
<xsl:function name="teo:longcomplexity">
   <xsl:param name="problem"/>
   <xsl:param name="complexity"/>
   <xsl:sequence select="
      if ($complexity = 'Unknown')
      then 'Unknown to ISGCI'
      else if ($problem = 'Cliquewidth expression' and
	    $complexity = 'NP-complete')
      then 'Unbounded or NP-complete'
      else $complexity"/>
</xsl:function>

<!--
  - Return the colour #rrggbb (string) for the given complexity string
  -->
<xsl:function name="teo:complexitycolour">
   <xsl:param name="complexity"/>
   <xsl:sequence select="
      if ($complexity = 'NP-complete' or $complexity = 'coNP-complete' or
	 $complexity = 'NP-hard' or $complexity = 'Unbounded')
      then '#800000'
      else if ($complexity = 'GI-complete')
      then '#CC8080'
      else if ($complexity = 'Polynomial')
      then '#008000'
      else if ($complexity = 'Linear' or $complexity = 'Bounded')
      then '#00CC00'
      else ''"/>
</xsl:function>


<!-- ISGCI homepage -->
<xsl:param name="homeurl" required="yes"/>
<!-- Output directory corresponding to this url -->
<xsl:param name="homedir" required="yes"/>
<!-- Absolute path on the webserver to the homedirectory, must NOT end in / -->
<xsl:param name="rooturl" required="yes"/>
<!-- Output directory, relative to $homedir or $homeurl-->
<xsl:param name="classesdir">.</xsl:param>
<!-- Maps directory, relative to $homedir or $homeurl-->
<xsl:param name="mapsdir">.</xsl:param>
<!-- Basename for the files with each a set (100) of references -->
<xsl:param name="refbasename">refs</xsl:param>
<!-- Statistics file -->
<xsl:param name="statsfile">./status.inc</xsl:param>
<!-- Outputfile with the classes links -->
<xsl:param name="classeshrefsfile">classes.hrefs</xsl:param>
<!-- Root of main input document -->
<xsl:variable name="theroot" select="/"/>
<!-- Document containing the links for smallgraphs -->
<xsl:param name="graphlinks" required="yes"/>

<xsl:template match="ISGCI">
   <!-- Create stats include file -->
   <xsl:apply-templates select="stats"/>
   <!-- Create graph pages -->
   <xsl:apply-templates select="GraphClasses"/>
   <!-- Create problem pages -->
   <xsl:apply-templates select="Problem[not(sparse)]"/>

   <!-- Create classic classes include file -->
   <xsl:result-document href="classics.inc" method="html"
	 encoding="utf-8">
      <xsl:apply-templates select="GraphClasses/GraphClass" mode="collect"/>
   </xsl:result-document>

   <!-- Create problems include file -->
   <xsl:result-document href="problems.inc" method="html"
	 encoding="utf-8">
      <xsl:apply-templates select="Problem[not(sparse)]" mode="collect">
	 <xsl:sort select="@name"/>
      </xsl:apply-templates>
   </xsl:result-document>

   <!-- Create javascript file -->
   <xsl:result-document href="{teo:file('','isgci.js')}" method="text"
	 encoding="utf-8">
      function toggle0(label, but, shown) {
	 if (document.getElementsByName) {
	    var elms = document.getElementsByName(label);
	    for (var i = 0; i &lt; elms.length; i++) {
	       elms[i].style.display = elms[i].style.display == 'none' ?
		  shown : 'none';
	    }
	    var x = document.getElementsByName(but);
	    if (x[0].innerHTML == '[+]Details')
	       x[0].innerHTML = '[-]Hide details';
	    else
	       x[0].innerHTML = '[+]Details';
	 }
      }
      function toggle(label, but) {
	 toggle0(label, but, 'inline');
      }
      function toggleT(label, but) {
	 toggle0(label, but, 'table-row');
      }
      function set0(label, but, disp, t) {
	 if (document.getElementsByName) {
	    var x = document.getElementsByName(but);
	    if (x.length > 0) {
	       if ((x[0].innerHTML == '[+]Details') == disp) {
		  if (t)
		     toggleT(label, but);
		  else
		     toggle(label, but);
	       }
	    }
	 }
      }
      function showall() {
	 set0('equdetails', 'equbutton', true, false);
	 set0('forbdetails', 'forbbutton', true, false);
	 set0('superdetails', 'superbutton', true, false);
	 set0('subdetails', 'subbutton', true, false);
	 set0('mapdetails', 'mapbutton', true, false);
	 <xsl:apply-templates select="Problem" mode="showall"/>
      }
      function hideall() {
	 set0('equdetails', 'equbutton', false, false);
	 set0('forbdetails', 'forbbutton', false, false);
	 set0('superdetails', 'superbutton', false, false);
	 set0('subdetails', 'subbutton', false, false);
	 set0('mapdetails', 'mapbutton', false, false);
	 <xsl:apply-templates select="Problem" mode="hideall"/>
      }
   </xsl:result-document>
</xsl:template>


<xsl:template match="GraphClasses">
   <xsl:result-document href="{$classeshrefsfile}" method="html"
	 encoding="utf-8">
   <xsl:for-each select="GraphClass">
      <xsl:apply-templates select="." mode="href"/>
      <br/><xsl:text>&#x0a;</xsl:text>
   </xsl:for-each>
   <xsl:apply-templates/>
   </xsl:result-document>
</xsl:template>

<xsl:template match="GraphClass">
   <xsl:variable name="page" select="concat(@id, '.html')"/>
   <xsl:result-document href="{teo:file($classesdir,$page)}"
	 method="html" indent="yes"
	 encoding="utf-8" doctype-public="-//W3C//DTD HTML 4.01//EN">
   <html>
   <head>
      <title><xsl:value-of select="name"/> graphs</title>
      <link rel="stylesheet" type="text/css" href="{teo:url('','global.css')}"/>
      <link rel="stylesheet" type="text/css" href="{teo:url('','data.css')}"/>
      <link rel="shortcut icon" type="image/x-icon" href="{
	 teo:url('','favicon.ico')}"/>
      <link rel="canonical" href="{teo:fullurl($classesdir,$page)}"/>
      <script type="text/javascript" src="{teo:url('','isgci.js')}"/>
      <xsl:call-template name="mathjax"/>
   </head>
   <body id="classpage">
      <xsl:call-template name="header"/>
      <div id="NavigationBox">
	 <xsl:call-template name="search"/>
	 <xsl:call-template name="mainmenu"/>
	 <ul class="navigation">
	    <li class="title">This class</li>
	    <li><a href="#definition">Definition</a></li>
	    <li><a href="#inclusions">Inclusions</a></li>
	    <li><a href="#problemssummary">Problems</a></li>
	    <li><a href="javascript:showall()">[+]Details</a></li>
	    <li><a href="javascript:hideall()">[-]Hide details</a></li>
	 </ul>
      </div>

      <h1>
	 <xsl:if test="@dir='directed'">Directed </xsl:if>
	 Graphclass: <xsl:apply-templates select="current()" mode="ref"/>
      </h1>
      
      <xsl:variable name="autodef">
	 <xsl:choose>
	    <xsl:when test="@type='induced-hereditary'">
	       <note name="definition">
	       A graph is an
	       <graphclass><xsl:value-of select="@id"/></graphclass> graph
	       iff every induced subgraph is a
	       <graphclass><xsl:value-of select="graphclass"/></graphclass>
	       graph.
	       </note>
	    </xsl:when>
	    <xsl:when test="@type='connected-hereditary'">
	       <note name="definition">
	       A graph is a
	       <graphclass><xsl:value-of select="@id"/></graphclass> graph
	       iff every connected induced subgraph is a
	       <graphclass><xsl:value-of select="graphclass"/></graphclass>
	       graph.
	       </note>
	    </xsl:when>
	    <xsl:when test="@type='isometric-hereditary'">
	       <note name="definition">
	       A graph is an 
	       <graphclass><xsl:value-of select="@id"/></graphclass> graph
	       iff every isometric connected induced subgraph is a
	       <graphclass><xsl:value-of select="graphclass"/></graphclass>
	       graph.
	       </note>
	    </xsl:when>
	    <xsl:when test="@type='probe'">
	       <note name="definition">
	       A graph is a
	       <graphclass><xsl:value-of select="@id"/></graphclass> graph
	       if its vertex set can be partitioned into two sets, probes (P)
	       and non-probes (N), such that N is independent and new edges can
	       be added between non-probes in such a way that the resulting
	       graph is a
	       <graphclass><xsl:value-of select="graphclass"/></graphclass>
	       graph.
	       </note>
	    </xsl:when>
	    <xsl:when test="@type='clique'">
	       <note name="definition">
	       A graph is in
	       <graphclass><xsl:value-of select="@id"/></graphclass>
	       graphs iff it is the <graphclass>gc_141</graphclass> graph of
	       some
	       <graphclass><xsl:value-of select="graphclass"/></graphclass>
	       graph.
	       </note>
	    </xsl:when>
	 </xsl:choose>
      </xsl:variable>

      <xsl:if test="count(note[@name='definition']|$autodef/*) = 1">
	 <div id="definition">
	    <div class="defhdr">Definition:</div>
	    <p>
	    <xsl:apply-templates select="note[@name='definition']|$autodef">
	       <xsl:with-param name="thisclass" tunnel="yes" select="@id"/>
	    </xsl:apply-templates>
	    </p>
	 </div>
      </xsl:if>
      <xsl:if test="count(note[@name='definition']|$autodef/*) > 1">
	 <xsl:variable name="here" select="."/>
	 <div id="definition">
	    <div class="defhdr">The following definitions are equivalent:</div>
	    <ol>
	    <xsl:for-each select="$autodef[exists(child::element())] |
		     note[@name='definition']">
	       <li><xsl:apply-templates select=".">
		  <xsl:with-param name="thisclass" tunnel="yes"
			select="$here/@id"/>
	       </xsl:apply-templates></li>
	    </xsl:for-each>
	    </ol>
	 </div>
      </xsl:if>

      <xsl:if test="clique-fixed">
	 <p>This class is fixed under the clique operator. That is,
	    <xsl:apply-templates select="current()" mode="ref"/> =
	    <xsl:apply-templates select="teo:graphclass('gc_141')" mode="href"/>
	    graphs of <xsl:apply-templates select="current()" mode="ref"/>.
	 </p>
      </xsl:if>

      <xsl:for-each select="note[not(@name)]">
	 <p><xsl:apply-templates/></p>
      </xsl:for-each>

      <xsl:if test="note[@name='open-problem']">
	 <div class="openproblems">
	    <h3>Open problems</h3>
	    <ol>
	       <xsl:for-each select="note[@name='open-problem']">
		  <li><xsl:apply-templates select="."/></li>
	       </xsl:for-each>
	    </ol>
	 </div>
      </xsl:if>

      <xsl:if test="note[@name='conjecture']">
	 <div class="conjectures">
	    <h3>Conjectures</h3>
	    <ol>
	       <xsl:for-each select="note[@name='conjecture']">
		  <li><xsl:apply-templates select="."/></li>
	       </xsl:for-each>
	    </ol>
	 </div>
      </xsl:if>

      <xsl:if test="ref">
	 <div class="references">
	    <h3>References</h3>
	    <p><xsl:apply-templates select="ref"/></p>
	 </div>
      </xsl:if>

      <xsl:variable name="this" select="."/>
      <xsl:variable name="equs" select=
	 "key('graphclass', note[@name='equivalents']/graphclass)"/>
      <xsl:variable name="equ0" select="$equs except current()"/>
      <xsl:if test="$equ0">
	 <div class="equivs">
	    <h3>Equivalent classes</h3>
	    <a href="javascript:toggle('equdetails', 'equbutton');">
	       <span class="collapsor"
		     id="equbutton" name="equbutton">[+]Details</span>
	    </a>
	    <div><p name="equdetails" id="equdetails" style="display:none">
	    <i>Only references for direct inclusions are given. Where no reference is given for an equivalent class, check other equivalent classes or use the Java application.</i>
	    </p></div>
	    <ul class="classeslist">
	    <xsl:for-each select="$equ0">
	       <li>
	       <xsl:apply-templates select="." mode="href"/>
	       <span id="equdetails" name="equdetails" style="display:none">
		  <xsl:variable name="refs" select=
			"(key('outedges', $this/@id)[@sub=current()/@id]|
			key('inedges', $this/@id)[@super=current()/@id])/ref"/>
		  <xsl:for-each-group select="$refs" group-by=".">
		     <xsl:apply-templates select="."/>
		  </xsl:for-each-group>
	       </span>
	       </li>
	    </xsl:for-each>
	    </ul>
	 </div>
      </xsl:if>

      <xsl:variable name="compls"
	    select="key('graphclass', note[@name='complements']/graphclass)"/>
      <xsl:if test="$compls">
	 <div class="complements">
	    <h3>Complement classes</h3>
	    <xsl:choose>
	       <xsl:when test="$compls[@id=current()/@id]">
		  <p>self-complementary</p>
	       </xsl:when>
	       <xsl:otherwise>
		  <ul class="classeslist">
		  <xsl:for-each select="$compls">
		     <li><xsl:apply-templates select="." mode="href"/></li>
		  </xsl:for-each>
		  </ul>
	       </xsl:otherwise>
	    </xsl:choose>
	 </div>
      </xsl:if>

      <xsl:variable name="seealsographclass" select="(
	       (key('graphclass', (.|note[@name='see-also'])/graphclass)|
	       key('seenby', current()/@id))|
	       (if (clique-fixed) then key('graphclass', 'gc_141') else ())|
	       (if (./@id='gc_141') then
		  $theroot/ISGCI/GraphClasses/GraphClass[clique-fixed] else ())
	       ) except ($compls|$equs)"/>
      <xsl:if test="$seealsographclass">
	 <div class="related">
	    <h3>Related classes</h3>
	    <ul class="classeslist">
	    <xsl:for-each select="$seealsographclass">
	       <li><xsl:apply-templates select="." mode="href"/></li>
	    </xsl:for-each>
	    </ul>
	 </div>
      </xsl:if>

      <xsl:variable name="seealsorest"
	    select="(note[@name='see-also']/*) except
	       note[@name='see-also']/graphclass"/>
      <xsl:if test="$seealsorest">
	 <div class="seealso">
	    <h3>See also</h3>
	    <div id="indent">
	    <xsl:apply-templates select="$seealsorest"/>
	    </div>
	 </div>
      </xsl:if>

      <xsl:if test="smallgraph">
	 <div class="forbidden">
	    <h3>Forbidden subgraphs</h3>
	    <a href="javascript:toggle('forbdetails', 'forbbutton');">
	       <span class="collapsor"
		     id="forbbutton" name="forbbutton">[+]Details</span>
	    </a>
	    <div><p id="forbdetails" name="forbdetails" style="display:none">
	    <xsl:apply-templates select="smallgraph" mode="image">
	       <xsl:sort select="string-length(substring-before(../name, .))"/>
	    </xsl:apply-templates>
	    </p></div>
	 </div>
      </xsl:if>

      <h2 id="inclusions">Inclusions</h2>

      <p><i>
      The map shows the inclusions between the current class and a fixed set of landmark classes. Minimal/maximal is with respect to the contents of ISGCI. Only references for direct inclusions are given. Where no reference is given, check equivalent classes or use the Java application. To check relations other than inclusion (e.g. disjointness) use the Java application, as well.
      </i></p>

      <div class="inclusionmap">
	 <h3>Map</h3>
	 <a href="javascript:toggle('mapdetails', 'mapbutton');">
	    <span class="collapsor"
		  id="mapbutton" name="mapbutton">[+]Details</span>
	 </a>
	 <div>
	 <img id="mapdetails" name="mapdetails" style="display:none"
	    src="{teo:url($mapsdir, concat(@id, '.png'))}"
	    alt="Inclusion map for {name}"/>
	 </div>
      </div>

      <xsl:variable name="in" select="key('graphclass',
	    (key('graphclass', key('inedges', $equs/@id)/@super) except $equs)
	 /note[@name='equivalents']/graphclass)"/>
      <xsl:if test="$in">
	 <xsl:variable name="properin"
	       select="key('inedges', $equs/@id)[@proper]"/>
	 <div class="minsuper">
	 <h3>Minimal superclasses</h3>
	 <a href="javascript:toggle('superdetails', 'superbutton');">
	    <span class="collapsor"
		  id="superbutton" name="superbutton">[+]Details</span>
	 </a>
	 <ul class="classeslist">
	 <xsl:for-each select="$in">
	    <li>
	    <xsl:apply-templates select="." mode="href"/>
	    <span id="superdetails" name="superdetails" style="display:none">
	       <xsl:apply-templates select=
		     "key('inedges', $this/@id)[@super=current()/@id]/ref"/>
	       <xsl:variable name="inequ"
		     select="./note[@name='equivalents']/graphclass"/>
	       <xsl:choose>
		  <xsl:when test="some
			$edge in $properin,
			$super in $inequ
			satisfies $edge/@super=$super">
		     <xsl:text>(known proper)</xsl:text>
		  </xsl:when>
		  <xsl:otherwise>
		     <xsl:text>(possibly equal)</xsl:text>
		  </xsl:otherwise>
	       </xsl:choose>
	    </span>
	    </li>
	 </xsl:for-each>
	 </ul>
	 </div>
      </xsl:if>

      <xsl:variable name="out" select="key('graphclass',
	    (key('graphclass', key('outedges', $equs/@id)/@sub) except $equs)
	 /note[@name='equivalents']/graphclass)"/>
      <xsl:if test="$out">
	 <div class="maxsub">
	 <xsl:variable name="properout"
	       select="key('outedges', $equs/@id)[@proper]"/>
	 <h3>Maximal subclasses</h3>
	 <a href="javascript:toggle('subdetails', 'subbutton');">
	    <span class="collapsor"
		  id="subbutton" name="subbutton">[+]Details</span>
	 </a>
	 <ul class="classeslist">
	 <xsl:for-each select="$out">
	    <li>
	    <xsl:apply-templates select="." mode="href"/>
	    <span id="subdetails" name="subdetails" style="display:none">
	       <xsl:apply-templates select=
		     "key('outedges', $this/@id)[@sub=current()/@id]/ref"/>
	       <xsl:variable name="outequ"
		     select="./note[@name='equivalents']/graphclass"/>
	       <xsl:choose>
		  <xsl:when test="some
			$edge in $properout,
			$sub in $outequ
			satisfies $edge/@sub=$sub">
		     <xsl:text> (known proper)</xsl:text>
		  </xsl:when>
		  <xsl:otherwise>
		     <xsl:text> (possibly equal)</xsl:text>
		  </xsl:otherwise>
	       </xsl:choose>
	    </span>
	    </li>
	 </xsl:for-each>
	 </ul>
	 </div>
      </xsl:if>

      <h2 id="problemssummary">Problems</h2>
      <p><i>Problems in italics have no summary page and are only listed when
      ISGCI contains a result for the current class.</i></p>
      <table style="table-layout:fixed">
	 <tr>
	 <td style="width:17em"/>
	 <td style="width:15em"/>
	 <td style="width:7em"/>
	 <td/>
	 </tr>
	 <xsl:apply-templates select="problem[@complexity ne 'Unknown' or 
	       not(key('problem', @name)/sparse)]" mode="summary">
	    <xsl:sort select="@name"/>
	 </xsl:apply-templates>
      </table>

      <xsl:call-template name="footer">
	 <xsl:with-param name="path" select="$classesdir"/>
	 <xsl:with-param name="page" select="$page"/>
      </xsl:call-template>
   </body>
   </html>
   </xsl:result-document>
</xsl:template>

<xsl:template match="GraphClass" mode="href">
   <xsl:param name="path" select="'.'"/>
   <span class="graphclass">
   <a href="{teo:url($classesdir, concat(@id,'.html'))}">
      <xsl:apply-templates select="current()" mode="ref"/>
   </a>
   </span>
</xsl:template>

<xsl:template match="GraphClass" mode="ref">
   <xsl:value-of disable-output-escaping="yes" select="XsltUtil:latex(name)"/>
</xsl:template>

<!-- *******************************************************************
  - Problems and algorithms
 -->

<xsl:template match="Problem">
   <xsl:result-document href="{teo:file($classesdir,teo:problemfile(@name))}"
	 method="html" indent="yes" encoding="utf-8"
	 doctype-public="-//W3C//DTD HTML 4.01//EN">
   <xsl:variable name="complexities" select="
	 if (@name='Cliquewidth')
	 then ('Bounded', 'Unbounded', 'Open', 'Unknown')
	 else ('Linear', 'Polynomial', 'GI-complete', 'NP-hard', 'NP-complete',
	    'coNP-complete', 'Open', 'Unknown')
	 "/>
   <html>
   <head>
      <title><xsl:value-of select="@name"/> problem</title>
      <link rel="stylesheet" type="text/css" href="{teo:url('','global.css')}"/>
      <link rel="stylesheet" type="text/css" href="{teo:url('','data.css')}"/>
      <link rel="shortcut icon" type="image/x-icon" href="{
	 teo:url('','favicon.ico')}"/>
      <link rel="canonical" href="{teo:fullurl($classesdir,teo:problemfile(@name))}"/>
   </head>
   <body id="problempage">
      <xsl:call-template name="header"/>
      <div id="NavigationBox">
	 <xsl:call-template name="search"/>
	 <xsl:call-template name="mainmenu"/>
      <ul class="navigation">
	 <li class="title">This problem</li>
	 <xsl:for-each select="$complexities">
	    <li><a href="#{.}"><xsl:value-of select="."/></a></li>
	 </xsl:for-each>
      </ul>
      </div>

      <h1>Problem: <xsl:value-of select="@name"/></h1>

      <div id="definition">
	 <div class="defhdr">Definition:</div>
	 <xsl:apply-templates select="note"/>
      </div>
      
      <xsl:variable name="this" select="current()"/>
      <xsl:for-each select="$complexities">
	 <xsl:call-template name="problemcomplexity">
	    <xsl:with-param name="problem" select="$this"/>
	    <xsl:with-param name="complexity" select="."/>
	 </xsl:call-template>
      </xsl:for-each>
      <xsl:call-template name="footer">
	 <xsl:with-param name="path" select="$classesdir"/>
	 <xsl:with-param name="page" select="teo:problemfile(@name)"/>
      </xsl:call-template>
   </body>
   </html>
   </xsl:result-document>
</xsl:template>

<xsl:template name="problemcomplexity">
   <xsl:param name="problem"/>
   <xsl:param name="complexity"/>

   <h3 id="{$complexity}"><xsl:value-of
	 select="teo:longcomplexity($problem/@name, $complexity)"/></h3>

   <xsl:variable name="nodes" select="root($problem)//GraphClass/problem
	    [@name=$problem/@name and @complexity=$complexity]/.."/>
   <xsl:if test="$nodes">
      <xsl:choose>
	 <xsl:when test="teo:complexitycolour($complexity)">
	    <div class="vbar" style=
		  "border-left-color: {teo:complexitycolour($complexity)}">
	    <ul class="classeslist">
	       <xsl:for-each select="$nodes">
		  <li><xsl:apply-templates select="." mode="href"/></li>
	       </xsl:for-each>
	    </ul>
	    </div>
	 </xsl:when>
	 <xsl:otherwise>
	    <ul class="classeslist">
	       <xsl:for-each select="$nodes">
		  <li><xsl:apply-templates select="." mode="href"/></li>
	       </xsl:for-each>
	    </ul>
	 </xsl:otherwise>
      </xsl:choose>
   </xsl:if>
   <xsl:call-template name="toplink"/>
</xsl:template>

<xsl:template match="problem" mode="summary">
   <xsl:variable name="colour" select="teo:complexitycolour(@complexity)"/>
   <xsl:variable name="complexity"
	 select="teo:longcomplexity(@name, @complexity)"/>
   <tr>
   <td>
      <xsl:choose>
	 <xsl:when test="key('problem', @name)[sparse]">
	    <span class="sparseproblem"><xsl:value-of select="@name"/></span>
	 </xsl:when>
	 <xsl:otherwise>
	    <xsl:value-of select="@name"/>
	 </xsl:otherwise>
      </xsl:choose>
      <xsl:text> </xsl:text>
      <div class="tooltip">
	 <xsl:choose>
	    <xsl:when test="key('problem', @name)[sparse]">
	       [?]
	    </xsl:when>
	    <xsl:otherwise>
	    <a href="{teo:problemfile(@name)}">[?]</a>
	    </xsl:otherwise>
	 </xsl:choose>
	 <div><xsl:apply-templates select="key('problem', @name)/note"/></div>
      </div>
   </td>
   <td>
   <xsl:choose>
      <xsl:when test="$colour">
	 <span style="color:{$colour}">
	 <xsl:value-of select="$complexity"/>
	 </span>
      </xsl:when>
      <xsl:otherwise>
	 <xsl:value-of select="$complexity"/>
      </xsl:otherwise>
   </xsl:choose>
   </td>
   <td><a href="javascript:toggleT(
	 '{concat(teo:name2id(@name),'details')}',
	 '{concat(teo:name2id(@name),'button')}' );">
	    <span class="collapsor"
	       id="{concat(teo:name2id(@name),'button')}"
	       name="{concat(teo:name2id(@name),'button')}"
	       >[+]Details</span>
	 </a>
   </td>
   <td></td>
   </tr>
   <xsl:apply-templates select="." mode="collapsed"/>
</xsl:template>

<xsl:template match="problem" mode="collapsed">
   <tr id="{concat(teo:name2id(@name),'details')}"
	 name="{concat(teo:name2id(@name),'details')}"
	 style="display:none">
   <td></td><td colspan="3">

   <xsl:apply-templates select=
	 "algo[not(graphclass) and @complexity='Linear']"/>
   <xsl:apply-templates select=
	 "algo[not(graphclass) and @complexity='Polynomial']"/>
   <xsl:apply-templates select=
	 "algo[not(graphclass) and @complexity='GI-complete']"/>
   <xsl:apply-templates select=
	 "algo[not(graphclass) and @complexity='NP-hard']"/>
   <xsl:apply-templates select=
	 "algo[not(graphclass) and @complexity='NP-complete']"/>
   <xsl:apply-templates select=
	 "algo[not(graphclass) and @complexity='coNP-complete']"/>
   <xsl:apply-templates select=
	 "algo[not(graphclass) and @complexity='Bounded']"/>
   <xsl:apply-templates select=
	 "algo[not(graphclass) and @complexity='Unbounded']"/>
   <xsl:apply-templates select=
	 "algo[not(graphclass) and @complexity='Open']"/>

   <xsl:if test="algo[not(graphclass)] and algo[graphclass]">
      <br/>
   </xsl:if>

   <xsl:apply-templates select="algo[graphclass and @complexity='Linear']">
      <xsl:sort select="teo:graphclass_sortkey(graphclass/text())"/>
   </xsl:apply-templates>
   <xsl:apply-templates select="algo[graphclass and @complexity='Polynomial']">
      <xsl:sort select="teo:graphclass_sortkey(graphclass/text())"/>
   </xsl:apply-templates>
   <xsl:apply-templates select="algo[graphclass and @complexity='GI-complete']">
      <xsl:sort select="teo:graphclass_sortkey(graphclass/text())"/>
   </xsl:apply-templates>
   <xsl:apply-templates select="algo[graphclass and @complexity='NP-hard']">
      <xsl:sort select="teo:graphclass_sortkey(graphclass/text())"/>
   </xsl:apply-templates>
   <xsl:apply-templates select="algo[graphclass and @complexity='NP-complete']">
      <xsl:sort select="teo:graphclass_sortkey(graphclass/text())"/>
   </xsl:apply-templates>
   <xsl:apply-templates select="algo[graphclass and @complexity='coNP-complete']">
      <xsl:sort select="teo:graphclass_sortkey(graphclass/text())"/>
   </xsl:apply-templates>
   <xsl:apply-templates select="algo[graphclass and @complexity='Bounded']">
      <xsl:sort select="teo:graphclass_sortkey(graphclass/text())"/>
   </xsl:apply-templates>
   <xsl:apply-templates select="algo[graphclass and @complexity='Unbounded']">
      <xsl:sort select="teo:graphclass_sortkey(graphclass/text())"/>
   </xsl:apply-templates>
   <xsl:apply-templates select="algo[@complexity='Open']">
      <xsl:sort select="teo:graphclass_sortkey(graphclass/text())"/>
   </xsl:apply-templates>
   <!-- No need to doing anything with Unknown complexity -->
   </td></tr>
</xsl:template>

<xsl:template match="Problem" mode="showall">
   set0('<xsl:value-of select="concat(teo:name2id(@name),'details')"/>',
	'<xsl:value-of select="concat(teo:name2id(@name),'button')"/>',
	 true, true );
</xsl:template>

<xsl:template match="Problem" mode="hideall">
   set0('<xsl:value-of select="concat(teo:name2id(@name),'details')"/>',
	'<xsl:value-of select="concat(teo:name2id(@name),'button')"/>',
	 false, true );
</xsl:template>

<xsl:template match="algo[not(graphclass)]">
   <i>
   <xsl:choose>
      <xsl:when test="@name='Cliquewidth expression' and
	    @complexity='NP-complete'">
	 Unbounded/NP-complete
      </xsl:when>
      <xsl:otherwise>
	 <xsl:value-of select="@complexity"/><xsl:text> </xsl:text>
      </xsl:otherwise>
   </xsl:choose>
   <xsl:if test="@bounds">
      [$<xsl:value-of select="@bounds"/>$]
   </xsl:if>
   <xsl:apply-templates select="ref"/>
   <xsl:apply-templates select="note"/>
   </i>
   <br/>
</xsl:template>

<xsl:template match="algo[graphclass]">
   <xsl:if test="key('graphclass',graphclass)">
      <xsl:choose>
	 <xsl:when test="@name='Cliquewidth expression' and
	       @complexity='NP-complete'">
	    Unbounded/NP-complete
	 </xsl:when>
	 <xsl:otherwise>
	    <xsl:value-of select="@complexity"/>
	 </xsl:otherwise>
      </xsl:choose>
      <xsl:if test="@bounds">
	 <xsl:text> </xsl:text>[$<xsl:value-of select="@bounds"/>$]
      </xsl:if>
      <xsl:apply-templates select="graphclass"/>
      <xsl:apply-templates select="ref"/>
      <xsl:choose>
	 <xsl:when test="note and
	       graphclass/text() eq ancestor::GraphClass/@id">
	    <xsl:apply-templates select="note" mode="indent"/>
	 </xsl:when>
	 <xsl:otherwise><br/>
	 </xsl:otherwise>
      </xsl:choose>
   </xsl:if>
</xsl:template>

<xsl:template match="algo/graphclass">
   <xsl:variable name="node" select=
	 "teo:graphclass(.) except ancestor::GraphClass"/>
   <xsl:if test="$node">
      <xsl:text> on </xsl:text>
      <xsl:apply-templates select="$node" mode="href"/>
   </xsl:if>
   <xsl:text>&#xa;</xsl:text>
</xsl:template>

<!-- *******************************************************************
  - Defining inclusions
 -->
<xsl:template match="Inclusions">
</xsl:template>

<xsl:template match="incl" mode="super">
   <xsl:copy-of select="teo:graphclass(@super)"/>
</xsl:template>

<xsl:template match="incl" mode="sub">
   <xsl:copy-of select="teo:graphclass(@sub)"/>
</xsl:template>

<!-- *******************************************************************
  - Notes.
  -->
<xsl:template match="note">
   <xsl:apply-templates/>
</xsl:template>

<xsl:template match="note" mode="indent">
   <div id="indent">
   <xsl:apply-templates/>
   </div>
</xsl:template>

<xsl:template match="note//*" priority="-0.5">
   <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates/>
   </xsl:copy>
</xsl:template>

<!-- *******************************************************************
  - Defining references
 -->

<!-- Return the number of a reference -->
<xsl:function name="teo:refnum">
   <xsl:param name="s"/>
   <xsl:sequence select="
      if (matches($s, '^[0-9]+$'))
      then $s
      else substring-after($s, 'ref_')"/>
</xsl:function>

<!-- Return the basenumber of a reference -->
<xsl:function name="teo:refbasenum">
   <xsl:param name="s"/>
   <xsl:variable name="num" select="teo:refnum($s)"/>
   <xsl:sequence select="concat(substring($num, 1, string-length($num)-2),
	 '00')"/>
</xsl:function>

<!-- Returns a string to be used as the name of the reference file for its
     parameter. Groups references per 100.
  -->
<xsl:function name="teo:refbase">
   <xsl:param name="s"/>
   <xsl:sequence select="concat($refbasename, teo:refbasenum($s), '.html')"/>
</xsl:function>

<xsl:template match="REFS">
   <xsl:variable name="maxgroup" as="xs:integer"
	 select="xs:integer(teo:refbasenum(Ref[position()=last()]/@id))
	    div 100"/>
   <xsl:for-each-group select="Ref" group-by="teo:refbase(current()/@id)">
      <xsl:variable name="file"
	 select="teo:refbase(current()/@id)"/>
      <xsl:variable name="series">
	 <xsl:value-of select="teo:refbasenum(current()/@id)"/>
	 <xsl:text> - </xsl:text>
	 <xsl:value-of select="number(teo:refbasenum(current()/@id))+99"/>
      </xsl:variable>
      
      <xsl:result-document href="{teo:file($classesdir,$file)}" method="html"
	    indent="yes"
	    encoding="utf-8"
	    doctype-public="-//W3C//DTD HTML 4.01//EN">
      <html>
	 <head>
	 <title>ISGCI References <xsl:value-of select="$series"/></title>
	 <link rel="stylesheet" type="text/css" href="{teo:url('','global.css')}"/>
	 <link rel="stylesheet" type="text/css" href="{teo:url('','data.css')}"/>
	 <link rel="shortcut icon" type="image/x-icon" href="{
	    teo:url('','favicon.ico')}"/>
	 <link rel="canonical" href="{teo:fullurl($classesdir,$file)}"/>
	 <xsl:call-template name="mathjax"/>
	 </head>

	 <body id="refpage"><xsl:text>&#xA;</xsl:text>
	    <xsl:call-template name="header"/>
	    <div id="NavigationBox">
	       <xsl:call-template name="search"/>
	       <xsl:call-template name="mainmenu"/>
	    </div>
	    <p><i>Note: The references are <b>not</b> ordered
	    alphabetically!</i></p>
	    <!-- Refs -->
	    <table><xsl:for-each select="current-group()">
	       <xsl:apply-templates select="current()"/>
	       <xsl:text>&#xA;</xsl:text>
	    </xsl:for-each></table>
	    <!-- Pagination -->
	    <div id="PaginationOuter"> <div id="PaginationInner">
	    <div id="PaginationBox">
	       <ul id="Pagination">
	       <xsl:if test="position() ne 1">
		  <li> <a href="{teo:url($classesdir, teo:refbase(
			string(100 * (position()-2))))}">&lt;</a></li>
	       </xsl:if>
	       <xsl:for-each select="0 to $maxgroup">
		  <xsl:variable name="pagfile" select="
			teo:refbase(string(100 * current()))"/>
		  <xsl:choose>
		     <xsl:when test="$file ne $pagfile">
			<li> <a href="{teo:url($classesdir, $pagfile)}">
			<xsl:value-of select="current()"/>
			</a></li>
		     </xsl:when>
		     <xsl:otherwise>
			<li class="active">
			<xsl:value-of select="current()"/>
			</li>
		     </xsl:otherwise>
		  </xsl:choose>
	       </xsl:for-each>
	       <xsl:if test="position() ne last()">
		  <li><a href="{teo:url($classesdir, teo:refbase(
			string(100 * (position()))))}">&gt;</a></li>
	       </xsl:if>
	       </ul>
	    </div>
	    </div> </div>
	    <!-- Footer -->
	    <xsl:call-template name="footer">
	       <xsl:with-param name="style" select=
		     "'margin-top:6ex; margin-bottom: 6ex'"/>
	    </xsl:call-template>
	 </body>
      </html>
      </xsl:result-document>
   </xsl:for-each-group> 
</xsl:template>

<xsl:template match="Ref">
   <tr>
   <td id="{@id}"><xsl:value-of select="teo:refnum(@id)"/></td>
   <td>
   <xsl:text>&#xA;</xsl:text>
   <xsl:apply-templates/>
   </td>
   </tr>
   <xsl:text>&#xA;</xsl:text>
</xsl:template>

<!-- Literature reference -->
<xsl:template match="bib">
   <xsl:apply-templates/>
</xsl:template>

<xsl:template match="authors">
   <xsl:value-of select="."/><br/><xsl:text>&#xA;</xsl:text>
</xsl:template>

<xsl:template match="title">
   <i><xsl:value-of select="."/></i><br/><xsl:text>&#xA;</xsl:text>
</xsl:template>

<xsl:template match="rest">
   <xsl:apply-templates/><br/><xsl:text>&#xA;</xsl:text>
</xsl:template>

<xsl:template match="doi">
   <a href="http://dx.doi.org/{.}">doi <xsl:value-of select="."/></a><br/><xsl:text>&#xA;</xsl:text>
</xsl:template>


<!-- *******************************************************************
  - Refering to stuff
 -->

<!-- A reference IDREF -->
<!-- Note that bibliography entries aren't generated at the same time as the
     references to them -->
<xsl:template match="ref">
   <xsl:choose>
      <xsl:when test=". = 'def'">
	 [by definition]
      </xsl:when>
      <xsl:when test=". = 'trivial'">
	 [trivial]
      </xsl:when>
      <xsl:when test=". = 'forbidden'">
	 [from the set of forbidden subgraphs]
      </xsl:when>
      <xsl:when test=". = 'complement'">
	 [from the complement]
      </xsl:when>
      <xsl:when test=". = 'hereditary'">
	 [by definition of hereditariness]
      </xsl:when>
      <xsl:when test=". = 'basederived'">
	 [from the baseclasses]
      </xsl:when>
      <xsl:otherwise>
	 [<a href="{teo:refbase(.)}#{.}"><xsl:value-of select="teo:refnum(.)"/>
	 </a>]
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<!-- A graphclass IDREF
     Create a hyperlink if it doesn't refer to the active graphclass.
     See also algo/graphclass
  -->
<xsl:template match="graphclass">
   <xsl:param name="thisclass" as="xs:string?" required="no" tunnel="yes"
	 select="ancestor::GraphClass/@id"/>
   <xsl:choose>
      <xsl:when test=". != $thisclass">
	 <xsl:apply-templates select="teo:graphclass(.)" mode="href"/>
      </xsl:when>
      <xsl:otherwise>
	 <xsl:apply-templates select="teo:graphclass(.)" mode ="ref"/>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<!-- Picture of a small graph -->
<xsl:template match="smallgraph" mode="image">
   <xsl:if test="not(key('smallgraph', ., $smallgraphs))">
      <xsl:message>Smallgraph link for <xsl:value-of select="."/> not found</xsl:message>
   </xsl:if>
   <xsl:apply-templates select="key('smallgraph', ., $smallgraphs)"
      mode="image"/>
   <xsl:text> </xsl:text>
</xsl:template>

<xsl:template match="SMALLGRAPHS/link" mode="image">
   <span style="display:inline-table; text-align:center; margin:1em">
   <span style="display:table-row"><span style="display:table-cell">
      <b><xsl:value-of disable-output-escaping="yes"
	 select="XsltUtil:latex(@name)"/></b>
   </span></span>
   <xsl:if test="@img">
      <xsl:if test="@imgname!=''">
	 <span style="display:table-row"><span style="display:table-cell">
	    (figure shows
	    <xsl:value-of disable-output-escaping="yes"
	       select="XsltUtil:latex(@imgname)"/>)
	 </span></span>
      </xsl:if>
      <span style="display:table-row"><span style="display:table-cell">
	 <a href="{teo:url('','smallgraphs.html')}#{@address}">
	 <img src="{@img}" alt="{@name}" style="border:none"/>
	 </a>
      </span></span>
   </xsl:if>
   <xsl:if test="not(@img)">
      <span style="display:table-row"><span style="display:table-cell">
	 <a href="{teo:url('','smallgraphs.html')}#{@address}">(no image)</a>
      </span></span>
   </xsl:if>
   </span>
</xsl:template>


<!-- Links to Zentralblatt/RRR etc -->
<xsl:template match="link">
   <xsl:if test="@rrr">
      <a href="{@rrr}"><xsl:value-of select="@rrr"/></a>
      <xsl:if test="@zm|@zm_user|@zm_prob">
	 <br/>
      </xsl:if>
   </xsl:if>

   <xsl:choose>
      <xsl:when test="@zm">
	 <a href="http://www.emis.de/MATH-item?{@zm}">ZMath <xsl:value-of select="@zm"/></a>
      </xsl:when>
      <xsl:when test="@zm_user">
	 <a href="http://www.emis.de/MATH-item?{@zm_user}">ZMath <xsl:value-of select="@zm"/></a>
      </xsl:when>
      <xsl:when test="@zm_prob">
	 <a href="http://www.emis.de/MATH-item?{@zm_prob}">ZMath <xsl:value-of select="@zm"/></a>
      </xsl:when>
   </xsl:choose>
</xsl:template>

<!-- ********* Statistics and other data for the static pages ************ -->
<!-- Statistics to put on homepage -->
<xsl:template match="stats">
   <xsl:result-document href="{$statsfile}" method="html"
	 encoding="utf-8">
      <xsl:value-of select="@nodes"/> classes<br/>
      <xsl:value-of select="@edges"/> inclusions<br/>
      updated <xsl:value-of select="@date"/>
   </xsl:result-document>
</xsl:template>

<!-- Collect the classic classes -->
<xsl:template match="GraphClass[@type='base' or @id='gc_547']" mode="collect">
   <xsl:param name="path" select="'classes'"/>
   <xsl:variable name="id" select="string(@id)"/>
      <xsl:variable name="equs" select=
	 "key('graphclass', note[@name='equivalents']/graphclass)"/>
   <xsl:variable name="rank" as="xs:integer" select="count(
	 /ISGCI/Inclusions/incl[@super=$id] |
	 /ISGCI/Inclusions/incl[@sub=$id]
      ) + count(
	 key('graphclass', problem/algo/graphclass) intersect $equs)"/>
   <xsl:if test="$rank ge 13 or $id eq 'gc_547' or @id eq 'gc_141' ">
      <li><xsl:apply-templates select="." mode="href"/></li>
   </xsl:if>
</xsl:template>

<xsl:template match="text()" mode="collect" priority="-1"/>

<!-- Collect the problems -->
<xsl:template match="Problem" mode="collect">
   <a href="{$classesdir}/{teo:problemfile(@name)}"><xsl:value-of select="@name"/></a>
   <br/><xsl:text>&#x0a;</xsl:text>
</xsl:template>

</xsl:stylesheet>


<!-- EOF -->
