package com.example.childmonitoringsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private List<ContactEntry> contactList;
    // private Context context; // Not strictly needed if all formatting is in POJO

    public ContactsAdapter(List<ContactEntry> contactList) {
        // this.context = context; // If context needed for resources in adapter
        this.contactList = contactList;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_contact, parent, false);
        return new ContactViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactEntry entry = contactList.get(position);

        holder.textViewContactDisplayName.setText(entry.getDisplayName());
        holder.textViewContactPhoneNumbers.setText(entry.getFormattedPhoneNumbers(holder.itemView.getContext()));

        // For debugging contactId, if textViewContactIdDebug was enabled:
        // holder.textViewContactIdDebug.setText("ID: " + entry.getContactId());
    }

    @Override
    public int getItemCount() {
        return contactList != null ? contactList.size() : 0;
    }

    public void updateContacts(List<ContactEntry> newContacts) {
        this.contactList.clear();
        if (newContacts != null) {
            this.contactList.addAll(newContacts);
        }
        notifyDataSetChanged(); // For simplicity. Use DiffUtil for better performance.
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView textViewContactDisplayName;
        TextView textViewContactPhoneNumbers;
        // TextView textViewContactIdDebug; // Optional for debugging

        ContactViewHolder(View view) {
            super(view);
            textViewContactDisplayName = view.findViewById(R.id.textViewContactDisplayName);
            textViewContactPhoneNumbers = view.findViewById(R.id.textViewContactPhoneNumbers);
            // textViewContactIdDebug = view.findViewById(R.id.textViewContactIdDebug);
        }
    }
}
