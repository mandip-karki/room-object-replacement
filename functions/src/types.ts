export type CompanyType = "main" | "client";
export type UserRole = "super_admin" | "client_admin" | "sub_account";
export type JobStatus = "pending" | "done" | "failed";

export interface AuthClaims {
  role: UserRole;
  companyId: string;
}

export interface ReplaceRequestBody {
  room_photo_url: string;
  tap_x: number;
  tap_y: number;
  replacement_image_url: string;
}

export interface ReplacementJobDoc {
  userId: string;
  companyId: string;
  roomPhotoUrl: string;
  tapX: number;
  tapY: number;
  replacementImageUrl: string;
  resultImageUrl: string | null;
  status: JobStatus;
  error: string | null;
  createdAt: FirebaseFirestore.FieldValue;
}
