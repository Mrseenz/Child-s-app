package com.example.childmonitoringchildapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactsUtil {

    private static final String TAG = "ContactsUtil";

    /**
     * Fetches all contacts from the device.
     * Each contact will include their display name and a list of phone numbers (with types).
     *
     * @param context The application context.
     * @return A list of ContactEntry objects.
     */
    public static List<ContactEntry> getAllContacts(Context context) {
        List<ContactEntry> contactEntries = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();

        // URI for querying contacts
        Uri contactsUri = ContactsContract.Contacts.CONTENT_URI;
        // Columns to fetch from Contacts table
        String[] contactsProjection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.HAS_PHONE_NUMBER // To quickly filter contacts with phone numbers
        };

        Cursor contactsCursor = null;
        try {
            contactsCursor = contentResolver.query(contactsUri, contactsProjection, null, null,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC");

            if (contactsCursor != null && contactsCursor.moveToFirst()) {
                int idIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts._ID);
                int nameIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
                int hasPhoneIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);

                while (contactsCursor.moveToNext()) {
                    long contactId = contactsCursor.getLong(idIndex);
                    String displayName = contactsCursor.getString(nameIndex);
                    int hasPhoneNumber = contactsCursor.getInt(hasPhoneIndex);

                    List<Map<String, String>> phoneNumbers = new ArrayList<>();

                    if (hasPhoneNumber > 0) { // Check if contact has at least one phone number
                        // URI for querying phone numbers for this contact
                        Uri phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                        String[] phoneProjection = {
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.TYPE
                        };
                        // Selection to get phone numbers for the current contactId
                        String phoneSelection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
                        String[] phoneSelectionArgs = {String.valueOf(contactId)};

                        Cursor phoneCursor = null;
                        try {
                            phoneCursor = contentResolver.query(phoneUri, phoneProjection, phoneSelection, phoneSelectionArgs, null);
                            if (phoneCursor != null && phoneCursor.moveToFirst()) {
                                int numberPhoneIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                                int typePhoneIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);

                                do {
                                    String number = phoneCursor.getString(numberPhoneIndex);
                                    int type = phoneCursor.getInt(typePhoneIndex);
                                    String typeLabel = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), type, "");

                                    Map<String, String> phoneDetail = new HashMap<>();
                                    phoneDetail.put("number", number);
                                    phoneDetail.put("type", typeLabel);
                                    phoneNumbers.add(phoneDetail);
                                } while (phoneCursor.moveToNext());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error querying phone numbers for contactId " + contactId, e);
                        } finally {
                            if (phoneCursor != null) {
                                phoneCursor.close();
                            }
                        }
                    }
                    // Only add contact if they have a display name and at least one phone number (optional criteria)
                    // For now, add even if no phone numbers were found after checking HAS_PHONE_NUMBER,
                    // as HAS_PHONE_NUMBER is just a hint. The actual query confirms.
                    if (displayName != null && !displayName.isEmpty()) {
                         contactEntries.add(new ContactEntry(contactId, displayName, phoneNumbers));
                    }
                }
                Log.d(TAG, "Fetched " + contactEntries.size() + " contacts.");
            } else {
                Log.d(TAG, "No contacts found on the device.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: READ_CONTACTS permission might be missing or revoked.", e);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching contacts: ", e);
        } finally {
            if (contactsCursor != null) {
                contactsCursor.close();
            }
        }
        return contactEntries;
    }
}
