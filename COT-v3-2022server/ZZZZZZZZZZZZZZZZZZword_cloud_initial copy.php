<?php

  //Arup Ghosh, March 2018
  //Updated on April 19, 2018
  //Updated on May 1, 2018

  require 'aws/aws-autoloader.php';

  include 'connectDB.php';

  //for testing
  //$_POST['id'] = '10';
  //$_POST['pid'] = '10';
  //$id = $_POST['id'];
  //$pid = $_POST['pid'];

  //conversations table

  $tablenameConversations = "conversations-" . $pid . "-" . $id;

  //echo $tablenameConversations . "<br>";



  // In the beginning, each and every aggregate score = 0
  //$queryConversations = "UPDATE `$tablenameConversations` SET `word-cloud`= NULL;";
  //$resultQueryConversations = mysqli_query($link, $queryConversations);


  $query = "SELECT number, `word-cloud-text` FROM `$tablenameConversations`;";

  $result = mysqli_query($link, $query);
  while($row = $result->fetch_assoc()) {

    //$myqueryresult = "text-" . $rowtexts["text"] . "mime-" . $rowtexts["mime"];

    //echo $myqueryresult . "<br>";

    $number = $row["number"];

    if($row["word-cloud-text"] != NULL) {
      $word_cloud_text = $row["word-cloud-text"];
      //echo $number . "<br>";
      $WordCloudText = $word_cloud_text;
      //echo $WordCloudText . "<br>";
      $WordCloudText = str_replace("People", "", $WordCloudText); //not needed if we refresh the db
      $WordCloudText = str_replace("Human", "", $WordCloudText);  //not needed if we refresh the db
      //echo "after deleting:" . $word_cloud_text . "<br>";
      if (strpos($WordCloudText, 'Person') !== false) {
        //echo "Person found" . "<br>";
        $WordCloudText = str_replace("Person", "", $WordCloudText);
        $word_cloud_text = $WordCloudText." "."Person";

      }

      $inputWC = array (
        'text' => $word_cloud_text,
        'scale' => 1,
        'width' => 600,
        'height' => 500,
        'colors' => array(
          '#375E97',
          '#FB6542',
          '#FFBB00',
          '#3F681C'
        ),
        'font'=> 'Tahoma',
        'use_stopwords' => true,
        'language' => 'en'


      );
      $inputWC_json = json_encode ( $inputWC );


      $ch = curl_init();
      $headers = array (
        'X-Mashape-Key:' . "2F44W4TqE6msh83O4YdfZflG1YPqp1qpn6ijsn2KSxzwoNk2hr",
        'Content-Type: application/json'
      );
      curl_setopt($ch, CURLOPT_URL, "https://textvis-word-cloud-v1.p.mashape.com/v1/textToCloud");
      curl_setopt ( $ch, CURLOPT_CUSTOMREQUEST,"POST");
      curl_setopt ( $ch, CURLOPT_HTTPHEADER, $headers );
      curl_setopt ( $ch, CURLOPT_POSTFIELDS, $inputWC_json );
      curl_setopt ( $ch, CURLOPT_RETURNTRANSFER, true);


      $resultWC = curl_exec($ch);
      //print_r($resultWC);
      list($mimetypeWC, $mediaWC) = explode(",", $resultWC);
      //echo $mediaWC;
      //echo '<img src="data:image/png;base64,'.$mediaWC.'"/>' . "<br>";

      if (curl_errno($ch)) {
        //echo 'Error:' . curl_error($ch);
      }
      curl_close ($ch);

      $queryWCImage = "UPDATE `$tablenameConversations` SET `word-cloud`='$mediaWC' WHERE number = '$number';";

      $resultQueryConversations = mysqli_query($link, $queryWCImage);


      if($resultQueryConversations) {

        //echo "word cloud uploaded successfully" . "<br>";

      }

      /*
      $queryWCImage = "SELECT `word-cloud` FROM `$tablenameConversations` WHERE number = '$number';";

      $resultQueryConversations = mysqli_query($link, $queryWCImage);



      if($resultQueryConversations) {

        //echo "word cloud downloaded successfully" . "<br>";

      }
      $rowWC = mysqli_fetch_array($resultQueryConversations, MYSQLI_NUM);

      echo '<img src="data:image/png;base64,'. $rowWC[0].'"/>' . "<br>";
      */

    } else {

      //echo $number . "<br>";
      //echo "empty" . "<br>";


    }

  }

  //mysqli_close($link);
?>
