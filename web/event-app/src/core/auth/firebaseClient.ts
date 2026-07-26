import { initializeApp, type FirebaseApp } from "firebase/app";
import { getAuth, type Auth } from "firebase/auth";
import { eventAppConfig } from "../config/environment";

let app: FirebaseApp | null = null;
let auth: Auth | null = null;

export function getFirebaseAuth(): Auth {
  app ??= initializeApp(eventAppConfig.firebase);
  auth ??= getAuth(app);
  return auth;
}
