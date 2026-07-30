import { createClient } from "jsr:@supabase/supabase-js@2";

/** Service-role client: bypasses RLS. Only ever used inside Edge Functions, never shipped to the app. */
export function supabaseAdmin() {
  return createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );
}
