package io.quintkard.quintkardapp.orchestrator;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.common.AuditableEntity;
import io.quintkard.quintkardapp.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "orchestrator_configs")
public class OrchestratorConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_fk", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String filteringPrompt;

    @Column(nullable = false)
    private String filteringModel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String routingPrompt;

    @Column(nullable = false)
    private String routingModel;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "orchestration_active_agents",
            joinColumns = @JoinColumn(name = "orchestration_config_fk"),
            inverseJoinColumns = @JoinColumn(name = "agent_fk")
    )
    private Set<AgentConfig> activeAgents = new LinkedHashSet<>();

    protected OrchestratorConfig() {
    }

    public OrchestratorConfig(
            User user,
            String filteringPrompt,
            String filteringModel,
            String routingPrompt,
            String routingModel,
            Set<AgentConfig> activeAgents
    ) {
        this.user = user;
        this.filteringPrompt = filteringPrompt;
        this.filteringModel = filteringModel;
        this.routingPrompt = routingPrompt;
        this.routingModel = routingModel;
        this.activeAgents = new LinkedHashSet<>(activeAgents);
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getFilteringPrompt() {
        return filteringPrompt;
    }

    public String getFilteringModel() {
        return filteringModel;
    }

    public String getRoutingPrompt() {
        return routingPrompt;
    }

    public String getRoutingModel() {
        return routingModel;
    }

    public Set<AgentConfig> getActiveAgents() {
        return activeAgents;
    }

    public void update(
            String filteringPrompt,
            String filteringModel,
            String routingPrompt,
            String routingModel,
            Set<AgentConfig> activeAgents
    ) {
        this.filteringPrompt = filteringPrompt;
        this.filteringModel = filteringModel;
        this.routingPrompt = routingPrompt;
        this.routingModel = routingModel;
        this.activeAgents = new LinkedHashSet<>(activeAgents);
    }
}
