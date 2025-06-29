package com.example.childmonitoringsystem;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import java.util.List;
import java.util.Map; // For HashMap if used explicitly, not for casting result.getData()

public class ChildAdapter extends ArrayAdapter<Child> {

    private Context mContext;
    private List<Child> mChildrenList;
    private UserChildrenManager userChildrenManager;

    public ChildAdapter(@NonNull Context context, @NonNull List<Child> list) {
        super(context, 0, list);
        mContext = context;
        mChildrenList = list;
        userChildrenManager = new UserChildrenManager();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_child, parent, false);
        }

        final Child currentChild = getItem(position);
        if (currentChild == null) {
            return listItem;
        }

        TextView name = listItem.findViewById(R.id.textViewChildName);
        name.setText(currentChild.getName());

        TextView deviceIdText = listItem.findViewById(R.id.textViewChildDeviceId);
        deviceIdText.setText(mContext.getString(R.string.device_id_label_prefix) + currentChild.getDeviceId());

        Button buttonEdit = listItem.findViewById(R.id.buttonEditChild);
        Button buttonDelete = listItem.findViewById(R.id.buttonDeleteChild);
        Button buttonSendNotification = listItem.findViewById(R.id.buttonSendNotification);
        Button buttonViewChildMap = listItem.findViewById(R.id.buttonViewChildMap);
        Button buttonPairDevice = listItem.findViewById(R.id.buttonPairDevice);
        Button buttonViewCallLogs = listItem.findViewById(R.id.buttonViewCallLogs);
        Button buttonViewSmsLogs = listItem.findViewById(R.id.buttonViewSmsLogs);
        Button buttonViewContacts = listItem.findViewById(R.id.buttonViewContacts);

        buttonEdit.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, AddChildActivity.class);
            intent.putExtra(IntentKeys.CHILD_NAME_TO_EDIT, currentChild.getName());
            intent.putExtra(IntentKeys.CHILD_DEVICE_ID_TO_EDIT, currentChild.getDeviceId());
            mContext.startActivity(intent);
        });

        buttonPairDevice.setOnClickListener(v -> callGeneratePairingCodeFunction(currentChild));

        buttonViewChildMap.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, MapViewActivity.class);
            intent.putExtra(MapViewActivity.EXTRA_DEVICE_ID, currentChild.getDeviceId());
            mContext.startActivity(intent);
        });

        buttonSendNotification.setOnClickListener(v -> showSendNotificationDialog(currentChild));

        buttonViewCallLogs.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ViewCallLogsActivity.class);
            intent.putExtra(ViewCallLogsActivity.EXTRA_DEVICE_ID, currentChild.getDeviceId());
            intent.putExtra(ViewCallLogsActivity.EXTRA_CHILD_NAME, currentChild.getName());
            mContext.startActivity(intent);
        });

        buttonViewSmsLogs.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ViewSmsLogsActivity.class);
            intent.putExtra(ViewSmsLogsActivity.EXTRA_DEVICE_ID, currentChild.getDeviceId());
            intent.putExtra(ViewSmsLogsActivity.EXTRA_CHILD_NAME, currentChild.getName());
            mContext.startActivity(intent);
        });

        buttonViewContacts.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ViewContactsActivity.class);
            intent.putExtra(ViewContactsActivity.EXTRA_DEVICE_ID, currentChild.getDeviceId());
            intent.putExtra(ViewContactsActivity.EXTRA_CHILD_NAME, currentChild.getName());
            mContext.startActivity(intent);
        });

        buttonDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(mContext)
                .setTitle(mContext.getString(R.string.confirm_delete_dialog_title))
                .setMessage(mContext.getString(R.string.confirm_delete_dialog_message, currentChild.getName()))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    // First, delete from local SharedPreferences
                    ChildPersistenceManager.deleteChild(mContext.getApplicationContext(), currentChild);

                    // Then, delete from Firestore
                    userChildrenManager.deleteChildFromFirestore(currentChild.getDeviceId(), new UserChildrenManager.OperationStatusListener() {
                        @Override
                        public void onSuccess() {
                            Log.d("ChildAdapter", "Child " + currentChild.getDeviceId() + " also deleted from Firestore.");
                        }
                        @Override
                        public void onFailure(String errorMessage) {
                            Log.e("ChildAdapter", "Failed to delete child " + currentChild.getDeviceId() + " from Firestore: " + errorMessage);
                            Toast.makeText(mContext, "Cloud delete failed: " + errorMessage + ". Local data removed.", Toast.LENGTH_LONG).show();
                            // Note: Local data is already removed. UX might need to handle this potential inconsistency.
                        }
                    });

                    // Update UI
                    mChildrenList.remove(currentChild); // Or remove by position
                    notifyDataSetChanged();
                    Toast.makeText(mContext, mContext.getString(R.string.deleted_child_toast_format, currentChild.getName()), Toast.LENGTH_SHORT).show();

                    if (mContext instanceof ViewChildrenActivity) {
                        ((ViewChildrenActivity) mContext).updateUI();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        });
        return listItem;
    }

    private void showSendNotificationDialog(final Child child) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.send_notification_dialog_title, child.getName()));
        // ... (rest of dialog implementation as before)
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        final EditText titleInput = new EditText(mContext);
        titleInput.setHint(R.string.notification_title_hint);
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(titleInput);

        final EditText messageInput = new EditText(mContext);
        messageInput.setHint(R.string.notification_message_hint);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        messageInput.setMinLines(2);
        layout.addView(messageInput);

        builder.setView(layout);
        builder.setPositiveButton(R.string.dialog_button_send, (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String message = messageInput.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(mContext, R.string.notification_title_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (message.isEmpty()) {
                Toast.makeText(mContext, R.string.notification_message_required, Toast.LENGTH_SHORT).show();
                return;
            }
            callSendNotificationFunction(child.getDeviceId(), title, message);
        });
        builder.setNegativeButton(R.string.dialog_button_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void callSendNotificationFunction(String targetDeviceId, String title, String message) {
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("targetDeviceId", targetDeviceId);
        data.put("messageTitle", title);
        data.put("messageBody", message);

        functions.getHttpsCallable("sendNotification").call(data)
            .addOnCompleteListener((Task<HttpsCallableResult> task) -> {
                if (task.isSuccessful()) {
                    Toast.makeText(mContext, R.string.notification_sent_successfully_toast, Toast.LENGTH_SHORT).show();
                } else {
                    Exception e = task.getException();
                    Log.e("ChildAdapter", "Error calling cloud function", e);
                    String errorMessage = e != null ? e.getMessage() : "Unknown error";
                    Toast.makeText(mContext, mContext.getString(R.string.notification_send_failed_toast, errorMessage), Toast.LENGTH_LONG).show();
                }
            });
    }

    private void showPairingCodeDialog(String childName, String pairingCode, String expiresAtStr) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.pairing_code_dialog_title, childName));
        // ... (rest of dialog implementation as before) ...
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        TextView codeLabel = new TextView(mContext);
        codeLabel.setText(R.string.pairing_code_label);
        layout.addView(codeLabel);

        TextView codeText = new TextView(mContext);
        codeText.setText(pairingCode);
        codeText.setTextIsSelectable(true);
        codeText.setTextSize(20f);
        StyleSpan boldSpan = new StyleSpan(android.graphics.Typeface.BOLD);
        SpannableString spannableCode = new SpannableString(pairingCode);
        spannableCode.setSpan(boldSpan, 0, pairingCode.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        codeText.setText(spannableCode);
        layout.addView(codeText);

        TextView expiryText = new TextView(mContext);
        expiryText.setText(mContext.getString(R.string.pairing_code_expires_at_label, expiresAtStr));
        layout.addView(expiryText);

        builder.setView(layout);
        builder.setPositiveButton(R.string.button_copy_code, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("PairingCode", pairingCode);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(mContext, R.string.pairing_code_copied_toast, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.button_close_dialog, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void callGeneratePairingCodeFunction(final Child child) {
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("childDeviceId", child.getDeviceId());
        data.put("childName", child.getName());

        functions.getHttpsCallable("generatePairingCode").call(data)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    HttpsCallableResult result = task.getResult();
                    if (result != null && result.getData() != null) {
                        // Assuming result.getData() is already a Map<String, String> or can be cast
                        @SuppressWarnings("unchecked") // Suppress warning for this cast
                        Map<String, String> resultData = (Map<String, String>) result.getData();
                        String pairingCode = resultData.get("pairingCode");
                        String expiresAt = resultData.get("expiresAt");
                        if (pairingCode != null && expiresAt != null) {
                            Toast.makeText(mContext, R.string.pairing_code_generated_toast, Toast.LENGTH_SHORT).show();
                            showPairingCodeDialog(child.getName(), pairingCode, expiresAt);
                        } else {
                            Toast.makeText(mContext, mContext.getString(R.string.pairing_code_generation_failed_toast,"Missing data in response"), Toast.LENGTH_LONG).show();
                        }
                    } else {
                         Toast.makeText(mContext, mContext.getString(R.string.pairing_code_generation_failed_toast,"Empty response data"), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Exception e = task.getException();
                    Log.e("ChildAdapter", "Error calling generatePairingCode function", e);
                    String errorMessage = e != null ? e.getMessage() : "Unknown error";
                    Toast.makeText(mContext, mContext.getString(R.string.pairing_code_generation_failed_toast, errorMessage), Toast.LENGTH_LONG).show();
                }
            });
    }
}
