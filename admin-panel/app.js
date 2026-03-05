import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const STORAGE_KEY = "mmm_admin_supabase";

const els = {
  projectUrl: document.querySelector("#projectUrl"),
  apiKey: document.querySelector("#apiKey"),
  connectBtn: document.querySelector("#connectBtn"),
  loadAllBtn: document.querySelector("#loadAllBtn"),
  connectionStatus: document.querySelector("#connectionStatus"),
  toast: document.querySelector("#toast"),

  newSpeakerBtn: document.querySelector("#newSpeakerBtn"),
  speakerFormContainer: document.querySelector("#speakerFormContainer"),
  speakerFormTitle: document.querySelector("#speakerFormTitle"),
  speakerForm: document.querySelector("#speakerForm"),
  cancelSpeakerBtn: document.querySelector("#cancelSpeakerBtn"),
  speakersTableBody: document.querySelector("#speakersTableBody"),

  newSessionBtn: document.querySelector("#newSessionBtn"),
  sessionFormContainer: document.querySelector("#sessionFormContainer"),
  sessionFormTitle: document.querySelector("#sessionFormTitle"),
  sessionForm: document.querySelector("#sessionForm"),
  cancelSessionBtn: document.querySelector("#cancelSessionBtn"),
  sessionsTableBody: document.querySelector("#sessionsTableBody"),
};

let supabase = null;
let speakers = [];
let sessions = [];
let editingSpeakerId = null;
let editingSessionId = null;

boot();

function boot() {
  hydrateConnectionForm();
  bindEvents();
}

function bindEvents() {
  els.connectBtn.addEventListener("click", connect);
  els.loadAllBtn.addEventListener("click", loadAllData);

  els.newSpeakerBtn.addEventListener("click", () => {
    openSpeakerForm();
  });
  els.cancelSpeakerBtn.addEventListener("click", closeSpeakerForm);
  els.speakerForm.addEventListener("submit", saveSpeaker);

  els.newSessionBtn.addEventListener("click", () => {
    openSessionForm();
  });
  els.cancelSessionBtn.addEventListener("click", closeSessionForm);
  els.sessionForm.addEventListener("submit", saveSession);
}

function hydrateConnectionForm() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return;
    const saved = JSON.parse(raw);
    els.projectUrl.value = saved.projectUrl || "";
    els.apiKey.value = saved.apiKey || "";
  } catch {
    localStorage.removeItem(STORAGE_KEY);
  }
}

function persistConnectionForm(projectUrl, apiKey) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ projectUrl, apiKey }));
}

function normalizeProjectUrl(url) {
  const cleaned = url.trim().replace(/\/+$/, "");
  return cleaned.endsWith("/rest/v1") ? cleaned.slice(0, -8) : cleaned;
}

async function connect() {
  const projectUrl = normalizeProjectUrl(els.projectUrl.value);
  const apiKey = els.apiKey.value.trim();

  if (!projectUrl || !apiKey) {
    setStatus("Please provide Project URL and API Key.", true);
    return;
  }

  try {
    supabase = createClient(projectUrl, apiKey, {
      auth: { persistSession: false, autoRefreshToken: false },
      global: {
        headers: {
          apikey: apiKey,
          Authorization: `Bearer ${apiKey}`,
        },
      },
    });

    const { error } = await supabase.from("speakers").select("id", { count: "exact", head: true });
    if (error) throw error;

    persistConnectionForm(projectUrl, apiKey);
    setConnectedUi(true);
    setStatus("Connected to Supabase.");
    await loadAllData();
  } catch (error) {
    setConnectedUi(false);
    setStatus(`Could not connect: ${error.message}`, true);
  }
}

async function loadAllData() {
  if (!supabase) return;
  await Promise.all([loadSpeakers(), loadSessions()]);
}

async function loadSpeakers() {
  const { data, error } = await supabase
    .from("speakers")
    .select("id,name,role,company,bio,photo_url,social_links")
    .order("name", { ascending: true });

  if (error) {
    showToast(`Error loading speakers: ${error.message}`, true);
    return;
  }

  speakers = data || [];
  renderSpeakers();
}

async function loadSessions() {
  const { data, error } = await supabase
    .from("sessions")
    .select("id,title,description,start_time,end_time,duration,room,day,track,type,level,speaker_ids,capacity,registered,tags,livestream_url,slides_url")
    .order("day", { ascending: true })
    .order("start_time", { ascending: true });

  if (error) {
    showToast(`Error loading sessions: ${error.message}`, true);
    return;
  }

  sessions = data || [];
  renderSessions();
}

function renderSpeakers() {
  els.speakersTableBody.innerHTML = "";

  for (const speaker of speakers) {
    const tr = document.createElement("tr");
    tr.appendChild(cell(speaker.id));
    tr.appendChild(cell(speaker.name));
    tr.appendChild(cell(speaker.role));
    tr.appendChild(cell(speaker.company));

    const actionsTd = document.createElement("td");
    const rowActions = document.createElement("div");
    rowActions.className = "row-actions";

    const editBtn = document.createElement("button");
    editBtn.type = "button";
    editBtn.className = "ghost";
    editBtn.textContent = "Edit";
    editBtn.addEventListener("click", () => openSpeakerForm(speaker));

    const deleteBtn = document.createElement("button");
    deleteBtn.type = "button";
    deleteBtn.className = "danger";
    deleteBtn.textContent = "Delete";
    deleteBtn.addEventListener("click", () => deleteSpeaker(speaker.id));

    rowActions.append(editBtn, deleteBtn);
    actionsTd.appendChild(rowActions);
    tr.appendChild(actionsTd);

    els.speakersTableBody.appendChild(tr);
  }
}

function renderSessions() {
  els.sessionsTableBody.innerHTML = "";

  for (const session of sessions) {
    const tr = document.createElement("tr");
    tr.appendChild(cell(session.id));
    tr.appendChild(cell(String(session.day)));
    tr.appendChild(cell(toHourLabel(session.start_time, session.end_time)));
    tr.appendChild(cell(session.title));
    tr.appendChild(cell(session.track));

    const actionsTd = document.createElement("td");
    const rowActions = document.createElement("div");
    rowActions.className = "row-actions";

    const editBtn = document.createElement("button");
    editBtn.type = "button";
    editBtn.className = "ghost";
    editBtn.textContent = "Edit";
    editBtn.addEventListener("click", () => openSessionForm(session));

    const deleteBtn = document.createElement("button");
    deleteBtn.type = "button";
    deleteBtn.className = "danger";
    deleteBtn.textContent = "Delete";
    deleteBtn.addEventListener("click", () => deleteSession(session.id));

    rowActions.append(editBtn, deleteBtn);
    actionsTd.appendChild(rowActions);
    tr.appendChild(actionsTd);

    els.sessionsTableBody.appendChild(tr);
  }
}

function cell(value) {
  const td = document.createElement("td");
  td.textContent = value ?? "";
  return td;
}

function setConnectedUi(connected) {
  els.loadAllBtn.disabled = !connected;
  els.newSpeakerBtn.disabled = !connected;
  els.newSessionBtn.disabled = !connected;
}

function setStatus(message, isError = false) {
  els.connectionStatus.textContent = message;
  els.connectionStatus.style.color = isError ? "#b42318" : "#475569";
}

function showToast(message, isError = false) {
  els.toast.textContent = message;
  els.toast.classList.remove("hidden");
  els.toast.style.background = isError ? "#7a271a" : "#0f172a";
  window.clearTimeout(showToast.timeoutId);
  showToast.timeoutId = window.setTimeout(() => {
    els.toast.classList.add("hidden");
  }, 3200);
}

function openSpeakerForm(speaker = null) {
  editingSpeakerId = speaker?.id || null;
  els.speakerFormTitle.textContent = editingSpeakerId ? "Edit speaker" : "New speaker";

  els.speakerForm.reset();
  setValue("#speakerId", speaker?.id || "");
  setValue("#speakerName", speaker?.name || "");
  setValue("#speakerRole", speaker?.role || "");
  setValue("#speakerCompany", speaker?.company || "");
  setValue("#speakerBio", speaker?.bio || "");
  setValue("#speakerPhotoUrl", speaker?.photo_url || "");
  setValue("#speakerSocialLinks", speaker?.social_links ? JSON.stringify(speaker.social_links, null, 2) : "{}");

  document.querySelector("#speakerId").readOnly = Boolean(editingSpeakerId);
  els.speakerFormContainer.classList.remove("hidden");
}

function closeSpeakerForm() {
  editingSpeakerId = null;
  els.speakerFormContainer.classList.add("hidden");
}

async function saveSpeaker(event) {
  event.preventDefault();
  if (!supabase) return;

  let socialLinks;
  try {
    socialLinks = JSON.parse(value("#speakerSocialLinks") || "{}");
  } catch {
    showToast("Social links must be valid JSON.", true);
    return;
  }

  const payload = {
    id: value("#speakerId").trim(),
    name: value("#speakerName").trim(),
    role: value("#speakerRole").trim(),
    company: value("#speakerCompany").trim(),
    bio: value("#speakerBio").trim(),
    photo_url: value("#speakerPhotoUrl").trim(),
    social_links: socialLinks,
  };

  const { error } = await supabase.from("speakers").upsert(payload, { onConflict: "id" });

  if (error) {
    showToast(`Error saving speaker: ${error.message}`, true);
    return;
  }

  showToast("Speaker saved.");
  closeSpeakerForm();
  await loadSpeakers();
}

async function deleteSpeaker(speakerId) {
  if (!supabase) return;
  const confirmed = window.confirm(`Delete speaker ${speakerId}?`);
  if (!confirmed) return;

  const { error } = await supabase.from("speakers").delete().eq("id", speakerId);
  if (error) {
    showToast(`Error deleting speaker: ${error.message}`, true);
    return;
  }

  showToast("Speaker deleted.");
  await loadSpeakers();
}

function openSessionForm(session = null) {
  editingSessionId = session?.id || null;
  els.sessionFormTitle.textContent = editingSessionId ? "Edit session" : "New session";

  els.sessionForm.reset();
  setValue("#sessionId", session?.id || "");
  setValue("#sessionDay", session?.day ?? 1);
  setValue("#sessionRoom", session?.room || "");
  setValue("#sessionTitle", session?.title || "");
  setValue("#sessionDescription", session?.description || "");
  setValue("#sessionStartTime", session?.start_time || "");
  setValue("#sessionEndTime", session?.end_time || "");
  setValue("#sessionDuration", session?.duration || "");
  setValue("#sessionTrack", session?.track || "AI_ML");
  setValue("#sessionType", session?.type || "SESSION");
  setValue("#sessionLevel", session?.level || "BEGINNER");
  setValue("#sessionCapacity", session?.capacity ?? 0);
  setValue("#sessionRegistered", session?.registered ?? 0);
  setValue("#sessionSpeakerIds", (session?.speaker_ids || []).join(","));
  setValue("#sessionTags", (session?.tags || []).join(","));
  setValue("#sessionLivestreamUrl", session?.livestream_url || "");
  setValue("#sessionSlidesUrl", session?.slides_url || "");

  document.querySelector("#sessionId").readOnly = Boolean(editingSessionId);
  els.sessionFormContainer.classList.remove("hidden");
}

function closeSessionForm() {
  editingSessionId = null;
  els.sessionFormContainer.classList.add("hidden");
}

async function saveSession(event) {
  event.preventDefault();
  if (!supabase) return;

  const speakerIds = csvToArray(value("#sessionSpeakerIds"));
  const tags = csvToArray(value("#sessionTags"));

  const payload = {
    id: value("#sessionId").trim(),
    title: value("#sessionTitle").trim(),
    description: value("#sessionDescription").trim(),
    start_time: value("#sessionStartTime").trim(),
    end_time: value("#sessionEndTime").trim(),
    duration: value("#sessionDuration").trim(),
    room: value("#sessionRoom").trim(),
    day: Number(value("#sessionDay")),
    track: value("#sessionTrack"),
    type: value("#sessionType"),
    level: value("#sessionLevel"),
    speaker_ids: speakerIds,
    capacity: Number(value("#sessionCapacity")),
    registered: Number(value("#sessionRegistered")),
    tags,
    livestream_url: emptyToNull(value("#sessionLivestreamUrl")),
    slides_url: emptyToNull(value("#sessionSlidesUrl")),
    updated_at: new Date().toISOString(),
  };

  const { error } = await supabase.from("sessions").upsert(payload, { onConflict: "id" });

  if (error) {
    showToast(`Error saving session: ${error.message}`, true);
    return;
  }

  showToast("Session saved.");
  closeSessionForm();
  await loadSessions();
}

async function deleteSession(sessionId) {
  if (!supabase) return;
  const confirmed = window.confirm(`Delete session ${sessionId}?`);
  if (!confirmed) return;

  const { error } = await supabase.from("sessions").delete().eq("id", sessionId);

  if (error) {
    showToast(`Error deleting session: ${error.message}`, true);
    return;
  }

  showToast("Session deleted.");
  await loadSessions();
}

function toHourLabel(startIso, endIso) {
  if (!startIso) return "";
  try {
    const start = new Date(startIso);
    const end = endIso ? new Date(endIso) : null;
    const startLabel = start.toISOString().slice(11, 16);
    if (!end) return startLabel;
    const endLabel = end.toISOString().slice(11, 16);
    return `${startLabel} - ${endLabel}`;
  } catch {
    return `${startIso || ""} ${endIso || ""}`.trim();
  }
}

function csvToArray(input) {
  return input
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function emptyToNull(value) {
  const trimmed = value.trim();
  return trimmed.length ? trimmed : null;
}

function value(selector) {
  return document.querySelector(selector).value;
}

function setValue(selector, val) {
  document.querySelector(selector).value = val;
}
