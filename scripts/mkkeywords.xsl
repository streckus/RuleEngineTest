<xsl:stylesheet version="2.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:teo="http://www.graphclasses.org"
   xmlns:XsltUtil="java:XsltUtil"
   extension-element-prefixes="teo XsltUtil">
<!-- $Header: /home/ux/CVSROOT/teo/mkkeywords.xsl,v 1.4 2011/04/07 07:28:27 ux Exp $ -->

<xsl:output method="text" encoding="utf-8"/>
<xsl:strip-space elements="*"/>
<xsl:key name="graphclass" match="GraphClass" use="@id"/>

<!-- Root of main input document -->
<xsl:variable name="theroot" select="/"/>

<xsl:function name="teo:graphclass">
   <xsl:param name="gc"/>
   <xsl:variable name="set" select="key('graphclass', $gc, $theroot)"/>
   <xsl:if test="not($set) and not(starts-with($gc, 'AUTO_'))">
      <xsl:message>Graphclass <xsl:value-of select="$gc"/> not found.</xsl:message>
   </xsl:if>
   <xsl:sequence select="$set"/>
</xsl:function>


<!-- Output directory -->
<xsl:param name="dir">.</xsl:param>
<!-- File with smallgraphs -->
<xsl:param name="smallgraphs">data/smallgraphs.xml</xsl:param>


<xsl:template match="GraphClasses">
   <xsl:apply-templates/>
</xsl:template>

<xsl:template match="GraphClass">
   <xsl:result-document href="{$dir}/{@id}" method="text" encoding="utf-8">
      <xsl:value-of select="name"/>
      <xsl:text disable-output-escaping="yes">&#xA;</xsl:text>
      <xsl:if test="note[@name='definition']">
         <xsl:apply-templates select="note[@name='definition']"/>
      </xsl:if>
      <xsl:if test="note[@name='keywords']">
         <xsl:apply-templates select="note[@name='keywords']"/>
      </xsl:if>
      <xsl:apply-templates select="smallgraph"/>
   </xsl:result-document>
</xsl:template>

<xsl:template match="GraphClass" mode="ref">
   <xsl:value-of disable-output-escaping="yes" select="name"/>
</xsl:template>

<!-- *******************************************************************
  - Notes.
  -->
<xsl:template match="note">
   <xsl:apply-templates/>
</xsl:template>

<xsl:template match="note//*" priority="-0.5">
   <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates/>
   </xsl:copy>
</xsl:template>

<xsl:template match="graphclass">
   <xsl:apply-templates select="teo:graphclass(.)" mode ="ref"/>
</xsl:template>

<!-- Name of a small graph -->
<xsl:template match="smallgraph">
   <xsl:value-of disable-output-escaping="yes" select="."/>
   <xsl:text> </xsl:text>
   <xsl:apply-templates select=
      "document($smallgraphs)/SMALLGRAPHS/simple[@name=current()]" mode="ref"/>
   <xsl:apply-templates select=
      "document($smallgraphs)/SMALLGRAPHS/simple/complement[@name=current()]"
      mode="ref"/>
   <xsl:text> </xsl:text>
</xsl:template>

<xsl:template match="simple" mode="ref">
   <xsl:value-of disable-output-escaping="yes" select="@name"/>
   <xsl:text> </xsl:text>
   <xsl:value-of disable-output-escaping="yes" select="alias/@name"/>
   <xsl:text> </xsl:text>
</xsl:template>

<xsl:template match="complement" mode="ref">
   <xsl:value-of disable-output-escaping="yes" select="@name"/>
   <xsl:text> </xsl:text>
   <xsl:value-of disable-output-escaping="yes" select="alias/@name"/>
   <xsl:text> </xsl:text>
</xsl:template>

<xsl:template match="Inclusions"/>

</xsl:stylesheet>

<!-- EOF -->
