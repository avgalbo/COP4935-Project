<?php
    include 'connectDB.php';
    /*
        This file is for setting contacts from the initial upload as in or out of the Circle of Trust.
        It should recieve a list of contacts as well as the user's parents id and teen id.
        It will use the id's to access the correct conversation table and then it will set all the contacts included in the list 
        as being in the Circle of Trust
    */


    //error_log(print_r($_POST, TRUE), 3, "error_log.log");
    
    include 'gtoken-signin.php';
    if($pid == ""){
        error_log("\npid is not set\n", 3, "error_log.log");
        exit;
    }
    error_log("\npid is set\n", 3, "error_log.log");
    $query = "SELECT id FROM teeninfo WHERE pid = '$pid';";
    $result = mysqli_query($link, $query);
    if(mysqli_num_rows($result) == 0) {
        error_log("No teen device paired!", 3, "error_log.log");
        exit;
    }
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $teenid = $row[0];
    
    $tablenameConversations = "conversations-" . $pid . "-" . $teenid;

    $numbers = json_decode($_POST['numbers']);

    foreach($numbers as $number){
        error_log("cot number " . $number . "\n", 3, "error_log.log");
        $query = "UPDATE `$tablenameConversations` SET `cot`='2' WHERE `number`='$number';";
        $result = mysqli_query($link, $query);
    }
    
    
    
    
    $query = "SELECT fcm_token FROM userinfo WHERE id = '$pid';";
      $result = mysqli_query($link, $query);
      if(mysqli_num_rows($result) == 1) {
        $row = mysqli_fetch_array($result, MYSQLI_NUM);
        $fcm_token_parent = $row[0];
        
        $query = "SELECT fcm_token FROM teeninfo WHERE pid = '$pid' AND id = '$teenid';";
        $result = mysqli_query($link, $query);
        $row = mysqli_fetch_array($result, MYSQLI_NUM);
        $fcm_token_child = $row[0];
        error_log("\n\nFCM NOTIFICATION REQUESTED INITIAL COT\nchild:$fcm_token_child\nparent:$fcm_token_parent\n", 3, "error_log.log");
        //fcm push to parent app
        $url = 'https://fcm.googleapis.com/fcm/send';
        $fields = array (
                'registration_ids' => [$fcm_token_parent, $fcm_token_child],
                'data' => array (
                  'initcot' => 'success'
                ),
                'priority' => 'high'
        );
        $fields = json_encode ( $fields );
        $headers = array (
                'Authorization: key=' . "AAAAz1-tpsM:APA91bEMOObtajHFwl7mNDJ16omYii70Jn67SWM8DstGw_GmM2v_-mvm-PBsH1SlgFR-cJLCCM5OpDk8eNkw7KzzKXHONsgoDTv0PUNcwmeOJxEM5X2dZZCy-qNtgFpvfPTNlcv_HTOz", //Suspect Key most likely firebase
                'Content-Type: application/json'
        );

        $ch = curl_init ();
        curl_setopt ( $ch, CURLOPT_URL, $url );
        curl_setopt ( $ch, CURLOPT_CUSTOMREQUEST,"POST");
        curl_setopt ( $ch, CURLOPT_HTTPHEADER, $headers );
        curl_setopt ( $ch, CURLOPT_POSTFIELDS, $fields );
        curl_setopt ( $ch, CURLOPT_RETURNTRANSFER, true);

        $result = curl_exec ( $ch );
        curl_close ( $ch );
      }
    
    
?>