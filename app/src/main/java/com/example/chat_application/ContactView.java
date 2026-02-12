package com.example.chat_application;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ContactView extends AppCompatActivity {
    ImageButton backbtn;
    EditText searchcontact;
    RecyclerView recyclerView;
    TextView contactCount;

    List<ContactModel> contactList = new ArrayList<>();
    ContactAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_view);

        backbtn = findViewById(R.id.back_btn_contacts_page);
        searchcontact = findViewById(R.id.search_bar_contact);
        recyclerView = findViewById(R.id.recyclerview_for_contacts);
        contactCount = findViewById(R.id.number_of_contacts);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactAdapter(this, contactList, null);
        recyclerView.setAdapter(adapter);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {

                            String currentUserPhone = querySnapshot.getDocuments()
                                    .get(0)
                                    .getString("phone");
                            currentUserPhone = currentUserPhone.replace("+91", "").replaceAll("\\s+", "");

                            String currentUserName = querySnapshot.getDocuments()
                                    .get(0)
                                    .getString("name");

                            adapter = new ContactAdapter(this, contactList, currentUserPhone);
                            recyclerView.setAdapter(adapter);
                            loadContacts();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ContactView", "Failed to get user phone", e);
                    });
        }


        backbtn.setOnClickListener(v -> finish());


        searchcontact.setCursorVisible(false);
        final boolean[] isPhoneInput = {false};

        searchcontact.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = searchcontact.getCompoundDrawables()[2];
                if (drawableEnd != null) {
                    int drawableWidth = drawableEnd.getBounds().width();


                    if (event.getX() >= (searchcontact.getWidth() - searchcontact.getPaddingEnd() - drawableWidth)) {

                        if (!isPhoneInput[0]) {

                            searchcontact.setInputType(InputType.TYPE_CLASS_PHONE);
                            searchcontact.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.search_icon_xml,
                                    0,
                                    R.drawable.keyboard_icon_xml,
                                    0
                            );
                            searchcontact.setHint("Enter phone number");
                            isPhoneInput[0] = true;
                            searchcontact.setCursorVisible(false);
                            searchcontact.clearFocus();
                        } else {

                            searchcontact.setInputType(InputType.TYPE_CLASS_TEXT);
                            searchcontact.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.search_icon_xml,
                                    0,
                                    R.drawable.dialer_icon_xml,
                                    0
                            );
                            searchcontact.setHint("Enter contact name");
                            isPhoneInput[0] = false;
                            searchcontact.setCursorVisible(false);
                            searchcontact.clearFocus();
                        }
                        return true;
                    }
                }
            }
            return false;
        });

        searchcontact.setOnFocusChangeListener((v, hasFocus) ->
                searchcontact.setCursorVisible(hasFocus));

        searchcontact.setOnClickListener(v -> {
            searchcontact.setCursorVisible(true);
            searchcontact.requestFocus();
        });
        searchcontact.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }


    private void loadContacts() {
        ContentResolver cr = getContentResolver();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        };
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Cursor cursor = cr.query(uri, projection, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

        if (cursor != null) {
            contactList.clear();
            HashSet<String> uniqueNumbers = new HashSet<>();
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String number = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                String photo = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                if(number.startsWith("+91")){
                    number = number.replace("+91","");
                }
                if (number != null) {
                    number = number.replaceAll("\\s+", "");
                }
                if (number != null && uniqueNumbers.add(number) && number.length() == 10) {

                    ContactModel model = new ContactModel(name, number, photo, null, name);
                    contactList.add(model);


                    db.collection("users")
                            .whereEqualTo("phone", "+91" + number)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    String uid = querySnapshot.getDocuments().get(0).getId();


                                    model.setUserId(uid);
                                    adapter.notifyDataSetChanged();
                                }
                            });
                }
            }
            cursor.close();
        }
        if (adapter != null) {
            adapter.contactListFull = new ArrayList<>(contactList);
            adapter.notifyDataSetChanged();
        }

        contactCount.setText(contactList.size() + " Contacts");
    }
}
