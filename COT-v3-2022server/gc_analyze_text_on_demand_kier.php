<?php
// At the moment, this file is only called from the %upload_texts%.php files

require 'vendor/autoload.php';
include(dirname(__FILE__)."/../breakfast.php");
include 'connectDB.php';

$rekognition = new \Aws\Rekognition\RekognitionClient([
    'version'       => 'latest',
    'region'        => 'us-east-1',
    'verify'        => 'false',
    'credentials'   => [
        'key'    => $hamburger,
        'secret' => $fries
    ]
]);

$comprehend = new \Aws\Comprehend\ComprehendClient([
    'version'       => 'latest',
    'region'        => 'us-east-1',
    'verify'        => 'false',
    'credentials'   => [
        'key'    => $hamburger,
        'secret' => $fries
    ]
]);


$get_latest_message = "
    SELECT vcNumber
         , AES_DECRYPT(vbMessage, UNHEX(SHA2('$milkshake',512))) AS text
         , mbImage
         , vcMime
         , vcTag
      FROM $convo_table
     WHERE pkGroupID = $groupID
     ORDER BY dtTimeReceived DESC
     LIMIT 1
    ;
";
$latest_message_data = mysqli_query($link, $get_latest_message);
if (mysqli_num_rows($latest_message_data) < 1) {
    exit("ERROR: There are no dates?");
} elseif (mysqli_num_rows($latest_message_data) > 1) {
    exit("ERROR: There are duplicate dates and the LIMIT doesn't work?");
}

$latest_message = $latest_message_data->fetch_assoc();

/******************* Comprehend Text Analysis *******************/
// This if statement closes on line 112
if ($latest_message['text']) {
    $sentiment = $comprehend->DetectSentiment([
        'LanguageCode' => 'en',
        'Text' => strval($latest_message["text"])
    ]);


/******************* Import the Sentiment Score *******************/
// I have to make hold_sentiment because you cannot pass 2D array into update_sentiment
$hold_sentiment = [
      $sentiment['SentimentScore']['Positive']
    , $sentiment['SentimentScore']['Negative']
    , $sentiment['SentimentScore']['Neutral']
    , $sentiment['SentimentScore']['Mixed']
    , $sentiment['Sentiment']
];
$update_sentiment = "
    UPDATE $convo_table
       SET dSentimentPos  = $hold_sentiment[0]
         , dSentimentNeg  = $hold_sentiment[1]
         , dSentimentNeu  = $hold_sentiment[2]
         , dSentimentMix  = $hold_sentiment[3]
         , vcNetSentiment = '$hold_sentiment[4]'
     WHERE pkGroupID      = $groupID
       AND vbMessage      = AES_ENCRYPT('{$latest_message["text"]}',UNHEX(SHA2('$milkshake',512)))
    ;
";
$push_sentiment = mysqli_query($link, $update_sentiment);

/******************* Comprehend Entity Detection *******************/
$entities = $comprehend->detectEntities([
    'LanguageCode' => 'en',
    'Text' => strval($latest_message["text"])
]);

$entities_string = "";
for ($i = 0; $i < sizeof($entities['Entities']); $i++) {
    // $entities_string = $entities_string
    //                  . $entities['Entities'][$i]['Score'] . ","
    //                  . $entities['Entities'][$i]['Text']  . ","
    //                  . $entities['Entities'][$i]['Type']  . ",";

    // {'Text':$entities['Entities'][$i]['Text'],'Type':$entities['Entities'][$i]['Type'],'Score':$entities['Entities'][$i]['Score']}
    $entities_string = $entities_string . "{"
                     . "'text':'"    . $entities['Entities'][$i]['Text']
                     . "','type':'"  . $entities['Entities'][$i]['Type']
                     . "','score':"  . $entities['Entities'][$i]['Score']
                     . "},";
}
$entities_string = substr($entities_string, 0, -1);


if (!empty($entities_string)) {
    $update_entities = "
        UPDATE $convo_table
           SET vcTextEntities = \"$entities_string\"
         WHERE pkGroupID      = $groupID
           AND vbMessage      = AES_ENCRYPT('{$latest_message["text"]}',UNHEX(SHA2('$milkshake',512)))
        ;
    ";
    $push_entities = mysqli_query($link, $update_entities);
}

}

//////////
//////////
//////////
//////////
////////// Anh, is the STIR Lab ML API yours?
//////////
////////// analyze_text_on_demand.php lines 286-320
//////////
//////////

/******************* Rekognition Image Recognition *******************/
if ($latest_message['mbImage'] && ($latest_message['mbImage'] = 'image/jpeg' || $latest_message['mbImage'] = 'image/png')) {
    $labels = $rekognition->detectLabels([ //Suspect Rekognition
        'Image'         => [
            'Bytes'     => base64_decode($latest_message["mbImage"]),
        ],
        'MaxLabels'     => 5,
        'MinConfidence' => 10,
    ]);

$labels_string    = '';
$labels_wordcloud = '';
for ($i = 0; $i < sizeof($labels['Labels']); $i++) {
    // $labels_string = $labels_string . $labels['Labels'][$i]['Confidence'] . "," . $labels['Labels'][$i]['Name'] . ",";
    $labels_string = $labels_string . "{"
        . "'label':'"        . $labels['Labels'][$i]['Name']
        . "','confidence':"  . $labels['Labels'][$i]['Confidence']
        . "},";

    // $labels_results_wc = $labels_results_wc . $labels['Labels'][$i]['Name'] . " ";
    $labels_wordcloud = $labels_wordcloud . $labels['Labels'][$i]['Name'] . ",";
}
$labels_string    = substr($labels_string, 0, -1);
$labels_wordcloud = substr($labels_wordcloud, 0, -1);

if (!empty($labels_string)) {
    $update_labels = "
        UPDATE $convo_table
           SET vcTextEntities = \"$labels_string\"
         WHERE pkGroupID      = $groupID
           AND mbImage        = {$latest_message["mbImage"]}
        ;
    ";
    $push_labels = mysqli_query($link, $update_labels);
}

//
//
// In the original file, they create the wordcloud string, insert,
// grab the string from conversation_x_x, then update, then re-insert???
//
// media-moderation is a word cloud thing...
//
//
// // This will not work... This feature may be deprecated...
// if (!empty($labels_wordcloud)) {
//     $update_labels_wordcloud = "
//         UPDATE $summary_table
//            SET txWordCloudText = \"$labels_wordcloud\"
//          WHERE pkGroupID       = $groupID
//         ;
//     ";
//     $push_labels_wordcloud = mysqli_query($link, $update_labels_wordcloud);
// }


}
















$analysis_results = ("\nDemand echo:"
. "\n--------------------------------\n"
. $entities_string . " "
. "\n--------------------------------\n"
. $link->error
);

?>