<html>

<head>
<title>Why-Parser</title>

<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css">
<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap-theme.min.css">
<style>
.spinner {
  margin: 100px auto;
  width: 40px;
  height: 40px;
  position: relative;
}

.container1 > div, .container2 > div, .container3 > div {
  width: 9px;
  height: 9px;
  background-color: #333;

  border-radius: 100%;
  position: absolute;
  -webkit-animation: bouncedelay 1.2s infinite ease-in-out;
  animation: bouncedelay 1.2s infinite ease-in-out;
  /* Prevent first frame from flickering when animation starts */
  -webkit-animation-fill-mode: both;
  animation-fill-mode: both;
}

.spinner .spinner-container {
  position: absolute;
  width: 100%;
  height: 100%;
}

.container2 {
  -webkit-transform: rotateZ(45deg);
  transform: rotateZ(45deg);
}

.container3 {
  -webkit-transform: rotateZ(90deg);
  transform: rotateZ(90deg);
}

.circle1 { top: 0; left: 0; }
.circle2 { top: 0; right: 0; }
.circle3 { right: 0; bottom: 0; }
.circle4 { left: 0; bottom: 0; }

.container2 .circle1 {
  -webkit-animation-delay: -1.1s;
  animation-delay: -1.1s;
}

.container3 .circle1 {
  -webkit-animation-delay: -1.0s;
  animation-delay: -1.0s;
}

.container1 .circle2 {
  -webkit-animation-delay: -0.9s;
  animation-delay: -0.9s;
}

.container2 .circle2 {
  -webkit-animation-delay: -0.8s;
  animation-delay: -0.8s;
}

.container3 .circle2 {
  -webkit-animation-delay: -0.7s;
  animation-delay: -0.7s;
}

.container1 .circle3 {
  -webkit-animation-delay: -0.6s;
  animation-delay: -0.6s;
}

.container2 .circle3 {
  -webkit-animation-delay: -0.5s;
  animation-delay: -0.5s;
}

.container3 .circle3 {
  -webkit-animation-delay: -0.4s;
  animation-delay: -0.4s;
}

.container1 .circle4 {
  -webkit-animation-delay: -0.3s;
  animation-delay: -0.3s;
}

.container2 .circle4 {
  -webkit-animation-delay: -0.2s;
  animation-delay: -0.2s;
}

.container3 .circle4 {
  -webkit-animation-delay: -0.1s;
  animation-delay: -0.1s;
}

@-webkit-keyframes bouncedelay {
  0%, 80%, 100% { -webkit-transform: scale(0.0) }
  40% { -webkit-transform: scale(1.0) }
}

@keyframes bouncedelay {
  0%, 80%, 100% { 
    transform: scale(0.0);
    -webkit-transform: scale(0.0);
  } 40% { 
    transform: scale(1.0);
    -webkit-transform: scale(1.0);
  }
}
</style>
</head>
<body>
<?php
require 'why';
error_reporting(E_ALL);

$handle = fopen("../log.txt", "r");
if ($handle) {
    while (($line = fgets($handle)) !== false) {
        // Check for comment lines
        // Display them as headers
        if (startsWith($line, '#')) {
            echo "<h3>". $line . "</h3>";
            continue;
        }
        preg_match('/(?P<id1>\d+) -> (?P<id2>\d+)(?P<rest>.*)/', $line, $matches);
        $id1 = $matches['id1'];
        $id2 = $matches['id2'];
        echo $id1 . " -> " . $id2 . " " . $matches['rest'] . " <span onClick='triggerScript($id1,$id2)' style='margin-left:80px' data-toggle='modal' data-backdrop='static' data-target='#myModal' style='cursor:pointer'>>></span><br />\n";
        //echo $matches['id2'] . "<br />\n";
        //echo $line . "<br />\n";
    }
} else {
    echo "Could not open the log file. Please check everything is in place!";
} 
fclose($handle);


/**
 * Returns true iff the line starts with the character given in `needle` 
 */
function startsWith($haystack, $needle) {
    return $needle === "" || strpos($haystack, $needle) === 0;
}
?>
<!-- Modal -->
<div class="modal fade" id="myModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
        <h4 class="modal-title" id="myModalLabel">Why? Results</h4>
      </div>
      <div class="modal-body">
        <div class="spinner">
          <div class="spinner-container container1">
            <div class="circle1"></div>
            <div class="circle2"></div>
            <div class="circle3"></div>
            <div class="circle4"></div>
          </div>
          <div class="spinner-container container2">
            <div class="circle1"></div>
            <div class="circle2"></div>
            <div class="circle3"></div>
            <div class="circle4"></div>
          </div>
          <div class="spinner-container container3">
            <div class="circle1"></div>
            <div class="circle2"></div>
            <div class="circle3"></div>
            <div class="circle4"></div>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
      </div>
    </div>
  </div>
</div>


<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.0/jquery.min.js"></script>
<script>
function nl2br (str, is_xhtml) {   
    var breakTag = (is_xhtml || typeof is_xhtml === 'undefined') ? '<br />' : '<br>';    
    return (str + '').replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1'+ breakTag +'$2');
}
function saveSpaces(str) {
    return (str + '').replace(/\s/g, '&nbsp;');
}

var circles = " <div class='spinner'> <div class='spinner-container container1'> <div class='circle1'></div> <div class='circle2'></div> <div class='circle3'></div> <div class='circle4'></div> </div> <div class='spinner-container container2'> <div class='circle1'></div> <div class='circle2'></div> <div class='circle3'></div> <div class='circle4'></div> </div> <div class='spinner-container container3'> <div class='circle1'></div> <div class='circle2'></div> <div class='circle3'></div> <div class='circle4'></div> </div> </div>";
function triggerScript(id1, id2) {
    console.log(id1, id2);
    $('.modal-body').html(circles);
    $.ajax({ 
        type: "POST", 
        url: "why-ajax.php", 
        data: { 
            id_1: id1, 
            id_2: id2 
        } 
    })
    .done(function(msg) {
        $('.modal-body').html(nl2br(saveSpaces(msg),true));
    })
    .error(function(msg) {
        $('.modal-body').html('An error occured: ' + msg);
    });
}
</script>
<script src="//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js"></script>

</body>
</html>
