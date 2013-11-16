<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xs="http://www.w3.org/2001/XMLSchema"
   xmlns:y="http://www.yworks.com/xml/graphml"
   extension-element-prefixes="xs">
<!-- $Header: /home/ux/CVSROOT/teo/smallgraphs-yed.xsl,v 1.4 2011/10/17 19:43:18 ux Exp $ -->
<xsl:output method="xml" indent="yes" encoding="utf-8"/>
<xsl:strip-space elements="*"/>

<xsl:template match="SMALLGRAPHS">
   <graphml
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns 
      http://www.yworks.com/xml/schema/graphml/1.0/ygraphml.xsd">
   <key id="d0" for="node" yfiles.type="nodegraphics"/>
   <key id="e0" for="edge" yfiles.type="edgegraphics"/>
   <graph id="Families" edgedefault="directed">
      <xsl:apply-templates/>
   </graph>
   </graphml>
</xsl:template>

<xsl:template match="family">
   <xsl:call-template name="node">
      <xsl:with-param name="name" select="@name"/>
      <xsl:with-param name="type" select="@type"/>
   </xsl:call-template>
   <xsl:apply-templates/>
</xsl:template>

<xsl:template match="simple|configuration|complement|alias">
   <xsl:if test="//SMALLGRAPHS/family/(contains|subfamily|(induced/smallgraph)|induced1|(induced-rest/smallgraph)|induced-rest1)
	    [text()=current()/@name]">
      <xsl:call-template name="node">
	 <xsl:with-param name="name" select="@name"/>
	 <xsl:with-param name="type" select="''"/>
      </xsl:call-template>
   </xsl:if>
   <xsl:apply-templates select="alias|(complement[@name!=current()/@name])"/>
</xsl:template>

<xsl:template match="contains|induced-rest1|induced1|subfamily">
   <xsl:call-template name="edge">
      <xsl:with-param name="src" select="text()"/>
      <xsl:with-param name="dest" select="../@name"/>
      <xsl:with-param name="type" select="replace(local-name(.), '1$', '')"/>
   </xsl:call-template>
</xsl:template>

<xsl:template match="induced|induced-rest">
   <xsl:variable name="this" select="string-join(smallgraph/text(), ';')"/>
   <xsl:call-template name="node">
      <xsl:with-param name="name" select="$this"/>
      <xsl:with-param name="type" select="'union'"/>
   </xsl:call-template>
   <xsl:for-each select="smallgraph">
      <xsl:call-template name="edge">
	 <xsl:with-param name="src" select="."/>
	 <xsl:with-param name="dest" select="$this"/>
	 <xsl:with-param name="type" select="'subfamily'"/>
      </xsl:call-template>
   </xsl:for-each>
   <xsl:call-template name="edge">
      <xsl:with-param name="src" select="$this"/>
      <xsl:with-param name="dest" select="../@name"/>
      <xsl:with-param name="type" select="local-name(.)"/>
   </xsl:call-template>
</xsl:template>

<xsl:template match="*" priority="-1"/>

<!-- Output templates -->

<xsl:template name="node">
   <xsl:param name="name" as="xs:string"/>
   <xsl:param name="type" as="xs:string"/>
   <node id="{$name}">
      <data key="d0">
	 <y:ShapeNode>
	    <xsl:choose>
	       <xsl:when test="$type eq 'union'">
		  <y:Shape type="trapezoid2"/>
	       </xsl:when>
	       <xsl:when test="$type eq 'simple'">
		  <y:Shape type="ellipse"/>
	       </xsl:when>
	       <xsl:when test="$type eq ''">
		  <y:Fill hasColor="false"/>
		  <y:Shape type="ellipse"/>
	       </xsl:when>
	       <xsl:otherwise>
		  <y:Shape type="rectangle"/>
	       </xsl:otherwise>
	    </xsl:choose>
	    <y:NodeLabel>
	       <xsl:value-of select="$name"/>
	       <xsl:if test="$type">
		  <xsl:text>&#x0a;</xsl:text>
		  <xsl:value-of select="$type"/>
	       </xsl:if>
	    </y:NodeLabel>
	 </y:ShapeNode>
      </data>
   </node>
</xsl:template>

<xsl:template name="edge">
   <xsl:param name="src" as="xs:string"/>
   <xsl:param name="dest" as="xs:string"/>
   <xsl:param name="type" as="xs:string"/>
   <edge source="{$src}" target="{$dest}">
      <data key="e0">
	 <y:PolyLineEdge>
	    <xsl:choose>
	       <xsl:when test="$type eq 'induced'">
		  <y:LineStyle type="dashed"/>
	       </xsl:when>
	       <xsl:when test="$type eq 'induced-rest'">
		  <y:LineStyle type="dashed_dotted"/>
	       </xsl:when>
	       <xsl:otherwise>
		  <y:LineStyle type="line"/>
	       </xsl:otherwise>
	    </xsl:choose>
	    <xsl:if test="$type ne 'subfamily'">
	       <y:EdgeLabel><xsl:value-of select="$type"/></y:EdgeLabel>
	    </xsl:if>
	 </y:PolyLineEdge>
      </data>
   </edge>
</xsl:template>

</xsl:stylesheet>
<!-- EOF -->
