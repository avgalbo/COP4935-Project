package edu.ucf.CD9;

/**
 * Created by abhiditya
 */

class MessageData{

    private String message;
    private String sentiments;
    private String name;
    private String number;
    private  long time;
    private boolean sent, analysis, textFlagged;
    private byte[] media;
    private String mimetype;
    private String imgModeratoin;
    private boolean showFlag;
    private String mediaLabels;

    MessageData(boolean analysis, String name, String number, Long time, String message, String sentiments,
                boolean sent, byte[] media, String mimetype, String imgModeration, String mediaLabels,
                boolean textFlagged, boolean showFlag){
        this.analysis = analysis;
        this.number = number;
        this.name = name;
        this.sent = sent;
        this.time = time;
        this.message = message;
        this.sentiments = sentiments;
        this.media = media;
        this.mimetype = mimetype;
        this.imgModeratoin = imgModeration;
        this.textFlagged = textFlagged;
        this.showFlag = showFlag;
        this.mediaLabels = mediaLabels;
    }

    boolean isAnalysis() {
        return analysis;
    }

    boolean isSent() {
        return sent;
    }

    boolean isTextFlagged() {
        return textFlagged;
    }

    String getMessage() {
        return message;
    }

    String getName() {
        return name;
    }

    String getNumber() {
        return number;
    }

    long getTime() {
        return time;
    }

    byte[] getMedia() {
        return media;
    }

    String getMimetype() {
        return mimetype;
    }

    String getImgModeratoin() {
        return imgModeratoin;
    }

    public boolean isShowFlag() {
        return showFlag;
    }

    public String getSentiments() {
        return sentiments;
    }

    public String getMediaLabels() {
        return mediaLabels;
    }
}