package android.iocl.sms_handler_8_0_below;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.iocl.dac_collector.ModelData.Message;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.adapter.MessageThreadAdapter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class SMSThreadActivity extends AppCompatActivity implements MessageThreadAdapter.MessageListener {

    public static final String EXTRA_ADDRESS = "extra_address";
    public static final String EXTRA_NAME = "extra_name";

    private RecyclerView rv;
    private MessageThreadAdapter adapter;
    private EditText edtReply;
    private MaterialButton btnSend;
    private String address, name;
    private TextView tvTitle;

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                loadAllMessages();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smsthread);

        tvTitle = findViewById(R.id.tvHeader);
        rv = findViewById(R.id.rvMessages);
        edtReply = findViewById(R.id.edtReply);
        btnSend = findViewById(R.id.btnSend);

        address = getIntent().getStringExtra(EXTRA_ADDRESS);
        name = getIntent().getStringExtra(EXTRA_NAME);
        tvTitle.setText(name != null ? name : address);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageThreadAdapter(this, new ArrayList<>(), this);
        rv.setAdapter(adapter);

        btnSend.setOnClickListener(v -> {
            String text = edtReply.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                sendSms(text);
            }
        });

        // ask for permissions if needed
        if (!hasPermissions()) {
            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS
            });
        } else {
            loadAllMessages();
        }
    }

    private boolean hasPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void loadAllMessages() {
        if (address == null) return;

        new Thread(() -> {
            List<Message> allMessages = new ArrayList<>();
            ContentResolver cr = getContentResolver();
            Uri uri = Telephony.Sms.CONTENT_URI;
            String[] projection = {
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE
            };

            String selection = Telephony.Sms.ADDRESS + "=?";
            String[] selectionArgs = { address };
            String sortOrder = Telephony.Sms.DATE + " ASC";

            try (Cursor cursor = cr.query(uri, projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
                        String addr = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                        String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                        long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                        int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));

                        allMessages.add(new Message(id, addr, body, date, type));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                adapter.setMessages(allMessages);
                if (!allMessages.isEmpty()) {
                    rv.scrollToPosition(adapter.getItemCount() - 1);
                }
            });
        }).start();
    }

    private void sendSms(String text) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SEND_SMS permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(text);
            sms.sendMultipartTextMessage(address, null, parts, null, null);

            // optimistic UI update
            Message sent = new Message("local-" + System.currentTimeMillis(), address, text, System.currentTimeMillis(), Telephony.Sms.MESSAGE_TYPE_SENT);
            runOnUiThread(() -> {
                adapter.addMessage(sent);
                rv.scrollToPosition(adapter.getItemCount() - 1);
                edtReply.setText("");
            });

            // reload to sync with provider
            loadAllMessages();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Send failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLongPressDelete(Message message) {
        new AlertDialog.Builder(this)
                .setTitle("Delete message")
                .setMessage("Delete this message?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteMessage(message))
                .show();
    }

    private void deleteMessage(Message m) {
        try {
            Uri deleteUri = Telephony.Sms.CONTENT_URI;
            int deleted = getContentResolver().delete(deleteUri, Telephony.Sms._ID + "=?", new String[]{m.getId()});
            if (deleted > 0) {
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                loadAllMessages();
            } else {
                Toast.makeText(this, "Failed to delete (are you default SMS app?)", Toast.LENGTH_LONG).show();
            }
        } catch (SecurityException se) {
            Toast.makeText(this, "Delete not allowed: app is not default SMS app", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
