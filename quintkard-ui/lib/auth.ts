export const apiBaseUrl = "http://localhost:8080";
export const authStorageKey = "quintkard-basic-auth";

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
}
