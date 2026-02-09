package com.example.chat_application;

import java.io.Serializable;

public class MessageModel implements Serializable {

    private String senderId;
    private String receiverId;
    private String messageid;
    private String message,fileuri;
    private long timestamp;
    private boolean seen = false;

    // For all message types
    private String type;          // e.g., "text", "image", "file"
    private String fileName;      // For file messages
    private String imageBase64;   // For image messages
    private String caption;       // Optional caption for image

    public MessageModel() {}

    public MessageModel(String senderId, String receiverId, String message, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.type = "text";
    }

    public String getMessageid() {
        return messageid;
    }

    public void setMessageid(String messageid) {
        this.messageid = messageid;
    }

    public String getFileUri() {
        return fileuri;
    }

    public void setFileUri(String fileuri) {
        this.fileuri = fileuri;
    }

    // Getters and Setters
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}
