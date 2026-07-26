import react from "@vitejs/plugin-react";
import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const devHost = envValue(env, "MINIS_EVENT_APP_DEV_HOST") ?? "localhost";
  const devPort = parsePort(env.MINIS_EVENT_APP_DEV_PORT, 5173);
  const proxyTarget =
    envValue(env, "MINIS_EVENT_APP_DEV_PROXY_TARGET") ?? "http://localhost:8080";

  return {
    plugins: [react()],
    preview: {
      host: devHost,
      port: parsePort(env.MINIS_EVENT_APP_PREVIEW_PORT, 4173),
      strictPort: true,
    },
    server: {
      host: devHost,
      port: devPort,
      proxy: {
        "/api": {
          changeOrigin: true,
          target: proxyTarget,
        },
        "/actuator": {
          changeOrigin: true,
          target: proxyTarget,
        },
      },
      strictPort: true,
    },
    test: {
      environment: "jsdom",
      globals: true,
      setupFiles: "./src/test/setup.ts",
    },
  };
});

function envValue(env: Record<string, string>, name: string): string | undefined {
  const value = env[name];
  if (!value) {
    return undefined;
  }
  const trimmed = value.trim();
  return trimmed || undefined;
}

function parsePort(value: string | undefined, fallback: number): number {
  const parsed = Number.parseInt(value ?? "", 10);
  if (Number.isInteger(parsed) && parsed > 0 && parsed < 65_536) {
    return parsed;
  }
  return fallback;
}
