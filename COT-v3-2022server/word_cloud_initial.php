<?php

  //Arup Ghosh, March 2018
  //Updated on April 19, 2018

  require 'aws/aws-autoloader.php';

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
      $WordCloudText = str_replace("People", "", $WordCloudText);
      $word_cloud_text = str_replace("Human", "", $WordCloudText);
      //echo "after deleting:" . $word_cloud_text . "<br>";

      $inputWC = array (
        'text' => $word_cloud_text,
        'scale' => 1,
        'width' => 400,
        'height' => 300,
        'colors' => array(
          '#9FFFCB',
          '#38A3A5',
          '#0BBDD8',
          '#57CC99'
        ),
        'font'=> 'Roboto',
        'use_stopwords' => true,
        'language' => 'en'


      );
      $inputWC_json = json_encode ( $inputWC );


      $ch = curl_init();
      $headers = array (
        'x-rapidapi-host:' . "textvis-word-cloud-v1.p.rapidapi.com",
        'x-rapidapi-key:' . "865c618e38mshb5a7a5cc6b2632ep1dfe0bjsnf0c66e52196b",
        'Content-Type: application/json'
      );
      curl_setopt($ch, CURLOPT_URL, "https://textvis-word-cloud-v1.p.rapidapi.com/v1/textToCloud");
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

        echo "word cloud downloaded successfully" . "<br>";

      }
      $rowWC = mysqli_fetch_array($resultQueryConversations, MYSQLI_NUM);

      echo '<img src="data:image/png;base64,'. $rowWC[0].'"/>' . "<br>";
      */

    } else {

      //echo $number . "<br>";
      //echo "empty" . "<br>";


    }

  }
//  mysqli_close($link);

?>
