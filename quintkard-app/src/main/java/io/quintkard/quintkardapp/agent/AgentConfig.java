package io.quintkard.quintkardapp.agent;

import io.quintkard.quintkardapp.common.AuditableEntity;
import io.quintkard.quintkardapp.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "agent_configs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_fk", "name"})
)
public class AgentConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_fk", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private double temperature;

    protected AgentConfig() {
    }

    public AgentConfig(
            User user,
            String name,
            String description,
            String prompt,
            String model,
            double temperature
    ) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.prompt = prompt;
        this.model = model;
        this.temperature = temperature;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getModel() {
        return model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void updateDefinition(
            String name,
            String description,
            String prompt,
            String model,
            double temperature
    ) {
        this.name = name;
        this.description = description;
        this.prompt = prompt;
        this.model = model;
        this.temperature = temperature;
    }
}
