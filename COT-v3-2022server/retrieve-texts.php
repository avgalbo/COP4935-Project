<?php
  include(dirname(__FILE__)."/../lunch.php");
  $teenid = "";
  $pid = "";
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

  } else if(isset($_POST['dev_id'])){
    //child incoming
    include 'connectDB.php';
    $devid = $_POST['dev_id'];
    $devid = mysqli_real_escape_string($link, $devid);

    $query = "SELECT pid, id from teeninfo where devicehash = '$devid';";
    $result = mysqli_query($link, $query);
    if(mysqli_num_rows($result) == 0) {
      exit;
    }
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $pid = $row[0];
    $teenid = $row[1];
  }

  if(isset($_POST['counts'])){
    $out="";

    $query = "SELECT dev_admin FROM teeninfo WHERE id=$teenid;";
    $result = mysqli_query($link, $query);
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $dev_admin_status = $row[0];
    if($dev_admin_status == "0"){
      $out = $out . "DEV_ADMIN_DISABLED" . "\r\n";
    }

    $once = false; //only retrieve pic once, not fully implemented
    $tablename = "conversations-" . $pid . "-" . $teenid;
    $texttablename = "texts-" . $pid . "-" . $teenid;

    if($once==false){
      $query = "SELECT name, number, sent, received, time, unread_count, cot, pic FROM `$tablename` ORDER BY time DESC;";
      $once = true;
    }
    else
      $query = "SELECT name, number, sent, received, time, unread_count, cot FROM `$tablename` ORDER BY time DESC;";

    if ($result = mysqli_query($link, $query))
    {
      while ($row = mysqli_fetch_row($result))
      {
        $query = "SELECT COUNT(number) FROM `$texttablename` WHERE `sent`='1' AND `number`='$row[1]'";
        $result1 = mysqli_query($link, $query);
        $row1 = mysqli_fetch_array($result1, MYSQLI_NUM);
        $textsSent = $row1[0];

        $query = "SELECT COUNT(number) FROM `$texttablename` WHERE `sent`='0' AND `number`='$row[1]'";
        $result1 = mysqli_query($link, $query);
        $row1 = mysqli_fetch_array($result1, MYSQLI_NUM);
        $textsRecd = $row1[0];
        
        if($row[0] == NULL)
          $out = $out . "null\n" . $row[1];
        else
          $out = $out . $row[0] . "\n" . $row[1];

        if(isset($row[7]))
          $out = $out . "\n" . base64_encode($row[7]);
        else
          $out = $out . "\n" . "null";

          $out = $out . "\n" . $textsSent . "\n". $textsRecd . "\n". $row[4] . "\n" . $row[5]. "\n" . $row[6];

          $query = "SELECT COUNT(number) FROM `$texttablename` WHERE NOT `media-moderation` IS NULL AND `number`='$row[1]' AND `sent`='1'";
          $result1 = mysqli_query($link, $query);
          $row1 = mysqli_fetch_array($result1, MYSQLI_NUM);
          $flaggedImagesSent = $row1[0];
          $query = "SELECT COUNT(number) FROM `$texttablename` WHERE NOT `media-moderation` IS NULL AND `number`='$row[1]' AND `sent`='0'";
          $result1 = mysqli_query($link, $query);
          $row1 = mysqli_fetch_array($result1, MYSQLI_NUM);
          $flaggedImagesRecd = $row1[0];

          $query = "SELECT COUNT(number) FROM `$texttablename` WHERE `text-flagged`='1' AND `number`='$row[1]' AND `sent`='1'";
          $result1 = mysqli_query($link, $query);
          $row1 = mysqli_fetch_array($result1, MYSQLI_NUM);
          $flaggedTextsSent = $row1[0];
          $query = "SELECT COUNT(number) FROM `$texttablename` WHERE `text-flagged`='1' AND `number`='$row[1]' AND `sent`='0'";
          $result1 = mysqli_query($link, $query);
          $row1 = mysqli_fetch_array($result1, MYSQLI_NUM);
          $flaggedTextsRecd = $row1[0];

          $flaggedSent = $flaggedTextsSent+$flaggedImagesSent;
          $flaggedRecd = $flaggedTextsRecd+$flaggedImagesRecd;
          $out = $out ."\n" . $flaggedSent ."\n" . $flaggedRecd . "\r\n";
      }

      if($out!="")
        echo $out;
      else
        echo "no data";
    }
    else{
      echo "no data";
    }

    mysqli_free_result($result);
    mysqli_close($link);
  }

  else if(isset($_POST['number'])){
    $contact_pic;
    $out = array();
    $number = mysqli_real_escape_string($link, $_POST['number']);

    if(isset($_POST['send_pic'])){
      //retrieve and piggyback profile pic
      $tablename = "conversations-" . $pid . "-" . $teenid;
      $query = "SELECT pic FROM `$tablename` WHERE number='$number';";
      $result = mysqli_query($link, $query);
      $row = mysqli_fetch_row($result);
        if($row[0] == NULL){
          $contact_pic = "null";
        } else{
          $contact_pic = base64_encode($row[0]);
        }
        $out[] = array('contact_pic' => $contact_pic,);
    }

    $tablename = "texts-" . $pid . "-" . $teenid;
    $query = "SELECT `name`, `number`, `time`, AES_DECRYPT(text, UNHEX(SHA2('$milkshake',512))) AS text, `sent`, `media`, `mime`, `text-flagged`, `media-moderation`  FROM `$tablename` WHERE number='$number' ORDER BY time ASC;";
    if ($result = mysqli_query($link, $query))
    {
      error_log(print_r($result, TRUE), 3, "error_log.log");
      while ($row = mysqli_fetch_row($result))
      {
        $flagged = "false";
        if($row[7]=="1" || $row[8]!=NULL)
          $flagged = "true";
        //printf ("%s\t%s\t%s\t%s\t%s\r\t\n", $row[0] == NULL?"null":$row[0], $row[1], $row[2], base64_encode($row[3]), $row[4], $row[5], $row[6]);
        $out[] = array('name' => $row[0], 'number' => $row[1], 'time' => $row[2],
        'text' => base64_encode($row[3]), 'sent' => $row[4], 'media' => $row[5],
         'mime' => $row[6], 'flag' => $flagged);
      }
      // mysqli_free_result($result);
    }
    $appJSON = json_encode($out);
    if($appJSON==null)
      echo "null";
    else
      echo $appJSON;

    //mark conversation as read
    $tablename = "conversations-" . $pid . "-" . $teenid;
    $query = "UPDATE `$tablename` SET `unread_count`= '0' WHERE `number`='$number';";
    mysqli_query($link, $query);

    mysqli_free_result($result);
    mysqli_close($link);
  }

?>
