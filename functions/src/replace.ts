import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import { inpaint, segmentAtPoint } from "./fal";
import { AuthClaims, ReplaceRequestBody, ReplacementJobDoc } from "./types";

function requireAuth(context: functions.https.CallableContext): AuthClaims {
  const auth = context.auth;
  if (!auth) {
    throw new functions.https.HttpsError("unauthenticated", "Sign in required.");
  }
  const role = auth.token.role;
  const companyId = auth.token.companyId;
  if (!role || !companyId) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "User is missing role/companyId custom claims."
    );
  }
  return { role, companyId };
}

/** POST /replace equivalent — callable so Auth ID tokens are verified automatically. */
export const replace = functions.https.onCall(async (data: ReplaceRequestBody, context) => {
  const claims = requireAuth(context);
  const uid = context.auth!.uid;

  const { room_photo_url, tap_x, tap_y, replacement_image_url } = data;
  if (!room_photo_url || !replacement_image_url || tap_x == null || tap_y == null) {
    throw new functions.https.HttpsError("invalid-argument", "Missing required fields.");
  }

  const db = admin.firestore();
  const job: ReplacementJobDoc = {
    userId: uid,
    companyId: claims.companyId,
    roomPhotoUrl: room_photo_url,
    tapX: tap_x,
    tapY: tap_y,
    replacementImageUrl: replacement_image_url,
    resultImageUrl: null,
    status: "pending",
    error: null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  };
  const ref = await db.collection("replacement_jobs").add(job);

  // Run the pipeline after returning job_id to the client (202-Accepted style).
  runReplacementPipeline(ref.id, room_photo_url, tap_x, tap_y, replacement_image_url).catch((err) => {
    functions.logger.error(`Replacement job ${ref.id} failed`, err);
  });

  return { job_id: ref.id };
});

async function runReplacementPipeline(
  jobId: string,
  roomPhotoUrl: string,
  tapX: number,
  tapY: number,
  replacementImageUrl: string
): Promise<void> {
  const db = admin.firestore();
  const jobRef = db.collection("replacement_jobs").doc(jobId);
  try {
    const maskUrl = await segmentAtPoint(roomPhotoUrl, tapX, tapY);
    const resultUrl = await inpaint(roomPhotoUrl, maskUrl, replacementImageUrl);
    await jobRef.update({ status: "done", resultImageUrl: resultUrl });
  } catch (err) {
    await jobRef.update({ status: "failed", error: err instanceof Error ? err.message : String(err) });
    throw err;
  }
}

/** GET /replace/:job_id equivalent. */
export const getReplacementJob = functions.https.onCall(async (data: { job_id: string }, context) => {
  const claims = requireAuth(context);
  const uid = context.auth!.uid;

  const doc = await admin.firestore().collection("replacement_jobs").doc(data.job_id).get();
  if (!doc.exists) {
    throw new functions.https.HttpsError("not-found", "Job not found.");
  }
  const job = doc.data() as ReplacementJobDoc;

  // Tenant isolation: only the owning user, or an admin in the same company, may read the job.
  const sameCompany = job.companyId === claims.companyId;
  const isOwner = job.userId === uid;
  const isCompanyAdmin = sameCompany && (claims.role === "client_admin" || claims.role === "super_admin");
  if (!isOwner && !isCompanyAdmin) {
    throw new functions.https.HttpsError("permission-denied", "Not authorized to view this job.");
  }

  return {
    status: job.status,
    result_image_url: job.resultImageUrl ?? undefined,
  };
});
