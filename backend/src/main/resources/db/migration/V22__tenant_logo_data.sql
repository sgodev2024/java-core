-- Add logo data column to store image directly in database
alter table platform.tenant add column if not exists logo_data bytea;
alter table platform.tenant add column if not exists logo_media_type varchar(120);

-- Grant permissions
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'core_app') THEN
    GRANT SELECT, UPDATE ON platform.tenant TO core_app;
    RAISE NOTICE 'V22 tenant logo data grants ok';
  END IF;
END $$;
