package edu.ucf.CD9;

/**
 * Created by abhiditya
 */

class SummaryData {

    String name;
    String number;
    byte[] pic;
    int sent;
    int received;
    long time;
    int unreadCount;
    int flaggedSent;
    int flaggedRecd;
    int trusted;
    boolean selected;

    SummaryData(String name, String number, byte[] pic, int sent, int received, long time, int unreadCount, int trusted, int flaggedSent, int flaggedRecd){
        this.number = number;
        this.name = name;
        this.pic = pic;
        this.sent = sent;
        this.received = received;
        this.time = time;
        this.unreadCount = unreadCount;
        this.trusted = trusted;
        this.flaggedSent = flaggedSent;
        this.flaggedRecd = flaggedRecd;
        this.selected = false;
    }

    public void setSelected(boolean selected){
        this.selected = selected;
    }

    public boolean isSelected(){
        return selected;
    }

}