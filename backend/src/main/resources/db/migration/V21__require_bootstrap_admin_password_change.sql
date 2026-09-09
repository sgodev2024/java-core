-- Legacy demo admin must be re-keyed by the configured bootstrap password.
update identity.account
set must_change_password = true
where email = 'admin@core.local'
  and password_algo = 'BCRYPT';
