const user = Auth.requireRole("ADMIN");
if (user) document.getElementById("userName").textContent = user.name;

let currentEventId = null;
let currentPhotos = []; // cached photo list for the open event

function logout() { Auth.clear(); window.location.href = "index.html"; }

// ---------- Event list ----------

async function loadEvents() {
  const container = document.getElementById("eventsContainer");
  try {
    const events = await apiRequest("/api/events");
    if (events.length === 0) {
      container.innerHTML = `<div class="empty-state">No events yet. Create your first event above.</div>`;
      return;
    }
    container.innerHTML = events.map(ev => `
      <div class="card" style="display:flex; justify-content:space-between; align-items:center; cursor:pointer;"
           onclick="openEvent(${ev.id})">
        <div>
          <h2 style="margin-bottom:4px;">${escapeHtml(ev.name)}</h2>
          <span style="color:var(--muted); font-size:0.85rem;">Created ${new Date(ev.createdAt).toLocaleDateString()}</span>
        </div>
        <button class="secondary small">Manage →</button>
      </div>
    `).join("");
  } catch (err) {
    container.innerHTML = `<div class="alert error">${escapeHtml(err.message)}</div>`;
  }
}

document.getElementById("createEventForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  hideAlert("createAlert");
  const name = document.getElementById("eventName").value.trim();
  try {
    await apiRequest("/api/events", { method: "POST", body: { name } });
    document.getElementById("eventName").value = "";
    loadEvents();
  } catch (err) {
    showAlert("createAlert", err.message);
  }
});

// ---------- Event detail ----------

async function openEvent(eventId) {
  currentEventId = eventId;
  document.getElementById("eventListView").style.display = "none";
  document.getElementById("eventDetailView").style.display = "block";
  switchTab("members");
  await refreshEventDetail();
}

function backToList() {
  document.getElementById("eventDetailView").style.display = "none";
  document.getElementById("eventListView").style.display = "block";
  currentEventId = null;
  loadEvents();
}

async function refreshEventDetail() {
  try {
    const detail = await apiRequest(`/api/events/${currentEventId}`);
    document.getElementById("detailEventName").textContent = detail.name;
    document.getElementById("membersTableBody").innerHTML = detail.members.length
      ? detail.members.map(m => `<tr><td>${escapeHtml(m.name)}</td><td>${escapeHtml(m.email)}</td></tr>`).join("")
      : `<tr><td colspan="2" style="color:var(--muted);">No team members added yet.</td></tr>`;
    document.getElementById("photoCounts").textContent =
      `${detail.totalPhotos} photo(s) uploaded · ${detail.selectedPhotos} selected for gallery`;
  } catch (err) {
    showAlert("memberAlert", err.message);
  }
}

function switchTab(tab) {
  ["members", "photos", "publish"].forEach(t => {
    document.getElementById("tab-" + t).style.display = (t === tab) ? "block" : "none";
  });
  document.querySelectorAll(".tab").forEach(btn => {
    btn.classList.toggle("active", btn.dataset.tab === tab);
  });
  if (tab === "photos") loadPhotos();
}

// ---------- Members ----------

document.getElementById("addMemberForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  hideAlert("memberAlert");
  const email = document.getElementById("memberEmail").value.trim();
  try {
    await apiRequest(`/api/events/${currentEventId}/members`, { method: "POST", body: { email } });
    document.getElementById("memberEmail").value = "";
    await refreshEventDetail();
  } catch (err) {
    showAlert("memberAlert", err.message);
  }
});

// ---------- Photos ----------

async function loadPhotos() {
  hideAlert("photoAlert");
  const grid = document.getElementById("photosGrid");
  try {
    currentPhotos = await apiRequest(`/api/events/${currentEventId}/photos`);
    if (currentPhotos.length === 0) {
      grid.innerHTML = `<div class="empty-state">No photos uploaded yet by the team.</div>`;
      return;
    }
    grid.innerHTML = currentPhotos.map(p => `
      <div class="photo-card">
        <input type="checkbox" class="checkbox-overlay" data-photo-id="${p.id}" ${p.selectedForGallery ? "checked" : ""}>
        <img src="${p.url}" alt="${escapeHtml(p.filename)}" loading="lazy">
        <div class="meta">${escapeHtml(p.uploadedByName)}<br>${(p.fileSize/1024).toFixed(0)} KB</div>
      </div>
    `).join("");
  } catch (err) {
    showAlert("photoAlert", err.message);
  }
}

function bulkSelect(state) {
  document.querySelectorAll('#photosGrid input[type="checkbox"]').forEach(cb => cb.checked = state);
}

async function saveSelection() {
  hideAlert("photoAlert");
  const checkboxes = document.querySelectorAll('#photosGrid input[type="checkbox"]');
  const selectedIds = [...checkboxes].filter(cb => cb.checked).map(cb => Number(cb.dataset.photoId));
  const deselectedIds = [...checkboxes].filter(cb => !cb.checked).map(cb => Number(cb.dataset.photoId));

  try {
    if (selectedIds.length) {
      await apiRequest(`/api/events/${currentEventId}/photos/selection`, {
        method: "PATCH", body: { photoIds: selectedIds, selected: true }
      });
    }
    if (deselectedIds.length) {
      await apiRequest(`/api/events/${currentEventId}/photos/selection`, {
        method: "PATCH", body: { photoIds: deselectedIds, selected: false }
      });
    }
    showAlert("photoAlert", "Selection saved.", "success");
    refreshEventDetail();
  } catch (err) {
    showAlert("photoAlert", err.message);
  }
}

// ---------- Publish ----------

async function publishGallery() {
  hideAlert("publishAlert");
  document.getElementById("publishResult").style.display = "none";
  const pin = document.getElementById("customPin").value.trim();
  try {
    const result = await apiRequest(`/api/events/${currentEventId}/gallery/publish`, {
      method: "POST", body: { pin: pin || undefined }
    });
    document.getElementById("resultLink").textContent = result.galleryUrl;
    document.getElementById("resultPin").textContent = result.pin;
    document.getElementById("publishResult").style.display = "block";
  } catch (err) {
    showAlert("publishAlert", err.message);
  }
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}

loadEvents();
