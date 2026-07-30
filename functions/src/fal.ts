import * as functions from "firebase-functions";

const FAL_BASE_URL = "https://fal.run";

function apiKey(): string {
  const key = process.env.FAL_API_KEY;
  if (!key) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "FAL_API_KEY is not configured. Set it with `firebase functions:secrets:set FAL_API_KEY`."
    );
  }
  return key;
}

async function callFal<T>(model: string, input: unknown): Promise<T> {
  const response = await fetch(`${FAL_BASE_URL}/${model}`, {
    method: "POST",
    headers: {
      Authorization: `Key ${apiKey()}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
  });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`fal.ai ${model} failed (${response.status}): ${body}`);
  }
  return (await response.json()) as T;
}

interface SegmentResult {
  mask_url: string;
}

/** SAM-style point-prompted segmentation: turns a tap into a mask of the tapped object. */
export async function segmentAtPoint(imageUrl: string, tapX: number, tapY: number): Promise<string> {
  const result = await callFal<SegmentResult>("fal-ai/sam2/image", {
    image_url: imageUrl,
    points: [[tapX, tapY]],
  });
  return result.mask_url;
}

interface InpaintResult {
  images: { url: string }[];
}

/** Composites the replacement image into the masked region of the room photo. */
export async function inpaint(roomPhotoUrl: string, maskUrl: string, referenceImageUrl: string): Promise<string> {
  const result = await callFal<InpaintResult>("fal-ai/flux-lora/inpainting", {
    image_url: roomPhotoUrl,
    mask_url: maskUrl,
    reference_image_url: referenceImageUrl,
  });
  const url = result.images[0]?.url;
  if (!url) throw new Error("fal.ai inpainting returned no image");
  return url;
}
