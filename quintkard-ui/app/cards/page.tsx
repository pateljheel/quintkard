"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "../../components/app-shell";
import { AuthGuard } from "../../components/auth-guard";
import { getStoredAuth } from "../../lib/auth";
import { apiFetch } from "../../lib/api";
import {
  CARD_PRIORITIES,
  CARD_STATUSES,
  CARD_TYPES,
  DEFAULT_CARD_PRIORITY,
  DEFAULT_CARD_STATUS,
  DEFAULT_CARD_TYPE
} from "../../lib/card-metadata";
import type {
  CardListItem,
  CardPriority,
  CardRequest,
  CardResponse,
  CardSliceResponse,
  CardStatus,
  CardType
} from "../../lib/types";

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

function createEmptyForm(): CardRequest {
  return {
    title: "",
    summary: null,
    content: "",
    cardType: DEFAULT_CARD_TYPE,
    status: DEFAULT_CARD_STATUS,
    priority: DEFAULT_CARD_PRIORITY,
    dueDate: null,
    sourceMessageId: null
  };
}

export default function CardsPage() {
  const [cards, setCards] = useState<CardListItem[]>([]);
  const [selectedCard, setSelectedCard] = useState<CardResponse | null>(null);
  const [form, setForm] = useState<CardRequest>(createEmptyForm());
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [loadingList, setLoadingList] = useState(false);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [mutating, setMutating] = useState(false);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function loadCards(nextPage = page) {
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
        path: `/api/cards?${params.toString()}`,
        headers: {
          Authorization: getStoredAuth()
        }
      });

      if (!response.ok) {
        throw new Error("Unable to load cards.");
      }

      const data: CardSliceResponse = await response.json();
      setCards(data.items);
      setHasNext(data.hasNext);
      setPage(data.page);

      if (data.items.length > 0) {
        const fallbackCardId =
          selectedCard && data.items.some((card) => card.id === selectedCard.id)
            ? selectedCard.id
            : data.items[0].id;
        await loadCard(fallbackCardId);
      } else {
        setSelectedCard(null);
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load cards.");
    } finally {
      setLoadingList(false);
    }
  }

  async function loadCard(cardId: string) {
    setLoadingDetail(true);

    try {
      const response = await apiFetch({
        path: `/api/cards/${cardId}`,
        headers: {
          Authorization: getStoredAuth()
        }
      });

      if (!response.ok) {
        throw new Error("Unable to load card details.");
      }

      const data: CardResponse = await response.json();
      setSelectedCard(data);
      setForm({
        title: data.title,
        summary: data.summary,
        content: data.content,
        cardType: data.cardType,
        status: data.status,
        priority: data.priority,
        dueDate: data.dueDate,
        sourceMessageId: data.sourceMessageId
      });
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load card details.");
    } finally {
      setLoadingDetail(false);
    }
  }

  useEffect(() => {
    void loadCards(0);
  }, []);

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    await loadCards(0);
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMutating(true);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch({
        path: "/api/cards",
        method: "POST",
        headers: buildHeaders(),
        body: JSON.stringify(form)
      });

      if (!response.ok) {
        throw new Error("Unable to create card.");
      }

      const data: CardResponse = await response.json();
      setNotice("Card created.");
      setShowCreateModal(false);
      setSelectedCard(data);
      setForm({
        title: data.title,
        summary: data.summary,
        content: data.content,
        cardType: data.cardType,
        status: data.status,
        priority: data.priority,
        dueDate: data.dueDate,
        sourceMessageId: data.sourceMessageId
      });
      await loadCards(0);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to create card.");
    } finally {
      setMutating(false);
    }
  }

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedCard) {
      return;
    }

    setMutating(true);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch({
        path: `/api/cards/${selectedCard.id}`,
        method: "PUT",
        headers: buildHeaders(),
        body: JSON.stringify(form)
      });

      if (!response.ok) {
        throw new Error("Unable to update card.");
      }

      const data: CardResponse = await response.json();
      setSelectedCard(data);
      setNotice("Card updated.");
      await loadCards(page);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to update card.");
    } finally {
      setMutating(false);
    }
  }

  async function handleDelete() {
    if (!selectedCard) {
      return;
    }

    setMutating(true);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch({
        path: `/api/cards/${selectedCard.id}`,
        method: "DELETE",
        headers: {
          Authorization: getStoredAuth()
        }
      });

      if (!response.ok) {
        throw new Error("Unable to delete card.");
      }

      setNotice("Card deleted.");
      setSelectedCard(null);
      await loadCards(page);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to delete card.");
    } finally {
      setMutating(false);
    }
  }

  function updateForm<K extends keyof CardRequest>(field: K, value: CardRequest[K]) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function openCreateModal() {
    setForm(createEmptyForm());
    setShowCreateModal(true);
  }

  return (
    <AuthGuard>
      <AppShell
        title="Cards"
        subtitle="Capture, organize, and search cards with hybrid keyword and semantic ranking."
      >
        {(error || notice) && (
          <div className={`message ${error ? "message-error" : "message-success"}`}>
            {error || notice}
          </div>
        )}

        <section className="dashboard-grid">
          <article className="panel">
            <div className="panel-header">
              <span className="panel-kicker">Browse</span>
              <span className="panel-badge">{loadingList ? "Loading" : `${cards.length} items`}</span>
            </div>

            <form className="stack" onSubmit={handleSearch}>
              <div className="form-grid">
                <label className="field">
                  <span>Query</span>
                  <input
                    placeholder="Search by keywords or meaning"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                  />
                </label>

                <label className="field">
                  <span>Status</span>
                  <select
                    className="select"
                    value={statusFilter}
                    onChange={(event) => setStatusFilter(event.target.value)}
                  >
                    <option value="">All</option>
                    {CARD_STATUSES.map((status) => (
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
                <button className="button" onClick={openCreateModal} type="button">
                  New Card
                </button>
                <button
                  className="button"
                  onClick={() => void loadCards(Math.max(page - 1, 0))}
                  disabled={page === 0 || loadingList}
                  type="button"
                >
                  Previous
                </button>
                <button
                  className="button"
                  onClick={() => void loadCards(page + 1)}
                  disabled={!hasNext || loadingList}
                  type="button"
                >
                  Next
                </button>
              </div>
            </form>

            <div className="message-list">
              {cards.map((card) => (
                <button
                  className={selectedCard?.id === card.id ? "message-row message-row-active" : "message-row"}
                  key={card.id}
                  onClick={() => void loadCard(card.id)}
                  type="button"
                >
                  <div className="list-card-top">
                    <span>{card.cardType}</span>
                    <strong>{card.status}</strong>
                  </div>
                  <p>{card.summary ?? card.title}</p>
                  <div className="message-meta">
                    <span>{card.priority}</span>
                    <span>{card.dueDate ?? "No due date"}</span>
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

            {selectedCard ? (
              <form className="profile-form" onSubmit={handleSave}>
                <div className="form-grid">
                  <label className="field">
                    <span>Title</span>
                    <input value={form.title} onChange={(event) => updateForm("title", event.target.value)} />
                  </label>

                  <label className="field">
                    <span>Due date</span>
                    <input
                      type="date"
                      value={form.dueDate ?? ""}
                      onChange={(event) => updateForm("dueDate", event.target.value || null)}
                    />
                  </label>
                </div>

                <label className="field">
                  <span>Summary</span>
                  <textarea
                    className="textarea"
                    rows={3}
                    value={form.summary ?? ""}
                    onChange={(event) => updateForm("summary", event.target.value || null)}
                  />
                </label>

                <label className="field">
                  <span>Content</span>
                  <textarea
                    className="textarea"
                    rows={8}
                    value={form.content}
                    onChange={(event) => updateForm("content", event.target.value)}
                  />
                </label>

                <div className="form-grid form-grid-3">
                  <label className="field">
                    <span>Type</span>
                    <select
                      className="select"
                      value={form.cardType}
                      onChange={(event) => updateForm("cardType", event.target.value as CardType)}
                    >
                      {CARD_TYPES.map((cardType) => (
                        <option key={cardType} value={cardType}>
                          {cardType}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="field">
                    <span>Status</span>
                    <select
                      className="select"
                      value={form.status}
                      onChange={(event) => updateForm("status", event.target.value as CardStatus)}
                    >
                      {CARD_STATUSES.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="field">
                    <span>Priority</span>
                    <select
                      className="select"
                      value={form.priority}
                      onChange={(event) => updateForm("priority", event.target.value as CardPriority)}
                    >
                      {CARD_PRIORITIES.map((priority) => (
                        <option key={priority} value={priority}>
                          {priority}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>

                <label className="field">
                  <span>Source message ID</span>
                  <input
                    value={form.sourceMessageId ?? ""}
                    onChange={(event) => updateForm("sourceMessageId", event.target.value || null)}
                  />
                </label>

                <div className="detail-meta-grid">
                  <div className="detail-block">
                    <span className="detail-label">Created</span>
                    <div>{new Date(selectedCard.createdAt).toLocaleString()}</div>
                  </div>
                  <div className="detail-block">
                    <span className="detail-label">Updated</span>
                    <div>{new Date(selectedCard.updatedAt).toLocaleString()}</div>
                  </div>
                </div>

                <div className="hero-actions">
                  <button className="button button-primary" disabled={mutating} type="submit">
                    {mutating ? "Saving..." : "Save Card"}
                  </button>
                  <button className="button button-danger" disabled={mutating} onClick={handleDelete} type="button">
                    Delete
                  </button>
                </div>
              </form>
            ) : (
              <div className="empty-state">Select a card to inspect its details.</div>
            )}
          </article>
        </section>

        {showCreateModal ? (
          <div
            className="modal-backdrop"
            onClick={() => !mutating && setShowCreateModal(false)}
            role="presentation"
          >
            <div
              className="modal-card"
              onClick={(event) => event.stopPropagation()}
              role="dialog"
              aria-modal="true"
              aria-labelledby="card-create-title"
            >
              <div className="panel-header">
                <div>
                  <span className="panel-kicker">Create</span>
                  <h2 id="card-create-title" className="modal-title">
                    New Card
                  </h2>
                </div>
                <button
                  className="button"
                  disabled={mutating}
                  onClick={() => setShowCreateModal(false)}
                  type="button"
                >
                  Close
                </button>
              </div>

              <form className="profile-form" onSubmit={handleCreate}>
                <div className="form-grid">
                  <label className="field">
                    <span>Title</span>
                    <input value={form.title} onChange={(event) => updateForm("title", event.target.value)} />
                  </label>

                  <label className="field">
                    <span>Due date</span>
                    <input
                      type="date"
                      value={form.dueDate ?? ""}
                      onChange={(event) => updateForm("dueDate", event.target.value || null)}
                    />
                  </label>
                </div>

                <label className="field">
                  <span>Summary</span>
                  <textarea
                    className="textarea"
                    rows={3}
                    value={form.summary ?? ""}
                    onChange={(event) => updateForm("summary", event.target.value || null)}
                  />
                </label>

                <label className="field">
                  <span>Content</span>
                  <textarea
                    className="textarea"
                    rows={8}
                    value={form.content}
                    onChange={(event) => updateForm("content", event.target.value)}
                  />
                </label>

                <div className="form-grid form-grid-3">
                  <label className="field">
                    <span>Type</span>
                    <select
                      className="select"
                      value={form.cardType}
                      onChange={(event) => updateForm("cardType", event.target.value as CardType)}
                    >
                      {CARD_TYPES.map((cardType) => (
                        <option key={cardType} value={cardType}>
                          {cardType}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="field">
                    <span>Status</span>
                    <select
                      className="select"
                      value={form.status}
                      onChange={(event) => updateForm("status", event.target.value as CardStatus)}
                    >
                      {CARD_STATUSES.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="field">
                    <span>Priority</span>
                    <select
                      className="select"
                      value={form.priority}
                      onChange={(event) => updateForm("priority", event.target.value as CardPriority)}
                    >
                      {CARD_PRIORITIES.map((priority) => (
                        <option key={priority} value={priority}>
                          {priority}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>

                <label className="field">
                  <span>Source message ID</span>
                  <input
                    value={form.sourceMessageId ?? ""}
                    onChange={(event) => updateForm("sourceMessageId", event.target.value || null)}
                  />
                </label>

                <div className="hero-actions">
                  <button
                    className="button button-primary"
                    disabled={mutating || !form.title.trim() || !form.content.trim()}
                    type="submit"
                  >
                    {mutating ? "Creating..." : "Create Card"}
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
