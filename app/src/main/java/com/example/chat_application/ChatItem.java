package com.example.chat_application;

public class ChatItem {
    private String chatId;
    private  String name;
    private  String lastMessage;
    private  String iconResId;
    private  String receiverId;
    private long timestamp;

    public ChatItem(String name, String lastMessage, String iconResId, String receiverId) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.iconResId = iconResId;
        this.receiverId = receiverId;
    }

    public String getName() {
        return name;
    }

    public String getChatId() {
        return chatId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getIconResId() {
        return iconResId;
    }
    public String getReceiverId() { return receiverId;}
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setIconResId(String iconResId) {
        this.iconResId = iconResId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setName(String name) {
        this.name = name;
    }
}

