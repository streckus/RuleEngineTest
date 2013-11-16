#!/usr/bin/perl -- -*-perl-*-


# Define fairly-constants

# This should match the mail program on your system.
$mailprog = '/usr/sbin/sendmail';

$recipient='feedback@graphclasses.org';
$recipientcc="";

# Get the recipient
#($name, $value) = split(/=/, $ENV{'QUERY_STRING'});
#$FORM{$name} = $value;

# Get the input
read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});

# Split the name-value pairs
@pairs = split(/&/, $buffer);

foreach $pair (@pairs)
{
    ($name, $value) = split(/=/, $pair);

    # Un-Webify plus signs and %-encoding
    $value =~ tr/+/ /;
    $value =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;

    # Stop people from using subshells to execute commands
    # Not a big deal when using sendmail, but very important
    # when using UCB mail (aka mailx).
    # $value =~ s/~!/ ~!/g;

    # Uncomment for debugging purposes
    # print "Setting $name to $value<P>";

    $FORM{$name} = $value;
}

$hamorspam = 'spam';
if ($FORM{'hamorspam1'} eq 'ham'  &&  $FORM{'hamorspam2'} ne 'spam') {
   $hamorspam = 'ham';
}

# Print out a content-type for HTTP/1.0 compatibility
print "Content-type: text/html\n\n";

$empty_form=0;
foreach $key (keys %FORM) {
        if ($FORM{$key} ne "") {
                $empty_form=1;
                break;
        }
}

if ($empty_form==0  ||  $hamorspam eq 'spam') {
        print << "End";
<html><head><title>Error</title></head>
<body>
<h1>Error</h1>
Please try again!
</center>
</body>
</html>

End
        exit(1);
}

print <<"END";
<Title>Thank You</title>
<body>
<h1>Thank you for contacting us!</h1>
We will analyze this.
</body>
END


if ($FORM{'email'} eq "") {$from="From: nobody";}
else
{$from="From: $FORM{'email'}";}

open (MAIL, "|$mailprog -t  $recipient") || die "Can't open $mailprog!\n";

print MAIL <<"END";
$from
To: $recipient
Cc: $recipientcc
Subject: mail an omission

The missing class/inclusion/algorithm:
$FORM{'miss'}

Refererence to the literature describing this or a short sketch of proof:
$FORM{'proof'}

Email: $FORM{'email'}


----------------------------------------------------------------------
Remote-Host: $ENV{'REMOTE_HOST'} ($ENV{'REMOTE_ADDR'})
END
close(MAIL);
