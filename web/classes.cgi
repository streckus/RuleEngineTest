#!/usr/bin/perl -- -*-perl-*-
use CGI qw(param, escapeHTML);

# option for search.cgi
my $opts="lh";

my $sterm=escapeHTML(param("search"));

print "Content-type: text/html\n\n";
while (<DATA>) {
   print $_;
}
print qq/<form action="classes.cgi" method="get">\n/;
print qq/Filter:&nbsp;&nbsp; <input name="search" type="text" size="50" maxlength="200" value="$sterm"\/>/;
print qq/<\/form>/;
print qq/<br>/;

if (!$ENV{'QUERY_STRING'}) {
   $ENV{'QUERY_STRING'} = "search=";
}
if (index($ENV{'QUERY_STRING'}, "options") < 0) {
   $ENV{'QUERY_STRING'} .= "&options=$opts";
}
system("./search.cgi");

print "<hr>\n   </body>\n</html>\n";

__DATA__
<!DOCTYPE html
  PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
   <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
      <title>List of graph classes</title>
      <link rel="stylesheet" type="text/css" href="global.css">
      <link rel="stylesheet" type="text/css" href="data.css">
      <link rel="shortcut icon" type="image/x-icon" href="favicon.ico">
      <link rel="canonical" href="http://www.graphclasses.org/classes.cgi">
   </head>
   <body>
      <div id="header"><a href="index.html"><img src="logo75.png" alt="ISGCI logo"></a><div>Information System on Graph Classes and their Inclusions</div>
      </div>
      <div id="NavigationBox">
         <ul class="navigation">
            <li class="title">Global</li>
            <li><a href="index.html">ISGCI&nbsp;home</a></li>
            <li><a href="isgci.jnlp">Java</a></li>
            <li id="refmenu"><a href="classes/refs00.html">References</a></li>
            <li id="smallgraphmenu"><a href="smallgraphs.html">Smallgraphs</a></li>
            <li><a href="contact.html">&#9993;</a></li>
         </ul>
      </div>
   To limit the display of classes, enter one or more keywords in the filter
   box and press ENTER. Only classes that contain all given keywords
   anywhere in their name or definition will be displayed. To list all
   available classes, clear the filter box and press ENTER.<p>
   Filtering is case insensitive and the filtering process has some limited
   knowledge about graph names. E.g. if you filter on 'chair', 'fork' will
   also be found. Use LaTeX notation for super-/subscripts e.g. K_3 for
   K<sub>3</sub> or S_{1,1,2} for S<sub>1,1,2</sub> and for intersection/union
   (cap/cup).<p>

