// Base URL of the Spring Boot backend. Change this if you deploy the API elsewhere.
const API_BASE = window.API_BASE || "";

const Auth = {
  getToken: () => localStorage.getItem("psp_token"),
  getUser: () => JSON.parse(localStorage.getItem("psp_user") || "null"),
  setSession: (token, user) => {
    localStorage.setItem("psp_token", token);
    localStorage.setItem("psp_user", JSON.stringify(user));
  },
  clear: () => {
    localStorage.removeItem("psp_token");
    localStorage.removeItem("psp_user");
  },
  requireRole(role) {
    const user = Auth.getUser();
    if (!Auth.getToken() || !user) {
      window.location.href = "index.html";
      return null;
    }
    if (role && user.role !== role) {
      window.location.href = user.role === "ADMIN" ? "admin-dashboard.html" : "team-dashboard.html";
      return null;
    }
    return user;
  }
};

/**
 * Thin fetch wrapper: attaches the JWT (when present), parses JSON,
 * and surfaces backend error messages consistently.
 */
async function apiRequest(path, { method = "GET", body, isForm = false } = {}) {
  const headers = {};
  const token = Auth.getToken();
  if (token) headers["Authorization"] = "Bearer " + token;
  if (!isForm && body !== undefined) headers["Content-Type"] = "application/json";

  const response = await fetch(API_BASE + path, {
    method,
    headers,
    body: isForm ? body : (body !== undefined ? JSON.stringify(body) : undefined)
  });

  let data = null;
  const text = await response.text();
  if (text) {
    try { data = JSON.parse(text); } catch (e) { data = text; }
  }

  if (!response.ok) {
    const message = (data && data.message) ? data.message : `Request failed (${response.status})`;
    throw new Error(message);
  }
  return data;
}

function showAlert(elementId, message, type = "error") {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.textContent = message;
  el.className = "alert " + type;
  el.style.display = "block";
}

function hideAlert(elementId) {
  const el = document.getElementById(elementId);
  if (el) el.style.display = "none";
}
