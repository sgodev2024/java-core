create schema if not exists facebook;

create table if not exists facebook.messenger_conversation (
  id text primary key,
  page_id text not null,
  customer_psid text not null,
  customer_name text not null,
  last_message text not null default '',
  unread_count integer not null default 0,
  updated_at timestamptz not null default now(),
  unique (page_id, customer_psid)
);

create index if not exists idx_messenger_conversation_updated
  on facebook.messenger_conversation(updated_at desc);

create table if not exists facebook.messenger_message (
  id text primary key,
  conversation_id text not null references facebook.messenger_conversation(id) on delete cascade,
  direction text not null check (direction in ('INBOUND', 'OUTBOUND')),
  message text not null default '',
  status text not null,
  attachment_type text,
  attachment_url text,
  sent_at timestamptz not null,
  created_at timestamptz not null default now()
);

create index if not exists idx_messenger_message_conversation_sent
  on facebook.messenger_message(conversation_id, sent_at);

