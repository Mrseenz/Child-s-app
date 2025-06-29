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

public class ChildAdapter extends ArrayAdapter<Child> {

    private Context mContext;
    private List<Child> mChildrenList;

    public ChildAdapter(@NonNull Context context, @NonNull List<Child> list) {
        super(context, 0, list);
        mContext = context;
        mChildrenList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_child, parent, false);
        }

        Child currentChild = mChildrenList.get(position);

        TextView name = listItem.findViewById(R.id.textViewChildName);
        name.setText(currentChild.getName());

        TextView deviceId = listItem.findViewById(R.id.textViewChildDeviceId);
        deviceId.setText("Device ID: " + currentChild.getDeviceId());

        Button buttonEdit = listItem.findViewById(R.id.buttonEditChild);
        Button buttonDelete = listItem.findViewById(R.id.buttonDeleteChild);
        Button buttonSendNotification = listItem.findViewById(R.id.buttonSendNotification);

        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement edit functionality
                // This might involve opening AddChildActivity with existing data
                // or a new dedicated EditChildActivity
                Toast.makeText(mContext, mContext.getString(R.string.edit_child_toast_format, currentChild.getName()), Toast.LENGTH_SHORT).show();
            }
        });

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement delete confirmation and logic
                // For now, just remove from list and notify adapter
                mChildrenList.remove(position);
                notifyDataSetChanged(); // This will refresh the ListView
                Toast.makeText(mContext, mContext.getString(R.string.deleted_child_toast_format, currentChild.getName()), Toast.LENGTH_SHORT).show();
                // Also update the underlying data source (SharedPreferences, DB, etc.)
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
}
