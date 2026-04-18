/**
 * Upload Gemma 3 1B (.task) model to Firebase Storage.
 *
 * Prerequisites:
 *   1. Firebase Storage MUST be activated on the project
 *      (visit https://console.firebase.google.com/project/project-d-r1997t/storage
 *       and click "Get Started")
 *
 *   2. A service account key at scripts/serviceAccountKey.json
 *      Download from:
 *      https://console.firebase.google.com/project/project-d-r1997t/settings/serviceaccounts/adminsdk
 *      Click "Generate new private key" -> save as scripts/serviceAccountKey.json
 *
 *   3. The model file at project root: gemma3-1b-it-int4.task (~550 MB)
 *      Download from:
 *      https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task
 *
 * Run with:  node scripts/upload-gemma.js
 */

const admin = require("firebase-admin");
const fs = require("fs");
const path = require("path");

// ─── Config ──────────────────────────────────────────────────────────────────
const PROJECT_ID = "project-d-r1997t";
const MODEL_FILE = "gemma3-1b-it-int4.task";
const REMOTE_PATH = `models/${MODEL_FILE}`;
const SERVICE_ACCOUNT = path.join(__dirname, "serviceAccountKey.json");
const LOCAL_MODEL = path.join(__dirname, "..", MODEL_FILE);

// ─── Pre-flight checks ───────────────────────────────────────────────────────
if (!fs.existsSync(SERVICE_ACCOUNT)) {
  console.error("[ERR] scripts/serviceAccountKey.json introuvable.");
  console.error(
    "      Telecharge la cle depuis la console Firebase :\n" +
      `      https://console.firebase.google.com/project/${PROJECT_ID}/settings/serviceaccounts/adminsdk`
  );
  process.exit(1);
}

if (!fs.existsSync(LOCAL_MODEL)) {
  console.error(`[ERR] Fichier modele introuvable : ${LOCAL_MODEL}`);
  console.error(
    "      Telecharge depuis :\n" +
      "      https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task"
  );
  process.exit(1);
}

const fileSize = fs.statSync(LOCAL_MODEL).size;
const fileSizeMB = (fileSize / 1024 / 1024).toFixed(1);
console.log(`[OK] Fichier modele trouve : ${fileSizeMB} MB`);

// ─── Init Admin SDK ──────────────────────────────────────────────────────────
const serviceAccount = require(SERVICE_ACCOUNT);
const bucketName =
  serviceAccount.storage_bucket ||
  `${PROJECT_ID}.firebasestorage.app`;

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: bucketName,
});

const bucket = admin.storage().bucket();
console.log(`[OK] Bucket cible : gs://${bucket.name}`);

// ─── Upload ──────────────────────────────────────────────────────────────────
console.log(`[..] Upload vers ${REMOTE_PATH} en cours...`);
const startTime = Date.now();
let lastLogged = 0;
let bytesUploaded = 0;

const readStream = fs.createReadStream(LOCAL_MODEL);
const writeStream = bucket.file(REMOTE_PATH).createWriteStream({
  resumable: true,
  metadata: {
    contentType: "application/octet-stream",
    cacheControl: "public, max-age=31536000", // 1 year (immutable file)
  },
});

readStream.on("data", (chunk) => {
  bytesUploaded += chunk.length;
  // Progress log every 25 MB
  if (bytesUploaded - lastLogged > 25 * 1024 * 1024) {
    const pct = ((bytesUploaded / fileSize) * 100).toFixed(1);
    const mb = (bytesUploaded / 1024 / 1024).toFixed(1);
    console.log(`    ${pct}% (${mb}/${fileSizeMB} MB)`);
    lastLogged = bytesUploaded;
  }
});

writeStream.on("error", (err) => {
  console.error("[ERR] Upload echoue :", err.message);
  if (err.message.includes("does not exist") || err.code === 404) {
    console.error(
      "      Firebase Storage n'est peut-etre pas encore active.\n" +
        `      Active-le sur : https://console.firebase.google.com/project/${PROJECT_ID}/storage`
    );
  }
  process.exit(1);
});

writeStream.on("finish", async () => {
  const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
  console.log(`[OK] Upload termine en ${elapsed}s`);

  // Verify upload
  const [metadata] = await bucket.file(REMOTE_PATH).getMetadata();
  console.log(`[OK] Fichier confirme : gs://${bucket.name}/${REMOTE_PATH}`);
  console.log(`     Taille : ${(metadata.size / 1024 / 1024).toFixed(1)} MB`);
  console.log(`     MD5    : ${metadata.md5Hash}`);
  console.log("");
  console.log(
    ">>> L'app DiaSmart peut maintenant telecharger Gemma depuis Firebase Storage."
  );
  process.exit(0);
});

readStream.pipe(writeStream);
