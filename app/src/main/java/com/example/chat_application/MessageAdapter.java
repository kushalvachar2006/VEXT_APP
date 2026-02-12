package com.example.chat_application;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MessageAdapter";
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    public static boolean isLongPressed = false;
    public static Set<Integer> selectedPositions = new HashSet<>();

    private final List<MessageModel> messageList;
    private final String currentUserId;

    public MessageAdapter(List<MessageModel> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sender_message_item, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.receiver_message_item, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messageList.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private void openFile(View v, File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    v.getContext(),
                    v.getContext().getPackageName() + ".provider",
                    file
            );

            String mimeType = getMimeType(file.getName());

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            v.getContext().startActivity(Intent.createChooser(intent, "Open with"));

        } catch (Exception e) {
            Log.e(TAG, "Error opening file: " + e.getMessage(), e);
            Toast.makeText(v.getContext(), "No app found to open file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "*/*";
        }

        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        switch (ext) {
            case "pdf": return "application/pdf";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "mp4": return "video/mp4";
            case "mp3": return "audio/mpeg";
            case "doc":
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt":
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls":
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt": return "text/plain";
            default: return "*/*";
        }
    }

    /**
     * Download file from Base64 string and save to local storage
     */
    private File downloadFileFromUrl(Context context, String fileUrl, String fileName) throws Exception {
        return downloadFileFromUrl(context, fileUrl, fileName, 0);
    }

    private File downloadFileFromUrl(Context context, String fileUrl, String fileName, int redirectCount) throws Exception {
        if (redirectCount > 5) {
            throw new Exception("Too many redirects");
        }

        File dir = new File(context.getExternalFilesDir(null), "VextApp/Downloads");
        if (!dir.exists()) dir.mkdirs();

        File outFile = new File(dir, fileName);

        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        connection.connect();

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String newUrl = connection.getHeaderField("Location");
            connection.disconnect();
            return downloadFileFromUrl(context, newUrl, fileName, redirectCount + 1);
        }

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Server returned HTTP " + connection.getResponseCode());
        }

        InputStream inputStream = connection.getInputStream();
        FileOutputStream outputStream = new FileOutputStream(outFile);

        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.flush();
        outputStream.close();
        inputStream.close();
        connection.disconnect();

        return outFile;
    }




    private String formatTime(long timestamp) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date(timestamp));
    }


    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, time, fileName;
        LinearLayout fileLayout;
        Button openButton;
        ImageView imageView, fileicon;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.rightMessage);
            fileLayout = itemView.findViewById(R.id.rightFileLayout);
            fileName = itemView.findViewById(R.id.rightFileName);
            openButton = itemView.findViewById(R.id.rightOpenButton);
            time = itemView.findViewById(R.id.rightMessageTime);
            imageView = itemView.findViewById(R.id.rightImage);
            fileicon = itemView.findViewById(R.id.rightfileimg);
        }

        void bind(MessageModel message) {
            textMessage.setVisibility(View.GONE);
            fileLayout.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;

                if (!isLongPressed) {
                    isLongPressed = true;
                    selectedPositions.clear();
                }

                if (selectedPositions.contains(pos)) {
                    selectedPositions.remove(pos);
                } else {
                    selectedPositions.add(pos);
                }

                notifyItemChanged(pos);

                if (v.getContext() instanceof Message_layout) {
                    ((Message_layout) v.getContext()).updateHeaderUI();
                }

                return true;
            });

            itemView.setOnClickListener(v -> {
                if (isLongPressed) {
                    int pos = getAdapterPosition();
                    if (selectedPositions.contains(pos)) {
                        selectedPositions.remove(pos);
                    } else {
                        selectedPositions.add(pos);
                    }
                    notifyItemChanged(pos);

                    if (selectedPositions.isEmpty()) {
                        isLongPressed = false;
                    }

                    if (v.getContext() instanceof Message_layout) {
                        ((Message_layout) v.getContext()).updateHeaderUI();
                    }
                }
            });

            if (selectedPositions.contains(getAdapterPosition())) {
                itemView.setBackgroundColor(0xFFE0E0E0);
            } else {
                itemView.setBackgroundColor(0x00000000);
            }

            String type = message.getType() == null ? "text" : message.getType();
            switch (type) {
                case "text":
                    textMessage.setVisibility(View.VISIBLE);
                    textMessage.setText(message.getMessage());
                    break;

                case "file":
                    fileLayout.setVisibility(View.VISIBLE);
                    fileName.setText(message.getFileName());

                    openButton.setText("Open");
                    openButton.setOnClickListener(v -> {
                        String fileData = message.getFileUri();

                        if (fileData == null || fileData.isEmpty()) {
                            Toast.makeText(v.getContext(), "File data is missing", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Toast.makeText(v.getContext(), "Preparing file...", Toast.LENGTH_SHORT).show();
                        openButton.setEnabled(false);
                        openButton.setText("Loading...");

                        new Thread(() -> {
                            try {
                                File localFile = downloadFileFromUrl(
                                        v.getContext(),
                                        fileData,
                                        message.getFileName()
                                );

                                ((Activity) v.getContext()).runOnUiThread(() -> {
                                    openButton.setEnabled(true);
                                    openButton.setText("Open");
                                    Toast.makeText(v.getContext(), "Opening file...", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "File exists: " + localFile.exists());
                                    Log.d(TAG, "File path: " + localFile.getAbsolutePath());
                                    Log.d(TAG, "File size: " + localFile.length());
                                    openFile(v, localFile);
                                });

                            } catch (Exception e) {
                                Log.e(TAG, "Error opening file", e);
                                ((Activity) v.getContext()).runOnUiThread(() -> {
                                    openButton.setEnabled(true);
                                    openButton.setText("Open");
                                    Toast.makeText(
                                            v.getContext(),
                                            "Unable to open file: " + e.getMessage(),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                });
                            }
                        }).start();
                    });


                    String ext = "";
                    int i = message.getFileName().lastIndexOf('.');
                    if (i > 0) ext = message.getFileName().substring(i + 1).toLowerCase();

                    int iconRes;
                    switch (ext) {
                        case "pdf": iconRes = R.drawable.ic_pdf; break;
                        case "doc":
                        case "docx": iconRes = R.drawable.ic_word; break;
                        case "xls":
                        case "xlsx": iconRes = R.drawable.ic_excel; break;
                        case "ppt":
                        case "pptx": iconRes = R.drawable.ic_ppt; break;
                        case "mp4":
                        case "mkv": iconRes = R.drawable.ic_video; break;
                        case "mp3":
                        case "wav": iconRes = R.drawable.ic_audio; break;
                        case "jpg":
                        case "jpeg":
                        case "png":
                        case "gif": iconRes = R.drawable.ic_image; break;
                        default: iconRes = R.drawable.ic_file;
                    }
                    fileicon.setVisibility(View.VISIBLE);
                    fileicon.setImageResource(iconRes);
                    break;

                case "image":
                    if (message.getImageBase64() != null) {
                        byte[] bytes = Base64.decode(message.getImageBase64(), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageBitmap(bitmap);
                        imageView.setOnClickListener(v -> {
                            try {
                                File cacheFile = new File(v.getContext().getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
                                FileOutputStream fos = new FileOutputStream(cacheFile);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                fos.flush();
                                fos.close();

                                Intent intent = new Intent(v.getContext(), Full_screen_profile.class);
                                intent.putExtra("imagePath", cacheFile.getAbsolutePath());
                                v.getContext().startActivity(intent);
                            } catch (Exception e) {
                                Log.e(TAG, "Error opening full screen image", e);
                            }
                        });
                    }
                    break;
            }

            time.setText(formatTime(message.getTimestamp()));
        }
    }


    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, time, fileName;
        LinearLayout fileLayout;
        Button openButton;
        ImageView imageView, fileicon;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.leftMessage);
            fileLayout = itemView.findViewById(R.id.leftFileLayout);
            fileName = itemView.findViewById(R.id.leftFileName);
            openButton = itemView.findViewById(R.id.leftOpenButton);
            time = itemView.findViewById(R.id.leftMessageTime);
            imageView = itemView.findViewById(R.id.leftImage);
            fileicon = itemView.findViewById(R.id.leftfileimg);
        }

        void bind(MessageModel message) {
            textMessage.setVisibility(View.GONE);
            fileLayout.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;

                if (!isLongPressed) {
                    isLongPressed = true;
                    selectedPositions.clear();
                    notifyDataSetChanged();
                }

                if (selectedPositions.contains(pos)) {
                    selectedPositions.remove(pos);
                } else {
                    selectedPositions.add(pos);
                }

                notifyItemChanged(pos);

                if (v.getContext() instanceof Message_layout) {
                    ((Message_layout) v.getContext()).updateHeaderUI();
                }

                return true;
            });

            itemView.setOnClickListener(v -> {
                if (isLongPressed) {
                    int pos = getAdapterPosition();
                    if (selectedPositions.contains(pos)) {
                        selectedPositions.remove(pos);
                    } else {
                        selectedPositions.add(pos);
                    }
                    notifyItemChanged(pos);

                    if (selectedPositions.isEmpty()) {
                        isLongPressed = false;
                    }

                    if (v.getContext() instanceof Message_layout) {
                        ((Message_layout) v.getContext()).updateHeaderUI();
                    }
                }
            });

            if (selectedPositions.contains(getAdapterPosition())) {
                itemView.setBackgroundColor(0xFFE0E0E0);
            } else {
                itemView.setBackgroundColor(0x00000000);
            }

            String type = message.getType() == null ? "text" : message.getType();
            switch (type) {
                case "text":
                    textMessage.setVisibility(View.VISIBLE);
                    textMessage.setText(message.getMessage());
                    break;

                case "file":
                    fileLayout.setVisibility(View.VISIBLE);
                    fileName.setText(message.getFileName());

                    File dir = new File(itemView.getContext().getExternalFilesDir(null), "VextApp/Downloads");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    File localFile = new File(dir, message.getFileName());

                    if (localFile.exists()) {
                        openButton.setText("Open");
                    } else {
                        openButton.setText("Download");
                    }

                    openButton.setOnClickListener(v -> {
                        if (localFile.exists()) {
                            openFile(v, localFile);
                        } else {
                            String fileData = message.getFileUri();

                            if (fileData == null || fileData.isEmpty()) {
                                Toast.makeText(v.getContext(), "File data is missing", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "File data is null or empty");
                                return;
                            }

                            Toast.makeText(v.getContext(), "Starting download...", Toast.LENGTH_SHORT).show();
                            openButton.setEnabled(false);
                            openButton.setText("Downloading...");

                            new Thread(() -> {
                                try {

                                    ((Activity) v.getContext()).runOnUiThread(() -> {
                                        Toast.makeText(v.getContext(), "Downloading file to device...", Toast.LENGTH_SHORT).show();
                                    });


                                    File downloadedFile = downloadFileFromUrl(
                                            v.getContext(),
                                            fileData,
                                            message.getFileName()
                                    );


                                    ((Activity) v.getContext()).runOnUiThread(() -> {
                                        openButton.setEnabled(true);
                                        openButton.setText("Open");
                                        Toast.makeText(v.getContext(), "Download complete! Opening file...", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "File exists: " + downloadedFile.exists());
                                        Log.d(TAG, "File path: " + downloadedFile.getAbsolutePath());
                                        Log.d(TAG, "File size: " + downloadedFile.length());

                                        openFile(v, downloadedFile);
                                    });

                                } catch (Exception e) {
                                    Log.e(TAG, "Error downloading file", e);
                                    ((Activity) v.getContext()).runOnUiThread(() -> {
                                        openButton.setEnabled(true);
                                        openButton.setText("Download");
                                        Toast.makeText(
                                                v.getContext(),
                                                "Download failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    });
                                }
                            }).start();
                        }
                    });


                    String ext = "";
                    int i = message.getFileName().lastIndexOf('.');
                    if (i > 0) ext = message.getFileName().substring(i + 1).toLowerCase();

                    int iconRes;
                    switch (ext) {
                        case "pdf": iconRes = R.drawable.ic_pdf; break;
                        case "doc":
                        case "docx": iconRes = R.drawable.ic_word; break;
                        case "xls":
                        case "xlsx": iconRes = R.drawable.ic_excel; break;
                        case "ppt":
                        case "pptx": iconRes = R.drawable.ic_ppt; break;
                        case "mp4":
                        case "mkv": iconRes = R.drawable.ic_video; break;
                        case "mp3":
                        case "wav": iconRes = R.drawable.ic_audio; break;
                        case "jpg":
                        case "jpeg":
                        case "png":
                        case "gif": iconRes = R.drawable.ic_image; break;
                        default: iconRes = R.drawable.ic_file;
                    }
                    fileicon.setVisibility(View.VISIBLE);
                    fileicon.setImageResource(iconRes);
                    break;

                case "image":
                    if (message.getImageBase64() != null) {
                        byte[] bytes = Base64.decode(message.getImageBase64(), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageBitmap(bitmap);
                        imageView.setOnClickListener(v -> {
                            try {
                                File cacheFile = new File(v.getContext().getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
                                FileOutputStream fos = new FileOutputStream(cacheFile);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                fos.flush();
                                fos.close();

                                Intent intent = new Intent(v.getContext(), Full_screen_profile.class);
                                intent.putExtra("imagePath", cacheFile.getAbsolutePath());
                                v.getContext().startActivity(intent);
                            } catch (Exception e) {
                                Log.e(TAG, "Error opening full screen image", e);
                            }
                        });
                    }
                    break;
            }

            time.setText(formatTime(message.getTimestamp()));
        }
    }
}
