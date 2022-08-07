<?php
  include(dirname(__FILE__)."/../lunch.php");
  $teenid = "";
  $pid = "";

  $result; $link;
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

  if(isset($_POST['number'])){
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
    $query = "SELECT `name`, `number`, `time`, `text-sentiment-score`, `sent`,
    `mime`, `media-moderation`, `text-flagged`, AES_DECRYPT(text, UNHEX(SHA2('$milkshake',512))) AS text, `media`, `mime`, `media-labels` FROM `$tablename` WHERE number='$number' ORDER BY time ASC;";
    if ($result = mysqli_query($link, $query))
    {
      while ($row = mysqli_fetch_row($result))
      {
        $flagged = "false";
        if($row[7]=="1" || $row[6]!=NULL)
          $flagged = "true";
        //printf ("%s\t%s\t%s\t%s\t%s\r\t\n", $row[0] == NULL?"null":$row[0], $row[1], $row[2], base64_encode($row[3]), $row[4], $row[5], $row[6]);
        $stack = array('name' => $row[0], 'number' => $row[1], 'time' => $row[2],
        'text-sentiments' => $row[3], 'sent' => $row[4], 'mime' => $row[5],
         'img-moderation' => $row[6], 'text-flagged' => $row[7], 'flag' => $flagged, 'media-labels' => $row[11]);
        if($flagged=="true"){
          $stack = array_merge($stack, array('media' => $row[9],
          'mime' => $row[10], 'text' => base64_encode($row[8])));
        }
        $out[] = $stack;
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

  }

  mysqli_free_result($result);
  mysqli_close($link);
?>