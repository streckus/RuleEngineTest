#!/usr/bin/perl -- -*-perl-*-
use CGI qw(param);

#Konfiguration:
#das Verzeichnis mit den Dateien
#$dir="@INSTALLDIR@/Keys";
$dir="./Keys";

#der Fehler, fuer den Fall, das eine Datei nicht gelesen werden kann
$file_not_found_error="Konnte Datei nicht oeffnen: $!\n";

#========================= Auxiliary functions ===========================
# Read in the pre-formatted html links of the classes
# We can't put this in a hashtable because the order is important!
sub readLinks() {
   $i=0;
   while (<DATA>) {
      $links[$i++] = $_;
   }
}


# die Namen aller vorhandener Datensaetze wiedergeben
sub returnAll(){
#DEBUG  print "returnAll()\n";
   if ($opt_l >= 0) {
      foreach (@links) {
         print $_;
      }
   } else {
      opendir(TT,$dir);
      while($datname=readdir(TT)){
         if($datname eq "." || $datname eq "..") {next;}
         open(DATEI,$dir."/".$datname) || die $file_not_found_error;
         # nur die erste Zeile ausgeben, dann abbrechen
         while(<DATEI>) {
            print "$_";
            last;
         }
         close(DATEI);
      }
   }
}

#================================= main ===================================
# Options:
# l: print classes as html links
# h: do not print html header
# suchwoerter mit Space getrennt im CGI-Parameter "search"
my $opt_l = index(param("options"), "l");
my $opt_h = index(param("options"), "h");
my $sterm=param("search");

readLinks();

#der HTML-Header
if ($opt_h < 0) {
   print "Content-type: text/html\n\n";
}
#print "<html><body>\n";

if($sterm eq "") {
   returnAll();
   exit(0);
}

# array mit den Suchwoertern
@terms=split(' ',$sterm);

$datnamesi=0;
opendir(TT,$dir);
while($datname=readdir(TT)){
   if($datname eq "." || $datname eq "..") {next;}
   open(DATEI,$dir."/".$datname) || die $file_not_found_error;
   #@found fuer alle Begriffe auf false setzen
   $i=0;
   while($i<scalar(@terms)) { $found[$i]=0; $i++; }
   $name='';            # in der ersten Zeile stehen die Namen der Klassen
   while(<DATEI>){
      $line=$_;
      chomp($line);
      #Namen speichern
      if($name eq '') { $name=$line; }
      $i=0;
      while($i<scalar(@terms)){#ueber die Suchbegriffe
         #wenn dieser Begriff schon in anderer Zeile gefunden weiter
         #if($found[$i]) { $i++; next; }
         #sonst in dieser Zeile nach diesem Begriff ($i) suchen
         #Gross/Kleinschreibung beachten hier
         if($line =~ /$terms[$i]/i) { 
            $found[$i]=1;       #Begriff gefunden
         }
         $i++;
      }
   }
   close(DATEI);
   #pruefen, ob alle gefunden (UND)
   #wenn alternativ ODER mgl sein soll, dann hier 
   $i=0; $erg=1;
   while($i<scalar(@found)) { $erg=$erg && $found[$i]; $i++; }
   if($erg) {      
      if ($opt_l >= 0) {
	 $datnames[$datnamesi] = $datname;
	 $datnamesi++;
      } else {
         print $name."\n"; 
      }
   }
}

if ($opt_l >= 0) {
   foreach $link (@links) {
      foreach $datname (@datnames) {
	 if (index($link, "$datname.html") > -1) {
	    print $link;
	 }
      }
   }
}
#print "</body></html>\n";

__END__
