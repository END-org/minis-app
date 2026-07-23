import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
  getAuth,
  onAuthStateChanged,
  reload,
  sendEmailVerification,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

const output = document.querySelector("#output");
const tokenStatus = document.querySelector("#token-status");
const uid = document.querySelector("#uid");
const userEmail = document.querySelector("#user-email");
const emailVerified = document.querySelector("#email-verified");
const providers = document.querySelector("#providers");
const emailInput = document.querySelector("#email");
const passwordInput = document.querySelector("#password");

let auth;
let currentToken = "";

boot();

async function boot() {
  try {
    const config = await getJson("/api/v1/dev/firebase-web-config");
    const app = initializeApp(config);
    auth = getAuth(app);
    onAuthStateChanged(auth, updateUser);
    bindActions();
    writeOutput({ status: "ready" });
  } catch (error) {
    writeOutput(errorPayload(error));
  }
}

function bindActions() {
  document.querySelector("#register-button").addEventListener("click", registerWithEmail);
  document.querySelector("#login-button").addEventListener("click", loginWithEmail);
  document.querySelector("#verify-email-button").addEventListener("click", sendVerificationEmail);
  document.querySelector("#reload-user-button").addEventListener("click", reloadFirebaseUser);
  document.querySelector("#google-button").addEventListener("click", googleLogin);
  document.querySelector("#logout-button").addEventListener("click", logout);
  document.querySelector("#refresh-token-button").addEventListener("click", refreshToken);
  document.querySelector("#copy-token-button").addEventListener("click", copyToken);
  document.querySelector("#bootstrap-button").addEventListener("click", bootstrapAccount);
  document.querySelector("#me-button").addEventListener("click", loadMe);
}

async function registerWithEmail() {
  await runAction(async () => {
    await createUserWithEmailAndPassword(auth, emailInput.value, passwordInput.value);
    writeOutput({ status: "registered", next: "Send verification email before bootstrap." });
  });
}

async function loginWithEmail() {
  await runAction(async () => {
    await signInWithEmailAndPassword(auth, emailInput.value, passwordInput.value);
    writeOutput({ status: "signed-in" });
  });
}

async function sendVerificationEmail() {
  await runAction(async () => {
    requireUser();
    await sendEmailVerification(auth.currentUser);
    writeOutput({ status: "verification-email-sent" });
  });
}

async function reloadFirebaseUser() {
  await runAction(async () => {
    requireUser();
    await reload(auth.currentUser);
    updateUser(auth.currentUser);
    writeOutput({ status: "firebase-user-reloaded" });
  });
}

async function googleLogin() {
  await runAction(async () => {
    await signInWithPopup(auth, new GoogleAuthProvider());
    writeOutput({ status: "signed-in-with-google" });
  });
}

async function logout() {
  await runAction(async () => {
    await signOut(auth);
    currentToken = "";
    tokenStatus.textContent = "No token requested.";
    writeOutput({ status: "signed-out" });
  });
}

async function refreshToken() {
  await runAction(async () => {
    currentToken = await token();
    tokenStatus.textContent = `Fresh token ready (${currentToken.length} characters).`;
    writeOutput({ status: "token-refreshed" });
  });
}

async function copyToken() {
  await runAction(async () => {
    if (!currentToken) {
      currentToken = await token();
    }
    await navigator.clipboard.writeText(currentToken);
    writeOutput({ status: "token-copied" });
  });
}

async function bootstrapAccount() {
  await runAction(async () => {
    const response = await callApi("/api/v1/auth/bootstrap", { method: "POST" });
    writeOutput(response);
  });
}

async function loadMe() {
  await runAction(async () => {
    const response = await callApi("/api/v1/me", { method: "GET" });
    writeOutput(response);
  });
}

async function callApi(path, options) {
  const idToken = await token();
  const response = await fetch(path, {
    ...options,
    headers: {
      Authorization: `Bearer ${idToken}`,
      "Content-Type": "application/json"
    }
  });
  const body = await response.json();
  return { ok: response.ok, status: response.status, body };
}

async function token() {
  requireUser();
  currentToken = await auth.currentUser.getIdToken(true);
  tokenStatus.textContent = `Fresh token ready (${currentToken.length} characters).`;
  return currentToken;
}

function requireUser() {
  if (!auth.currentUser) {
    throw new Error("Sign in with Firebase first.");
  }
}

function updateUser(user) {
  if (!user) {
    uid.textContent = "Not signed in";
    userEmail.textContent = "Not signed in";
    emailVerified.textContent = "Not signed in";
    providers.textContent = "Not signed in";
    return;
  }
  uid.textContent = user.uid;
  userEmail.textContent = user.email || "No email";
  emailVerified.textContent = String(user.emailVerified);
  providers.textContent = user.providerData.map((provider) => provider.providerId).join(", ");
}

async function getJson(path) {
  const response = await fetch(path);
  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }
  return response.json();
}

async function runAction(action) {
  try {
    await action();
  } catch (error) {
    writeOutput(errorPayload(error));
  }
}

function writeOutput(value) {
  output.textContent = JSON.stringify(value, null, 2);
}

function errorPayload(error) {
  return {
    ok: false,
    message: error.message || "Unexpected error"
  };
}
