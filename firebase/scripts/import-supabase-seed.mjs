import {readFile} from "node:fs/promises";
import {createRequire} from "node:module";
import path from "node:path";
import {fileURLToPath} from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, "../..");
const seedPath = path.join(repoRoot, "supabase/seed.sql");
const dryRun = process.argv.includes("--dry-run");

const sql = await readFile(seedPath, "utf8");
const speakers = parseInsert(sql, "speakers").map(toSpeaker);
const sessions = parseInsert(sql, "sessions").map(toSession);

if (dryRun) {
  console.log(`Parsed ${speakers.length} speakers and ${sessions.length} sessions from ${seedPath}.`);
  process.exit(0);
}

const requireFromFunctions = createRequire(path.join(repoRoot, "firebase/functions/package.json"));
const {initializeApp, applicationDefault} = requireFromFunctions("firebase-admin/app");
const {getFirestore} = requireFromFunctions("firebase-admin/firestore");

initializeApp({
  credential: applicationDefault(),
});

const db = getFirestore();

await writeCollection(db, "speakers", speakers);
await writeCollection(db, "sessions", sessions);

console.log(`Imported ${speakers.length} speakers and ${sessions.length} sessions into Firestore.`);

async function writeCollection(firestore, collectionName, rows) {
  let batch = firestore.batch();
  let count = 0;

  for (const row of rows) {
    batch.set(firestore.collection(collectionName).doc(row.id), row, {merge: true});
    count += 1;

    if (count % 400 === 0) {
      await batch.commit();
      batch = firestore.batch();
    }
  }

  if (count % 400 !== 0) {
    await batch.commit();
  }
}

function parseInsert(source, tableName) {
  const marker = `insert into public.${tableName}`;
  const start = source.indexOf(marker);
  if (start < 0) throw new Error(`Could not find insert for ${tableName}`);

  const valuesStart = source.indexOf("values", start);
  const conflictStart = source.indexOf("on conflict", valuesStart);
  if (valuesStart < 0 || conflictStart < 0) {
    throw new Error(`Could not parse insert block for ${tableName}`);
  }

  const block = source.slice(valuesStart + "values".length, conflictStart);
  return splitTuples(block).map(splitTupleFields);
}

function splitTuples(block) {
  const tuples = [];
  let quote = false;
  let depth = 0;
  let start = -1;

  for (let index = 0; index < block.length; index += 1) {
    const char = block[index];
    const next = block[index + 1];

    if (char === "'" && quote && next === "'") {
      index += 1;
      continue;
    }

    if (char === "'") {
      quote = !quote;
      continue;
    }

    if (!quote && char === "(") {
      if (depth === 0) start = index + 1;
      depth += 1;
      continue;
    }

    if (!quote && char === ")") {
      depth -= 1;
      if (depth === 0 && start >= 0) {
        tuples.push(block.slice(start, index));
        start = -1;
      }
    }
  }

  return tuples;
}

function splitTupleFields(tuple) {
  const fields = [];
  let quote = false;
  let bracketDepth = 0;
  let fieldStart = 0;

  for (let index = 0; index < tuple.length; index += 1) {
    const char = tuple[index];
    const next = tuple[index + 1];

    if (char === "'" && quote && next === "'") {
      index += 1;
      continue;
    }

    if (char === "'") {
      quote = !quote;
      continue;
    }

    if (!quote && char === "[") bracketDepth += 1;
    if (!quote && char === "]") bracketDepth -= 1;

    if (!quote && bracketDepth === 0 && char === ",") {
      fields.push(parseSqlValue(tuple.slice(fieldStart, index).trim()));
      fieldStart = index + 1;
    }
  }

  fields.push(parseSqlValue(tuple.slice(fieldStart).trim()));
  return fields;
}

function parseSqlValue(value) {
  if (/^null$/i.test(value)) return null;
  if (/^now\(\)$/i.test(value)) return new Date().toISOString();
  if (/^-?\d+(\.\d+)?$/.test(value)) return Number(value);

  const arrayMatch = value.match(/^array\[(.*)]::text\[]$/is);
  if (arrayMatch) {
    return splitTupleFields(arrayMatch[1]).map(String);
  }

  const jsonMatch = value.match(/^'(.*)'::jsonb$/is);
  if (jsonMatch) {
    return JSON.parse(unescapeSqlString(jsonMatch[1]));
  }

  const timestampMatch = value.match(/^'(.*)'::timestamptz$/is);
  if (timestampMatch) {
    return timestampMatch[1];
  }

  const stringMatch = value.match(/^'(.*)'$/is);
  if (stringMatch) {
    return unescapeSqlString(stringMatch[1]);
  }

  return value;
}

function toSpeaker(fields) {
  const [id, name, role, company, bio, photoUrl, socialLinks] = fields;
  return {id, name, role, company, bio, photoUrl, socialLinks};
}

function toSession(fields) {
  const [
    id,
    title,
    description,
    startTime,
    endTime,
    duration,
    room,
    day,
    track,
    type,
    level,
    speakerIds,
    capacity,
    registered,
    tags,
    livestreamUrl,
    slidesUrl,
    updatedAt,
  ] = fields;

  return {
    id,
    title,
    description,
    startTime,
    endTime,
    duration,
    room,
    day,
    track,
    type,
    level,
    speakerIds,
    capacity,
    registered,
    tags,
    livestreamUrl,
    slidesUrl,
    updatedAt: updatedAt || new Date().toISOString(),
  };
}

function unescapeSqlString(value) {
  return value.replace(/''/g, "'");
}
