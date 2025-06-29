package com.example.childmonitoringsystem;

public class Child {
    private String name;
    private String deviceId;
    // Potentially add a unique ID for database operations later
    // private long id;

    public Child(String name, String deviceId) {
        this.name = name;
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    // Override toString() for simple display in ArrayAdapter if not using custom layout initially
    @Override
    public String toString() {
        return "Name: " + name + "\\nDevice ID: " + deviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Child child = (Child) o;
        // Assuming deviceId is unique and the primary identifier for equality.
        // If name can also uniquely identify or is part of a composite key, adjust accordingly.
        return deviceId != null ? deviceId.equals(child.deviceId) : child.deviceId == null;
    }

    @Override
    public int hashCode() {
        // Using deviceId for hashCode as it's assumed to be the unique identifier.
        return deviceId != null ? deviceId.hashCode() : 0;
    }
}
