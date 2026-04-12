import { apiBaseUrl, getStoredAuth, getStoredCsrfToken, setStoredCsrfToken } from "./auth";

type ApiRequestInit = RequestInit & {
  path: string;
};

const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

type CsrfTokenResponse = {
  headerName: string;
  parameterName: string;
  token: string;
};

async function ensureCsrfToken(headers: Headers) {
  const existingToken = getStoredCsrfToken();
  if (existingToken) {
    headers.set(CSRF_HEADER_NAME, existingToken);
    return;
  }

  const authHeader = headers.get("Authorization") ?? getStoredAuth();
  const tokenHeaders = new Headers();
  if (authHeader) {
    tokenHeaders.set("Authorization", authHeader);
  }

  const response = await fetch(`${apiBaseUrl}/api/csrf`, {
    method: "GET",
    headers: tokenHeaders,
    credentials: "include"
  });

  if (!response.ok) {
    throw new Error("Unable to initialize CSRF protection.");
  }

  const payload = (await response.json()) as CsrfTokenResponse;
  setStoredCsrfToken(payload.token);
  headers.set(payload.headerName || CSRF_HEADER_NAME, payload.token);
}

export async function apiFetch({ path, ...init }: ApiRequestInit) {
  const url = `${apiBaseUrl}${path}`;
  const method = init.method ?? "GET";
  const startedAt = performance.now();
  const headers = new Headers(init.headers);

  try {
    if (MUTATING_METHODS.has(method.toUpperCase())) {
      await ensureCsrfToken(headers);
    }

    const response = await fetch(url, {
      ...init,
      headers,
      credentials: "include"
    });
    const latencyMs = performance.now() - startedAt;

    console.info(
      `[api] ${method} ${path} -> ${response.status} ${response.statusText} (${latencyMs.toFixed(1)} ms)`
    );

    return response;
  } catch (error) {
    const latencyMs = performance.now() - startedAt;

    console.error(
      `[api] ${method} ${path} -> network error (${latencyMs.toFixed(1)} ms)`,
      error
    );

    throw error;
  }
}
