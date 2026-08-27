-- Update all system_config values to true
update platform.system_config set value = 'true'::jsonb where key = 'mfa.enabled';
update platform.system_config set value = 'true'::jsonb where key = 'mfa.require_admin';
update platform.system_config set value = 'true'::jsonb where key = 'mfa.allow_bootstrap';
