<?xml version="1.0"?>
<!DOCTYPE xsl:stylesheet [
<!ENTITY nbsp "&#xA0;">
<!ENTITY envelope "&#x2709;">
]>
<xsl:stylesheet version="2.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xs="http://www.w3.org/2001/XMLSchema"
   xmlns:teo="http://www.graphclasses.org"
   xmlns:XsltUtil="java:teo.XsltUtil"
   extension-element-prefixes="teo xs XsltUtil">
<!-- $Header: /home/ux/CVSROOT/teo/smallgraphs.xsl,v 1.26 2013/03/30 18:12:35 ux Exp $ -->

<xsl:output method="xml" encoding="utf-8" doctype-system="smallgraphs.dtd"/>
<xsl:output name="html" method="html" encoding="utf-8"
	 doctype-public="-//W3C//DTD HTML 4.01//EN"/>
<!-- <xsl:strip-space elements="*"/> -->

<!-- Mind the trailing space and the tab in the sed expression! -->
<xsl:param name="fig2gif">sed -e 's/^[ 	]*//' -e '/^$/d' | fig2dev -L gif </xsl:param>

<!-- ISGCI homepage -->
<xsl:param name="homeurl" required="yes"/>
<!-- Output directory corresponding to this url -->
<xsl:param name="homedir" required="yes"/>
<!-- Absolute path on the webserver to the homedirectory, must NOT end in / -->
<xsl:param name="rooturl" required="yes"/>
<!-- Directory for images -->
<xsl:param name="imagedir">images</xsl:param>
<!-- Smallgraphs outputfile for Sage -->
<xsl:param name="sagefile" required="yes"/>
<!-- Document containing the links for smallgraphs -->
<xsl:param name="graphlinks" required="yes"/>

<!-- name of smallgraphs html file to (partly) generate -->
<xsl:param name="graphshtml">smallgraphs.html</xsl:param>
<!-- name of tail of the smallgraphs html file (with configs/families) -->
<xsl:param name="tailfile">data/smallgraphstail.html</xsl:param>
<!-- Root of main input document -->
<xsl:variable name="theroot" select="/"/>

<!-- Return the number of edges in a smallgraph edges element -->
<xsl:function name="teo:edgecount" as="xs:integer">
   <xsl:param name="edges" as="xs:string"/>
   <xsl:sequence select="count(tokenize($edges, ';')) - 1"/>
</xsl:function>

<!-- Return the minimum of edgecount and its complement -->
<xsl:function name="teo:countMinEdges" as="xs:integer">
   <xsl:param name="nodecount" as="xs:integer"/>
   <xsl:param name="edgecount" as="xs:integer"/>
   <xsl:variable name="kn" as="xs:integer"
	 select="($nodecount * ($nodecount - 1)) idiv 2"/>
   <xsl:sequence select="min(($edgecount, $kn - $edgecount))"/>
</xsl:function>

<!-- Create the collating version of a smallgraph name. All this does is
  - replace X_{..} by X_{0..}.
  -->
<xsl:function name="teo:coll" as="xs:string">
   <xsl:param name="s"/>
   <xsl:sequence select="replace(replace($s, 'X([CFZ]?)_\{([^}]{2})\}', 'X$1_{0$2}'),
	'X([CFZ]?)_([^{])', 'X$1_{00$2}') "/>
</xsl:function>

<!-- Return the graph6 representation of the given simple smallgraph
  -  (complopt='C'), or of its complement (complopt='')
  -->
<xsl:function name="teo:graph6" as="xs:string">
   <xsl:param name="g"/>
   <xsl:param name="complopt"/>
   <xsl:variable name="res" select="XsltUtil:systemOut(concat(
	 'sgt -6qC', $complopt, ' ',
	 substring-after(base-uri($theroot), ':'), ' ',
	 '''', $g, ''''))"/>
   <xsl:if test="not($res)">
      <xsl:message>Cannot convert <xsl:value-of select="$g"/> to graph6.</xsl:message>
   </xsl:if>
   <xsl:sequence select="$res"/>
</xsl:function>

<xsl:include href="lib.xsl"/>

<xsl:template match="SMALLGRAPHS">
   <xsl:apply-templates select="." mode="link"/>

   <!-- Create smallgraphs for Sage file -->
   <xsl:result-document href="{$sagefile}" method="text" indent="no"
	 encoding="utf-8">
      <xsl:apply-templates select=
	    "simple|(simple/complement[@name!=../@name])" mode="sage"/>
   </xsl:result-document>

   <!-- Create smallgraphs.html -->
   <xsl:result-document format="html" href="{teo:file('',$graphshtml)}">
   <html>
   <head>
   <title>List of small graphs</title>
   <link rel="stylesheet" type="text/css" href="{teo:url('','global.css')}"/>
   <link rel="stylesheet" type="text/css" href="{teo:url('','data.css')}"/>
   <link rel="shortcut icon" type="image/x-icon" href="{
      teo:url('','favicon.ico')}"/>
   <link rel="canonical" href="{teo:fullurl('',$graphshtml)}"/>
   </head>
   <body id="smallgraphpage">
   <xsl:call-template name="header"/>
   <div id="NavigationBox">
      <xsl:call-template name="search"/>
      <xsl:call-template name="mainmenu"/>
   </div>
  
   <!-- Table of contents -->
   <h2 id="contents">Contents</h2>
   <ul>
      <li><a href="#alphabet">Graphs ordered alphabetically</a></li>
      <li><a href="#order_by_number">Graphs ordered by number of vertices</a>
	 <ul>
	 <xsl:for-each-group select="simple" group-by="nodes/@count">
	    <xsl:sort select="nodes/@count" data-type="number"/>
	    <li><a href="#nodes{nodes/@count}">
	       <xsl:value-of select="nodes/@count"/> vertices</a>
	    </li>
	 </xsl:for-each-group>
	 </ul>
      </li>
      <li><a href="#forbidden_configurations_XC">Configurations XC</a></li>
      <li><a href="#forbidden_configurations_XZ">Configurations XZ</a></li>
      <li><a href="#families_XF">Families XF</a></li>
      <li><a href="#families">General families</a></li>
   </ul>

   <!-- Create alphabetical list of simples -->
   <h2 id="alphabet">Graphs ordered alphabetically</h2>
   <p>Note that complements are usually not listed. So for e.g. co-fork,
   look for fork.</p>
   <xsl:variable name="roughsimples" select="
	 simple[expl|complement/expl]|
	 simple[expl|complement/expl]/alias|
	 simple[expl|complement/expl]/complement[@name!=../@name]|
	 simple[expl|complement/expl]/complement/alias|
	 configuration|
	 family[expl]|
	 family[expl]/alias|
	 family/complement[expl]|
	 family/complement[expl]/alias|
	 fakefamily|
	 fakefamily/alias"/>
   <xsl:variable name="simples">
      <xsl:for-each select="$roughsimples">
	 <xsl:sort select="teo:coll(@name)"/>
	 <xsl:if test="
	    not(
	       starts-with(@name, '\co{') and
	       ends-with(@name, '}') and
	       $roughsimples[@name eq substring(current()/@name, 5,
		     string-length(current()/@name)-5)]
	    ) and
	    not(
	       starts-with(@name, 'co-') and
		  $roughsimples[@name eq substring(current()/@name, 4)]
	    )">
	    <xsl:element name="simpleitem">
	       <xsl:attribute name="name">
		  <xsl:value-of select="@name"/>
	       </xsl:attribute>
	       <xsl:attribute name="link">
		  <xsl:value-of select="
		  (ancestor-or-self::*[exists(../..)]/link)[last()]/@address"/>
	       </xsl:attribute>
	    </xsl:element>
	 </xsl:if>
      </xsl:for-each>
   </xsl:variable>
   <xsl:variable name="columns" select="5"/>
   <xsl:variable name="tablen"
	 select="ceiling(count($simples/*) div $columns)"/>

   <div class="alfacolumns">
      <xsl:for-each-group select="$simples/*"
	    group-by="(position()-1) idiv $tablen">
	 <ul class="alfacolumn">
	    <xsl:for-each select="current-group()">
	       <li><xsl:apply-templates select="." mode="href"/></li>
	    </xsl:for-each>
	 </ul>
      </xsl:for-each-group>
   </div>

      <!-- The graphs ordered by node/edgecount -->
      <h2 id="order_by_number">Graphs ordered by number of vertices</h2>

      <xsl:for-each-group select="simple" group-by="nodes/@count">
	 <xsl:sort select="nodes/@count" data-type="number"/>
	 <xsl:text>&#xa;</xsl:text>
	 <h3 id="nodes{nodes/@count}">
	    <xsl:value-of select="nodes/@count"/> vertices
	    <span class="plaintext"> - Graphs are ordered by increasing number
	    of edges in the left column.</span>
	 </h3>
	 <xsl:text>&#xa;</xsl:text>
	 <xsl:for-each select="current-group()">
	    <xsl:sort select="teo:countMinEdges(nodes/@count,
		  teo:edgecount(edges))"
		  data-type="number"/>
	    <xsl:if test="expl|complement/expl">
	       <div id="{link/@address}" class="graphpair">
		  <xsl:choose>
		     <xsl:when test="teo:edgecount(edges) le
			   teo:countMinEdges(nodes/@count,
			      teo:edgecount(edges))">
			<div class="leftgraph">
			   <xsl:apply-templates select="." mode="names"/>
			   <xsl:apply-templates select="expl"/>
			</div>
			<div class="rightgraph">
			<xsl:if test="@name != complement/@name">
			   <xsl:apply-templates select="complement" mode="names"/>
			</xsl:if>
			<xsl:apply-templates select="complement/expl"/>
			</div>
		     </xsl:when>
		     <xsl:otherwise>
			<div class="leftgraph">
			   <xsl:apply-templates select="complement" mode="names"/>
			   <xsl:apply-templates select="complement/expl"/>
			</div>
			<div class="rightgraph">
			   <xsl:if test="@name != complement/@name">
			      <xsl:apply-templates select="." mode="names"/>
			   </xsl:if>
			   <xsl:apply-templates select="expl"/>
			</div>
		     </xsl:otherwise>
		  </xsl:choose>
	       </div>
	       <xsl:call-template name="toplink"/>
	    </xsl:if>
	 </xsl:for-each>
      </xsl:for-each-group>

      <!-- The configurations XC -->
      <h3 id="forbidden_configurations_XC">Configurations XC</h3>
      <p>
	 A <i>configuration</i> XC represents a family of graphs by specifying
	 edges that must be present (solid lines), edges that must not be
	 present (dotted lines), and edges that may or may not be present (not
	 drawn). For example,
	 <a href="#XC1">XC<sub>1</sub></a> represents
	 <a href="#W4">W<sub>4</sub></a>,
	 <a href="#gem">gem</a>.
      </p>

      <xsl:for-each select="configuration[starts-with(@name, 'XC')]">
	 <xsl:sort select="teo:coll(@name)"/>
	 <xsl:apply-templates select="."/>
      </xsl:for-each>

      <!-- The configurations XZ -->
      <h3 id="forbidden_configurations_XZ">Configurations XZ</h3>
      <p>
	 A <i>configuration</i> XZ represents a family of graphs by specifying
	 edges that must be present (solid lines), edges that must not be
	 present (not drawn), and edges that may or may not be present (red
	 dotted lines).
      </p>

      <xsl:for-each select="configuration[starts-with(@name, 'XZ')]">
	 <xsl:sort select="teo:coll(@name)"/>
	 <xsl:apply-templates select="."/>
      </xsl:for-each>

      <!-- The families XF -->
      <h3 id="families_XF">Families XF</h3>
      <p>
	 Families are normally specified as
	 XF<sub><i>i</i></sub><sup><i>f(n)</i></sup> where <i>n</i> implicitly
	 starts from 0. For example, XF<sub>1</sub><sup><i>2n+3</i></sup> is
	 the set XF<sub>1</sub><sup>3</sup>, XF<sub>1</sub><sup>5</sup>,
	 XF<sub>1</sub><sup>7</sup>...
      </p>

      <xsl:for-each select="fakefamily[starts-with(@name, 'XF')]|
	    family[starts-with(@name, 'XF')]">
	 <xsl:sort select="teo:coll(@name)"/>
	 <xsl:apply-templates select="."/>
      </xsl:for-each>

      <!-- Other families -->
      <h3 id="families">General families</h3>
      <xsl:for-each select="fakefamily[not(starts-with(@name, 'XF'))]|
	    family[not(starts-with(@name, 'XF'))][expl]|
	    family/complement[expl]">
	 <xsl:sort select="teo:coll(@name)"/>
	 <xsl:apply-templates select="."/>
      </xsl:for-each>

      <xsl:call-template name="footer">
	 <xsl:with-param name="page" select="$graphshtml"/>
      </xsl:call-template>
   </body>
   </html>
   </xsl:result-document>
</xsl:template>

<!--
<xsl:template match="simple|complement">
   <xsl:apply-templates select="expl"/>
</xsl:template>
-->

<xsl:template match="simple|complement" mode="names">
   <h4>
      <xsl:if test="expl">
	 <xsl:apply-templates select="." mode="ref"/>
	 <xsl:for-each select="alias">
	    = <xsl:apply-templates select="." mode="ref"/>
	 </xsl:for-each>
	 <xsl:apply-templates select="." mode="graph6"/>
      </xsl:if>
   </h4>
</xsl:template>

<xsl:template match="simple" mode="graph6">
   <span class="graph6"><xsl:value-of select="teo:graph6(@name, 'C')"/></span>
</xsl:template>

<xsl:template match="simple/complement" mode="graph6">
   <span class="graph6"><xsl:value-of select="teo:graph6(../@name, '')"/></span>
</xsl:template>


<xsl:template match="simple" mode="sage">
   <xsl:apply-templates select="@name"/>
   <xsl:text>&#09;</xsl:text>
   <xsl:value-of select="teo:graph6(@name, 'C')"/>
</xsl:template>

<xsl:template match="simple/complement" mode="sage">
   <xsl:apply-templates select="@name"/>
   <xsl:text>&#09;</xsl:text>
   <xsl:value-of select="teo:graph6(../@name, '')"/>
</xsl:template>


<xsl:template match="simpleitem" mode="href">
   <a href="#{@link}"><xsl:value-of disable-output-escaping="yes" select="XsltUtil:latex(@name)"/></a>
</xsl:template>


<xsl:template match="configuration">
   <div class="graphpair">
      <h4 id="{link/@address}"><xsl:apply-templates select="." mode="ref"/></h4>
      <xsl:apply-templates select="expl"/>
   </div>
   <xsl:call-template name="toplink"/>
</xsl:template>


<xsl:template match="family[expl]|family/complement[expl]|fakefamily">
   <div class="graphpair">
      <h4 id="{link/@address}">
	 <xsl:apply-templates select="." mode="ref"/>
	 <xsl:for-each select="alias">
	    = <xsl:apply-templates select="." mode="ref"/>
	 </xsl:for-each>
      </h4>
      <xsl:apply-templates select="expl"/>
   </div>
   <xsl:call-template name="toplink"/>
</xsl:template>

<xsl:template match="simple|complement|alias|configuration|family|fakefamily"
      mode="href">
   <a href="#{ancestor-or-self::*[exists(../..)]/link/@address}"><xsl:apply-templates select="." mode="ref"/></a>
</xsl:template>


<xsl:template match="simple|complement|alias|configuration|family|fakefamily"
      mode="ref">
   <xsl:value-of disable-output-escaping="yes" select="XsltUtil:latex(@name)"/>
</xsl:template>


<xsl:template match="expl">
   <xsl:if test="xfig">
      <img src="{teo:url($imagedir, teo:gifname(xfig/@file))}"
	    alt="{xfig/@file}"/>
      <xsl:apply-templates select="xfig" mode="gif"/>
   </xsl:if>
   <xsl:apply-templates/>
</xsl:template>

<xsl:template match="expl//*" priority="-1">
   <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates/>
   </xsl:copy>
</xsl:template>

<xsl:template match="xfig"/>

<xsl:template match="xfig" mode="gif">
   <xsl:value-of select="XsltUtil:systemIn(concat($fig2gif, '>', teo:file($imagedir, teo:gifname(@file))), .)"/>
</xsl:template>

</xsl:stylesheet>

<!-- EOF -->
