package com.example.chat_application;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> implements Filterable {
    Context context;
    List<ContactModel> contactList;
    public static List<ContactModel> contactListFull; // backup list for filtering
    FirebaseFirestore db=FirebaseFirestore.getInstance();
    private String currentUserPhone;
    public static HashMap<String, String> uidToLocalName = new HashMap<>();
    public static String currentname;
    public static String currentProfileBase64 = "";


    public  static boolean isVideoCall=false;

    public ContactAdapter(Context context, List<ContactModel> contactList,String currentUserPhone) {
        this.context = context;
        this.contactList = contactList;
        this.contactListFull = new ArrayList<>(contactList);
        this.currentUserPhone = currentUserPhone;

        checkContactsInFirestore();
    }
    private void checkContactsInFirestore() {
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    HashSet<String> registeredNumbers = new HashSet<>();
                    HashMap<String, String> phoneToUserId = new HashMap<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String phone = doc.getString("phone");
                        if (phone != null) {
                            registeredNumbers.add(phone);
                            phoneToUserId.put(phone, doc.getId());
                        }
                    }

                    List<ContactModel> registeredContacts = new ArrayList<>();
                    List<ContactModel> nonRegisteredContacts = new ArrayList<>();

                    for (ContactModel contact : contactListFull) {
                        if (registeredNumbers.contains(contact.getPhoneNumber())) {
                            contact.setAlreadyRegistered(true);
                            contact.setUserId(phoneToUserId.get(contact.getPhoneNumber()));
                            registeredContacts.add(contact);
                            uidToLocalName.put(contact.getUserId(), contact.getName());
                        } else {
                            contact.setAlreadyRegistered(false);
                            nonRegisteredContacts.add(contact);
                        }
                    }

                    // Merge lists: registered first
                    contactList.clear();
                    contactList.addAll(registeredContacts);
                    contactList.addAll(nonRegisteredContacts);


                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("ContactAdapter", "Failed to fetch registered contacts", e));
    }



    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.contacts_container_page, parent, false);
        return new ContactViewHolder(view);
    }



    @Override
    public void onBindViewHolder(ContactViewHolder holder, int position) {
        ContactModel contact = contactList.get(position);
        String contactname;
        DatabaseReference myProfileRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUserPhone)   // ⚠️ if phone = uid else change to uid variable
                .child("profilePicBase64");

        myProfileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String base64 = snapshot.getValue(String.class);
                if (base64 != null && !base64.isEmpty()) {
                    currentProfileBase64 = base64;
                } else {
                    currentProfileBase64 = "";
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                currentProfileBase64 = "";
            }
        });


        // --- Display name ---
        if (contact.getPhoneNumber() != null && contact.getPhoneNumber().equals(currentUserPhone)) {
            contactname = contact.getName()+" (You)";
        } else {
            contactname = contact.getName();
        }
        holder.name.setText(contactname);

        holder.phone.setText(contact.getPhoneNumber());

        // --- Load profile image ---
        loadcontact(holder, contact, position);

        // --- Fullscreen profile click ---
        holder.icon.setOnClickListener(v -> openContactFullScreen(contact));

        // --- Gradient text for invite ---
        Paint paint = holder.invite.getPaint();
        float width = paint.measureText(holder.invite.getText().toString());
        Shader shader = new LinearGradient(
                0, 0, width, holder.invite.getTextSize(),
                new int[]{Color.parseColor("#00C8F3"), Color.parseColor("#00CF55")},
                null, Shader.TileMode.CLAMP
        );
        holder.invite.getPaint().setShader(shader);
        holder.invite.invalidate();

        //Message layout intent
        holder.contactinfo.setOnClickListener(v -> {
            Intent intent = new Intent(context, Message_layout.class);

            String senderLocalName = getLocalNameByPhone(currentUserPhone);
            // Receiver's name as saved in sender's phonebook
            String receiverLocalName=contact.getPhoneNumber().equals(currentUserPhone)?contactname:(contact.isAlreadyRegistered() ?
                    getLocalNameByUid(contact.getUserId(), contact.getName()) : contact.getName());

            db.collection("users")
                    .document(contact.getUserId())
                    .update("contactname", receiverLocalName);

            currentname = receiverLocalName;
            intent.putExtra("contactName", receiverLocalName);
            intent.putExtra("contactPhone", contact.getPhoneNumber());


            if (contact.isAlreadyRegistered() && contact.getUserId() != null) {
                intent.putExtra("receiverId", contact.getUserId());

                DatabaseReference userRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(contact.getUserId())
                        .child("profilePicBase64");

                // Fetch the profile picture once before opening Message_layout
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String base64 = snapshot.getValue(String.class);

                        if (base64 != null && !base64.isEmpty()) {
                            intent.putExtra("profilePicBase64", base64);
                        } else {
                            intent.putExtra("profilePicBase64", ""); // fallback empty
                        }
                        context.startActivity(intent);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        intent.putExtra("profilePicBase64", ""); // fallback empty
                        context.startActivity(intent);
                    }
                });
            } else {
                intent.putExtra("profilePicBase64", "");
                context.startActivity(intent);
            }
        });


        // --- Invite button ---
        if (contact.isAlreadyRegistered()) {
            if(MainPage.isCallIcon==true){
                holder.newcallbtn.setVisibility(View.VISIBLE);
                holder.invite.setVisibility(View.GONE);
            }
            else{
                holder.newcallbtn.setVisibility(View.GONE);
                holder.invite.setVisibility(View.GONE);
            }
        } else {
            holder.newcallbtn.setVisibility(View.GONE);
            holder.invite.setVisibility(View.VISIBLE);
            holder.invite.setOnClickListener(v -> {
                String phoneNumber = contact.getPhoneNumber();
                String message = "Hey " + contact.getName() + ",\n\n" +
                        "Join the new world of conversation with *VextApp* 🚀\n" +
                        "The next-gen communication platform — smarter, faster, better!\n\n" +
                        "Download now and connect with me today!";

                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + Uri.encode(phoneNumber)));
                smsIntent.putExtra("sms_body", message);

                try {
                    context.startActivity(smsIntent);
                } catch (Exception e) {
                    Log.e("ContactAdapter", "SMS failed to open", e);
                }
            });
        }
        holder.newcallbtn.setOnClickListener(v->{
            Animation slideIn = AnimationUtils.loadAnimation(context, R.anim.slide_in);
            holder.callinfolayout.startAnimation(slideIn);
            holder.callinfolayout.setVisibility(View.VISIBLE);
        });
        holder.itemView.setOnClickListener(v->{
            if (holder.callinfolayout.getVisibility() == View.VISIBLE) {
                Animation slideOut = AnimationUtils.loadAnimation(context, R.anim.slide_out);
                holder.callinfolayout.startAnimation(slideOut);
                slideOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        holder.callinfolayout.setVisibility(View.GONE);
                    }
                    @Override public void onAnimationRepeat(Animation animation) {}
                });
            }
        });

        holder.voicecall.setOnClickListener(v -> {
            if (contact.isAlreadyRegistered() && contact.getUserId() != null) {

                CallLauncher.startCall(
                        context,
                        contact.getUserId(),
                        contact.getName(),
                        contact.getPhoneNumber(),
                        false     // voice call
                );
            }
        });
        holder.videocall.setOnClickListener(v -> {
            if (contact.isAlreadyRegistered() && contact.getUserId() != null) {

                CallLauncher.startCall(
                        context,
                        contact.getUserId(),
                        contact.getName(),
                        contact.getPhoneNumber(),
                        true      // video call
                );
            }
        });



// Optional: mirror text clicks to buttons
        holder.voicecalltext.setOnClickListener(v -> holder.voicecall.performClick());
        holder.videocalltext.setOnClickListener(v -> holder.videocall.performClick());




    }
    public static String resolveName(String phone) {
        phone = phone.replaceAll("[\\s\\-()\\+]", "");

        for (ContactModel c : contactListFull) {
            if (c.getPhoneNumber().replaceAll("[\\s\\-()\\+]", "").endsWith(phone)) {
                return c.getName();
            }
        }
        return null;
    }

    public static String getLocalNameByUid(String uid, String fallbackName) {
        if (uid != null && uidToLocalName.containsKey(uid)) {
            return uidToLocalName.get(uid);
        } else {
            return fallbackName;
        }
    }
    public static String getLocalNameByPhone(String phoneNumber) {
        for (ContactModel contact : contactListFull) {
            if (contact.getPhoneNumber().equals(phoneNumber)) {
                return contact.getName();
            }
        }
        return "You"; // fallback
    }


    private void loadcontact(ContactViewHolder holder, ContactModel contact, int position) {
        if (contact.isAlreadyRegistered() && contact.getUserId() != null && !contact.getUserId().isEmpty()) {

            // Placeholder first
            Glide.with(context)
                    .load(R.drawable.profile_icon)
                    .placeholder(R.drawable.profile_icon)
                    .circleCrop()
                    .into(holder.icon);

            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(contact.getUserId())
                    .child("profilePicBase64");

            // 🔹 Real-time updates for profile icon only
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String base64 = snapshot.getValue(String.class);
                        if (base64 != null && !base64.isEmpty()) {
                            try {
                                byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

                                Glide.with(context)
                                        .load(bitmap)
                                        .placeholder(R.drawable.profile_icon)
                                        .error(R.drawable.profile_icon)
                                        .circleCrop()
                                        .into(holder.icon);

                            } catch (Exception e) {
                                holder.icon.setImageResource(R.drawable.profile_icon);
                            }
                        } else {
                            holder.icon.setImageResource(R.drawable.profile_icon);
                        }
                    } else {
                        holder.icon.setImageResource(R.drawable.profile_icon);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    holder.icon.setImageResource(R.drawable.profile_icon);
                }
            });

        } else {
            holder.icon.setImageResource(R.drawable.profile_icon);
        }
    }

    private void openContactFullScreen(ContactModel contact) {
        if (contact.isAlreadyRegistered() && contact.getUserId() != null && !contact.getUserId().isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(contact.getUserId())
                    .child("profilePicBase64");

            // Fetch once only when user clicks
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String base64 = snapshot.getValue(String.class);
                        if (base64 != null && !base64.isEmpty()) {
                            try {
                                byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

                                File file = saveBitmapToCache(bitmap);
                                if (file != null) {
                                    openFullScreenProfile(file);
                                } else {
                                    openDefaultFullScreen();
                                }
                            } catch (Exception e) {
                                openDefaultFullScreen();
                            }
                        } else {
                            openDefaultFullScreen();
                        }
                    } else {
                        openDefaultFullScreen();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    openDefaultFullScreen();
                }
            });
        } else {
            openDefaultFullScreen();
        }
    }
    private File saveBitmapToCache(Bitmap bitmap) {
        File cacheDir = context.getCacheDir(); // app's cache folder
        File file = new File(cacheDir, "profile_temp.jpg"); // temp file

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    // Open full-screen profile view activity
    private void openFullScreenProfile(File file) {
        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent(context, Full_screen_profile.class);
        intent.putExtra("imagePath", uri.toString());
        context.startActivity(intent);
    }

    // Open full-screen with default icon
    private void openDefaultFullScreen() {
        Intent intent = new Intent(context, Full_screen_profile.class);
        intent.putExtra("imagePath", Uri.parse("android.resource://" + context.getPackageName() + "/" + R.drawable.profile_icon).toString());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    @Override
    public Filter getFilter() {
        return contactFilter;
    }

    private Filter contactFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<ContactModel> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(contactListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (ContactModel contact : contactListFull) {
                    if (contact.getName().toLowerCase().contains(filterPattern) ||
                            contact.getPhoneNumber().toLowerCase().contains(filterPattern)) {
                        filteredList.add(contact);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            contactList.clear();
            contactList.addAll((List) results.values);
            notifyDataSetChanged();
        }

    };
    public static class ContactViewHolder extends RecyclerView.ViewHolder {

        TextView name, phone, invite;
        LinearLayout contactinfo,voicecalltext,videocalltext;
        ImageButton icon,newcallbtn,voicecall,videocall;
        CardView callinfolayout;

        public ContactViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.contact_icon);
            name = itemView.findViewById(R.id.contact_name);
            phone = itemView.findViewById(R.id.contact_phone);
            invite = itemView.findViewById(R.id.contact_invite);
            contactinfo = itemView.findViewById(R.id.contactinfo);
            newcallbtn = itemView.findViewById(R.id.newcallbtn);
            callinfolayout = itemView.findViewById(R.id.callinfo_layout);
            voicecall = itemView.findViewById(R.id.voicecall_icon);
            voicecalltext = itemView.findViewById(R.id.voicecall);
            videocall = itemView.findViewById(R.id.videocall_icon);
            videocalltext = itemView.findViewById(R.id.videocall);


        }
    }
}
