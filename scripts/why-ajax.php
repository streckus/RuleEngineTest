<?php
require 'db.inc.php';
require 'why.php';

$id1 = $_POST['id_1'];
$id2 = $_POST['id_2'];
$names = ($_POST['name'] == 1 ? true : false);

echo printwhy($mysqli, $id1, $id2, $names);