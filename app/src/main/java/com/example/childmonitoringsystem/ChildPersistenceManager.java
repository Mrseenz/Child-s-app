package com.example.childmonitoringsystem;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ChildPersistenceManager {

    private static final String PREFS_NAME = "ChildMonitorPrefs";
    private static final String CHILDREN_KEY = "childrenList";
    private static Gson gson = new Gson();

    // Helper method to get SharedPreferences instance
    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Loads the list of children from SharedPreferences.
     * @param context the application context
     * @return List of Child objects, or an empty list if none found.
     */
    public static List<Child> loadChildren(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String json = prefs.getString(CHILDREN_KEY, null);
        if (json == null) {
            return new ArrayList<>(); // Return empty list if no data
        }
        Type type = new TypeToken<ArrayList<Child>>() {}.getType();
        List<Child> children = gson.fromJson(json, type);
        return children != null ? children : new ArrayList<Child>(); // Ensure non-null return
    }

    /**
     * Saves the entire list of children to SharedPreferences.
     * This will overwrite any existing list.
     * @param context the application context
     * @param children List of Child objects to save.
     */
    public static void saveChildren(Context context, List<Child> children) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        String json = gson.toJson(children);
        editor.putString(CHILDREN_KEY, json);
        editor.apply();
    }

    /**
     * Adds a single child to the existing list in SharedPreferences.
     * If a child with the same deviceId already exists, it won't be added again (basic check).
     * @param context the application context
     * @param newChild the Child object to add.
     */
    public static void addChild(Context context, Child newChild) {
        List<Child> children = loadChildren(context);
        // Prevent duplicates based on deviceId - uses Child.equals()
        if (!children.contains(newChild)) {
            children.add(newChild);
            saveChildren(context, children);
        }
        // Consider logging or returning a boolean if duplicate was found and not added
    }

    /**
     * Deletes a child from the list in SharedPreferences.
     * Identification is based on Child.equals() (i.e., deviceId).
     * @param context the application context
     * @param childToRemove the Child object to remove.
     */
    public static void deleteChild(Context context, Child childToRemove) {
        List<Child> children = loadChildren(context);
        if (children.remove(childToRemove)) { // uses Child.equals()
            saveChildren(context, children);
        }
        // Consider logging or returning a boolean if child was not found/removed
    }

    /**
     * Updates an existing child in the list in SharedPreferences.
     * The childToUpdate is identified by its deviceId (via Child.equals()).
     * Its details are then replaced with those from updatedChildDetails.
     * Note: This assumes deviceId of updatedChildDetails is the same as childToUpdate,
     * or that childToUpdate is identified by its old state if its deviceId can change.
     * For simplicity, we'll assume deviceId is immutable or the primary key for lookup.
     *
     * @param context the application context
     * @param childToUpdate The original child object (or an object matching its ID) to find in the list.
     * @param updatedChildDetails The child object containing the new details.
     */
    public static boolean updateChild(Context context, Child childToUpdate, Child updatedChildDetails) {
        List<Child> children = loadChildren(context);
        int index = children.indexOf(childToUpdate); // uses Child.equals()

        if (index != -1) {
            // Ensure the updated child details retain the original identifier if it's not part of the editable fields
            // or handle cases where the identifier itself might be updated.
            // For now, we assume updatedChildDetails has the correct, potentially updated, information.
            children.set(index, updatedChildDetails);
            saveChildren(context, children);
            return true;
        }
        return false; // Child not found
    }
}
