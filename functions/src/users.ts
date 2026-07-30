import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import { UserRole } from "./types";

interface CreateSubAccountRequest {
  email: string;
  password: string;
}

/**
 * Client Admin creates a Sub-Account under their own company. The new user's role/companyId
 * are set here from the caller's own verified claims, never from client-supplied input,
 * so a Client Admin cannot mint accounts in another tenant.
 */
export const createSubAccount = functions.https.onCall(
  async (data: CreateSubAccountRequest, context) => {
    const auth = context.auth;
    if (!auth || auth.token.role !== "client_admin") {
      throw new functions.https.HttpsError("permission-denied", "Only a Client Admin can do this.");
    }
    const companyId = auth.token.companyId as string;

    const userRecord = await admin.auth().createUser({
      email: data.email,
      password: data.password,
    });

    const role: UserRole = "sub_account";
    await admin.auth().setCustomUserClaims(userRecord.uid, { role, companyId });
    await admin.firestore().collection("users").doc(userRecord.uid).set({
      companyId,
      role,
      email: data.email,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return { uid: userRecord.uid };
  }
);
