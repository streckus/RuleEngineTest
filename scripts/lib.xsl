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
   xmlns:XsltUtil="java:teo.isgci.appl.XsltUtil"
   extension-element-prefixes="xs saxon teo XsltUtil">

<!--
  - Return a valid HTML id generate from the given name (string).
 -->
<xsl:function name="teo:name2id">
   <xsl:param name="name"/>
   <xsl:sequence select="concat('i', translate($name,' ','-'))"/>
</xsl:function>

<!-- Prefix to add to the generated gifs to create a relative ref to a file -->
<xsl:param name="imgprefix">g_</xsl:param>

<!-- Return the filename of the gif file with the given basename (relative) -->
<xsl:function name="teo:gifname" as="xs:string">
   <xsl:param name="s" as="xs:string"/>
   <xsl:sequence select="concat($imgprefix, $s, '.gif')"/>
</xsl:function>

<!-- Return the number part of a reference -->
<xsl:function name="teo:refnum">
   <xsl:param name="s"/>
   <xsl:sequence select="
      if (matches($s, '^[0-9]+$'))
      then $s
      else substring-after($s, 'ref_')"/>
</xsl:function>

<!-- Turn a ref id into a number -->
<xsl:function name="teo:ref2int" as="xs:integer">
   <xsl:param name="ref" as="xs:string"/>
   <xsl:sequence select="xs:integer(teo:refnum($ref))"/>
</xsl:function>

<!-- ************************* Smallgraph links ************************** -->

<!-- Document containing the links for smallgraphs -->
<!--<xsl:param name="graphlinks">build/data/graphlinks.xml</xsl:param> -->
<xsl:variable name="smallgraphs" select="doc($graphlinks)"/>

<xsl:key name="smallgraph" match="SMALLGRAPHS/link" use="@name"/>

<!-- Name of a small graph -->
<xsl:template match="smallgraph">
   <xsl:if test="not(key('smallgraph', ., $smallgraphs))">
      <xsl:message>
      Smallgraph link for <xsl:value-of select="."/> not found</xsl:message>
   </xsl:if>
   <xsl:variable name="file" select="
      if (ends-with(base-uri(), 'smallgraphs.xml'))
      then ''
      else teo:url('', 'smallgraphs.html')"/>
   <a href="{$file}#{key('smallgraph', ., $smallgraphs)[last()]/@address}">
   <xsl:value-of disable-output-escaping="yes" select="XsltUtil:latex(.)"/>
   </a>
   <xsl:text> </xsl:text>
</xsl:template>


<!-- ************************** URLs and paths *************************** -->
<!--
  - Join the three path components with / between them and return the result.
 -->
<xsl:function name="teo:pathjoin">
   <xsl:param name="root"/>
   <xsl:param name="path"/>
   <xsl:param name="basename"/>
   <xsl:sequence select="
      if ($basename)
      then string-join(($root,
         if ($path)
         then string-join(($path,$basename), '/')
         else $basename), '/')
      else $homeurl"/>
</xsl:function>


<!--
  - Return the (base) filename for the given problem.
 -->
<xsl:function name="teo:problemfile">
   <xsl:param name="problem"/>
   <xsl:sequence select="concat('problem_',translate($problem, ' ', '_'),
         '.html')"/>
</xsl:function>

<!--
  - Return the full (absolute) url for the given basename.
 -->
<xsl:function name="teo:fullurl">
   <xsl:param name="path"/>
   <xsl:param name="basename"/>
   <xsl:sequence select="teo:pathjoin($homeurl, $path, $basename)"/>
</xsl:function>

<!--
  - Return the webserver-relative (rooted) url for the given basename.
 -->
<xsl:function name="teo:url">
   <xsl:param name="path"/>
   <xsl:param name="basename"/>
   <xsl:sequence select="teo:pathjoin($rooturl, $path, $basename)"/>
</xsl:function>

<!--
  - Return the full path for the given basename.
 -->
<xsl:function name="teo:file">
   <xsl:param name="path"/>
   <xsl:param name="basename"/>
   <xsl:sequence select="teo:pathjoin($homedir, $path, $basename)"/>
</xsl:function>

<!-- **************** Generic items (header,menu,footer) **************** -->

<!-- MathJax activation and configuration -->
<xsl:template name="mathjax">
   <script type="text/x-mathjax-config">
      MathJax.Hub.Config({
         tex2jax: {inlineMath: [ ['$','$'], ["\((","\))"] ]}
      });
   </script>
   <script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"/>
</xsl:template>

<!-- Back to top link -->
<xsl:template name="toplink">
   <div class="toplink"><a href="#header">back to top</a></div>
</xsl:template>

<!-- Web page header -->
<xsl:template name="header">
   <div id="header">
      <a href="{teo:url('','index.html')}"><img src="{
            teo:url('','logo75.png')}" alt="ISGCI logo"/></a>
      <div>Information System on Graph Classes and their Inclusions</div>
   </div>
</xsl:template>


<!-- Search box -->
<xsl:template name="search">
   <form id="search" action="{teo:url('','classes.cgi')}" method="get"><div>
      <label> Find class
         <input type="text" name="search" size="12" maxlength="50"/>
      </label>
   </div></form>
</xsl:template>


<!-- Main menu -->
<xsl:template name="mainmenu">
   <ul class="navigation">
      <li class="title">Global</li>
      <li><a href="{teo:url('','index.html')}">ISGCI&nbsp;home</a></li>
      <li><a href="{teo:url('','isgci.jnlp')}">Java</a></li>
      <li><a href="{teo:url('','classes.cgi')}">All&nbsp;classes</a></li>
      <li id="refmenu"><a href="{teo:url('classes','refs00.html')}">References</a></li>
      <li id="smallgraphmenu"><a href="{teo:url('','smallgraphs.html')}">Smallgraphs</a></li>
      <li><a href="{teo:url('','contact.html')}">&#9993;</a></li>
   </ul>
</xsl:template>

<!-- Web page footer -->
<xsl:template name="footer">
   <xsl:param name="path"/>
   <xsl:param name="page"/>
   <xsl:param name="style"/>
   <div id="footer">
      <xsl:if test="$style">
         <xsl:attribute name="style">
            <xsl:value-of select="$style"/>
         </xsl:attribute>
      </xsl:if>
      <xsl:value-of select="teo:fullurl($path, $page)"/><br/>
      <xsl:if test="$page">part of the</xsl:if>
      Information System on Graph Classes and their Inclusions (ISGCI)<br/>
      by H.N. de Ridder et al.
      2001-<xsl:value-of select="string(format-date(current-date(), '[Y]'))"/>
      updated
      <xsl:value-of select="format-date(current-date(), '[D] [MNn] [Y]')"/>
      <a rel="license" style="margin-left:1ex" href="http://creativecommons.org/licenses/by-sa/3.0/"><img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br/>
   </div>
</xsl:template>

</xsl:stylesheet>

<!-- EOF -->
