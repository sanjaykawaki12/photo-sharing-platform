const user = Auth.requireRole("TEAM_MEMBER");

if (user) {
  document.getElementById("userName").textContent = user.name;
}

let currentEventId = null;


// =========================================================
// LOGOUT
// =========================================================

function logout() {
  Auth.clear();
  window.location.href = "index.html";
}


// =========================================================
// HTML ESCAPE
// =========================================================

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}


// =========================================================
// EVENT LIST
// =========================================================

async function loadEvents() {

  const container =
    document.getElementById("eventsContainer");

  try {

    const events =
      await apiRequest("/api/events");

    if (events.length === 0) {

      container.innerHTML = `
        <div class="empty-state">
          You haven't been assigned to any events yet.
          Ask your Admin/Lead to add you.
        </div>
      `;

      return;
    }

    container.innerHTML = events.map(ev => `

      <div
        class="card"
        style="
          display:flex;
          justify-content:space-between;
          align-items:center;
          cursor:pointer;
        "
        onclick="openEvent(${ev.id})"
      >

        <div>

          <h2 style="margin-bottom:4px;">
            ${escapeHtml(ev.name)}
          </h2>

          <span
            style="
              color:var(--muted);
              font-size:0.85rem;
            "
          >
            Created
            ${new Date(ev.createdAt).toLocaleDateString()}
          </span>

        </div>

        <button
          class="secondary small"
          type="button"
        >
          Open →
        </button>

      </div>

    `).join("");

  } catch (err) {

    container.innerHTML = `
      <div class="alert error">
        ${escapeHtml(err.message)}
      </div>
    `;
  }
}


// =========================================================
// OPEN EVENT
// =========================================================

async function openEvent(eventId) {

  currentEventId = eventId;

  document.getElementById(
    "eventListView"
  ).style.display = "none";

  document.getElementById(
    "eventDetailView"
  ).style.display = "block";

  try {

    const detail =
      await apiRequest(
        `/api/events/${eventId}`
      );

    document.getElementById(
      "detailEventName"
    ).textContent = detail.name;

  } catch (err) {

    showAlert(
      "uploadAlert",
      err.message
    );
  }

  loadMyPhotos();
}


// =========================================================
// BACK TO EVENT LIST
// =========================================================

function backToList() {

  document.getElementById(
    "eventDetailView"
  ).style.display = "none";

  document.getElementById(
    "eventListView"
  ).style.display = "block";

  currentEventId = null;

  loadEvents();
}


// =========================================================
// UPLOAD PHOTOS
// =========================================================

async function uploadPhotos() {

  hideAlert("uploadAlert");

  const input =
    document.getElementById("photoFiles");

  if (
    !input.files ||
    input.files.length === 0
  ) {

    showAlert(
      "uploadAlert",
      "Choose at least one photo to upload."
    );

    return;
  }

  const formData =
    new FormData();

  [...input.files].forEach(file => {
    formData.append("files", file);
  });

  try {

    await apiRequest(
      `/api/events/${currentEventId}/photos`,
      {
        method: "POST",
        body: formData,
        isForm: true
      }
    );

    input.value = "";

    showAlert(
      "uploadAlert",
      "Photos uploaded successfully.",
      "success"
    );

    loadMyPhotos();

  } catch (err) {

    showAlert(
      "uploadAlert",
      err.message
    );
  }
}


// =========================================================
// LOAD MY PHOTOS
// =========================================================

async function loadMyPhotos() {

  const grid =
    document.getElementById("photosGrid");

  try {

    const photos =
      await apiRequest(
        `/api/events/${currentEventId}/photos`
      );

    if (photos.length === 0) {

      grid.innerHTML = `
        <div class="empty-state">
          You haven't uploaded any photos
          for this event yet.
        </div>
      `;

      return;
    }

    grid.innerHTML = photos.map(p => `

      <div class="photo-card">

        <div class="photo-image-wrapper">

          <img
            src="${p.url}"
            alt="${escapeHtml(p.filename)}"
            loading="lazy"
          >

        </div>

        <div class="team-photo-info">

          <div class="meta">

            ${(p.fileSize / 1024).toFixed(0)} KB

            ${
              p.selectedForGallery
                ? " · ✅ selected"
                : ""
            }

          </div>

          <button
            type="button"
            class="delete-photo-btn"
            onclick="deletePhoto(${p.id}, event)"
          >
            Delete
          </button>

        </div>

      </div>

    `).join("");

  } catch (err) {

    grid.innerHTML = `
      <div class="alert error">
        ${escapeHtml(err.message)}
      </div>
    `;
  }
}


// =========================================================
// DELETE PHOTO
// =========================================================

async function deletePhoto(
  photoId,
  event
) {

  /*
   * Prevent the click from triggering
   * parent elements if any.
   */
  if (event) {
    event.stopPropagation();
  }

  const confirmed =
    window.confirm(
      "Are you sure you want to delete this photo?"
    );

  if (!confirmed) {
    return;
  }

  try {

    await apiRequest(
      `/api/photos/${photoId}`,
      {
        method: "DELETE"
      }
    );

    showAlert(
      "uploadAlert",
      "Photo deleted successfully.",
      "success"
    );

    loadMyPhotos();

  } catch (err) {

    showAlert(
      "uploadAlert",
      err.message
    );
  }
}


// =========================================================
// INITIAL LOAD
// =========================================================

loadEvents();