-- FAIZMART INDIA SHOPPING V50
-- Run this once in Supabase SQL Editor.

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  phone text,
  email text,
  role text not null default 'customer' check (role in ('customer','seller','admin')),
  created_at timestamptz not null default now()
);

create table if not exists public.products (
  id uuid primary key default gen_random_uuid(),
  seller_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  price numeric(12,2) not null check (price >= 0),
  stock integer not null default 0 check (stock >= 0),
  image_url text,
  active boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.orders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete restrict,
  total_amount numeric(12,2) not null check (total_amount >= 0),
  payment_method text not null default 'COD',
  commission_rate numeric(5,2) not null default 10,
  commission_amount numeric(12,2) not null default 0,
  status text not null default 'pending',
  created_at timestamptz not null default now()
);

create table if not exists public.order_items (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null references public.orders(id) on delete cascade,
  product_id uuid not null references public.products(id) on delete restrict,
  quantity integer not null check (quantity > 0),
  unit_price numeric(12,2) not null check (unit_price >= 0)
);

alter table public.profiles enable row level security;
alter table public.products enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;

-- Profiles: user can create/update only their own profile.
drop policy if exists "profile own select" on public.profiles;
create policy "profile own select" on public.profiles for select to authenticated using (id = auth.uid());
drop policy if exists "profile own insert" on public.profiles;
create policy "profile own insert" on public.profiles for insert to authenticated with check (id = auth.uid());
drop policy if exists "profile own update" on public.profiles;
create policy "profile own update" on public.profiles for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

-- Products: everyone can browse active products; authenticated users can add products as themselves.
drop policy if exists "products public read" on public.products;
create policy "products public read" on public.products for select to anon, authenticated using (active = true);
drop policy if exists "products seller insert" on public.products;
create policy "products seller insert" on public.products for insert to authenticated with check (seller_id = auth.uid());
drop policy if exists "products seller update" on public.products;
create policy "products seller update" on public.products for update to authenticated using (seller_id = auth.uid()) with check (seller_id = auth.uid());
drop policy if exists "products seller delete" on public.products;
create policy "products seller delete" on public.products for delete to authenticated using (seller_id = auth.uid());

-- Orders: customers can create/read their own orders.
drop policy if exists "orders own read" on public.orders;
create policy "orders own read" on public.orders for select to authenticated using (user_id = auth.uid());
drop policy if exists "orders own insert" on public.orders;
create policy "orders own insert" on public.orders for insert to authenticated with check (user_id = auth.uid());

-- Order items: a customer can create items only for their own order.
drop policy if exists "order items own read" on public.order_items;
create policy "order items own read" on public.order_items for select to authenticated using (exists (select 1 from public.orders o where o.id = order_id and o.user_id = auth.uid()));
drop policy if exists "order items own insert" on public.order_items;
create policy "order items own insert" on public.order_items for insert to authenticated with check (exists (select 1 from public.orders o where o.id = order_id and o.user_id = auth.uid()));

-- Storage bucket for product photos.
insert into storage.buckets (id, name, public)
values ('product-images', 'product-images', true)
on conflict (id) do update set public = true;

-- Public image viewing.
drop policy if exists "product images public read" on storage.objects;
create policy "product images public read" on storage.objects for select to anon, authenticated using (bucket_id = 'product-images');

-- Seller/user can upload only inside their own user-id folder.
drop policy if exists "product images own upload" on storage.objects;
create policy "product images own upload" on storage.objects for insert to authenticated
with check (bucket_id = 'product-images' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "product images own update" on storage.objects;
create policy "product images own update" on storage.objects for update to authenticated
using (bucket_id = 'product-images' and (storage.foldername(name))[1] = auth.uid()::text)
with check (bucket_id = 'product-images' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "product images own delete" on storage.objects;
create policy "product images own delete" on storage.objects for delete to authenticated
using (bucket_id = 'product-images' and (storage.foldername(name))[1] = auth.uid()::text);
