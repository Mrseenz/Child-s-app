package com.example.childmonitoringsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class ChildAdapter extends ArrayAdapter<Child> {

    private Context mContext;
    private List<Child> mChildrenList; // This list is managed by the adapter

    public ChildAdapter(@NonNull Context context, @NonNull List<Child> list) {
        super(context, 0, list);
        mContext = context;
        mChildrenList = list; // Keep a reference to modify it directly
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_child, parent, false);
        }

        // Use getItem(position) for safety, though mChildrenList should be in sync
        final Child currentChild = getItem(position);

        if (currentChild == null) {
            // Should not happen if adapter is managed correctly, but good for safety
            return listItem;
        }

        TextView name = listItem.findViewById(R.id.textViewChildName);
        name.setText(currentChild.getName());

        TextView deviceId = listItem.findViewById(R.id.textViewChildDeviceId);
        deviceId.setText("Device ID: " + currentChild.getDeviceId());

        Button buttonEdit = listItem.findViewById(R.id.buttonEditChild);
        Button buttonDelete = listItem.findViewById(R.id.buttonDeleteChild);
        Button buttonSendNotification = listItem.findViewById(R.id.buttonSendNotification);
        Button buttonViewChildMap = listItem.findViewById(R.id.buttonViewChildMap);

        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, AddChildActivity.class);
                intent.putExtra(IntentKeys.CHILD_NAME_TO_EDIT, currentChild.getName());
                intent.putExtra(IntentKeys.CHILD_DEVICE_ID_TO_EDIT, currentChild.getDeviceId());
                mContext.startActivity(intent);
            }
        });

        buttonViewChildMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MapViewActivity.class);
                intent.putExtra(MapViewActivity.EXTRA_DEVICE_ID, currentChild.getDeviceId());
                mContext.startActivity(intent);
            }
        });

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(mContext)
                    .setTitle(mContext.getString(R.string.confirm_delete_dialog_title))
                    .setMessage(mContext.getString(R.string.confirm_delete_dialog_message, currentChild.getName()))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ChildPersistenceManager.deleteChild(mContext.getApplicationContext(), currentChild);
                            // Remove from the adapter's list and notify
                            mChildrenList.remove(currentChild); // Or use position if getItem(position) was used
                            notifyDataSetChanged();
                            Toast.makeText(mContext, mContext.getString(R.string.deleted_child_toast_format, currentChild.getName()), Toast.LENGTH_SHORT).show();

                            // If ViewChildrenActivity needs to update its "No children" message,
                            // it might need a callback or to re-check count after deletion.
                            // For now, adapter handles its own list. ViewChildrenActivity will refresh onResume.
                            // Alternatively, pass a Runnable or listener to the adapter to call updateUI on ViewChildrenActivity.
                            if (mContext instanceof ViewChildrenActivity) {
                                ((ViewChildrenActivity) mContext).updateUI();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            }
        });

        buttonSendNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement actual notification sending (e.g., via FCM)
                // This would require child's device token, server-side logic, etc.
                String notificationMessage = mContext.getString(R.string.sending_mock_notification_toast_format, currentChild.getName());
                Toast.makeText(mContext, notificationMessage, Toast.LENGTH_LONG).show();

                // Example of what might be sent:
                // sendNotificationToServer(currentChild.getDeviceId(), "Your custom message here");
            }
        });

        return listItem;
    }

    // Helper method to get context, as mContext is private
    // private Context getAdapterContext() { // Not strictly needed as mContext is available
    //     return mContext;
    // }
}
