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
// SimpleDateFormat is now used in CallLogEntry's getFormattedCallDate, so not strictly needed here if using that.
// import java.text.SimpleDateFormat;
import java.util.Date; // Still needed if manipulating date objects here, but getFormattedCallDate returns String
import java.util.List;
import java.util.Locale;

public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder> {

    private List<CallLogEntry> callLogList;
    private Context context;

    public CallLogAdapter(Context context, List<CallLogEntry> callLogList) {
        this.context = context;
        this.callLogList = callLogList;
    }

    @NonNull
    @Override
    public CallLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_call_log, parent, false);
        return new CallLogViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CallLogViewHolder holder, int position) {
        CallLogEntry entry = callLogList.get(position);

        holder.textViewPhoneNumber.setText(entry.getPhoneNumber());
        holder.textViewCallDuration.setText(entry.getFormattedDuration());
        holder.textViewCallDate.setText(entry.getFormattedCallDate()); // Uses improved formatting from POJO

        String callTypeStr = entry.getType() != null ? entry.getType().toUpperCase(Locale.ROOT) : "UNKNOWN";
        // Use string resource for "Type: " prefix
        holder.textViewCallTypeLabel.setText(context.getString(R.string.call_log_item_type_label) + callTypeStr);

        // Set call type icon based on string type
        // TODO: Create or find appropriate drawable icons for call types
        switch (callTypeStr) {
            case "INCOMING":
                holder.imageViewCallType.setImageResource(android.R.drawable.sym_call_incoming);
                holder.imageViewCallType.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                break;
            case "OUTGOING":
                holder.imageViewCallType.setImageResource(android.R.drawable.sym_call_outgoing);
                holder.imageViewCallType.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_blue_dark));
                break;
            case "MISSED":
                holder.imageViewCallType.setImageResource(android.R.drawable.sym_call_missed);
                holder.imageViewCallType.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                break;
            case "VOICEMAIL":
                holder.imageViewCallType.setImageResource(android.R.drawable.ic_menu_call); // Placeholder
                holder.imageViewCallType.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
                break;
            case "REJECTED":
            case "BLOCKED":
                holder.imageViewCallType.setImageResource(android.R.drawable.ic_menu_call); // Placeholder
                holder.imageViewCallType.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray));
                break;
            default: // UNKNOWN or other types
                holder.imageViewCallType.setImageResource(android.R.drawable.ic_menu_call);
                holder.imageViewCallType.clearColorFilter();
                break;
        }
    }

    @Override
    public int getItemCount() {
        return callLogList != null ? callLogList.size() : 0;
    }

    public void updateCallLogs(List<CallLogEntry> newLogs) {
        this.callLogList.clear();
        if (newLogs != null) {
            this.callLogList.addAll(newLogs);
        }
        notifyDataSetChanged(); // For simplicity. Use DiffUtil for better performance.
    }

    static class CallLogViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCallType;
        TextView textViewPhoneNumber;
        TextView textViewCallDuration;
        TextView textViewCallDate;
        TextView textViewCallTypeLabel;

        CallLogViewHolder(View view) {
            super(view);
            imageViewCallType = view.findViewById(R.id.imageViewCallType);
            textViewPhoneNumber = view.findViewById(R.id.textViewPhoneNumber);
            textViewCallDuration = view.findViewById(R.id.textViewCallDuration);
            textViewCallDate = view.findViewById(R.id.textViewCallDate);
            textViewCallTypeLabel = view.findViewById(R.id.textViewCallTypeLabel);
        }
    }
}
