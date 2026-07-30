const HF_API_BASE = "https://api-inference.huggingface.co/models";

// Free-tier Hugging Face Inference Providers has no mask-based "inpaint this
// exact reference photo into a masked region" pipeline (that's what fal.ai's
// flux-lora/inpainting gave us). Instead we caption both the tapped object
// and the replacement product, then hand FLUX.1-Kontext-dev a text instruction
// to edit the whole photo. It's genuinely free (one HF token, no separate
// fal/replicate account) but the result is guided by a *description* of the
// product rather than a pixel-exact composite of the photo itself.
const CAPTION_MODEL = "Salesforce/blip-image-captioning-large";
const EDIT_MODEL = "black-forest-labs/FLUX.1-Kontext-dev";

function hfToken(): string {
  const token = Deno.env.get("HF_TOKEN");
  if (!token) throw new Error("HF_TOKEN is not configured as a Supabase Edge Function secret.");
  return token;
}

async function fetchImageBuffer(url: string): Promise<ArrayBuffer> {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`Failed to fetch image ${url}: ${response.status}`);
  return await response.arrayBuffer();
}

function bufferToBase64(buffer: ArrayBuffer): string {
  let binary = "";
  for (const byte of new Uint8Array(buffer)) binary += String.fromCharCode(byte);
  return btoa(binary);
}

async function hfPost(model: string, body: BodyInit, contentType?: string): Promise<Response> {
  const headers: Record<string, string> = { Authorization: `Bearer ${hfToken()}` };
  if (contentType) headers["Content-Type"] = contentType;
  const response = await fetch(`${HF_API_BASE}/${model}`, { method: "POST", headers, body });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Hugging Face ${model} failed (${response.status}): ${text}`);
  }
  return response;
}

/** Short caption for an image, e.g. "a gray marble hexagon floor tile". */
export async function captionImage(imageUrl: string): Promise<string> {
  const buffer = await fetchImageBuffer(imageUrl);
  const response = await hfPost(CAPTION_MODEL, new Blob([buffer]), "application/octet-stream");
  const result = (await response.json()) as { generated_text: string }[];
  const caption = result[0]?.generated_text;
  if (!caption) throw new Error("Hugging Face captioning returned no text");
  return caption;
}

/** Instruction-edits the room photo with FLUX.1-Kontext-dev; returns the edited image bytes. */
export async function editImage(imageUrl: string, instruction: string): Promise<Uint8Array> {
  const imageBuffer = await fetchImageBuffer(imageUrl);
  const payload = {
    inputs: bufferToBase64(imageBuffer),
    parameters: { prompt: instruction },
  };
  const response = await hfPost(EDIT_MODEL, JSON.stringify(payload), "application/json");
  return new Uint8Array(await response.arrayBuffer());
}
