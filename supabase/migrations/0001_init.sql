-- Schema for the RoomSwap multi-tenant room object replacement app.
-- auth.users (Supabase Auth) holds credentials; profiles holds our app-specific
-- role/company_id, since Supabase JWTs don't carry custom claims without an
-- Auth Hook, and RLS subqueries against profiles are simpler to set up by hand.

create type company_type as enum ('main', 'client');
create type user_role as enum ('super_admin', 'client_admin', 'sub_account');
create type job_status as enum ('pending', 'done', 'failed');

create table companies (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  type company_type not null default 'client',
  created_by uuid references auth.users(id),
  created_at timestamptz not null default now()
);

create table profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  company_id uuid not null references companies(id),
  role user_role not null default 'sub_account',
  email text not null,
  created_at timestamptz not null default now()
);

create table products (
  id uuid primary key default gen_random_uuid(),
  owner_company_id uuid not null references companies(id),
  name text not null,
  category text not null,
  image_url text not null,
  created_at timestamptz not null default now()
);

create table room_photos (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id),
  image_url text not null,
  created_at timestamptz not null default now()
);

create table replacement_jobs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id),
  company_id uuid not null references companies(id),
  room_photo_url text not null,
  tap_x double precision not null,
  tap_y double precision not null,
  product_id uuid references products(id),
  custom_item_image_url text,
  tapped_label text,
  result_image_url text,
  status job_status not null default 'pending',
  error text,
  created_at timestamptz not null default now()
);

-- Helper functions used by RLS policies below. security definer + a fixed
-- search_path so they can read `profiles` regardless of the calling role's
-- own row-level access, without being hijackable via search_path tricks.
create or replace function auth_role() returns user_role
language sql stable security definer set search_path = public as $$
  select role from profiles where id = auth.uid()
$$;

create or replace function auth_company_id() returns uuid
language sql stable security definer set search_path = public as $$
  select company_id from profiles where id = auth.uid()
$$;

alter table companies enable row level security;
alter table profiles enable row level security;
alter table products enable row level security;
alter table room_photos enable row level security;
alter table replacement_jobs enable row level security;

-- companies: Super Admin manages the tenant list; a company can read its own row.
create policy "companies_select" on companies for select
  using (auth_role() = 'super_admin' or id = auth_company_id());
create policy "companies_write" on companies for all
  using (auth_role() = 'super_admin') with check (auth_role() = 'super_admin');

-- profiles: Super Admin sees all; Client Admin manages their own company's users;
-- anyone can read their own profile.
create policy "profiles_select" on profiles for select
  using (
    auth_role() = 'super_admin'
    or id = auth.uid()
    or (auth_role() = 'client_admin' and company_id = auth_company_id())
  );
create policy "profiles_insert" on profiles for insert
  with check (
    auth_role() = 'super_admin'
    or (auth_role() = 'client_admin' and company_id = auth_company_id() and role = 'sub_account')
  );
create policy "profiles_update_delete" on profiles for update using (
    auth_role() = 'super_admin'
    or (auth_role() = 'client_admin' and company_id = auth_company_id())
  );

-- products: catalog is owned by the Main Company; every signed-in user can read it.
create policy "products_select" on products for select using (auth.uid() is not null);
create policy "products_write" on products for all
  using (auth_role() = 'super_admin') with check (auth_role() = 'super_admin');

-- room_photos: users only see/write their own.
create policy "room_photos_all" on room_photos for all
  using (user_id = auth.uid()) with check (user_id = auth.uid());

-- replacement_jobs: written only by the Edge Function (service role bypasses RLS);
-- clients may read jobs they own, or that belong to their own company (admins).
create policy "replacement_jobs_select" on replacement_jobs for select
  using (
    user_id = auth.uid()
    or ((auth_role() = 'client_admin' or auth_role() = 'super_admin') and company_id = auth_company_id())
  );

-- Storage buckets and their access policies.
insert into storage.buckets (id, name, public)
values ('room-photos', 'room-photos', false),
       ('products', 'products', true),
       ('results', 'results', false)
on conflict (id) do nothing;

-- room-photos/{uid}/... — only the owning user may read/write their own uploads.
create policy "room_photos_storage" on storage.objects for all
  using (bucket_id = 'room-photos' and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'room-photos' and (storage.foldername(name))[1] = auth.uid()::text);

-- products/... — public bucket, readable by anyone; writes restricted to Super Admin.
create policy "products_storage_write" on storage.objects for insert
  with check (bucket_id = 'products' and auth_role() = 'super_admin');
create policy "products_storage_update_delete" on storage.objects for update
  using (bucket_id = 'products' and auth_role() = 'super_admin');

-- results/... has no client-facing storage policy at all: the replace Edge
-- Function (service role, bypasses RLS) writes results and hands the client a
-- signed URL directly. A signed URL is self-authorizing, so the client never
-- calls the Storage API against this bucket.
