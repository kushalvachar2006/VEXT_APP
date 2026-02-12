// ContactModel.java
package com.example.chat_application;

public class ContactModel {
    private String name;
    private String phoneNumber;
    private String localPhotoPath;
    private String profilePicBase64;
    private String userId;


    private boolean alreadyRegistered;
    private String otherNameUserPhone,senderNameAsPerReceiver;

    public ContactModel(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.localPhotoPath = null;
        this.profilePicBase64 = null;
        this.otherNameUserPhone = null;
        this.alreadyRegistered = false;
        this.userId = null;
        this.senderNameAsPerReceiver = null;
    }
    public ContactModel(String name, String phoneNumber, String localPhotoPath, String profilePicBase64,String otherNameUserPhone) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.localPhotoPath = localPhotoPath;
        this.profilePicBase64 = profilePicBase64;
        this.otherNameUserPhone = otherNameUserPhone;
    }

    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }

    public String getOtherNameUserPhone() {
        return otherNameUserPhone;
    }

    public String getSenderNameAsPerReceiver() {
        return senderNameAsPerReceiver;
    }

    public void setSenderNameAsPerReceiver(String senderNameAsPerReceiver) {
        this.senderNameAsPerReceiver = senderNameAsPerReceiver;
    }

    public void setOtherNameUserPhone(String otherNameUserPhone) {
        this.otherNameUserPhone = otherNameUserPhone;
    }

    public String getLocalPhotoPath() { return localPhotoPath; }
    public String getProfilePicBase64() { return profilePicBase64; }
    public boolean isAlreadyRegistered() { return alreadyRegistered; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserId() { return userId; }

    public void setLocalPhotoPath(String path) { this.localPhotoPath = path; }
    public void setPhoneNumber(String phoneNumber){this.phoneNumber=phoneNumber;}
    public void setProfilePicBase64(String base64) { this.profilePicBase64 = base64; }
    public void setAlreadyRegistered(boolean val) { this.alreadyRegistered = val; }
}
