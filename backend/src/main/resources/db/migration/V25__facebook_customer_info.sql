create table if not exists facebook.customer_info (
  id uuid primary key default gen_random_uuid(),
  page_id text not null check (btrim(page_id) <> ''),
  phone varchar(32) not null check (btrim(phone) <> ''),
  name varchar(160) not null check (btrim(name) <> ''),
  psid varchar(128) not null check (btrim(psid) <> ''),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (page_id, psid)
);

create index if not exists idx_customer_info_phone
  on facebook.customer_info(page_id, phone);

do $$
begin
  if exists (select 1 from pg_roles where rolname = 'core_app') then
    grant select, insert, update, delete
      on facebook.customer_info
      to core_app;
  end if;
end $$;
