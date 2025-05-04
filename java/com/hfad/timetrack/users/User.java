package com.hfad.timetrack.users;

public class User {
    private String id;
    private String name;
    private String profileImage;

    public User() {
    }

    public User(String id, String name, String profileImage) {
        this.id = id;
        this.name = name;
        this.profileImage = profileImage == null || profileImage.isEmpty()
                ? "default_image_url_or_empty"
                : profileImage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getUsername() {
        return name;
    }
}