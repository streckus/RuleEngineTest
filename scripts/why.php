<?php

function printwhy($mysqli, $sub, $super, $names, $prefix = "") {
    if ($names) {
        $sql = "SELECT sub, super, type, dependencies, tn1.name as sub_name, tn2.name as sup_name FROM TraceData, TraceNames as tn2, TraceNames as tn1 WHERE sub = '$sub' AND super = '$super' AND tn1.id = '$sub' AND tn2.id = '$super'";
    }
    else {
        $sql = "SELECT * FROM TraceData WHERE sub = '$sub' AND super = '$super'";
    }

    $result = $mysqli->query($sql);
    $row = $result->fetch_assoc();

    if ($names) {
        $res = $prefix . "(" . $row['sub'] . ") " . $row['sub_name'] . " -> (" . $row['super'] . ") " .  $row['sup_name'] . "    " . $row['type'] . "\n";
    }
    else {
        $res = $prefix . $row['sub'] . " -> " . $row['super'] . "    " . $row['type'] . "\n";
    }
    
    $dependencies = json_decode($row['dependencies']);
    foreach ($dependencies as $dependency) {
        $res .= printwhy($mysqli, $dependency->sub, $dependency->sup, $names, $prefix . "    ");
    }
    return $res;
}
