"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "../../components/app-shell";
import { AuthGuard } from "../../components/auth-guard";
import { getStoredAuth } from "../../lib/auth";
import { apiFetch } from "../../lib/api";
import type {
  MessageListItem,
  MessageResponse,
  MessageSliceResponse,
  MessageStatus
} from "../../lib/types";

const statusOptions: MessageStatus[] = ["PENDING", "PROCESSING", "FAILED", "SUCCESS"];

function buildHeaders() {
  const authHeader = getStoredAuth();
  const headers: Record<string, string> = {
    "Content-Type": "application/json"
  };

  if (authHeader) {
    headers.Authorization = authHeader;
  }

  return headers;
}

export default function MessagesPage() {
  const [messages, setMessages] = useState<MessageListItem[]>([]);
  const [selectedMessage, setSelectedMessage] = useState<MessageResponse | null>(null);
  const [showIngestModal, setShowIngestModal] = useState(false);
  const [loadingList, setLoadingList] = useState(false);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [mutating, setMutating] = useState(false);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [sourceService, setSourceService] = useState("gmail");
  const [messageType, setMessageType] = useState("email");
  const [externalMessageId, setExternalMessageId] = useState("");
  const [payload, setPayload] = useState("");

  async function loadMessages(nextPage = page) {
    setLoadingList(true);
    setError("");

    const params = new URLSearchParams({
      page: String(nextPage),
      size: "20"
    });

    if (query.trim()) {
      params.set("query", query.trim());
    }

    if (statusFilter) {
      params.set("status", statusFilter);
    }

    try {
      const response = await apiFetch({
        path: `/api/messages?${params.toString()}`,
        headers: {
          Authorization: getStoredAuth()
        }
      });

      if (!response.ok) {
        throw new Error("Unable to load messages.");
      }

      const data: MessageSliceResponse = await response.json();
      setMessages(data.items);
      setHasNext(data.hasNext);
      setPage(data.page);

      if (data.items.length > 0) {
        await loadMessage(data.items[0].id);
      } else {
        setSelectedMessage(null);
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load messages.");
    } finally {
      setLoadingList(false);
    }
  }

  async function loadMessage(messageId: string) {
    setLoadingDetail(true);

    try {
      const response = await apiFetch({
        path: `/api/messages/${messageId}`,
        headers: {
          Authorization: getStoredAuth()
        }
      });

      if (!response.ok) {
        throw new Error("Unable to load message details.");
      }

      const data: MessageResponse = await response.json();
      setSelectedMessage(data);
    } catch (requestError) {
      setError(
        requestError instanceof Error ? requestError.message : "Unable to load message details."
      );
    } finally {
      setLoadingDetail(false);
    }
  }

  useEffect(() => {
    void loadMessages(0);
  }, []);

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    await loadMessages(0);
  }

  async function handleStatusChange(status: MessageStatus) {
    if (!selectedMessage) {
      return;
    }

    setMutating(true);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch({
        path: `/api/messages/${selectedMessage.id}/status`,
        method: "PATCH",
        headers: buildHeaders(),
        body: JSON.stringify({ status })
      });

      if (!response.ok) {
        throw new Error("Unable to update status.");
      }

      const data: MessageResponse = await response.json();
      setSelectedMessage(data);
      setNotice("Message status updated.");
      await loadMessages(page);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to update status.");
    } finally {
      setMutating(false);
    }
  }

  async function handleDelete() {
    if (!selectedMessage) {
      return;
    }

    setMutating(true);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch({
        path: `/api/messages/${selectedMessage.id}`,
        method: "DELETE",
        headers: {
          Authorization: getStoredAuth()
        }
      });

      if (!response.ok) {
        throw new Error("Unable to delete message.");
      }

      setNotice("Message deleted.");
      setSelectedMessage(null);
      await loadMessages(page);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to delete message.");
    } finally {
      setMutating(false);
    }
  }

  async function handleIngest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMutating(true);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch({
        path: "/api/messages/ingest",
        method: "POST",
        headers: buildHeaders(),
        body: JSON.stringify({
          sourceService,
          externalMessageId: externalMessageId || null,
          messageType,
          payload,
          metadata: null,
          details: null,
          sourceCreatedAt: new Date().toISOString()
        })
      });

      if (!response.ok) {
        throw new Error("Unable to ingest message.");
      }

      const data: MessageResponse = await response.json();
      setNotice("Message ingested.");
      setPayload("");
      setExternalMessageId("");
      setShowIngestModal(false);
      await loadMessages(0);
      setSelectedMessage(data);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to ingest message.");
    } finally {
      setMutating(false);
    }
  }

  return (
    <AuthGuard>
      <AppShell
        title="Messages"
        subtitle="Search, inspect, ingest, update status, and delete messages from the new message service."
      >
        {(error || notice) && (
          <div className={`message ${error ? "message-error" : "message-success"}`}>
            {error || notice}
          </div>
        )}

        <section className="dashboard-grid">
          <article className="panel">
            <div className="panel-header">
              <span className="panel-kicker">Search</span>
              <span className="panel-badge">{loadingList ? "Loading" : `${messages.length} items`}</span>
            </div>

            <form className="stack" onSubmit={handleSearch}>
              <div className="form-grid">
                <label className="field">
                  <span>Query</span>
                  <input value={query} onChange={(event) => setQuery(event.target.value)} />
                </label>

                <label className="field">
                  <span>Status</span>
                  <select
                    className="select"
                    value={statusFilter}
                    onChange={(event) => setStatusFilter(event.target.value)}
                  >
                    <option value="">All</option>
                    {statusOptions.map((status) => (
                      <option key={status} value={status}>
                        {status}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <div className="hero-actions">
                <button className="button button-primary" type="submit">
                  Search
                </button>
                <button
                  className="button"
                  onClick={() => setShowIngestModal(true)}
                  type="button"
                >
                  Ingest Message
                </button>
                <button
                  className="button"
                  onClick={() => void loadMessages(Math.max(page - 1, 0))}
                  disabled={page === 0 || loadingList}
                  type="button"
                >
                  Previous
                </button>
                <button
                  className="button"
                  onClick={() => void loadMessages(page + 1)}
                  disabled={!hasNext || loadingList}
                  type="button"
                >
                  Next
                </button>
              </div>
            </form>

            <div className="message-list">
              {messages.map((message) => (
                <button
                  className={
                    selectedMessage?.id === message.id ? "message-row message-row-active" : "message-row"
                  }
                  key={message.id}
                  onClick={() => void loadMessage(message.id)}
                  type="button"
                >
                  <div className="list-card-top">
                    <span>{message.messageType}</span>
                    <strong>{message.status}</strong>
                  </div>
                  <p>{message.summary ?? "No summary available."}</p>
                  <div className="message-meta">
                    <span>{message.sourceService}</span>
                    <span>{new Date(message.ingestedAt).toLocaleString()}</span>
                  </div>
                </button>
              ))}
            </div>
          </article>

          <article className="panel">
            <div className="panel-header">
              <span className="panel-kicker">Details</span>
              <span className="panel-badge">{loadingDetail ? "Loading" : "Selected"}</span>
            </div>

            {selectedMessage ? (
              <div className="stack">
                <div className="list-card">
                  <div className="list-card-top">
                    <span>Summary</span>
                    <strong>{selectedMessage.status}</strong>
                  </div>
                  <p>{selectedMessage.summary ?? "No summary available."}</p>
                </div>

                <div className="detail-block">
                  <span className="detail-label">Payload</span>
                  <pre className="code-block">{selectedMessage.payload}</pre>
                </div>

                <div className="form-grid">
                  <label className="field">
                    <span>Source service</span>
                    <input readOnly value={selectedMessage.sourceService} />
                  </label>
                  <label className="field">
                    <span>Message type</span>
                    <input readOnly value={selectedMessage.messageType} />
                  </label>
                </div>

                <div className="hero-actions">
                  {statusOptions.map((status) => (
                    <button
                      className="button"
                      disabled={mutating || selectedMessage.status === status}
                      key={status}
                      onClick={() => void handleStatusChange(status)}
                      type="button"
                    >
                      {status}
                    </button>
                  ))}
                  <button className="button button-danger" disabled={mutating} onClick={handleDelete} type="button">
                    Delete
                  </button>
                </div>
              </div>
            ) : (
              <div className="empty-state">Select a message to inspect its details.</div>
            )}
          </article>
        </section>

        {showIngestModal ? (
          <div
            className="modal-backdrop"
            onClick={() => !mutating && setShowIngestModal(false)}
            role="presentation"
          >
            <div
              className="modal-card"
              onClick={(event) => event.stopPropagation()}
              role="dialog"
              aria-modal="true"
              aria-labelledby="ingest-title"
            >
              <div className="panel-header">
                <div>
                  <span className="panel-kicker">Ingest</span>
                  <h2 id="ingest-title" className="modal-title">Create Message</h2>
                </div>
                <button
                  className="button"
                  disabled={mutating}
                  onClick={() => setShowIngestModal(false)}
                  type="button"
                >
                  Close
                </button>
              </div>

              <form className="profile-form" onSubmit={handleIngest}>
                <div className="form-grid">
                  <label className="field">
                    <span>Source service</span>
                    <input value={sourceService} onChange={(event) => setSourceService(event.target.value)} />
                  </label>

                  <label className="field">
                    <span>Message type</span>
                    <input value={messageType} onChange={(event) => setMessageType(event.target.value)} />
                  </label>
                </div>

                <label className="field">
                  <span>External message ID</span>
                  <input
                    value={externalMessageId}
                    onChange={(event) => setExternalMessageId(event.target.value)}
                  />
                </label>

                <label className="field">
                  <span>Payload</span>
                  <textarea
                    className="textarea"
                    rows={8}
                    value={payload}
                    onChange={(event) => setPayload(event.target.value)}
                  />
                </label>

                <div className="hero-actions">
                  <button className="button button-primary" disabled={mutating || !payload.trim()} type="submit">
                    {mutating ? "Ingesting..." : "Ingest Message"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        ) : null}
      </AppShell>
    </AuthGuard>
  );
}
