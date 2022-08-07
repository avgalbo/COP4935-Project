<?php
  include(dirname(__FILE__)."/../db.php");

  $link = mysqli_init();
  $success = mysqli_real_connect(
     $link,
     $servername,
     $user,
     $password,
     $db,
     $port
  );
  
  if (mysqli_connect_errno()) {
      error_log("DB CONNECTION FAILED\n", 3, "error_log.log");
      printf("Connect failed: %s\n", mysqli_connect_error());
      exit();
 }

  mysqli_query($link, 'SET CHARACTER SET utf8mb4');

?>
