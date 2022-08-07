<?php

/*

testing hash:
089a95b1dac45778c56297c6f00e501918c6631b0779f349999bc5629d741eac6097babecccc77901b9dfea0fb543d9c395a296caf7bb0809ffce9bb11c89a39

// $link is gotten from connectDB.php
/******************* Initiate Connection *******************/
include 'connectDB.php';
include(dirname(__FILE__)."/../lunch.php");

/******************* Get the Device ID *******************/
if (empty($_POST['deviceID'])) {
    exit("ERROR: No device ID was passed.");
}

$initial = 0;
$deviceID = mysqli_real_escape_string($link, $_POST['deviceID']);

/******************* Get Teen-Parent Pair *******************/
// $pair = [ <teen ID>, <parent ID>, <teen name> ]
$get_pair = "
    SELECT id
         , pid
         , name
      FROM teeninfo
     WHERE devicehash = '$deviceID'
    ;
";
$pair_result = mysqli_query($link, $get_pair);
if (mysqli_num_rows($pair_result) == 0) {
    exit("ERROR: No valid pairs in 'teen-info'");
} elseif (mysqli_num_rows($pair_result) > 1) {
    exit("ERROR: More than one valid pair.\nRESULTS: ($pair)");
}
$pair = mysqli_fetch_array($pair_result, MYSQLI_NUM);

/******************* Grab & Format POST *******************/
$groupID                = $_POST['group_id'];
$messageID              = mysqli_real_escape_string($_POST['msg_id']);
$sender_number          = mysqli_real_escape_string($link, $_POST['number']);
$sender_name            = mysqli_real_escape_string($link, $_POST['name']);
$time_sent              = $_POST['time'];
$message                = mysqli_real_escape_string($link, $_POST['text']);
$message_type           = 1;
$outgoing               = intval($_POST['outgoing']);
$contact_pic_encoded    = NULL;

// If message is outgoing, there's no sender.
// This can be deleted if we choose to use the teen's info to populate table
$incoming = 1;
if ($outgoing) {
    $sender_number = NULL;
    $sender_name   = NULL;
    $incoming      = 0;
}

/******************* Begin Upload to DB *******************/
// pair[0] -> Parent ID
// pair[1] -> Child ID
// pair[2] -> Child Name
$summary_table = "gc_summary_" . $pair[0] . "_" . $pair[1];
$convo_table = "gc_conversation_" . $pair[0] . "_" . $pair[1];

// Message contains media.
if (!empty($_POST['mime']) && !empty($_FILES['media'])) {
    $message_type = 2;
    $media_file = $_FILES['media']['temp'];
    $mime_type = mysqli_real_escape_string($link, $_POST['mime']);
    $file_data = file_get_contents($_FILES['media']['temp']);
    $media_data = base64_encode($file_data);
}

// If the GC has a contact image.
if (!empty($_FILES['contact_image'])) {
    $contact_image = file_get_contents($_FILES['contact_image']['temp']);
    $contact_image_encoded = base64_encode($contact_image);
}

// If message contains text.
if (empty($mime_type) && empty($media_data)) {
    $insert_text = "
        INSERT INTO $convo_table
               (
                  pkGroupID
                , vcNumber
                , vcName
                , vbMessage
                , bOutgoing
                , iMessageType
                , dtTimeReceived
               )
        VALUES (
                  $groupID
                , '$sender_number'
                , '$sender_name'
                , AES_ENCRYPT('$message',UNHEX(SHA2('$milkshake',512)))
                , $outgoing
                , 1
                , DEFAULT
               )
        ;
    ";
    $insertion = mysqli_query($link, $insert_text);
// If message contains media.
} else {
    $insert_text = "
        INSERT INTO $convo_table
               (
                  pkGroupID
                , vcNumber
                , vcName
                , vbMessage
                , bOutgoing
                , iMessageType
                , dtTimeReceived
                , mbImage
                , vcMime
               )
        VALUES (
                  $groupID
                , '$sender_number'
                , '$sender_name'
                , AES_ENCRYPT('$message',UNHEX(SHA2('$milkshake',512)))
                , $outgoing
                , 2
                , DEFAULT
                , {$media_data}
                , $mime_type
               )
        ;
    ";
    $insertion = mysqli_query($link, $insert_text);
}

/******************* Update Summary Table *******************/
// Check that the GC record exists
$check_gc = "
    SELECT 1
      FROM $summary_table
     WHERE pkGroupID = $groupID
    ;
";
$gc_existence = mysqli_num_rows(mysqli_query($link, $check_gc));

// If record exists.
if ($gc_existence) {
    $change_convo = "
        UPDATE $summary_table
          SET iNetIncoming      = iNetIncoming + $incoming
            , iNetOutgoing      = iNetOutgoing + $outgoing
            , iNetTexts         = iNetTexts + 1
        WHERE pkGroupID         = $groupID
        ;
    ";
// If record does not exist.
} else {
    if ($message_type != 1) {
        $increment_text  = 0;
        $increment_image = 1;
    } else {
        $increment_text  = 1;
        $increment_image = 0;
    }

    $change_convo = "INSERT INTO $summary_table (pkGroupID, iNetIncoming, iNetOutgoing, iNetTexts, iNetImages) VALUES ($groupID, $incoming, $outgoing, $increment_text, $increment_image);";
}
$changes = mysqli_query($link, $change_convo);

/******************* Perform Analysis *******************/
$analysis_results = 'This should be replaced by the analyze.';
include 'gc_analyze_text_on_demand_kier.php';

/******************* Send Notification *******************/
// Get firebase token
$get_token = "
    SELECT fcm_token
      FROM userinfo
     WHERE id = '$pair[0]'
    ;
";
$token = mysqli_query($link, $get_token);
if (mysqli_num_rows($token) != 1) {
    exit("ERROR: More than one token for this parent.");
}

$firebase = 'https://fcm.googleapis.com/fcm/send';

// Construct the notification message
$contact = ($sender_name != "") ? ($sender_name) : ($sender_number);
if ($outgoing) {
    $notification = $pair[2] . " sent a message to " . $contact . ".";
} else {
    $notification = $pair[2] . " recieved a message from " . $contact . ".";
}

// Check if group is in the Circle of Trust
$is_trusted = "
    SELECT bInCircle
      FROM $summary_table
     WHERE pkGroupID = $groupID
    ;
";
$trusted = mysqli_fetch_array(mysqli_query($link, $is_trusted));

// Build FB call
$fields = array (
    'to' => $token,
    'notification' => array (
        'sound' => 'default',
        'title' => 'Texting Activity on '. $pair[2] .'\'s Device',
        'body' => $notification,
        'tag' => $contact,
        'click_action' => 'MAIN_ACTIVITY'
    ),
    'data' => array (
        'action' => 'text',
        'name' => $contact,
        'number' => $sender_number,
        'trusted' => $trusted
    )
);
$fields = json_encode ( $fields );
$headers = array (
      'Authorization: key='
    . "AIzaSyBHqaFqyPBaJ0PiRZNX-k45DQgzRU3wfuw" //Suspect API FireBase --// This comment from last team
    , 'Content-Type: application/json'
);

// Make the FB call
// $ch = curl_init ();
// curl_setopt ( $ch, CURLOPT_URL, $firebase );
// curl_setopt ( $ch, CURLOPT_CUSTOMREQUEST,"POST");
// curl_setopt ( $ch, CURLOPT_HTTPHEADER, $headers );
// curl_setopt ( $ch, CURLOPT_POSTFIELDS, $fields );
// curl_setopt ( $ch, CURLOPT_RETURNTRANSFER, true);

// $result = curl_exec ( $ch );
// curl_close ( $ch );

echo "\nUpload echo:"
. "\n--------------------------------\n"
. $analysis_results
. "\n--------------------------------\n"
. $link->error; // Project Not Permitted

exit;
?>