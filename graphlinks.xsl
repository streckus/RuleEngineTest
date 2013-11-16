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
   extension-element-prefixes="teo xs teo XsltUtil">
<!-- $Header: /home/ux/CVSROOT/teo/graphlinks.xsl,v 1.2 2012/01/20 19:26:33 ux Exp $ -->

<xsl:output method="xml" encoding="utf-8" doctype-system="smallgraphs.dtd"/>
<xsl:strip-space elements="*"/>

<!-- ISGCI homepage -->
<xsl:param name="homeurl" required="yes"/>
<!-- Output directory corresponding to this url -->
<xsl:param name="homedir" required="yes"/>
<!-- Absolute path on the webserver to the homedirectory, must NOT end in / -->
<xsl:param name="rooturl" required="yes"/>
<!-- Directory for images -->
<xsl:param name="imagedir">images</xsl:param>

<xsl:include href="lib.xsl"/>

<xsl:template match="SMALLGRAPHS">
   <xsl:apply-templates select="." mode="link"/>
</xsl:template>

<!-- *********************************************************************
  - mode=link creates info for links and inline images on class pages.
  -->
<xsl:template match="SMALLGRAPHS" mode="link">
   <SMALLGRAPHS>
   <xsl:text>&#xa;</xsl:text>
   <xsl:apply-templates select="*" mode="link"/>
   </SMALLGRAPHS>
</xsl:template>


<xsl:template match="simple|alias|family|configuration|fakefamily" mode="link">
   <xsl:apply-templates select="." mode="link-data"/>
   <xsl:text>&#xa;</xsl:text>
   <xsl:apply-templates select="alias|complement" mode="link"/>
</xsl:template>


<xsl:template match="complement" mode="link">
   <xsl:if test="@name!=../@name">
      <xsl:apply-templates select="." mode="link-data"/>
      <xsl:text>&#xa;</xsl:text>
   </xsl:if>
   <xsl:apply-templates select="alias" mode="link"/>
</xsl:template>

<xsl:template match="link" mode="link">
   <xsl:if test="not(@relay)">
      <xsl:copy-of select="."/>
   </xsl:if>
   <xsl:if test="@relay">
      <xsl:variable name="target" select="@relay"/>
      <xsl:variable name="relay"
	 select="/SMALLGRAPHS/*[@name=$target]|
	    /SMALLGRAPHS/*/complement[@name=$target]"/>
      <xsl:if test="not($relay)">
	 <xsl:message>Relay <xsl:value-of select="@relay"/> not found.</xsl:message>
      </xsl:if>
      <xsl:call-template name="linkrelay">
	 <xsl:with-param name="name" select="@name"/>
	 <xsl:with-param name="relay" select="$relay"/>
      </xsl:call-template>
   </xsl:if>
   <xsl:text>&#xa;</xsl:text>
</xsl:template>


<xsl:template name="linkrelay">
   <xsl:param name="name" required="yes"/>
   <xsl:param name="relay" required="yes"/>
   <xsl:choose>
      <xsl:when test="$relay/expl/xfig">
	 <link name="{$name}"
	       address="{$relay/link/@address}"
	       img="{teo:url($imagedir,
		  teo:gifname($relay/expl/xfig/@file))}"
	       imgname="{$relay/@name}"/>
      </xsl:when>
      <xsl:otherwise>
	 <link name="{$name}"
	       address="{$relay/link/@address}"/>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>


<xsl:template match="simple|complement|alias|family|configuration|fakefamily"
      mode="link-data">
   <xsl:variable name="link"
	 select="(ancestor-or-self::*[exists(../..)]/link)[last()]"/>
   <xsl:choose>
      <xsl:when test="expl/xfig">
	 <link name="{@name}"
	       address="{$link/@address}"
	       img="{teo:url($imagedir, teo:gifname(expl/xfig/@file))}"/>
      </xsl:when>
      <xsl:when test="../expl/xfig">
	 <link name="{@name}"
	       address="{$link/@address}"
	       img="{teo:url($imagedir, teo:gifname(../expl/xfig/@file))}"
	       imgname="{../@name}"/>
      </xsl:when>
      <xsl:when test="$link/@relay">
	 <xsl:variable name="relay"
	    select="/SMALLGRAPHS/*[@name=$link/@relay]|
	       /SMALLGRAPHS/*/complement[@name=$link/@relay]"/>
	 <xsl:if test="not($relay)">
	    <xsl:message>Relay <xsl:value-of select="$link/@relay"/> not found.</xsl:message>
	 </xsl:if>
	 <xsl:call-template name="linkrelay">
	    <xsl:with-param name="name" select="@name"/>
	    <xsl:with-param name="relay" select="$relay"/>
	 </xsl:call-template>
	 <!-- <xsl:choose>
	    <xsl:when test="$relay/expl/xfig">
	       <link name="{@name}"
		     address="{$relay/link/@address}"
		     img="{teo:url($imagedir,
			teo:gifname($relay/expl/xfig/@file))}"
		     imgname="{$relay/@name}"/>
	    </xsl:when>
	    <xsl:otherwise>
	       <link name="{@name}"
		     address="{$relay/link/@address}"/>
	    </xsl:otherwise>
	 </xsl:choose> -->
      </xsl:when>
      <xsl:when test="$link/@img">
	 <link name="{@name}"
	       address="{$link/@address}"
	       img="{teo:url($imagedir, $link/@img)}"
	       imgname="{$link/@imgname}"/>
      </xsl:when>
      <xsl:otherwise>
	 <link name="{@name}"
	       address="{$link/@address}"/>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<xsl:template match="text()" mode="link" priority="-1"/>

</xsl:stylesheet>

<!-- EOF -->
