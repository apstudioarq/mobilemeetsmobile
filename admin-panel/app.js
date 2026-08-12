import { deleteApp, getApps, initializeApp } from "https://www.gstatic.com/firebasejs/10.12.5/firebase-app.js";
import {
  browserLocalPersistence,
  connectAuthEmulator,
  getAuth,
  GoogleAuthProvider,
  onAuthStateChanged,
  setPersistence,
  signInWithPopup,
  signOut,
} from "https://www.gstatic.com/firebasejs/10.12.5/firebase-auth.js";
import {
  connectDatabaseEmulator,
  get,
  getDatabase,
  ref,
  remove,
  set,
} from "https://www.gstatic.com/firebasejs/10.12.5/firebase-database.js";

const CONFERENCES_PATH = "test/conferences";
const RATINGS_PATH = "ratings";
const LOCAL_FIREBASE_CONFIG = {
  apiKey: "demo-local-api-key",
  authDomain: "ingtechrating.firebaseapp.com",
  databaseURL: "https://ingtechrating-default-rtdb.europe-west1.firebasedatabase.app",
  projectId: "ingtechrating",
};
const AUTH_EMULATOR_HOST = "127.0.0.1";
const AUTH_EMULATOR_PORT = 9099;
const DATABASE_EMULATOR_HOST = "127.0.0.1";
const DATABASE_EMULATOR_PORT = 9000;

const els = {
  loginScreen: document.querySelector("#loginScreen"),
  appShell: document.querySelector("#appShell"),
  sessionStatus: document.querySelector("#sessionStatus"),
  loginStatus: document.querySelector("#loginStatus"),
  loginPanel: document.querySelector("#loginPanel"),
  googleSignInBtn: document.querySelector("#googleSignInBtn"),
  configStatus: document.querySelector("#configStatus"),
  refreshBtn: document.querySelector("#refreshBtn"),
  signOutBtn: document.querySelector("#signOutBtn"),
  tabs: [...document.querySelectorAll(".tab")],
  conferencesView: document.querySelector("#conferencesView"),
  ratingsView: document.querySelector("#ratingsView"),
  newConferenceBtn: document.querySelector("#newConferenceBtn"),
  conferenceList: document.querySelector("#conferenceList"),
  conferenceForm: document.querySelector("#conferenceForm"),
  conferenceFormTitle: document.querySelector("#conferenceFormTitle"),
  conferenceSummary: document.querySelector("#conferenceSummary"),
  conferenceId: document.querySelector("#conferenceId"),
  conferenceTitle: document.querySelector("#conferenceTitle"),
  audience: document.querySelector("#audience"),
  eventType: document.querySelector("#eventType"),
  organizingCountry: document.querySelector("#organizingCountry"),
  conferenceStartDate: document.querySelector("#conferenceStartDate"),
  roomsContainer: document.querySelector("#roomsContainer"),
  existingRoomSelect: document.querySelector("#existingRoomSelect"),
  addExistingRoomBtn: document.querySelector("#addExistingRoomBtn"),
  addRoomBtn: document.querySelector("#addRoomBtn"),
  cancelConferenceBtn: document.querySelector("#cancelConferenceBtn"),
  deleteConferenceBtn: document.querySelector("#deleteConferenceBtn"),
  ratingsConferenceFilter: document.querySelector("#ratingsConferenceFilter"),
  ratingsSummary: document.querySelector("#ratingsSummary"),
  ratingDistributionChart: document.querySelector("#ratingDistributionChart"),
  sessionPerformanceChart: document.querySelector("#sessionPerformanceChart"),
  ratingsTableBody: document.querySelector("#ratingsTableBody"),
  roomTemplate: document.querySelector("#roomTemplate"),
  presentationTemplate: document.querySelector("#presentationTemplate"),
  toast: document.querySelector("#toast"),
};

let app = null;
let auth = null;
let db = null;
let conferences = {};
let ratings = {};
let editingConferenceKey = null;

boot();

function boot() {
  document.body.classList.add("is-login");
  bindGlobalErrorHandlers();
  bindEvents();
  renderEmptyState();
}

function bindGlobalErrorHandlers() {
  window.addEventListener("error", (event) => {
    showToast(event.error?.message || event.message || "Unexpected runtime error.", true);
  });
  window.addEventListener("unhandledrejection", (event) => {
    showToast(readableError(event.reason), true);
  });
}

function bindEvents() {
  els.googleSignInBtn.addEventListener("click", connect);
  els.signOutBtn.addEventListener("click", disconnect);
  els.refreshBtn.addEventListener("click", loadAllData);
  els.newConferenceBtn.addEventListener("click", () => openConferenceForm());
  els.addExistingRoomBtn.addEventListener("click", addSelectedExistingRoom);
  els.addRoomBtn.addEventListener("click", () => addRoom(createGeneratedRoom(), false));
  els.cancelConferenceBtn.addEventListener("click", () => {
    const selected = editingConferenceKey ? conferences[editingConferenceKey] : null;
    openConferenceForm(selected, editingConferenceKey);
  });
  els.deleteConferenceBtn.addEventListener("click", deleteSelectedConference);
  els.conferenceForm.addEventListener("submit", saveConference);
  els.conferenceForm.addEventListener("input", updateConferenceSummaryFromForm);
  els.ratingsConferenceFilter.addEventListener("change", renderRatings);

  for (const tab of els.tabs) {
    tab.addEventListener("click", () => selectTab(tab.dataset.tab));
  }
}

async function connect(event) {
  event.preventDefault?.();
  setStatus("Connecting...");

  try {
    await resetFirebaseApp();
    const config = await loadFirebaseHostingConfig();
    app = initializeApp(config);
    auth = getAuth(app);
    db = getDatabase(app);
    connectLocalEmulators();
    await setPersistence(auth, browserLocalPersistence);
    const credential = await signInWithPopup(auth, new GoogleAuthProvider());
    setConnectedUi(true, credential.user.email, credential.user.uid);

    onAuthStateChanged(auth, (user) => {
      if (!user) {
        setConnectedUi(false);
        return;
      }
      setConnectedUi(true, user.email, user.uid);
    });

    await loadAllData();
  } catch (error) {
    setConnectedUi(false);
    showToast(readableError(error), true);
    setStatus("Connection failed", true);
  }
}

function connectLocalEmulators() {
  if (!isLocalhost()) return;

  connectAuthEmulator(auth, `http://${AUTH_EMULATOR_HOST}:${AUTH_EMULATOR_PORT}`, {
    disableWarnings: true,
  });
  connectDatabaseEmulator(db, DATABASE_EMULATOR_HOST, DATABASE_EMULATOR_PORT);
  appendText(els.configStatus, " Local emulators enabled.");
}

function isLocalhost() {
  return ["localhost", "127.0.0.1", "0.0.0.0"].includes(window.location.hostname);
}

async function loadFirebaseHostingConfig() {
  if (window.location.protocol === "file:") {
    throw new Error("Open the admin panel through Firebase Hosting or the emulator, not as a file:// URL.");
  }

  const response = await fetch("/__/firebase/init.json", {
    headers: { Accept: "application/json" },
  });

  if (!response.ok) {
    throw new Error(
      "Firebase config is not available. Run this through Firebase Hosting or the Firebase Hosting emulator.",
    );
  }

  const body = await response.text();
  if (!body.trim()) {
    if (isLocalhost()) {
      setElementText(els.configStatus, `Using local Firebase emulator config for ${LOCAL_FIREBASE_CONFIG.projectId}.`);
      return LOCAL_FIREBASE_CONFIG;
    }

    throw new Error(
      "Firebase Hosting returned an empty config. Link this Hosting site to a Firebase Web App in Project settings.",
    );
  }

  let config;
  try {
    config = JSON.parse(body);
  } catch {
    throw new Error("Firebase Hosting config is not valid JSON.");
  }

  if (!config.apiKey || !config.databaseURL || !config.projectId) {
    throw new Error("Firebase Hosting config is missing apiKey, databaseURL, or projectId.");
  }

  setElementText(els.configStatus, `Using Firebase project ${config.projectId}.`);
  return config;
}

async function resetFirebaseApp() {
  const apps = getApps();
  await Promise.all(apps.map((existingApp) => deleteApp(existingApp)));
  app = null;
  auth = null;
  db = null;
}

async function disconnect() {
  if (auth) {
    await signOut(auth);
  }
  conferences = {};
  ratings = {};
  editingConferenceKey = null;
  renderEmptyState();
  setConnectedUi(false);
}

async function loadAllData() {
  if (!db) return;

  try {
    const conferenceSnapshot = await get(ref(db, CONFERENCES_PATH));
    conferences = normalizeConferences(conferenceSnapshot.val());
    renderConferenceList();
    renderRatingsFilter();

    const selected = editingConferenceKey && conferences[editingConferenceKey]
      ? { key: editingConferenceKey, conference: conferences[editingConferenceKey] }
      : firstConference();
    openConferenceForm(selected?.conference || null, selected?.key || null);

    try {
      const ratingsSnapshot = await get(ref(db, RATINGS_PATH));
      ratings = ratingsSnapshot.val() || {};
    } catch (error) {
      ratings = {};
      if (isPermissionDenied(error)) {
        showToast(readableError(error), true);
      } else {
        throw error;
      }
    }

    renderRatings();
    showToast("Data loaded.");
  } catch (error) {
    if (isPermissionDenied(error)) {
      setStatus("Missing admin permissions", true);
    }
    showToast(readableError(error), true);
  }
}

function normalizeConferences(raw) {
  if (!raw) return {};
  if (Array.isArray(raw)) {
    return raw.reduce((acc, conference, index) => {
      if (!conference) return acc;
      const key = conference.id || `conference-${index + 1}`;
      acc[key] = normalizeConference(conference, key);
      return acc;
    }, {});
  }

  return Object.entries(raw).reduce((acc, [key, conference]) => {
    if (!conference) return acc;
    acc[key] = normalizeConference(conference, key);
    return acc;
  }, {});
}

function normalizeConference(conference, key) {
  return {
    ...conference,
    id: conference.id || key,
    rooms: toArray(conference.rooms).map(normalizeRoom),
  };
}

function normalizeRoom(room) {
  return {
    ...room,
    presentations: toArray(room.presentations),
  };
}

function toArray(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value.filter(Boolean);
  if (typeof value === "object") return Object.values(value).filter(Boolean);
  return [];
}

function setConnectedUi(connected, email = "", uid = "") {
  document.body.classList.toggle("is-login", !connected);
  els.loginScreen.hidden = connected;
  els.appShell.hidden = !connected;
  els.signOutBtn.hidden = !connected;
  els.refreshBtn.disabled = !connected;
  els.newConferenceBtn.disabled = !connected;
  els.ratingsConferenceFilter.disabled = !connected;

  setStatus(connected ? `Connected as ${email} · UID ${uid}` : "Not connected");
}

function renderEmptyState() {
  els.conferenceList.innerHTML = `<p class="empty">No conferences loaded.</p>`;
  els.ratingsSummary.innerHTML = "";
  els.ratingDistributionChart.innerHTML = "";
  els.sessionPerformanceChart.innerHTML = "";
  els.ratingsTableBody.innerHTML = "";
  els.ratingsConferenceFilter.innerHTML = "";
  els.conferenceSummary.innerHTML = "";
  renderExistingRoomOptions();
  openConferenceForm(null, null);
}

function renderConferenceList() {
  const entries = sortedConferences();
  els.conferenceList.innerHTML = "";

  if (!entries.length) {
    els.conferenceList.innerHTML = `<p class="empty">No conferences yet.</p>`;
    return;
  }

  for (const { key, conference } of entries) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `conference-list-item${key === editingConferenceKey ? " active" : ""}`;
    button.innerHTML = `
      <span>${escapeHtml(conference.title || conference.id || key)}</span>
      <small>${formatConferenceMeta(conference)}</small>
    `;
    button.addEventListener("click", () => openConferenceForm(conference, key));
    els.conferenceList.appendChild(button);
  }
}

function firstConference() {
  const [first] = sortedConferences();
  return first || null;
}

function sortedConferences() {
  return Object.entries(conferences)
    .map(([key, conference]) => ({ key, conference }))
    .sort((a, b) => (a.conference.startDate || 0) - (b.conference.startDate || 0));
}

function formatConferenceMeta(conference) {
  const count = (conference.rooms || []).reduce(
    (total, room) => total + (room.presentations || []).length,
    0,
  );
  return `${formatDateTime(conference.startDate)} · ${count} sessions`;
}

function openConferenceForm(conference = null, key = null) {
  editingConferenceKey = key;
  const activeConference = conference || createBlankConference();

  els.conferenceFormTitle.textContent = key ? "Edit Conference" : "New Conference";
  els.deleteConferenceBtn.hidden = !key;
  els.conferenceId.value = activeConference.id || key || "";
  setLockedInput(els.conferenceId, Boolean(activeConference.id || key));
  els.conferenceTitle.value = activeConference.title || "";
  els.audience.value = activeConference.audience || "";
  els.eventType.value = activeConference.eventType || "";
  els.organizingCountry.value = activeConference.organizingCountry || "";
  els.conferenceStartDate.value = epochToInputValue(activeConference.startDate);
  els.roomsContainer.innerHTML = "";

  const activeRooms = toArray(activeConference.rooms);
  for (const room of activeRooms) {
    addRoom(room, Boolean(key));
  }

  renderConferenceSummary(activeConference);
  renderExistingRoomOptions();
  renderConferenceList();
}

function createBlankConference() {
  return {
    id: generateUniqueId("conference", conferenceIdsInUse()),
    title: "",
    audience: "",
    eventType: "Conference",
    organizingCountry: "",
    startDate: Math.floor(Date.now() / 1000),
    rooms: [],
  };
}

function createBlankRoom() {
  return {
    id: "",
    name: "",
    description: "",
    presentations: [],
  };
}

function createGeneratedRoom() {
  return {
    ...createBlankRoom(),
    id: generateUniqueId("room", roomIdsInUse()),
    name: "New room",
  };
}

function createBlankPresentation() {
  return {
    id: "",
    title: "",
    description: "",
    durationMinutes: 45,
    presenters: [],
    startDate: inputValueToEpoch(els.conferenceStartDate.value) || Math.floor(Date.now() / 1000),
    tags: [],
    technology: "Android",
    type: "Session",
  };
}

function createGeneratedPresentation() {
  return {
    ...createBlankPresentation(),
    id: generateUniqueId("presentation", presentationIdsInUse()),
    title: "New presentation",
  };
}

function addRoom(room = createBlankRoom(), isExisting = false) {
  const node = els.roomTemplate.content.firstElementChild.cloneNode(true);
  const roomIdInput = node.querySelector(".room-id");
  roomIdInput.value = room.id || "";
  setLockedInput(roomIdInput, Boolean(room.id));
  node.querySelector(".room-name").value = room.name || "";
  node.querySelector(".room-description").value = room.description || "";
  node.querySelector(".remove-room-btn").addEventListener("click", () => {
    node.remove();
    renderExistingRoomOptions();
    updateConferenceSummaryFromForm();
  });
  node.querySelector(".add-presentation-btn").addEventListener("click", () => addPresentation(node));
  node.querySelector(".add-existing-presentation-btn").addEventListener("click", () => addSelectedExistingPresentation(node));

  const presentations = toArray(room.presentations);
  for (const presentation of presentations) {
    addPresentation(node, presentation, isExisting);
  }

  els.roomsContainer.appendChild(node);
  renderExistingPresentationOptions(node);
  updateRoomHeading(node);
  updateConferenceSummaryFromForm();
  return node;
}

function addPresentation(roomNode, presentation = createGeneratedPresentation(), isExisting = false) {
  const container = roomNode.querySelector(".presentations");
  const node = els.presentationTemplate.content.firstElementChild.cloneNode(true);

  const presentationIdInput = node.querySelector(".presentation-id");
  presentationIdInput.value = presentation.id || "";
  setLockedInput(presentationIdInput, Boolean(presentation.id));
  node.querySelector(".presentation-title").value = presentation.title || "";
  node.querySelector(".presentation-description").value = presentation.description || "";
  node.querySelector(".presentation-duration").value = presentation.durationMinutes ?? 45;
  node.querySelector(".presentation-presenters").value = (presentation.presenters || []).join(", ");
  node.querySelector(".presentation-start-date").value = epochToInputValue(presentation.startDate);
  node.querySelector(".presentation-tags").value = (presentation.tags || []).join(", ");
  node.querySelector(".presentation-technology").value = presentation.technology || "Android";
  node.querySelector(".presentation-type").value = presentation.type || "Session";
  node.querySelector(".remove-presentation-btn").addEventListener("click", () => {
    node.remove();
    updateRoomHeading(roomNode);
    renderExistingPresentationOptions(roomNode);
    updateConferenceSummaryFromForm();
  });

  container.appendChild(node);
  updatePresentationHeading(node);
  updateRoomHeading(roomNode);
  renderExistingPresentationOptions(roomNode);
  updateConferenceSummaryFromForm();
}

function addSelectedExistingRoom() {
  const room = roomLibrary().find((candidate) => candidate.id === els.existingRoomSelect.value);
  if (!room) {
    showToast("Select an existing room first.", true);
    return;
  }

  if (roomIdsInForm().has(room.id)) {
    showToast("That room is already in this conference.", true);
    return;
  }

  addRoom(cloneComponent(room), true);
  renderExistingRoomOptions();
}

function addSelectedExistingPresentation(roomNode) {
  const select = roomNode.querySelector(".existing-presentation-select");
  const presentation = presentationLibrary().find((candidate) => candidate.id === select.value);
  if (!presentation) {
    showToast("Select an existing presentation first.", true);
    return;
  }

  const currentIds = new Set(
    [...roomNode.querySelectorAll(".presentation-id")].map((input) => value(input)).filter(Boolean),
  );
  if (currentIds.has(presentation.id)) {
    showToast("That presentation is already in this room.", true);
    return;
  }

  addPresentation(roomNode, cloneComponent(presentation), true);
  renderExistingPresentationOptions(roomNode);
}

function renderExistingRoomOptions() {
  const usedRoomIds = roomIdsInForm();
  renderSelectOptions(
    els.existingRoomSelect,
    roomLibrary().filter((room) => !usedRoomIds.has(room.id)),
    "Select existing room",
    (room) => `${room.name || room.id} (${room.id})`,
  );
}

function renderExistingPresentationOptions(roomNode) {
  const usedPresentationIds = new Set(
    [...roomNode.querySelectorAll(".presentation-id")].map((input) => value(input)).filter(Boolean),
  );
  renderSelectOptions(
    roomNode.querySelector(".existing-presentation-select"),
    presentationLibrary().filter((presentation) => !usedPresentationIds.has(presentation.id)),
    "Select existing presentation",
    (presentation) => `${presentation.title || presentation.id} (${presentation.id})`,
  );
}

function renderAllExistingPresentationOptions() {
  for (const roomNode of els.roomsContainer.querySelectorAll(".room-item")) {
    renderExistingPresentationOptions(roomNode);
  }
}

function renderSelectOptions(select, items, placeholder, labelFor) {
  if (!select) return;

  select.innerHTML = "";
  const placeholderOption = document.createElement("option");
  placeholderOption.value = "";
  placeholderOption.textContent = placeholder;
  select.appendChild(placeholderOption);

  for (const item of items) {
    const option = document.createElement("option");
    option.value = item.id;
    option.textContent = labelFor(item);
    select.appendChild(option);
  }

  select.disabled = items.length === 0;
}

function roomLibrary() {
  const roomsById = new Map();
  for (const conference of Object.values(conferences)) {
    for (const room of toArray(conference.rooms)) {
      if (room.id && !roomsById.has(room.id)) {
        roomsById.set(room.id, normalizeRoom(room));
      }
    }
  }
  return [...roomsById.values()].sort((a, b) => (a.name || a.id).localeCompare(b.name || b.id));
}

function presentationLibrary() {
  const presentationsById = new Map();
  for (const room of roomLibrary()) {
    for (const presentation of toArray(room.presentations)) {
      if (presentation.id && !presentationsById.has(presentation.id)) {
        presentationsById.set(presentation.id, presentation);
      }
    }
  }
  return [...presentationsById.values()].sort((a, b) => (a.title || a.id).localeCompare(b.title || b.id));
}

function roomIdsInUse() {
  return new Set([...roomLibrary().map((room) => room.id), ...roomIdsInForm()]);
}

function conferenceIdsInUse() {
  return new Set([
    ...Object.keys(conferences),
    ...Object.values(conferences).map((conference) => conference.id).filter(Boolean),
    value(els.conferenceId),
  ].filter(Boolean));
}

function roomIdsInForm() {
  return new Set(
    [...els.roomsContainer.querySelectorAll(".room-id")].map((input) => value(input)).filter(Boolean),
  );
}

function presentationIdsInUse() {
  return new Set([
    ...presentationLibrary().map((presentation) => presentation.id),
    ...[...els.roomsContainer.querySelectorAll(".presentation-id")].map((input) => value(input)).filter(Boolean),
  ]);
}

function generateUniqueId(prefix, usedIds) {
  let index = 1;
  let candidate = `${prefix}-${index}`;
  while (usedIds.has(candidate)) {
    index += 1;
    candidate = `${prefix}-${index}`;
  }
  return candidate;
}

function cloneComponent(component) {
  return JSON.parse(JSON.stringify(component));
}

async function saveConference(event) {
  event.preventDefault();
  if (!db) return;

  const conference = readConferenceForm();
  if (!conference.rooms.length) {
    showToast("Add at least one room.", true);
    return;
  }

  const key = editingConferenceKey || conference.id;
  try {
    await set(ref(db, `${CONFERENCES_PATH}/${key}`), conference);
    conferences[key] = conference;
    editingConferenceKey = key;
    renderConferenceList();
    renderExistingRoomOptions();
    renderAllExistingPresentationOptions();
    renderRatingsFilter();
    showToast("Conference saved.");
  } catch (error) {
    showToast(readableError(error), true);
  }
}

function readConferenceForm() {
  return {
    id: value(els.conferenceId),
    title: value(els.conferenceTitle),
    audience: value(els.audience),
    eventType: value(els.eventType),
    organizingCountry: value(els.organizingCountry),
    startDate: inputValueToEpoch(els.conferenceStartDate.value),
    rooms: [...els.roomsContainer.querySelectorAll(".room-item")].map(readRoom).filter(Boolean),
  };
}

function readRoom(roomNode) {
  const room = {
    id: value(roomNode.querySelector(".room-id")),
    name: value(roomNode.querySelector(".room-name")),
    description: value(roomNode.querySelector(".room-description")),
    presentations: [...roomNode.querySelectorAll(".presentation-item")]
      .map(readPresentation)
      .filter(Boolean),
  };
  return room.id && room.name ? room : null;
}

function readPresentation(node) {
  const presentation = {
    id: value(node.querySelector(".presentation-id")),
    title: value(node.querySelector(".presentation-title")),
    description: value(node.querySelector(".presentation-description")),
    durationMinutes: Number(value(node.querySelector(".presentation-duration"))) || 0,
    presenters: csvToArray(value(node.querySelector(".presentation-presenters"))),
    startDate: inputValueToEpoch(value(node.querySelector(".presentation-start-date"))),
    tags: csvToArray(value(node.querySelector(".presentation-tags"))),
    technology: value(node.querySelector(".presentation-technology")),
    type: value(node.querySelector(".presentation-type")),
  };
  return presentation.id && presentation.title ? presentation : null;
}

function setLockedInput(input, locked) {
  input.readOnly = locked;
  input.classList.toggle("locked-id", locked);
}

function updateConferenceSummaryFromForm() {
  updateComponentHeadings();
  renderConferenceSummary(readConferenceForm());
}

function updateComponentHeadings() {
  for (const roomNode of els.roomsContainer.querySelectorAll(".room-item")) {
    updateRoomHeading(roomNode);
    for (const presentationNode of roomNode.querySelectorAll(".presentation-item")) {
      updatePresentationHeading(presentationNode);
    }
  }
}

function updateRoomHeading(roomNode) {
  const name = value(roomNode.querySelector(".room-name"));
  const id = value(roomNode.querySelector(".room-id"));
  const presentationCount = roomNode.querySelectorAll(".presentation-item").length;
  roomNode.querySelector(".room-heading").textContent = name || "Room";
  roomNode.querySelector(".room-meta").textContent = `${id || "new-room"} · ${presentationCount} presentations`;
}

function updatePresentationHeading(node) {
  const title = value(node.querySelector(".presentation-title"));
  const id = value(node.querySelector(".presentation-id"));
  const technology = value(node.querySelector(".presentation-technology"));
  node.querySelector(".presentation-heading").textContent = title || "Presentation";
  node.querySelector(".presentation-meta").textContent = `${id || "new-presentation"} · ${technology}`;
}

function renderConferenceSummary(conference) {
  const rooms = toArray(conference.rooms);
  const presentationCount = rooms.reduce((total, room) => total + (room.presentations || []).length, 0);
  const roomItems = rooms
    .map((room) => {
      const presentations = (room.presentations || [])
        .map((presentation) => `<li>${escapeHtml(presentation.title || presentation.id || "Presentation")}</li>`)
        .join("");
      return `
        <li>
          <strong>${escapeHtml(room.name || room.id || "Room")}</strong>
          <span>${(room.presentations || []).length} presentations</span>
          <ul>${presentations}</ul>
        </li>
      `;
    })
    .join("");

  els.conferenceSummary.innerHTML = `
    <div class="summary-metrics">
      <span>${rooms.length} rooms</span>
      <span>${presentationCount} presentations</span>
      <span>${escapeHtml(conference.organizingCountry || "No country")}</span>
    </div>
    <ol class="summary-tree">${roomItems}</ol>
  `;
}

async function deleteSelectedConference() {
  if (!db || !editingConferenceKey) return;

  const conference = conferences[editingConferenceKey];
  const confirmed = window.confirm(`Delete ${conference?.title || editingConferenceKey}?`);
  if (!confirmed) return;

  try {
    await remove(ref(db, `${CONFERENCES_PATH}/${editingConferenceKey}`));
    delete conferences[editingConferenceKey];
    const selected = firstConference();
    openConferenceForm(selected?.conference || null, selected?.key || null);
    renderRatingsFilter();
    renderRatings();
    showToast("Conference deleted.");
  } catch (error) {
    showToast(readableError(error), true);
  }
}

function renderRatingsFilter() {
  const entries = sortedConferences();
  els.ratingsConferenceFilter.innerHTML = `<option value="all">All conferences</option>`;

  for (const { key, conference } of entries) {
    const option = document.createElement("option");
    option.value = key;
    option.textContent = conference.title || conference.id || key;
    els.ratingsConferenceFilter.appendChild(option);
  }
}

function renderRatings() {
  const filter = els.ratingsConferenceFilter.value || "all";
  const sessionMap = buildSessionMap(filter);
  const stats = aggregateRatings(sessionMap);
  renderRatingsSummary(stats);
  renderRatingDistributionChart(stats);
  renderSessionPerformanceChart(stats);
  renderRatingsTable(stats);
}

function buildSessionMap(filter) {
  const selectedEntries = sortedConferences().filter(({ key }) => filter === "all" || key === filter);
  const map = new Map();

  for (const { conference } of selectedEntries) {
    for (const room of conference.rooms || []) {
      for (const presentation of room.presentations || []) {
        map.set(presentation.id, {
          title: presentation.title || presentation.id,
          room: room.name || room.id,
          startDate: presentation.startDate || conference.startDate,
        });
      }
    }
  }

  return map;
}

function aggregateRatings(sessionMap) {
  const stats = new Map();

  for (const [sessionId, session] of sessionMap.entries()) {
    stats.set(sessionId, {
      sessionId,
      title: session.title,
      room: session.room,
      startDate: session.startDate,
      votes: 0,
      total: 0,
      distribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
    });
  }

  for (const rating of Object.values(ratings || {})) {
    const ratingValue = Number(rating.rating);
    if (!stats.has(rating.sessionId) || ratingValue < 1 || ratingValue > 5) continue;
    const item = stats.get(rating.sessionId);
    item.votes += 1;
    item.total += ratingValue;
    item.distribution[ratingValue] += 1;
  }

  return [...stats.values()].sort((a, b) => b.votes - a.votes || (a.startDate || 0) - (b.startDate || 0));
}

function renderRatingsSummary(stats) {
  const totalVotes = stats.reduce((sum, item) => sum + item.votes, 0);
  const weightedTotal = stats.reduce((sum, item) => sum + item.total, 0);
  const ratedSessions = stats.filter((item) => item.votes > 0).length;
  const average = totalVotes ? weightedTotal / totalVotes : 0;

  els.ratingsSummary.innerHTML = `
    ${metric("Votes", totalVotes)}
    ${metric("Average", average ? average.toFixed(2) : "-")}
    ${metric("Rated sessions", ratedSessions)}
    ${metric("Total sessions", stats.length)}
  `;
}

function renderRatingDistributionChart(stats) {
  const distribution = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
  for (const item of stats) {
    for (const rating of [1, 2, 3, 4, 5]) {
      distribution[rating] += item.distribution[rating];
    }
  }

  const max = Math.max(...Object.values(distribution), 1);
  els.ratingDistributionChart.innerHTML = [5, 4, 3, 2, 1]
    .map((rating) => {
      const count = distribution[rating];
      const width = Math.round((count / max) * 100);
      return `
        <div class="distribution-row">
          <span>${rating} star</span>
          <div class="bar-track">
            <div class="bar-fill rating-${rating}" style="width: ${width}%"></div>
          </div>
          <strong>${count}</strong>
        </div>
      `;
    })
    .join("");
}

function renderSessionPerformanceChart(stats) {
  const ratedStats = stats.filter((item) => item.votes > 0);
  if (!ratedStats.length) {
    els.sessionPerformanceChart.innerHTML = `<p class="empty">No ratings yet.</p>`;
    return;
  }

  const maxVotes = Math.max(...ratedStats.map((item) => item.votes), 1);
  els.sessionPerformanceChart.innerHTML = ratedStats
    .slice(0, 12)
    .map((item) => {
      const average = item.total / item.votes;
      const voteWidth = Math.round((item.votes / maxVotes) * 100);
      const averageWidth = Math.round((average / 5) * 100);
      return `
        <article class="session-bar">
          <div class="session-bar-title">
            <strong>${escapeHtml(item.title)}</strong>
            <span>${average.toFixed(2)} avg · ${item.votes} votes</span>
          </div>
          <div class="dual-bars">
            <div class="bar-track" aria-label="Average rating">
              <div class="bar-fill average-fill" style="width: ${averageWidth}%"></div>
            </div>
            <div class="bar-track compact" aria-label="Vote volume">
              <div class="bar-fill votes-fill" style="width: ${voteWidth}%"></div>
            </div>
          </div>
        </article>
      `;
    })
    .join("");
}

function renderRatingsTable(stats) {
  els.ratingsTableBody.innerHTML = "";

  if (!stats.length) {
    els.ratingsTableBody.innerHTML = `<tr><td colspan="8" class="empty-cell">No sessions available.</td></tr>`;
    return;
  }

  for (const item of stats) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>
        <strong>${escapeHtml(item.title)}</strong>
        <small>${escapeHtml(item.room || "")}</small>
      </td>
      <td>${item.votes}</td>
      <td>${item.votes ? (item.total / item.votes).toFixed(2) : "-"}</td>
      <td>${item.distribution[5]}</td>
      <td>${item.distribution[4]}</td>
      <td>${item.distribution[3]}</td>
      <td>${item.distribution[2]}</td>
      <td>${item.distribution[1]}</td>
    `;
    els.ratingsTableBody.appendChild(row);
  }
}

function metric(label, valueText) {
  return `
    <article class="metric">
      <span>${label}</span>
      <strong>${valueText}</strong>
    </article>
  `;
}

function selectTab(tabName) {
  for (const tab of els.tabs) {
    tab.classList.toggle("active", tab.dataset.tab === tabName);
  }
  els.conferencesView.classList.toggle("active", tabName === "conferences");
  els.ratingsView.classList.toggle("active", tabName === "ratings");
  if (tabName === "ratings") renderRatings();
}

function inputValueToEpoch(inputValue) {
  if (!inputValue) return 0;
  const date = new Date(inputValue);
  return Number.isNaN(date.getTime()) ? 0 : Math.floor(date.getTime() / 1000);
}

function epochToInputValue(epochSeconds) {
  if (!epochSeconds) return "";
  const date = new Date(epochSeconds * 1000);
  if (Number.isNaN(date.getTime())) return "";
  const offsetMs = date.getTimezoneOffset() * 60 * 1000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

function formatDateTime(epochSeconds) {
  if (!epochSeconds) return "No date";
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(epochSeconds * 1000));
}

function csvToArray(input) {
  return input
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function value(input) {
  return input.value.trim();
}

function setStatus(message, isError = false) {
  setElementText(els.sessionStatus, message);
  setElementText(els.loginStatus, message);
  els.sessionStatus?.classList.toggle("error", isError);
  els.loginStatus?.classList.toggle("error", isError);
}

function showToast(message, isError = false) {
  if (!els.toast) {
    window.alert(message);
    return;
  }

  setElementText(els.toast, message);
  els.toast.classList.toggle("error", isError);
  els.toast.classList.remove("hidden");
  window.clearTimeout(showToast.timeoutId);
  showToast.timeoutId = window.setTimeout(() => {
    els.toast.classList.add("hidden");
  }, 3200);
}

function setElementText(element, message) {
  if (element) {
    element.textContent = message;
  }
}

function appendText(element, message) {
  if (element) {
    element.textContent += message;
  }
}

function readableError(error) {
  if (isPermissionDenied(error)) {
    const uid = auth?.currentUser?.uid;
    const databaseName = isLocalhost() ? "Realtime Database emulator" : "Realtime Database";
    if (uid) {
      return `Permission denied. Add {"admins":{"${uid}":true}} in the same ${databaseName}.`;
    }
    return `Permission denied. The signed-in user is not marked as admin in ${databaseName}.`;
  }

  return error?.message?.replace(/^Firebase:\s*/, "") || "Unexpected error.";
}

function isPermissionDenied(error) {
  const message = `${error?.code || ""} ${error?.message || ""}`.toLowerCase();
  return message.includes("permission_denied") || message.includes("permission denied");
}

function escapeHtml(text) {
  return String(text ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
