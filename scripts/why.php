#!/usr/bin/php

<?php
error_reporting(E_ALL);

$mysqli = new mysqli("127.0.0.1", "root", "root", "ISGCI", 3306);
if ($mysqli->connect_errno) {
        echo "Failed to connect to MySQL: (" . $mysqli->connect_errno . ") " . $mysqli->connect_error;
}

function printwhy($mysqli, $sub, $super, $prefix) {
    $sql = "SELECT * FROM TraceData WHERE sub = '$sub' AND super = '$super'";
    $result = $mysqli->query($sql);
    $row = $result->fetch_assoc();

    echo $prefix . $row['sub'] . " -> " . $row['super'] . "    " . $row['type'] . "\n";
    
    $dependencies = json_decode($row['dependencies']);
    foreach ($dependencies as $dependency) {
        printwhy($mysqli, $dependency->sub, $dependency->sup, $prefix . "    ");
    }
}

printwhy($mysqli, 1, 16, "");
