<?php

include 'connectDB.php';
include(dirname(__FILE__)."/../lunch.php");
if(!isset($_POST['dev_id']))
	exit;

$devid = $_POST['dev_id'];
$devid = mysqli_real_escape_string($link, $devid);
$initial = 0;

$query = "SELECT id, pid, name from teeninfo where devicehash = '$devid';";
$result = mysqli_query($link, $query);
if(mysqli_num_rows($result) == 0) {
  exit;
}
$row = mysqli_fetch_array($result, MYSQLI_NUM);
$id = $row[0];
$pid = $row[1];
$teenName = $row[2];
echo $id . ' ' . $pid . ' ' . $teenName;
$group_id = $_POST['group_id'];
$number = $_POST['number'];
$time = $_POST['time'];
$message = $_POST['text']; //location for encryption
$name = $_POST['name'];
$sent = $_POST['sent'];
$msgId = $_POST['msg_id'];
$mimeType = NULL;
$media_data = NULL;
$contact_pic_encoded = NULL;
$iv = NULL;
$tag = NULL;


//////////////////////////////////////////////////////////////////////////////    UPLOAD TO DATABASE      ///////////////////////////////////////////////////////////////////////////////////


if(isset($_POST['mime']) && isset($_FILES['media'])){
  
  //error_log(print_r($_FILES, TRUE), 3, "error_log.log");
  //error_log("\n", 3, "error_log.log");
  
  $media_file = $_FILES['media']['tmp_name'];
  $mimeType = mysqli_real_escape_string($link, $_POST['mime']);
  //$handle = fopen($media_file, "r");
  //$file_data = fread($handle, filesize($media_file)); //file_get_contents($media_file)
  $file_data = file_get_contents($_FILES['media']['tmp_name']);
  $media_data = base64_encode($file_data);
  error_log("read image data\n", 3, "error_log.log");
  
  /*
  $filename = basename($_FILES['media']['tmp_name']);
  $fileType = pathinfo($fileName, PATHINFO_EXTENSION); 
  $image = $_FILES['image']['tmp_name']; 
  $imgContent = addslashes(file_get_contents($image)); 
  */
}

if(isset($_FILES['contact_pic'])){
  $contact_pic = file_get_contents($_FILES['contact_pic']['tmp_name']);
  $contact_pic_encoded = base64_encode($contact_pic);
}

$number = mysqli_real_escape_string($link, $number);
$time = mysqli_real_escape_string($link, $time);
//  $message = mysqli_real_escape_string($link, base64_decode($message));
$message = addslashes(base64_decode($message));
$name = mysqli_real_escape_string($link, $name);
$sent = mysqli_real_escape_string($link, $sent);
$msgId = mysqli_real_escape_string($link, $msgId);

// if($sent == "true")
//   $sent = 1;
// else
//   $sent = 0;


$tablename = "gc_conversation_" . $pid . '_' . $id;
//$iv = openssl_random_pseudo_bytes(16);

if($mimeType==NULL || $media_data==NULL)
  $query = "INSERT INTO `$tablename` (`pkGroupID`, `vcNumber`, `vcName`, `vbMessage`, `bOutgoing`, `iMessageType`, `dtTimeReceived`) VALUES ('$group_id', '$number', '$name', AES_ENCRYPT('$message',UNHEX(SHA2('$milkshake',512))), '$sent', 1, '$time') ON DUPLICATE KEY UPDATE `vcName`='$name';";
else {
  $query = "INSERT INTO `$tablename` (`pkGroupID`, `vcNumber` , `vcName`, `vbMessage`, `dtTimeReceived`, `bOutgoing`, `mbImage`, `mime`, `vcTag`, `vcIV`) VALUES ('$group_id','$number', '$name', AES_ENCRYPT('$message',UNHEX(SHA2('$milkshake',512))), '$time', '$sent', '{$media_data}', '$mimeType', '$tag' , '$iv') ON DUPLICATE KEY UPDATE `vcName`='$name';";
}
$result = mysqli_query($link, $query);

if($result){

  //update conversations table
  $tablename = "gc_summary_" . $id;
  $received = abs($sent-1);

  if(isset($_POST['initial-upload'])){
    if($contact_pic_encoded==NULL)
      $query = "INSERT INTO `$tablename` (`pkGroupID`, `iNetOutgoing`, `iNetIncoming`, `iUnread_count`) VALUES ('$group_id', '$sent', '$received', '0') ON DUPLICATE KEY UPDATE `iNetOutgoing`=iNetOutgoing+$sent, `iNetIncoming`=iNetIncoming+$received, `iUnread_count`='0';";
    else
      $query = "INSERT INTO `$tablename` (`pkGroupID`, `iNetOutgoing`, `iNetIncoming`, /*`pic`,*/ `iUnread_count`) VALUES ('$group_id', '$sent', '$received', /*'{$contact_pic_encoded}',*/ '0') ON DUPLICATE KEY UPDATE `iNetOutgoing`=iNetOutgoing+$sent, `iNetIncoming`=iNetIncoming+$received, /*`pic`='{$contact_pic_encoded}',*/ `iUnread_count`='0';";

    mysqli_query($link, $query);

    $query = "INSERT INTO `processing-status` (`pid`, `id`, `texts`, `processed`) VALUES ('$pid', '$id', '1', '0') ON DUPLICATE KEY UPDATE `texts`=texts+1;";
    mysqli_query($link, $query);

    //call text procesing script
    // $tablename = "gc_conversation_" . $pid . "_" . $id;
    // $query = "SELECT MAX(tid) from `$tablename`;";
    // $result = mysqli_query($link, $query);
    // $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $tid = $id;
    $initial = 1;

//
//
//
//

    include 'gc_analyze_text_on_demand.php';
//
//
//
//
    exit;
  }else{
    $query = "SELECT lastMsgId FROM `$tablename` WHERE `number` = '$number';";
    $result = mysqli_query($link, $query);
    if(mysqli_num_rows($result) == 0) {
      if($contact_pic_encoded==NULL)
        $query = "INSERT INTO `$tablename` (`pkGroupID`, `iNetOutgoing`, `iNetIncoming`, `iUnread_count`, `vcLastMessageID`) VALUES ('$group_id', '$sent', '$received', '1', '$msgId' ) ON DUPLICATE KEY UPDATE `iNetOutgoing`=iNetOutgoing+$sent, `iNetIncoming`=iNetIncoming+$received, `iUnread_count`=iUnread_count+1;";
      else
        $query = "INSERT INTO `$tablename` (`pkGroupID`, `iNetOutgoing`, `iNetIncoming`, /*`pic`,*/ `iUnread_count`, `vcLastMessageID`) VALUES ('$group_id', '$sent', '$received', /*'{$contact_pic_encoded}',*/ '1', '$msgId') ON DUPLICATE KEY UPDATE `iNetOutgoing`=iNetOutgoing+$sent, `iNetIncoming`=iNetIncoming+$received, /*`pic`='{$contact_pic_encoded}',*/ `iUnread_count`=iUnread_count+1;";
      mysqli_query($link, $query);
      $query = "INSERT INTO `processing-status` (`pid`, `id`, `texts`, `processed`) VALUES ('$pid', '$id', '1', '0') ON DUPLICATE KEY UPDATE `texts`=texts+1;";
      mysqli_query($link, $query);
    } else{
      $row = mysqli_fetch_array($result, MYSQLI_NUM);
      $lastMsgid = $row[0];
      if($lastMsgid != $msgId){
        if($contact_pic_encoded==NULL){
          $query = "INSERT INTO `$tablename` (`pkGroupID`, `iNetOutgoing`, `iNetIncoming`, `iUnread_count`, `vcLastMessageID`) VALUES ('$group_id', '$sent', '$received', '1', '$msgId' ) ON DUPLICATE KEY UPDATE `iNetOutgoing`=iNetOutgoing+$sent, `iNetIncoming`=iNetIncoming+$received, `iUnread_count`=iUnread_count+1, `vcLastMessageID` = '$msgId';";
        }
        else
          $query = "INSERT INTO `$tablename` (`pkGroupID`, `iNetOutgoing`, `iNetIncoming`, /*`pic`,*/ `iUnread_count`, `vcLastMessageID`) VALUES ('$group_id', '$sent', '$received', /*'{$contact_pic_encoded}',*/ '1', '$msgId') ON DUPLICATE KEY UPDATE `iNetOutgoing`=iNetOutgoing+$sent, `iNetIncoming`=iNetIncoming+$received, /*`pic`='{$contact_pic_encoded}',*/ `iUnread_count`=iUnread_count+1, `vcLastMessageID` = '$msgId';";
        mysqli_query($link, $query);
        $query = "INSERT INTO `processing-status` (`pid`, `id`, `texts`, `processed`) VALUES ('$pid', '$id', '1', '0') ON DUPLICATE KEY UPDATE `texts`=texts+1;";
        mysqli_query($link, $query);
      }else{
        //duplicate msg
        exit;
      }
    }
  }

  //send notification
  $query = "SELECT fcm_token FROM userinfo WHERE id = '$pid';";
  $result = mysqli_query($link, $query);
  if(mysqli_num_rows($result) == 1) {
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $fcm_token = $row[0];
  }
  else {
    exit;
  }

  $url = 'https://fcm.googleapis.com/fcm/send'; 
  $message = "";
  if($name == "")
    $name = $number;

  if($sent == 1)
    $message = $teenName . " sent a text to " . $name . ".";
  else
    $message = $teenName . " received a text from " . $name . ".";

  $query = "SELECT cot FROM `$tablename` WHERE `number`='$number';";
  $result = mysqli_query($link, $query);
  $row = mysqli_fetch_row($result);
  $cot = $row[0];
  $trusted = "false";
  if($cot=="2"){
    $trusted = "true";
  }

  $fields = array (
          'to' => $fcm_token,
          'notification' => array (
            'sound' => 'default',
            'title' => 'Texting Activity on '. $teenName .'\'s Device',
            'body' => $message,
            'tag' => $name,
            'click_action' => 'MAIN_ACTIVITY'
          ),
          'data' => array (
            'action' => 'text',
            'name' => $name,
            'number' => $number,
            'trusted' => $trusted
          )
  );
  $fields = json_encode ( $fields );
  $headers = array (
          'Authorization: key=' . "AIzaSyBHqaFqyPBaJ0PiRZNX-k45DQgzRU3wfuw",
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

  //call text procesing script
  // $tablename = "conversation_" . $pid . "_" . $id;
  // $query = "SELECT MAX(tid) from `$tablename`;";
  // $result = mysqli_query($link, $query);
  // $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $tid = $id;
  //
  //
  //
  //
  include 'gc_analyze_text_on_demand.php';
  //
  //
  //
  //
  
}
?>
