-- System config table for runtime settings (MFA toggle)
create table if not exists platform.system_config(
  key varchar(100) primary key,
  value jsonb not null default '{}',
  updated_at timestamptz not null default now(),
  updated_by uuid references identity.account(id)
);

-- Insert default MFA config (can be overridden via API)
insert into platform.system_config(key, value) values 
('mfa.enabled', 'true'::jsonb),
('mfa.require_admin', 'true'::jsonb),
('mfa.allow_bootstrap', 'false'::jsonb)
on conflict (key) do nothing;

-- Grant permissions
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'core_app') THEN
    GRANT SELECT, UPDATE ON platform.system_config TO core_app;
    RAISE NOTICE 'granted system_config to core_app';
  ELSE
    RAISE NOTICE 'role core_app not present; skipping grants';
  END IF;
END $$;
