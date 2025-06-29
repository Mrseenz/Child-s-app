package com.example.childmonitoringsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class SmsLogAdapter extends RecyclerView.Adapter<SmsLogAdapter.SmsLogViewHolder> {

    private List<SmsLogEntry> smsLogList;
    private Context context;

    public SmsLogAdapter(Context context, List<SmsLogEntry> smsLogList) {
        this.context = context;
        this.smsLogList = smsLogList;
    }

    @NonNull
    @Override
    public SmsLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_sms_log, parent, false);
        return new SmsLogViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsLogViewHolder holder, int position) {
        SmsLogEntry entry = smsLogList.get(position);

        holder.textViewSmsAddress.setText(entry.getAddress());
        holder.textViewSmsDate.setText(entry.getFormattedMessageDate());
        holder.textViewSmsTypeLabel.setText(context.getString(R.string.sms_log_item_type_label) + entry.getFormattedType());
        holder.textViewSmsBodyLength.setText(context.getString(R.string.sms_log_item_length_label) + entry.getBodyLength() + context.getString(R.string.sms_log_item_chars_suffix));
        holder.textViewSmsReadStatus.setText(context.getString(R.string.sms_log_item_status_label) + (entry.isRead() ? context.getString(R.string.sms_log_item_status_read) : context.getString(R.string.sms_log_item_status_unread)));

        // Set SMS type icon (using placeholders for now)
        // TODO: Create or find appropriate drawable icons for SMS types
        String smsTypeStr = entry.getType() != null ? entry.getType().toUpperCase(Locale.ROOT) : "UNKNOWN";
        switch (smsTypeStr) {
            case "INBOX":
                holder.imageViewSmsType.setImageResource(android.R.drawable.sym_action_email); // Placeholder for received
                holder.imageViewSmsType.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                break;
            case "SENT":
                holder.imageViewSmsType.setImageResource(android.R.drawable.ic_menu_send); // Placeholder for sent
                holder.imageViewSmsType.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_blue_light));
                break;
            case "DRAFT":
                holder.imageViewSmsType.setImageResource(android.R.drawable.ic_menu_edit); // Placeholder for draft
                holder.imageViewSmsType.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_light));
                break;
            default:
                holder.imageViewSmsType.setImageResource(android.R.drawable.ic_menu_chat); // Default placeholder
                holder.imageViewSmsType.clearColorFilter();
                break;
        }
        // Hide read status if not an inbox message, or adjust text accordingly
        if (!"INBOX".equals(smsTypeStr)) {
            holder.textViewSmsReadStatus.setVisibility(View.GONE);
        } else {
            holder.textViewSmsReadStatus.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return smsLogList != null ? smsLogList.size() : 0;
    }

    public void updateSmsLogs(List<SmsLogEntry> newLogs) {
        this.smsLogList.clear();
        if (newLogs != null) {
            this.smsLogList.addAll(newLogs);
        }
        notifyDataSetChanged(); // For simplicity. Use DiffUtil for better performance.
    }

    static class SmsLogViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewSmsType;
        TextView textViewSmsAddress;
        TextView textViewSmsDate;
        TextView textViewSmsTypeLabel;
        TextView textViewSmsBodyLength;
        TextView textViewSmsReadStatus;

        SmsLogViewHolder(View view) {
            super(view);
            imageViewSmsType = view.findViewById(R.id.imageViewSmsType);
            textViewSmsAddress = view.findViewById(R.id.textViewSmsAddress);
            textViewSmsDate = view.findViewById(R.id.textViewSmsDate);
            textViewSmsTypeLabel = view.findViewById(R.id.textViewSmsTypeLabel);
            textViewSmsBodyLength = view.findViewById(R.id.textViewSmsBodyLength);
            textViewSmsReadStatus = view.findViewById(R.id.textViewSmsReadStatus);
        }
    }
}
