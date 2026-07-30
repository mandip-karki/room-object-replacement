import { createClient } from "jsr:@supabase/supabase-js@2";
import { corsHeaders } from "../_shared/cors.ts";
import { supabaseAdmin } from "../_shared/supabaseAdmin.ts";

interface CreateSubAccountRequest {
  email: string;
  password: string;
}

/**
 * Client Admin creates a Sub-Account under their own company. The new
 * profile's company_id is read from the caller's own row, never from the
 * request body, so a Client Admin can't mint accounts in another tenant.
 */
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

    const { data: callerProfile, error: profileError } = await userClient
      .from("profiles")
      .select("company_id, role")
      .eq("id", userData.user.id)
      .single();
    if (profileError || !callerProfile) throw new Error("No profile found for this user");
    if (callerProfile.role !== "client_admin") {
      throw new Error("Only a Client Admin can create sub-accounts");
    }

    const body = (await req.json()) as CreateSubAccountRequest;
    if (!body.email || !body.password) throw new Error("email and password are required");

    const admin = supabaseAdmin();
    const { data: created, error: createError } = await admin.auth.admin.createUser({
      email: body.email,
      password: body.password,
      email_confirm: true,
    });
    if (createError || !created.user) throw new Error(createError?.message ?? "Failed to create user");

    const { error: insertError } = await admin.from("profiles").insert({
      id: created.user.id,
      company_id: callerProfile.company_id,
      role: "sub_account",
      email: body.email,
    });
    if (insertError) throw new Error(insertError.message);

    return new Response(JSON.stringify({ uid: created.user.id }), {
      status: 201,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: err instanceof Error ? err.message : String(err) }), {
      status: 400,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
