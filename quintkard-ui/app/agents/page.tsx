"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "../../components/app-shell";
import { AuthGuard } from "../../components/auth-guard";
import { getStoredAuth } from "../../lib/auth";
import { apiFetch } from "../../lib/api";
import type {
  AgentConfigMetadataResponse,
  AgentConfigRequest,
  AgentConfigResponse,
  AgentSummaryResponse,
  AgentModelConfigResponse
} from "../../lib/types";

function buildHeaders() {
  return {
    Authorization: getStoredAuth(),
    "Content-Type": "application/json"
  };
}

const defaultForm: AgentConfigRequest = {
  name: "",
  description: "",
  prompt: "",
  model: "",
  temperature: 0.7
};

export default function AgentsPage() {
  const [agents, setAgents] = useState<AgentSummaryResponse[]>([]);
  const [models, setModels] = useState<AgentModelConfigResponse[]>([]);
  const [defaultAgentModelId, setDefaultAgentModelId] = useState("");
  const [selectedAgentId, setSelectedAgentId] = useState("");
  const [form, setForm] = useState<AgentConfigRequest>(defaultForm);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const selectedModel =
    models.find((model) => model.id === form.model) ??
    models[0] ??
    null;

  async function loadAgents() {
    setLoading(true);
    setError("");

    try {
      const response = await apiFetch({
        path: "/api/agents",
        headers: {
          Authorization: getStoredAuth()
        }
      });

      if (!response.ok) {
        throw new Error("Unable to load agents.");
      }

      const data: AgentSummaryResponse[] = await response.json();
      setAgents(data);

      if (data.length > 0 && !selectedAgentId) {
        await selectAgent(data[0].id);
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load agents.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadAgentConfig();
    void loadAgents();
  }, []);

  async function loadAgentConfig() {
    try {
      const response = await apiFetch({
        path: "/api/agents/config",
        headers: {
          Authorization: getStoredAuth()
        }
      });

      if (!response.ok) {
        throw new Error("Unable to load agent config.");
      }

      const data: AgentConfigMetadataResponse = await response.json();
      setModels(data.models);
      setDefaultAgentModelId(data.defaultAgentModelId);

      const defaultModel =
        data.models.find((model) => model.id === data.defaultAgentModelId) ??
        data.models[0];

      if (defaultModel) {
        setForm((current) => ({
          ...current,
          model: current.model || defaultModel.id,
          temperature:
            current.model === defaultModel.id && current.temperature
              ? current.temperature
              : defaultModel.defaultTemperature
        }));
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load agent config.");
    }
  }

  async function selectAgent(agentId: string) {
    setSelectedAgentId(agentId);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch({
        path: `/api/agents/${agentId}`,
        headers: {
          Authorization: getStoredAuth()
        }
      });

      if (!response.ok) {
        throw new Error("Unable to load agent details.");
      }

      const agent: AgentConfigResponse = await response.json();
      setForm({
        name: agent.name,
        description: agent.description,
        prompt: agent.prompt,
        model: agent.model,
        temperature: agent.temperature
      });
    } catch (requestError) {
      setError(
        requestError instanceof Error ? requestError.message : "Unable to load agent details."
      );
    }
  }

  function resetForm() {
    setSelectedAgentId("");
    const defaultModel =
      models.find((model) => model.id === defaultAgentModelId) ??
      models[0];
    setForm({
      ...defaultForm,
      model: defaultModel?.id ?? defaultForm.model,
      temperature: defaultModel?.defaultTemperature ?? defaultForm.temperature
    });
    setError("");
    setNotice("");
  }

  function updateField<Key extends keyof AgentConfigRequest>(key: Key, value: AgentConfigRequest[Key]) {
    setForm((current) => ({
      ...current,
      [key]: value
    }));
  }

  function handleModelChange(modelId: string) {
    const model = models.find((item) => item.id === modelId);
    if (!model) {
      return;
    }

    setForm((current) => ({
      ...current,
      model: model.id,
      temperature: Math.min(
        model.maxTemperature,
        Math.max(model.minTemperature, current.temperature || model.defaultTemperature)
      )
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setNotice("");

    try {
      const method = selectedAgentId ? "PUT" : "POST";

      const response = await apiFetch({
        path: selectedAgentId ? `/api/agents/${selectedAgentId}` : "/api/agents",
        method,
        headers: buildHeaders(),
        body: JSON.stringify({
          ...form,
          temperature: Number(form.temperature)
        })
      });

      if (!response.ok) {
        throw new Error(selectedAgentId ? "Unable to update agent." : "Unable to create agent.");
      }

      const data: AgentConfigResponse = await response.json();
      await loadAgents();
      await selectAgent(data.id);
      setNotice(selectedAgentId ? "Agent updated." : "Agent created.");
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : selectedAgentId
            ? "Unable to update agent."
            : "Unable to create agent."
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!selectedAgentId) {
      return;
    }

    setSaving(true);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch({
        path: `/api/agents/${selectedAgentId}`,
        method: "DELETE",
        headers: {
          Authorization: getStoredAuth()
        }
      });

      if (!response.ok) {
        throw new Error("Unable to delete agent.");
      }

      const remainingAgents = agents.filter((agent) => agent.id !== selectedAgentId);
      setAgents(remainingAgents);
      setNotice("Agent deleted.");
      if (remainingAgents.length > 0) {
        await selectAgent(remainingAgents[0].id);
      } else {
        resetForm();
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to delete agent.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <AuthGuard>
      <AppShell
        title="Agents"
        subtitle="Manage saved agent definitions, prompts, models, and generation settings."
      >
        {(error || notice) && (
          <div className={`message ${error ? "message-error" : "message-success"}`}>
            {error || notice}
          </div>
        )}

        <section className="dashboard-grid">
          <article className="panel">
            <div className="panel-header">
              <span className="panel-kicker">Saved Agents</span>
              <span className="panel-badge">{loading ? "Loading" : `${agents.length} total`}</span>
            </div>

            <div className="hero-actions">
              <button className="button button-primary" onClick={resetForm} type="button">
                New Agent
              </button>
            </div>

            <div className="message-list">
              {agents.map((agent) => (
                <button
                  className={
                    selectedAgentId === agent.id ? "message-row message-row-active" : "message-row"
                  }
                  key={agent.id}
                  onClick={() => void selectAgent(agent.id)}
                  type="button"
                >
                  <div className="list-card-top">
                    <span>{agent.name}</span>
                    <strong>{agent.model}</strong>
                  </div>
                  <p>{agent.description}</p>
                  <div className="message-meta">
                    <span>Temperature {agent.temperature.toFixed(1)}</span>
                    <span>{agent.userId}</span>
                  </div>
                </button>
              ))}

              {!loading && agents.length === 0 ? (
                <div className="empty-state">No agents yet. Create the first one from the form.</div>
              ) : null}
            </div>
          </article>

          <article className="panel">
            <div className="panel-header">
              <span className="panel-kicker">{selectedAgentId ? "Edit Agent" : "Create Agent"}</span>
              <span className="panel-badge">{selectedAgentId ? "PUT /api/agents/{id}" : "POST /api/agents"}</span>
            </div>

            <form className="profile-form" onSubmit={handleSubmit}>
              <div className="form-grid">
                <label className="field">
                  <span>Name</span>
                  <input
                    value={form.name}
                    onChange={(event) => updateField("name", event.target.value)}
                  />
                </label>

                <label className="field">
                  <span>Model</span>
                  <select
                    className="select"
                    value={form.model}
                    onChange={(event) => handleModelChange(event.target.value)}
                  >
                    {models.map((model) => (
                      <option key={model.id} value={model.id}>
                        {model.label}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <label className="field">
                <span>Description</span>
                <textarea
                  className="textarea"
                  rows={4}
                  value={form.description}
                  onChange={(event) => updateField("description", event.target.value)}
                />
              </label>

              <label className="field">
                <span>Prompt</span>
                <textarea
                  className="textarea"
                  rows={10}
                  value={form.prompt}
                  onChange={(event) => updateField("prompt", event.target.value)}
                />
              </label>

              <label className="field field-narrow">
                <span>Temperature</span>
                <input
                  max={selectedModel?.maxTemperature ?? 2}
                  min={selectedModel?.minTemperature ?? 0}
                  step="0.1"
                  type="number"
                  value={form.temperature}
                  onChange={(event) => updateField("temperature", Number(event.target.value))}
                />
              </label>

              {selectedModel ? (
                <div className="hint-text">
                  {selectedModel.label} supports temperature from{" "}
                  {selectedModel.minTemperature.toFixed(1)} to{" "}
                  {selectedModel.maxTemperature.toFixed(1)}.
                </div>
              ) : null}

              <div className="hero-actions">
                <button className="button button-primary" disabled={saving} type="submit">
                  {saving ? "Saving..." : selectedAgentId ? "Save Agent" : "Create Agent"}
                </button>
                {selectedAgentId ? (
                  <>
                    <button className="button" onClick={resetForm} type="button">
                      New Agent
                    </button>
                    <button className="button button-danger" disabled={saving} onClick={handleDelete} type="button">
                      Delete Agent
                    </button>
                  </>
                ) : null}
              </div>
            </form>
          </article>
        </section>
      </AppShell>
    </AuthGuard>
  );
}
