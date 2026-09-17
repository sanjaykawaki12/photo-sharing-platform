function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}

// Pre-fill link code when arriving through:
// /gallery.html?code=abc123
(function prefillFromQuery() {
  const params = new URLSearchParams(window.location.search);
  let code = params.get("code");

  if (code) {
    try {
      const url = new URL(code);
      const parts = url.pathname.split("/").filter(Boolean);

      // Full gallery URL:
      // http://localhost:8080/gallery/corcbi8d
      if (parts[0] === "gallery" && parts[1]) {
        code = parts[1];
      }

      // Full API URL:
      // http://localhost:8080/api/gallery/corcbi8d/access
      else if (
        parts[0] === "api" &&
        parts[1] === "gallery" &&
        parts[2]
      ) {
        code = parts[2];
      }
    } catch (e) {
      // Already a simple code such as "corcbi8d"
    }

    document.getElementById("linkCode").value = code;
  }
})();

async function viewGallery() {
  hideAlert("pinAlert");

  let linkCode = document.getElementById("linkCode").value.trim();
  const pin = document.getElementById("pin").value.trim();

  if (!linkCode || !pin) {
    showAlert("pinAlert", "Enter both the link code and the PIN.");
    return;
  }

  // -----------------------------------------
  // Convert pasted full URL to link code
  // -----------------------------------------

  if (
    linkCode.startsWith("http://") ||
    linkCode.startsWith("https://")
  ) {
    try {
      const url = new URL(linkCode);
      const parts = url.pathname.split("/").filter(Boolean);

      // Example:
      // /gallery/corcbi8d
      if (parts[0] === "gallery" && parts[1]) {
        linkCode = parts[1];
      }

      // Example:
      // /api/gallery/corcbi8d/access
      else if (
        parts[0] === "api" &&
        parts[1] === "gallery" &&
        parts[2]
      ) {
        linkCode = parts[2];
      } else {
        showAlert("pinAlert", "Invalid gallery link.");
        return;
      }
    } catch (e) {
      showAlert("pinAlert", "Invalid gallery link.");
      return;
    }
  }

  console.log("Final Link Code:", linkCode);
  console.log("PIN:", pin);

  try {
    // Public gallery access.
    // No account and no JWT required.
    const result = await apiRequest(
      `/api/gallery/${encodeURIComponent(linkCode)}/access`,
      {
        method: "POST",
        body: {
          pin: pin
        }
      }
    );

    // -----------------------------------------
    // Hide PIN screen
    // -----------------------------------------

    document.getElementById("pinView").style.display = "none";

    // -----------------------------------------
    // Show gallery
    // -----------------------------------------

    document.getElementById("galleryView").style.display = "block";

    document.getElementById("galleryTitle").textContent =
      result.eventName;

    document.getElementById("galleryCount").textContent =
      `${result.photos.length} photo(s)`;

    // -----------------------------------------
    // Display photos
    // -----------------------------------------

    document.getElementById("galleryGrid").innerHTML =
      result.photos.map(p => `
        <div class="photo-card">
          <img
            src="${p.url}"
            alt="${escapeHtml(p.filename)}"
            loading="lazy"
          >
        </div>
      `).join("");

  } catch (err) {
    console.error("Gallery access error:", err);

    showAlert(
      "pinAlert",
      err.message || "Unable to load gallery."
    );
  }
}