import {initializeApp} from "firebase-admin/app";
import {FieldValue, getFirestore, Timestamp} from "firebase-admin/firestore";
import {onRequest} from "firebase-functions/v2/https";
import type {Request, Response} from "express";

initializeApp();

const db = getFirestore();
const region = "europe-west1";

type SessionDocument = {
  id: string;
  title: string;
  description: string;
  startTime: string;
  endTime: string;
  duration: string;
  room: string;
  day: number;
  track: string;
  type: string;
  level: string;
  speakerIds: string[];
  capacity: number;
  registered: number;
  tags: string[];
  livestreamUrl?: string | null;
  slidesUrl?: string | null;
  updatedAt?: string | Timestamp;
};

type SpeakerDocument = {
  id: string;
  name: string;
  role: string;
  company: string;
  bio: string;
  photoUrl: string;
  photoBase64?: string;
  photoMimeType?: string;
  socialLinks: Record<string, string>;
};

export const api = onRequest({region, cors: true}, async (req, res) => {
  try {
    if (req.method === "OPTIONS") {
      res.status(204).send("");
      return;
    }

    const path = req.path.replace(/^\/+|\/+$/g, "");
    const segments = path ? path.split("/") : [];

    if (req.method === "GET" && segments[0] === "sessions" && segments.length === 1) {
      await listSessions(req, res);
      return;
    }

    if (req.method === "GET" && segments[0] === "sessions" && segments[1] === "search") {
      await searchSessions(req, res);
      return;
    }

    if (req.method === "GET" && segments[0] === "sessions" && segments.length === 2) {
      await getSession(segments[1], res);
      return;
    }

    if (req.method === "GET" && segments[0] === "speakers" && segments.length === 1) {
      await listSpeakers(res);
      return;
    }

    if (req.method === "GET" && segments[0] === "speakers" && segments.length === 2) {
      await getSpeaker(segments[1], res);
      return;
    }

    if (segments[0] === "users" && segments[2] === "bookmarks") {
      await handleBookmarks(req, res, segments);
      return;
    }

    res.status(404).json({error: "Not found"});
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    res.status(500).json({error: message});
  }
});

async function listSessions(req: Request, res: Response) {
  let query: FirebaseFirestore.Query = db.collection("sessions");

  const day = readSingleQueryParam(req.query.day);
  const track = readSingleQueryParam(req.query.track);

  if (day) {
    const dayNumber = Number(day);
    if (!Number.isNaN(dayNumber)) {
      query = query.where("day", "==", dayNumber);
    }
  }

  if (track) {
    query = query.where("track", "==", track);
  }

  const snapshot = await query.orderBy("startTime", "asc").get();
  res.json(snapshot.docs.map((doc) => normalizeSession(doc.data() as SessionDocument, doc.id)));
}

async function searchSessions(req: Request, res: Response) {
  const query = readSingleQueryParam(req.query.q).trim().toLowerCase();
  const snapshot = await db.collection("sessions").orderBy("startTime", "asc").get();
  const sessions = snapshot.docs
    .map((doc) => normalizeSession(doc.data() as SessionDocument, doc.id))
    .filter((session) => {
      if (!query) return true;
      return session.title.toLowerCase().includes(query) ||
        session.description.toLowerCase().includes(query);
    });

  res.json(sessions);
}

async function getSession(id: string, res: Response) {
  const doc = await db.collection("sessions").doc(id).get();
  if (!doc.exists) {
    res.status(404).json({error: `Session not found: ${id}`});
    return;
  }

  res.json(normalizeSession(doc.data() as SessionDocument, doc.id));
}

async function listSpeakers(res: Response) {
  const snapshot = await db.collection("speakers").orderBy("name", "asc").get();
  res.json(snapshot.docs.map((doc) => normalizeSpeaker(doc.data() as SpeakerDocument, doc.id)));
}

async function getSpeaker(id: string, res: Response) {
  const doc = await db.collection("speakers").doc(id).get();
  if (!doc.exists) {
    res.status(404).json({error: `Speaker not found: ${id}`});
    return;
  }

  res.json(normalizeSpeaker(doc.data() as SpeakerDocument, doc.id));
}

async function handleBookmarks(
  req: Request,
  res: Response,
  segments: string[],
) {
  const userId = segments[1];
  const sessionId = segments[3];
  const bookmarks = db.collection("users").doc(userId).collection("bookmarks");

  if (req.method === "GET" && !sessionId) {
    const snapshot = await bookmarks.get();
    res.json(snapshot.docs.map((doc) => doc.id));
    return;
  }

  if (req.method === "POST") {
    const requestedSessionId = req.body?.sessionId as string | undefined;
    if (!requestedSessionId) {
      res.status(400).json({error: "sessionId is required"});
      return;
    }

    const bookmarkRef = bookmarks.doc(requestedSessionId);
    const bookmark = await bookmarkRef.get();
    if (bookmark.exists) {
      await bookmarkRef.delete();
      res.json({sessionId: requestedSessionId, isBookmarked: false});
      return;
    }

    await bookmarkRef.set({sessionId: requestedSessionId, createdAt: FieldValue.serverTimestamp()});
    res.json({sessionId: requestedSessionId, isBookmarked: true});
    return;
  }

  if (req.method === "DELETE" && sessionId) {
    await bookmarks.doc(sessionId).delete();
    res.status(204).send("");
    return;
  }

  res.status(405).json({error: "Method not allowed"});
}

function normalizeSession(session: SessionDocument, documentId: string): SessionDocument {
  return {
    id: session.id || documentId,
    title: session.title || "",
    description: session.description || "",
    startTime: stringifyTimestamp(session.startTime),
    endTime: stringifyTimestamp(session.endTime),
    duration: session.duration || "",
    room: session.room || "",
    day: Number(session.day || 0),
    track: session.track || "ANDROID",
    type: session.type || "SESSION",
    level: session.level || "BEGINNER",
    speakerIds: Array.isArray(session.speakerIds) ? session.speakerIds : [],
    capacity: Number(session.capacity || 0),
    registered: Number(session.registered || 0),
    tags: Array.isArray(session.tags) ? session.tags : [],
    livestreamUrl: session.livestreamUrl ?? null,
    slidesUrl: session.slidesUrl ?? null,
    updatedAt: stringifyTimestamp(session.updatedAt),
  };
}

function normalizeSpeaker(speaker: SpeakerDocument, documentId: string): SpeakerDocument {
  return {
    id: speaker.id || documentId,
    name: speaker.name || "",
    role: speaker.role || "",
    company: speaker.company || "",
    bio: speaker.bio || "",
    photoUrl: speaker.photoUrl || "",
    photoBase64: speaker.photoBase64 || "",
    photoMimeType: speaker.photoMimeType || "",
    socialLinks: speaker.socialLinks || {},
  };
}

function stringifyTimestamp(value: unknown): string {
  if (!value) return "";
  if (typeof value === "string") return value;
  if (typeof value === "object" && "toDate" in value && typeof value.toDate === "function") {
    return value.toDate().toISOString();
  }
  return String(value);
}

function readSingleQueryParam(value: unknown): string {
  if (Array.isArray(value)) return String(value[0] ?? "");
  return String(value ?? "");
}
