<?php 
    include 'connectDB.php';
    $teenid = "";
    $pid = "";
    $message_plaintext = $_POST['message'];
    $tid = $_POST['tid'];
    $result;

    if(isset($_POST['auth_token'])){
    //parent incoming
        include 'gtoken-signin.php';
        if($pid == "")
            exit;

        $query = "SELECT id FROM teeninfo WHERE pid = '$pid';";
        $result = mysqli_query($link, $query);
        if(mysqli_num_rows($result) == 0) {
            echo "No teen device paired!";
            exit;
        }
        $row = mysqli_fetch_array($result, MYSQLI_NUM);
        $teenid = $row[0];
    }else{
        exit;
    }
  
    
    $tablename = "texts-" . $pid . "-" . $teenid;
    $query = "UPDATE `$tablename` SET `false-positive`='1' WHERE `tid`='$tid';";
    $result = mysqli_query($link, $query);
    if($result){
        error_log("Message flagged false-positive on texts-" . $pid . "-" . $teenid . " with tid:" . $tid . "\n", 3, "error_log.log");
    
        $tablename = "false-positives";
        $query = "INSERT INTO `$tablename` (`pid`, `teenid`, `tid`, `message`) VALUES ('$pid', '$teenid', '$tid', '$message');";
        $result = mysqli_query($link, $query);
        
        if($result){
            error_log("Message entered into false-positives table\n", 3, "error_log.log");
        }else{
            error_log("Message was not entered into false-positives table\n", 3, "error_log.log");
            exit;
        }    
    }else{
        error_log("Could not flag message false-positive on texts table\n", 3, "error_log.log");
        exit;
    }
    
    exit;
?>