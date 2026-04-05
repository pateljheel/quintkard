import { apiBaseUrl } from "./auth";

type ApiRequestInit = RequestInit & {
  path: string;
};

export async function apiFetch({ path, ...init }: ApiRequestInit) {
  const url = `${apiBaseUrl}${path}`;
  const method = init.method ?? "GET";
  const startedAt = performance.now();

  try {
    const response = await fetch(url, init);
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
