do $$
begin
  if exists (select 1 from pg_roles where rolname = 'core_app') then
    grant usage on schema facebook to core_app;
    grant select, insert, update, delete
      on facebook.messenger_conversation, facebook.messenger_message
      to core_app;
  end if;
end $$;
