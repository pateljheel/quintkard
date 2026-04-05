package io.quintkard.quintkardapp.user;

import io.quintkard.quintkardapp.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "redaction_enabled", nullable = false)
    private boolean redactionEnabled;

    protected User() {
    }

    public User(String userId, String displayName, String email, String passwordHash, boolean redactionEnabled) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.redactionEnabled = redactionEnabled;
    }
    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isRedactionEnabled() {
        return redactionEnabled;
    }

    public void updateProfile(String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateRedactionEnabled(boolean redactionEnabled) {
        this.redactionEnabled = redactionEnabled;
    }
}
