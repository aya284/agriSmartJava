package entities;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String password;
    private String phone;
    private String address;
    private String documentFile;
    private String image;
    private String status;
    private String cinNumber; // 8-digit Tunisian CIN
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String googleId;

    public User() {}

    public User(String firstName, String lastName, String email,
                String role, String password, String phone, String address, String cinNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.cinNumber = cinNumber;
        this.status = "active";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCinNumber() { return cinNumber; }
    public void setCinNumber(String cinNumber) { this.cinNumber = cinNumber; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getDocumentFile() { return documentFile; }
    public void setDocumentFile(String documentFile) { this.documentFile = documentFile; }

    public String getFullName() { return firstName + " " + lastName; }
}