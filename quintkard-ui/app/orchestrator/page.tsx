"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "../../components/app-shell";
import { AuthGuard } from "../../components/auth-guard";
import { getStoredAuth } from "../../lib/auth";
import { apiFetch } from "../../lib/api";
import type {
  AgentConfigMetadataResponse,
  AgentModelConfigResponse,
  AgentSummaryResponse,
  OrchestratorConfigRequest,
  OrchestratorConfigResponse
} from "../../lib/types";

const defaultForm: OrchestratorConfigRequest = {
  filteringPrompt: "",
  filteringModel: "",
  routingPrompt: "",
  routingModel: "",
  activeAgentIds: []
};

export default function OrchestratorPage() {
  const [agents, setAgents] = useState<AgentSummaryResponse[]>([]);
  const [models, setModels] = useState<AgentModelConfigResponse[]>([]);
  const [form, setForm] = useState<OrchestratorConfigRequest>(defaultForm);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    void loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    setError("");

    try {
      const [agentsResponse, modelsResponse, orchestratorResponse] = await Promise.all([
        apiFetch({
          path: "/api/agents",
          headers: {
            Authorization: getStoredAuth()
          }
        }),
        apiFetch({
          path: "/api/agents/config",
          headers: {
            Authorization: getStoredAuth()
          }
        }),
        apiFetch({
          path: "/api/orchestrator",
          headers: {
            Authorization: getStoredAuth()
          }
        })
      ]);

      if (!agentsResponse.ok || !modelsResponse.ok || !orchestratorResponse.ok) {
        throw new Error("Unable to load orchestrator workspace.");
      }

      const agentData: AgentSummaryResponse[] = await agentsResponse.json();
      const modelData: AgentConfigMetadataResponse = await modelsResponse.json();
      const orchestratorData: OrchestratorConfigResponse = await orchestratorResponse.json();

      setAgents(agentData);
      setModels(modelData.models);
      setForm({
        filteringPrompt: orchestratorData.filteringPrompt,
        filteringModel: orchestratorData.filteringModel || modelData.models[0]?.id || "",
        routingPrompt: orchestratorData.routingPrompt,
        routingModel: orchestratorData.routingModel || modelData.models[0]?.id || "",
        activeAgentIds: orchestratorData.activeAgents.map((agent) => agent.id)
      });
    } catch (requestError) {
      setError(
        requestError instanceof Error ? requestError.message : "Unable to load orchestrator workspace."
      );
    } finally {
      setLoading(false);
    }
  }

  function updateField<Key extends keyof OrchestratorConfigRequest>(
    key: Key,
    value: OrchestratorConfigRequest[Key]
  ) {
    setForm((current) => ({
      ...current,
      [key]: value
    }));
  }

  function toggleAgent(agentId: string) {
    setForm((current) => ({
      ...current,
      activeAgentIds: current.activeAgentIds.includes(agentId)
        ? current.activeAgentIds.filter((id) => id !== agentId)
        : [...current.activeAgentIds, agentId]
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch({
        path: "/api/orchestrator",
        method: "PUT",
        headers: {
          Authorization: getStoredAuth(),
          "Content-Type": "application/json"
        },
        body: JSON.stringify(form)
      });

      if (!response.ok) {
        throw new Error("Unable to save orchestrator config.");
      }

      const data: OrchestratorConfigResponse = await response.json();
      setForm({
        filteringPrompt: data.filteringPrompt,
        filteringModel: data.filteringModel,
        routingPrompt: data.routingPrompt,
        routingModel: data.routingModel,
        activeAgentIds: data.activeAgents.map((agent) => agent.id)
      });
      setNotice("Orchestrator config saved.");
    } catch (requestError) {
      setError(
        requestError instanceof Error ? requestError.message : "Unable to save orchestrator config."
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <AuthGuard>
      <AppShell
        title="Orchestrator"
        subtitle="Configure filtering, routing, and the active agents available to the orchestrator."
      >
        {(error || notice) && (
          <div className={`message ${error ? "message-error" : "message-success"}`}>
            {error || notice}
          </div>
        )}

        <section className="dashboard-grid">
          <article className="panel">
            <div className="panel-header">
              <span className="panel-kicker">Active Agents</span>
              <span className="panel-badge">
                {loading ? "Loading" : `${form.activeAgentIds.length} selected`}
              </span>
            </div>

            <div className="message-list">
              {agents.map((agent) => {
                const active = form.activeAgentIds.includes(agent.id);
                return (
                  <label
                    className={active ? "message-row message-row-active" : "message-row"}
                    key={agent.id}
                  >
                    <div className="checkbox-row">
                      <input
                        checked={active}
                        onChange={() => toggleAgent(agent.id)}
                        type="checkbox"
                      />
                      <div className="checkbox-copy">
                        <div className="list-card-top">
                          <span>{agent.name}</span>
                          <strong>{agent.model}</strong>
                        </div>
                        <p>{agent.description}</p>
                      </div>
                    </div>
                  </label>
                );
              })}

              {!loading && agents.length === 0 ? (
                <div className="empty-state">Create agents first before configuring orchestration.</div>
              ) : null}
            </div>
          </article>

          <article className="panel">
            <div className="panel-header">
              <span className="panel-kicker">Prompts</span>
              <span className="panel-badge">/api/orchestrator</span>
            </div>

            <form className="profile-form" onSubmit={handleSubmit}>
              <label className="field">
                <span>Filtering model</span>
                <select
                  className="select"
                  value={form.filteringModel}
                  onChange={(event) => updateField("filteringModel", event.target.value)}
                >
                  {models.map((model) => (
                    <option key={model.id} value={model.id}>
                      {model.label}
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                <span>Filtering prompt</span>
                <textarea
                  className="textarea"
                  rows={8}
                  value={form.filteringPrompt}
                  onChange={(event) => updateField("filteringPrompt", event.target.value)}
                />
              </label>

              <label className="field">
                <span>Routing model</span>
                <select
                  className="select"
                  value={form.routingModel}
                  onChange={(event) => updateField("routingModel", event.target.value)}
                >
                  {models.map((model) => (
                    <option key={model.id} value={model.id}>
                      {model.label}
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                <span>Routing prompt</span>
                <textarea
                  className="textarea"
                  rows={10}
                  value={form.routingPrompt}
                  onChange={(event) => updateField("routingPrompt", event.target.value)}
                />
              </label>

              <div className="hero-actions">
                <button className="button button-primary" disabled={saving} type="submit">
                  {saving ? "Saving..." : "Save Orchestrator"}
                </button>
              </div>
            </form>
          </article>
        </section>
      </AppShell>
    </AuthGuard>
  );
}
