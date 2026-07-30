import { createClient } from "jsr:@supabase/supabase-js@2";
import { corsHeaders } from "../_shared/cors.ts";
import { supabaseAdmin } from "../_shared/supabaseAdmin.ts";
import { captionImage, editImage } from "../_shared/hf.ts";

interface ReplaceRequestBody {
  room_photo_url: string;
  tap_x: number;
  tap_y: number;
  /** Small crop of the room photo centered on the tap, produced client-side; used to caption "what was tapped". */
  tapped_region_image_url: string;
  replacement_image_url: string;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) throw new Error("Missing Authorization header");

    const userClient = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: authHeader } } }
    );

    const { data: userData, error: userError } = await userClient.auth.getUser();
    if (userError || !userData.user) throw new Error("Not authenticated");
    const userId = userData.user.id;

    const { data: profile, error: profileError } = await userClient
      .from("profiles")
      .select("company_id, role")
      .eq("id", userId)
      .single();
    if (profileError || !profile) throw new Error("No profile found for this user");

    const body = (await req.json()) as ReplaceRequestBody;
    if (!body.room_photo_url || !body.replacement_image_url || !body.tapped_region_image_url) {
      throw new Error("Missing required fields");
    }

    const admin = supabaseAdmin();
    const { data: job, error: insertError } = await admin
      .from("replacement_jobs")
      .insert({
        user_id: userId,
        company_id: profile.company_id,
        room_photo_url: body.room_photo_url,
        tap_x: body.tap_x,
        tap_y: body.tap_y,
        status: "pending",
      })
      .select()
      .single();
    if (insertError || !job) throw new Error(insertError?.message ?? "Failed to create job");

    // Don't block the 202-style response on the AI pipeline; it updates the row when done.
    runPipeline(job.id, body).catch((err) => console.error(`Job ${job.id} failed`, err));

    return new Response(JSON.stringify({ job_id: job.id }), {
      status: 202,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: err instanceof Error ? err.message : String(err) }), {
      status: 400,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});

const RESULT_SIGNED_URL_EXPIRY_SECONDS = 60 * 60 * 24 * 7; // 7 days

async function runPipeline(jobId: string, body: ReplaceRequestBody): Promise<void> {
  const admin = supabaseAdmin();
  try {
    const [tappedLabel, productLabel] = await Promise.all([
      captionImage(body.tapped_region_image_url),
      captionImage(body.replacement_image_url),
    ]);

    const instruction =
      `Replace the ${tappedLabel} with ${productLabel}. ` +
      "Keep everything else in the photo unchanged, photorealistic, matching the original lighting and perspective.";
    const resultBytes = await editImage(body.room_photo_url, instruction);

    const resultPath = `${jobId}.png`;
    const { error: uploadError } = await admin.storage
      .from("results")
      .upload(resultPath, resultBytes, { contentType: "image/png", upsert: true });
    if (uploadError) throw new Error(uploadError.message);

    const { data: signed, error: signError } = await admin.storage
      .from("results")
      .createSignedUrl(resultPath, RESULT_SIGNED_URL_EXPIRY_SECONDS);
    if (signError || !signed) throw new Error(signError?.message ?? "Failed to sign result URL");

    await admin
      .from("replacement_jobs")
      .update({ status: "done", tapped_label: tappedLabel, result_image_url: signed.signedUrl })
      .eq("id", jobId);
  } catch (err) {
    await admin
      .from("replacement_jobs")
      .update({ status: "failed", error: err instanceof Error ? err.message : String(err) })
      .eq("id", jobId);
    throw err;
  }
}
