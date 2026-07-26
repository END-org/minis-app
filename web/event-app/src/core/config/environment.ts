import type { FirebaseOptions } from "firebase/app";

interface EventAppConfig {
  readonly apiBaseUrl: string;
  readonly firebase: FirebaseOptions;
}

function requiredEnv(name: keyof ImportMetaEnv, value: string | undefined): string {
  const trimmed = value?.trim();
  if (!trimmed) {
    throw new Error(`Missing required environment variable ${String(name)}.`);
  }
  return trimmed;
}

function optionalEnv(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  if (!trimmed) {
    return undefined;
  }
  return trimmed;
}

function normalizeBaseUrl(value: string | undefined): string {
  const trimmed = value?.trim();
  if (!trimmed) {
    return "";
  }
  return trimmed.replace(/\/+$/u, "");
}

export const eventAppConfig: EventAppConfig = {
  apiBaseUrl: normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL),
  firebase: firebaseConfig(),
};

function firebaseConfig(): FirebaseOptions {
  const config: FirebaseOptions = {
    apiKey: requiredEnv("VITE_FIREBASE_API_KEY", import.meta.env.VITE_FIREBASE_API_KEY),
    appId: requiredEnv("VITE_FIREBASE_APP_ID", import.meta.env.VITE_FIREBASE_APP_ID),
    authDomain: requiredEnv(
      "VITE_FIREBASE_AUTH_DOMAIN",
      import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
    ),
    projectId: requiredEnv("VITE_FIREBASE_PROJECT_ID", import.meta.env.VITE_FIREBASE_PROJECT_ID),
  };
  const storageBucket = optionalEnv(import.meta.env.VITE_FIREBASE_STORAGE_BUCKET);
  if (storageBucket) {
    config.storageBucket = storageBucket;
  }
  return config;
}
