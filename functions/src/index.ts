import * as admin from "firebase-admin";

admin.initializeApp();

export { getReplacementJob, replace } from "./replace";
export { createSubAccount } from "./users";
