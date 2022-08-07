<?php
  $teenid = "";
  $pid = "";
  $link;

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


  $number = $_POST['number'];
  $number = mysqli_real_escape_string($link, $number);

  $tablename = "texts-" . $pid . "-" . $teenid;
  $appJSON;
  $positive = 0;
  $negative = 0;
  $neutral = 0;
  $mixed = 0;
  $flaggedTexts = 0;
  $flaggedImages = 0;
  $totalTexts = 0;
  $totalImg = 0;
  $totalVid = 0;
  $oddhrs = 0;

  $query = "SELECT COUNT(number) FROM `$tablename` WHERE `text-sentiment`='POSITIVE' AND `number`='$number'";
  $result = mysqli_query($link, $query);
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $positive = $row[0];

  $query = "SELECT COUNT(number) FROM `$tablename` WHERE `text-sentiment`='NEGATIVE' AND `number`='$number'";
  $result = mysqli_query($link, $query);
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $negative = $row[0];

  $query = "SELECT COUNT(number) FROM `$tablename` WHERE `text-sentiment`='NEUTRAL' AND `number`='$number'";
  $result = mysqli_query($link, $query);
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $neutral = $row[0];

  $query = "SELECT COUNT(number) FROM `$tablename` WHERE `text-sentiment`='MIXED' AND `number`='$number'";
  $result = mysqli_query($link, $query);
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $mixed = $row[0];

  $query = "SELECT COUNT(number) FROM `$tablename` WHERE `text-flagged`='1' AND `number`='$number'";
  $result = mysqli_query($link, $query);
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $flaggedTexts = $row[0];

  $query = "SELECT COUNT(number) FROM `$tablename` WHERE NOT `media-moderation` IS NULL AND `number`='$number'";
  $result = mysqli_query($link, $query);
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $flaggedImages = $row[0];

  $query = "SELECT COUNT(number) FROM `$tablename` WHERE mime IS NULL AND number = '$number'";
  $result = mysqli_query($link, $query);
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $totalTexts = $row[0];

  $query = "SELECT COUNT(mime) FROM `$tablename` WHERE mime LIKE 'image%' AND number = '$number'";
  $result = mysqli_query($link, $query);
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $totalImg = $row[0];

  $query = "SELECT COUNT(mime) FROM `$tablename` WHERE mime LIKE 'video%' AND number = '$number'";
  $result = mysqli_query($link, $query);
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $totalVid = $row[0];

  $tablename = "conversations-" . $pid . "-" . $teenid;
  $query = "SELECT `cot`, `word-cloud` FROM `$tablename` WHERE number = '$number'";
  $result = mysqli_query($link, $query);
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $trusted = $row[0];
  $wordCloud = $row[1]; //

  $total = $positive + $negative + $neutral + $mixed;
  if($total == 0)
    $total = 1;
  $positive = $positive/$total*100;
  $negative = $negative/$total*100;
  $neutral = $neutral/$total*100;
  $mixed = $mixed/$total*100;
  $flaggedTotal = $flaggedTexts+$flaggedImages;

  $out = array();
  $out[] = array('positive' => $positive, 'negative' => $negative, 'neutral' => $neutral,
  'mixed' => $mixed, 'total-flagged' => $flaggedTotal, 'texts-flagged' => $flaggedTexts,
  'images-flagged' => $flaggedImages, 'total-texts' => $totalTexts, 'total-images' => $totalImg,
  'total-videos' => $totalVid, 'cot' => $trusted, 'wc' => $wordCloud);
  $appJSON = json_encode($out);

  if($appJSON==null)
    echo "null";
  else
    echo $appJSON;

?>
