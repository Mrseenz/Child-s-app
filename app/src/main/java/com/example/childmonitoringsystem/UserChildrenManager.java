package com.example.childmonitoringsystem;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions; // For merge options if needed

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserChildrenManager {

    private static final String TAG = "UserChildrenManager";
    private static final String USERS_COLLECTION = "users";
    private static final String CHILDREN_SUBCOLLECTION = "children";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public interface UserChildrenListener {
        void onChildrenLoaded(List<Child> children); // Using existing Child POJO for simplicity
        void onError(String errorMessage);
    }

    public interface OperationStatusListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public UserChildrenManager() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private String getCurrentParentUid() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        return null;
    }

    public void addChildToFirestore(String childDeviceId, String childName, @NonNull final OperationStatusListener listener) {
        String parentUid = getCurrentParentUid();
        if (parentUid == null) {
            listener.onFailure("User not authenticated.");
            return;
        }

        if (childDeviceId == null || childDeviceId.isEmpty() || childName == null || childName.isEmpty()) {
            listener.onFailure("Child Device ID and Name cannot be empty.");
            return;
        }

        Map<String, Object> childData = new HashMap<>();
        childData.put("childName", childName);
        childData.put("createdAt", FieldValue.serverTimestamp());
        // Add other fields like monitoringPreferences with defaults if needed

        db.collection(USERS_COLLECTION).document(parentUid)
                .collection(CHILDREN_SUBCOLLECTION).document(childDeviceId)
                .set(childData) // Use set() to create or overwrite if it somehow exists
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Child " + childName + " (ID: " + childDeviceId + ") added to Firestore for parent " + parentUid);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding child " + childDeviceId + " to Firestore for parent " + parentUid, e);
                    listener.onFailure(e.getMessage());
                });
    }

    public void updateChildInFirestore(String childDeviceId, String newChildName, @NonNull final OperationStatusListener listener) {
        String parentUid = getCurrentParentUid();
        if (parentUid == null) {
            listener.onFailure("User not authenticated.");
            return;
        }
        if (childDeviceId == null || childDeviceId.isEmpty() || newChildName == null || newChildName.isEmpty()) {
            listener.onFailure("Child Device ID and new Name cannot be empty.");
            return;
        }

        DocumentReference childDocRef = db.collection(USERS_COLLECTION).document(parentUid)
                .collection(CHILDREN_SUBCOLLECTION).document(childDeviceId);

        childDocRef.update("childName", newChildName)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Child " + childDeviceId + " name updated to " + newChildName + " in Firestore for parent " + parentUid);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating child " + childDeviceId + " in Firestore for parent " + parentUid, e);
                    listener.onFailure(e.getMessage());
                });
    }

    public void deleteChildFromFirestore(String childDeviceId, @NonNull final OperationStatusListener listener) {
        String parentUid = getCurrentParentUid();
        if (parentUid == null) {
            listener.onFailure("User not authenticated.");
            return;
        }
        if (childDeviceId == null || childDeviceId.isEmpty()) {
            listener.onFailure("Child Device ID cannot be empty.");
            return;
        }

        db.collection(USERS_COLLECTION).document(parentUid)
                .collection(CHILDREN_SUBCOLLECTION).document(childDeviceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Child " + childDeviceId + " deleted from Firestore for parent " + parentUid);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting child " + childDeviceId + " from Firestore for parent " + parentUid, e);
                    listener.onFailure(e.getMessage());
                });
    }

    public void loadChildrenFromFirestore(@NonNull final UserChildrenListener listener) {
        String parentUid = getCurrentParentUid();
        if (parentUid == null) {
            listener.onError("User not authenticated.");
            return;
        }

        db.collection(USERS_COLLECTION).document(parentUid)
                .collection(CHILDREN_SUBCOLLECTION)
                .orderBy("childName", Query.Direction.ASCENDING) // Optional: order by name
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Child> children = new ArrayList<>();
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                String deviceId = document.getId();
                                String name = document.getString("childName");
                                // Create Child object. The existing Parent App's Child POJO only has name and deviceId.
                                // This is fine for now. If more fields from Firestore are needed in the list, update Child POJO.
                                if (name != null && deviceId != null) {
                                    children.add(new Child(name, deviceId));
                                }
                            }
                        }
                        Log.d(TAG, "Loaded " + children.size() + " children from Firestore for parent " + parentUid);
                        listener.onChildrenLoaded(children);
                    } else {
                        Log.e(TAG, "Error loading children from Firestore for parent " + parentUid, task.getException());
                        listener.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error loading children.");
                    }
                });
    }
}
