package android.iocl.sms_handler_8_0_below;


import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.iocl.dac_collector.ModelData.Conversation;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Receivers.smsReceivers;
import android.iocl.dac_collector.adapter.ConversationAdapter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsActivity extends AppCompatActivity implements ConversationAdapter.ConversationListener {


    private RecyclerView rv;
    private ConversationAdapter adapter;
    private FloatingActionButton fab;
    private SearchView searchView;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sms);

        hideSystemBars();

        EdgeToEdge.enable(this);





        rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter(this, new ArrayList<>(), this);
        rv.setAdapter(adapter);

        searchView = findViewById(R.id.searchView);
        fab = findViewById(R.id.fabCompose);

        fab.setOnClickListener(v -> showComposeDialog());

        int id = searchView.getContext()
                .getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(id);
        if (searchPlate != null) {
            searchPlate.setBackground(null); // removes underline
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });



       loadConversations();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars());
                insetsController.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
    }



    private void loadConversations() {
        new Thread(() -> {
            List<Conversation> conversations = queryConversations(this);
            runOnUiThread(() -> adapter.setData(conversations));
        }).start();
    }

    private void showComposeDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_compose_custom, null);
        AutoCompleteTextView etNumber = v.findViewById(R.id.edtRecipientAuto);
        EditText etMessage = v.findViewById(R.id.edtMessageAuto);
        Button btnSend = v.findViewById(R.id.btnSendDialog);
        Button btnCancel = v.findViewById(R.id.btnCancelDialog);

        // adapter backed by contact suggestions (simple string list: "Name <number>")
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
        etNumber.setAdapter(adapter);
        etNumber.setThreshold(1);

        // prefilling suggestions initially (could be empty if permission missing)
        List<String> initial = getContactSuggestions("");
        adapter.clear();
        adapter.addAll(initial);
        adapter.notifyDataSetChanged();

        // update suggestions as user types (lightweight)
        etNumber.addTextChangedListener(new TextWatcher() {
            private Runnable r;
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                final String q = s.toString();
                // fetch suggestions on background thread to avoid UI freeze for big addressbooks
                new Thread(() -> {
                    List<String> res = getContactSuggestions(q);
                    runOnUiThread(() -> {
                        adapter.clear();
                        adapter.addAll(res);
                        adapter.notifyDataSetChanged();
                        if (!res.isEmpty() && etNumber.isPopupShowing()) {
                            etNumber.showDropDown();
                        }
                    });
                }).start();
            }
        });

        // When user taps a suggestion, extract the phone number part
        etNumber.setOnItemClickListener((parent, view1, position, id) -> {
            String picked = adapter.getItem(position);
            if (picked != null) {
                String number = extractNumberFromSuggestion(picked);
                if (number != null) etNumber.setText(number);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppTheme)
                .setView(v)
                .create();

        btnCancel.setOnClickListener(x -> dialog.dismiss());
        btnSend.setOnClickListener(x -> {
            String num = etNumber.getText().toString().trim();
            String msg = etMessage.getText().toString().trim();
            if (TextUtils.isEmpty(num) || TextUtils.isEmpty(msg)) {
                Toast.makeText(this, "Number and message required", Toast.LENGTH_SHORT).show();
                return;
            }

            // basic validation: strip spaces and non-digit symbols except +
            String normalized = num.replaceAll("[^0-9+]", "");
            sendSms(normalized, msg);
            dialog.dismiss();
        });

        dialog.show();
    }

    // returns list of "DisplayName <number>" matching the query.
// requires READ_CONTACTS permission.
    private List<String> getContactSuggestions(String query) {
        List<String> suggestions = new ArrayList<>();



        String sel = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ? OR "
                + ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?";
        String like = "%" + query + "%";
        String[] selArgs = new String[]{like, like};

        Cursor c = null;
        try {
            c = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    sel,
                    selArgs,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (c != null) {
                while (c.moveToNext()) {
                    String name = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    // format suggestion
                    String item = name + " <" + number + ">";
                    if (!suggestions.contains(item)) suggestions.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
        }
        return suggestions;
    }

    private String extractNumberFromSuggestion(String suggestion) {
        // suggestion format: "Name <number>"
        int lt = suggestion.indexOf('<');
        int gt = suggestion.indexOf('>');
        if (lt >= 0 && gt > lt) {
            return suggestion.substring(lt + 1, gt).trim();
        }
        // fallback: attempt to pull last token that looks like number
        String[] parts = suggestion.split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String p = parts[i].replaceAll("[^0-9+]", "");
            if (p.length() >= 6) return p;
        }
        return null;
    }


    private void sendSms(String number, String text) {

        try {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(text);
            sms.sendMultipartTextMessage(number, null, parts, null, null);
            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
            loadConversations();
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + Uri.encode(number)));
            intent.putExtra("sms_body", text);
            try {
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Failed to send message: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }



    // --- Cache-backed queryConversations (replace the existing method) ---
    private static final String CACHE_FILENAME = "sms_cache.json";
    private static final long CACHE_TTL_MS = 30 * 1000L; // 30 seconds

    public static List<Conversation> queryConversations(Context ctx) {
        // Try load from cache first
        try {
            List<Conversation> cached = loadConversationsFromCache(ctx);
            if (cached != null && !cached.isEmpty()) {
                // If cache is present, decide whether to trigger a background refresh
                File cacheFile = new File(ctx.getFilesDir(), CACHE_FILENAME);
                long age = System.currentTimeMillis() - cacheFile.lastModified();
                if (age > CACHE_TTL_MS) {
                    // refresh in background (non-blocking)
                    new Thread(() -> {
                        List<Conversation> fresh = fetchConversationsFromProvider(ctx);
                        if (fresh != null && !fresh.isEmpty()) {
                            saveConversationsToCache(ctx, fresh);
                        }
                    }).start();
                }
                return cached;
            }
        } catch (Throwable t) {
            // If cache read fails, fall through to fresh query
            t.printStackTrace();
        }

        List<Conversation> fresh = fetchConversationsFromProvider(ctx);


        Log.d("Senderr", "fetchConversationsFromProvider: " + fresh.get(0).getAddress());


        if (fresh != null && !fresh.isEmpty()) {
            saveConversationsToCache(ctx, fresh);
        }
        return (fresh != null) ? fresh : Collections.emptyList();
    }

    // --- Helper: query the provider exactly like original method (refactored) ---
    private static List<Conversation> fetchConversationsFromProvider(Context ctx) {
        Map<Long, Conversation> map = new HashMap<>();
        ContentResolver cr = ctx.getContentResolver();

        Uri uri = Telephony.Sms.CONTENT_URI;
        String[] projection = new String[]{
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.THREAD_ID
        };

        Cursor c = null;
        try {
            // Add sorting by date to ensure we get latest messages first
            c = cr.query(uri, projection, null, null, Telephony.Sms.DATE + " DESC");
            if (c == null) return Collections.emptyList();

            while (c.moveToNext()) {
                long threadId = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID));
                String id = c.getString(c.getColumnIndexOrThrow(Telephony.Sms._ID));
                String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
                int type = c.getInt(c.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                long date = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE));
                String rawAddress = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));

                Log.d("SMS_FETCH", "ThreadId: " + threadId + ", Address: " + rawAddress +
                        ", Body: " + body + ", Type: " + type + ", Date: " + date);

                // Check if this is an alphanumeric sender
                if (rawAddress != null && !rawAddress.matches("\\d+")) {
                    Log.d("SMS_FETCH", "Found alphanumeric sender: " + rawAddress);
                }

                Conversation conv = map.get(threadId);
                if (conv == null) {
                    conv = new Conversation();
                    conv.setThread_id(threadId); // Make sure you store threadId
                    conv.setAddress(rawAddress);
                    conv.setContactName(getDisplayName(ctx, rawAddress)); // Use custom method
                    conv.setPhotoUri(lookupContactPhoto(ctx, rawAddress));
                    conv.setLastMessage(body);
                    conv.setTimestamp(date);
                    conv.setFirstMsgId(id);
                    conv.setLastMessageType(type);
                    conv.setMessageCount(1);
                    map.put(threadId, conv);
                } else {
                    conv.setMessageCount(conv.getMessageCount() + 1);
                    // Since we're sorting DESC, first message we see is latest
                    // No need to update if we're processing in DESC order
                }
            }
        } catch (Exception e) {
            Log.e("SMS_FETCH", "Error fetching SMS", e);
        } finally {
            if (c != null) c.close();
        }

        return new ArrayList<>(map.values());
    }

    private static String getDisplayName(Context context, String address) {
        if (address == null) {
            return "Unknown";
        }

        // If it's alphanumeric (company SMS), use it as is
        if (!address.matches("\\d+")) {
            return address;
        }

        // For numeric addresses, you can lookup contact name
        return lookupContactName(context, address); // Your existing method
    }




    // --- Helper: save to internal cache file as JSON ---
    private static void saveConversationsToCache(Context ctx, List<Conversation> convs) {
        if (convs == null) return;
        File cache = new File(ctx.getFilesDir(), CACHE_FILENAME);
        JSONArray arr = new JSONArray();
        try {
            for (Conversation conv : convs) {
                JSONObject o = new JSONObject();
                o.put("address", conv.getAddress());
                o.put("contactName", conv.getContactName());
                o.put("photoUri", conv.getPhotoUri());
                o.put("lastMessage", conv.getLastMessage());
                o.put("timestamp", conv.getTimestamp());
                o.put("firstMsgId", conv.getFirstMsgId());
                o.put("lastMessageType", conv.getLastMessageType());
                o.put("messageCount", conv.getMessageCount());
                arr.put(o);
            }
            try (OutputStream os = new FileOutputStream(cache)) {
                os.write(arr.toString().getBytes("UTF-8"));
                os.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Helper: load from internal cache file ---
    private static List<Conversation> loadConversationsFromCache(Context ctx) {
        File cache = new File(ctx.getFilesDir(), CACHE_FILENAME);
        if (!cache.exists()) return null;
        try (InputStream is = new FileInputStream(cache)) {
            int size = (int) cache.length();
            byte[] data = new byte[size];
            int read = is.read(data);
            if (read <= 0) return null;
            String s = new String(data, 0, read, "UTF-8");
            JSONArray arr = new JSONArray(s);
            List<Conversation> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Conversation conv = new Conversation();
                conv.setAddress(o.optString("address", null));
                conv.setContactName(o.optString("contactName", null));
                conv.setPhotoUri(o.optString("photoUri", null));
                conv.setLastMessage(o.optString("lastMessage", null));
                conv.setTimestamp(o.optLong("timestamp", 0L));
                conv.setFirstMsgId(o.optString("firstMsgId", null));
                conv.setLastMessageType(o.optInt("lastMessageType", 0));
                conv.setMessageCount(o.optInt("messageCount", 1));
                out.add(conv);
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }





    private static String lookupContactName(Context ctx, String phoneNumber) {
        if (phoneNumber == null) return null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String name = null;
        try (Cursor cursor = ctx.getContentResolver().query(uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }
    private static String lookupContactPhoto(Context ctx, String phoneNumber) {
        if (phoneNumber == null) return null;
        Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
        );
        String photoUri = null;
        try (Cursor cursor = ctx.getContentResolver().query(
                uri,
                new String[]{ContactsContract.PhoneLookup.PHOTO_URI},
                null, null, null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                photoUri = cursor.getString(0); // may be null if no photo
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return photoUri;
    }


    @Override
    public void onDeleteConversation(Conversation conv) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("Delete all messages with " + (conv.getContactName() != null ? conv.getContactName() : conv.getAddress()) + "?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteConversation(conv))
                .show();
    }

    private void deleteConversation(Conversation conv) {
        try {
            getContentResolver().delete(Telephony.Sms.CONTENT_URI, Telephony.Sms.ADDRESS + "=?", new String[]{conv.getAddress()});
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
            loadConversations();
        } catch (Exception e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }




}