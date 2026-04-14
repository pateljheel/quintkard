export const apiBaseUrl = "http://localhost:8080";
export const authStorageKey = "quintkard-basic-auth";
export const csrfStorageKey = "quintkard-csrf-token";

export function toBasicAuth(username: string, password: string) {
  if (typeof window === "undefined") {
    return "";
  }

  return `Basic ${window.btoa(`${username}:${password}`)}`;
}

export function getStoredAuth() {
  if (typeof window === "undefined") {
    return "";
  }

  return window.sessionStorage.getItem(authStorageKey) ?? "";
}

export function setStoredAuth(header: string) {
  if (typeof window === "undefined") {
    return;
  }

  window.sessionStorage.setItem(authStorageKey, header);
}

export function clearStoredAuth() {
  if (typeof window === "undefined") {
    return;
  }

  window.sessionStorage.removeItem(authStorageKey);
  window.sessionStorage.removeItem(csrfStorageKey);
}

export function getStoredCsrfToken() {
  if (typeof window === "undefined") {
    return "";
  }

  return window.sessionStorage.getItem(csrfStorageKey) ?? "";
}

export function setStoredCsrfToken(token: string) {
  if (typeof window === "undefined") {
    return;
  }

  window.sessionStorage.setItem(csrfStorageKey, token);
}
