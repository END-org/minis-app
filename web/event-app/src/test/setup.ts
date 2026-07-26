import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, vi } from "vitest";

vi.stubEnv("VITE_FIREBASE_API_KEY", "test-api-key");
vi.stubEnv("VITE_FIREBASE_AUTH_DOMAIN", "test.firebaseapp.com");
vi.stubEnv("VITE_FIREBASE_PROJECT_ID", "test-project");
vi.stubEnv("VITE_FIREBASE_APP_ID", "test-app-id");
vi.stubEnv("VITE_FIREBASE_STORAGE_BUCKET", "test-project.appspot.com");
vi.stubEnv("VITE_API_BASE_URL", "");

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});
