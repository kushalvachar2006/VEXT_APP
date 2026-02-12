package com.example.chat_application;

import android.os.Parcel;
import android.os.Parcelable;

public class CallItem implements Parcelable {
    private String callId;
    private String callName;
    private String callStatus;
    private String callerId;
    private String otherUserId;
    private String profilePicBase64;
    private boolean isVideo;
    private long timestamp;
    private int callIcon;
    private String direction;

    public CallItem(String callId, String callName, String callStatus,
                    String callerId, boolean isVideo, long timestamp, int callIcon, String direction) {
        this.callId = callId;
        this.callName = callName;
        this.callStatus = callStatus;
        this.callerId = callerId;
        this.isVideo = isVideo;
        this.timestamp = timestamp;
        this.callIcon = callIcon;
        this.direction = direction;
    }

    public CallItem(String callId, String callName, String callStatus,
                    String callerId, String otherUserId, String profilePicBase64,
                    boolean isVideo, long timestamp, int callIcon, String direction) {
        this(callId, callName, callStatus, callerId, isVideo, timestamp, callIcon, direction);
        this.otherUserId = otherUserId;
        this.profilePicBase64 = profilePicBase64;
    }


    public CallItem(String callName, String callStatus, int callIcon) {
        this.callId = null;
        this.callName = callName;
        this.callStatus = callStatus;
        this.callerId = null;
        this.isVideo = false;
        this.timestamp = System.currentTimeMillis();
        this.callIcon = callIcon;
        this.direction = "";
    }


    protected CallItem(Parcel in) {
        callId = in.readString();
        callName = in.readString();
        callStatus = in.readString();
        callerId = in.readString();
        otherUserId = in.readString();
        profilePicBase64 = in.readString();
        isVideo = in.readByte() != 0;
        timestamp = in.readLong();
        callIcon = in.readInt();
        direction = in.readString();
    }

    public static final Creator<CallItem> CREATOR = new Creator<CallItem>() {
        @Override
        public CallItem createFromParcel(Parcel in) {
            return new CallItem(in);
        }

        @Override
        public CallItem[] newArray(int size) {
            return new CallItem[size];
        }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(callId);
        dest.writeString(callName);
        dest.writeString(callStatus);
        dest.writeString(callerId);
        dest.writeString(otherUserId);
        dest.writeString(profilePicBase64);
        dest.writeByte((byte) (isVideo ? 1 : 0));
        dest.writeLong(timestamp);
        dest.writeInt(callIcon);
        dest.writeString(direction);
    }


    public String getCallId() { return callId; }
    public String getCallName() { return callName; }
    public String getCallStatus() { return callStatus; }
    public String getCallerId() { return callerId; }
    public String getOtherUserId() { return otherUserId; }
    public String getProfilePicBase64() { return profilePicBase64; }
    public boolean isVideo() { return isVideo; }
    public long getTimestamp() { return timestamp; }
    public int getCallIcon() { return callIcon; }
    public String getDirection() { return direction; }

    public void setCallId(String callId) { this.callId = callId; }
    public void setCallName(String callName) { this.callName = callName; }
    public void setCallStatus(String callStatus) { this.callStatus = callStatus; }
    public void setCallerId(String callerId) { this.callerId = callerId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }
    public void setProfilePicBase64(String profilePicBase64) { this.profilePicBase64 = profilePicBase64; }
    public void setVideo(boolean video) { isVideo = video; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setCallIcon(int callIcon) { this.callIcon = callIcon; }
    public void setDirection(String direction) { this.direction = direction; }

}
