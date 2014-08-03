<?php
require 'db.inc.php';
require 'why';

$id1 = $_POST['id_1'];
$id2 = $_POST['id_2'];

echo printwhy($mysqli, $id1, $id2);
