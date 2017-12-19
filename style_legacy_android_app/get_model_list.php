<?php
$list = []; // Since 5.4.x you can use shorthand syntax to init an array
$dir = dirname(__FILE__); // Just to get the path to current dir

// glob returns an array with the files it found matching the search pattern (* = wildcard)
$files = glob($dir."/models/*");

// Looping through all the files found in ./models
foreach ($files as $file) {
    // Removing the full path and appending the file to the $list array.
    // Now it will look like ".pic/filename.ext"
    $list[] = str_replace($dir."/", "", $file); 
}
echo json_encode($list, JSON_PRETTY_PRINT); // JSON_PRETTY_PRINT for beautifying the output
?>

