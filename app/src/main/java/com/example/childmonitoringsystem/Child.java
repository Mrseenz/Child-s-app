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
}
