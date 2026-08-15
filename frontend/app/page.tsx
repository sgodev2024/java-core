"use client";

import { useEffect, useMemo, useState } from "react";

type View = "overview" | "modules" | "resources" | "access" | "activity" | "files" | "settings";
type AuthStep = "login" | "mfa";
const API_URL = process.env.NEXT_PUBLIC_CORE_API_URL ?? "https://corejava.sgodata.com";

function LoginScreen({ onAuthenticated }: { onAuthenticated: (token: string, remember: boolean) => void }) {
  const [step, setStep] = useState<AuthStep>("login");
  const [email, setEmail] = useState("admin@core.local");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(true);
  const [otp, setOtp] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [challengeId, setChallengeId] = useState("");

  const submitLogin = async (event: React.FormEvent) => {
    event.preventDefault();
    setError("");
    if (!/^\S+@\S+\.\S+$/.test(email)) return setError("Vui lòng nhập địa chỉ email hợp lệ.");
    if (password.length < 8) return setError("Mật khẩu phải có ít nhất 8 ký tự.");
    setLoading(true);
    try {
      const response = await fetch(`${API_URL}/api/v1/auth/login`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password }) });
      const body = await response.json();
      if (!response.ok) throw new Error(body.detail ?? "Không thể đăng nhập.");
      setChallengeId(body.challengeId); setStep("mfa");
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Backend chưa sẵn sàng. Vui lòng thử lại."); }
    finally { setLoading(false); }
  };

  const submitOtp = async (event: React.FormEvent) => {
    event.preventDefault();
    setError("");
    if (!/^\d{6}$/.test(otp)) return setError("Mã xác thực phải gồm đúng 6 chữ số.");
    setLoading(true);
    try {
      const response = await fetch(`${API_URL}/api/v1/auth/mfa`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ challengeId, code: otp, remember }) });
      const body = await response.json();
      if (!response.ok) throw new Error(body.detail ?? "Mã xác thực không hợp lệ.");
      onAuthenticated(body.accessToken, remember);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể xác thực."); }
    finally { setLoading(false); }
  };

  return <div className="auth-page">
    <section className="auth-brand-panel" aria-label="Giới thiệu Core Platform">
      <div className="auth-brand"><div className="brand-mark large"><i /><i /><i /><i /></div><div><strong>Core</strong><span>Platform</span></div></div>
      <div className="auth-message"><span className="auth-kicker">Enterprise application foundation</span><h1>Nền tảng cốt lõi.<br />Sẵn sàng để mở rộng.</h1><p>Quản trị modules, tài nguyên, phân quyền và vận hành hệ thống từ một trung tâm duy nhất.</p></div>
      <div className="auth-trust"><div><StatusDot /><span>Hệ thống hoạt động ổn định</span></div><small>Core v1.0.0-rc.4 · Secure access</small></div>
    </section>
    <section className="auth-form-panel">
      <div className="auth-card">
        {step === "login" ? <>
          <div className="auth-heading"><span className="mobile-auth-logo">CP</span><h2>Đăng nhập</h2><p>Sử dụng tài khoản nội bộ để truy cập Core Platform.</p></div>
          <form onSubmit={submitLogin} noValidate>
            <label>Email công việc<input autoFocus type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@company.vn" /></label>
            <label>Mật khẩu<div className="password-field"><input type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Nhập mật khẩu" /><span>●●●</span></div></label>
            <div className="auth-options"><label className="check-label"><input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} /> Ghi nhớ đăng nhập</label><button type="button" className="auth-link" onClick={() => setError("Vui lòng liên hệ Platform Administrator để đặt lại mật khẩu.")}>Quên mật khẩu?</button></div>
            {error && <p className="auth-error" role="alert">{error}</p>}
            <button className="auth-submit" disabled={loading}>{loading ? "Đang xác thực..." : "Tiếp tục"}<span>→</span></button>
          </form>
          <div className="auth-security"><span>◆</span><p><strong>Kết nối được bảo vệ</strong><small>Phiên đăng nhập được mã hóa và ghi nhận audit.</small></p></div>
        </> : <>
          <button className="auth-back" onClick={() => { setStep("login"); setError(""); }}>← Quay lại</button>
          <div className="auth-heading"><span className="mfa-icon">✓</span><h2>Xác thực hai lớp</h2><p>Nhập mã 6 chữ số từ ứng dụng xác thực của bạn.</p></div>
          <form onSubmit={submitOtp} noValidate>
            <label>Mã xác thực<input className="otp-input" autoFocus inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, ""))} placeholder="000000" /></label>
            {error && <p className="auth-error" role="alert">{error}</p>}
            <button className="auth-submit" disabled={loading}>{loading ? "Đang kiểm tra..." : "Xác nhận đăng nhập"}<span>→</span></button>
            <p className="otp-help">Chưa nhận được mã? <button type="button" onClick={() => setError("Mã mới đã được tạo trong ứng dụng xác thực.")}>Gửi lại mã</button></p>
          </form>
        </>}
        <footer>© 2026 Core Platform <span>·</span> Trợ giúp <span>·</span> Chính sách bảo mật</footer>
      </div>
    </section>
  </div>;
}

const navItems: Array<{ id: View; label: string; icon: string; badge?: string }> = [
  { id: "overview", label: "Tổng quan", icon: "⌂" },
  { id: "modules", label: "Modules", icon: "◫", badge: "8" },
  { id: "resources", label: "Resources", icon: "◇" },
  { id: "access", label: "Truy cập", icon: "◎" },
  { id: "activity", label: "Events & Jobs", icon: "↯", badge: "12" },
  { id: "files", label: "Tệp tin", icon: "▱" },
  { id: "settings", label: "Cấu hình", icon: "⚙" },
];

const modules = [
  { name: "Local Identity", key: "local-identity", version: "1.0.0", status: "Healthy", tone: "teal", detail: "Tài khoản, phiên đăng nhập và MFA", events: "4 contracts" },
  { name: "Permission", key: "permission", version: "1.0.0", status: "Healthy", tone: "teal", detail: "Policy, vai trò và record scope", events: "6 policies" },
  { name: "Audit Store", key: "audit-store", version: "1.0.0", status: "Healthy", tone: "teal", detail: "Audit append-only và checkpoint", events: "12.4k records" },
  { name: "Event Outbox", key: "event-outbox", version: "1.0.0", status: "Attention", tone: "amber", detail: "Outbox, inbox và delivery relay", events: "12 pending" },
  { name: "Job Queue", key: "job-queue", version: "1.0.0", status: "Healthy", tone: "teal", detail: "Job, scheduler và retry", events: "3 running" },
  { name: "File Management", key: "file-management", version: "1.0.0", status: "Healthy", tone: "teal", detail: "Upload, scan và object storage", events: "84.2 GB" },
  { name: "Dynamic Resource", key: "dynamic-resource", version: "1.0.0", status: "Healthy", tone: "teal", detail: "Definition, generic API và history", events: "24 definitions" },
  { name: "Webhook", key: "webhook", version: "1.0.0", status: "Disabled", tone: "gray", detail: "Delivery tới hệ thống bên ngoài", events: "Optional" },
];

const resources = [
  { name: "Approval Request", type: "DOMAIN", module: "sample-domain", records: "1,248", version: "v3", updated: "2 phút trước" },
  { name: "Customer Preference", type: "DYNAMIC", module: "dynamic-resource", records: "8,492", version: "v2", updated: "8 phút trước" },
  { name: "Service Account", type: "DOMAIN", module: "local-identity", records: "42", version: "v1", updated: "34 phút trước" },
  { name: "Notification Template", type: "DYNAMIC", module: "notification", records: "86", version: "v4", updated: "1 giờ trước" },
  { name: "File Object", type: "DOMAIN", module: "file-management", records: "24,903", version: "v2", updated: "2 giờ trước" },
];

const activities = [
  { type: "event", title: "approval-request.approved.v1", meta: "sample-domain · tenant acme-vn", time: "10:42:18", state: "Published" },
  { type: "job", title: "file.reconcile", meta: "job-01J8... · attempt 1/3", time: "10:41:52", state: "Running" },
  { type: "event", title: "identity.session-revoked.v1", meta: "local-identity · security", time: "10:39:07", state: "Published" },
  { type: "job", title: "audit.checkpoint", meta: "job-01J7... · 12,400 events", time: "10:35:11", state: "Succeeded" },
  { type: "event", title: "webhook.delivery-failed", meta: "endpoint misa-adapter · retry 2/5", time: "10:31:43", state: "Retrying" },
];

const roles = [
  { name: "Platform Administrator", users: 3, policies: 12, scope: "Toàn deployment", color: "violet" },
  { name: "Module Maintainer", users: 8, policies: 7, scope: "Theo module", color: "blue" },
  { name: "Security Auditor", users: 2, policies: 5, scope: "Chỉ đọc + export", color: "amber" },
  { name: "Application User", users: 184, policies: 9, scope: "Theo organization", color: "teal" },
];

function StatusDot({ tone = "teal" }: { tone?: string }) {
  return <span className={`status-dot ${tone}`} aria-hidden="true" />;
}

function PageTitle({ eyebrow, title, description, action }: { eyebrow: string; title: string; description: string; action?: React.ReactNode }) {
  return (
    <div className="page-heading">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p className="page-description">{description}</p>
      </div>
      {action}
    </div>
  );
}

function Overview({ onNavigate }: { onNavigate: (view: View) => void }) {
  return (
    <>
      <PageTitle
        eyebrow="Core Control Plane"
        title="Chào buổi sáng, Minh"
        description="Hệ thống đang ổn định. Có 12 sự kiện đang chờ xử lý và một cảnh báo cần xem xét."
        action={<button className="primary-button" onClick={() => onNavigate("resources")}><span>＋</span> Tạo resource</button>}
      />

      <section className="health-banner" aria-label="Trạng thái hệ thống">
        <div className="health-mark"><span>✓</span></div>
        <div className="health-copy">
          <div className="health-title"><strong>Tất cả dịch vụ cốt lõi đang hoạt động</strong><span className="live-pill"><StatusDot /> Live</span></div>
          <p>Database 18 ms · Object storage 42 ms · Audit checkpoint 4 phút trước</p>
        </div>
        <button className="text-button" onClick={() => onNavigate("activity")}>Xem chi tiết <span>→</span></button>
      </section>

      <section className="metric-grid" aria-label="Chỉ số chính">
        <article className="metric-card">
          <div className="metric-icon blue">◇</div><span className="metric-label">Resource records</span>
          <strong className="metric-value">34,771</strong><span className="metric-trend positive">↑ 8.4%</span><small>so với 30 ngày trước</small>
        </article>
        <article className="metric-card">
          <div className="metric-icon violet">◫</div><span className="metric-label">Modules hoạt động</span>
          <strong className="metric-value">7 <em>/ 8</em></strong><span className="metric-trend neutral">Ổn định</span><small>1 module đang tắt</small>
        </article>
        <article className="metric-card">
          <div className="metric-icon amber">↯</div><span className="metric-label">Outbox đang chờ</span>
          <strong className="metric-value">12</strong><span className="metric-trend warning">+5 mới</span><small>cũ nhất 48 giây</small>
        </article>
        <article className="metric-card">
          <div className="metric-icon teal">◷</div><span className="metric-label">API latency p95</span>
          <strong className="metric-value">184 <em>ms</em></strong><span className="metric-trend positive">↓ 12 ms</span><small>mục tiêu ≤ 300 ms</small>
        </article>
      </section>

      <div className="dashboard-grid">
        <section className="panel activity-panel">
          <div className="panel-header"><div><h2>Hoạt động gần đây</h2><p>Luồng sự kiện và tác vụ trên toàn hệ thống</p></div><button className="ghost-button" onClick={() => onNavigate("activity")}>Xem tất cả</button></div>
          <div className="activity-list">
            {activities.slice(0, 4).map((item) => (
              <div className="activity-row" key={item.title}>
                <div className={`activity-icon ${item.type}`}>{item.type === "event" ? "↯" : "◷"}</div>
                <div className="activity-main"><strong>{item.title}</strong><span>{item.meta}</span></div>
                <span className={`state ${item.state.toLowerCase()}`}>{item.state}</span>
                <time>{item.time}</time>
              </div>
            ))}
          </div>
        </section>

        <aside className="panel quick-panel">
          <div className="panel-header"><div><h2>Truy cập nhanh</h2><p>Các tác vụ thường dùng</p></div></div>
          <button onClick={() => onNavigate("modules")}><span className="quick-icon">◫</span><div><strong>Quản lý modules</strong><small>Bật, tắt và kiểm tra phiên bản</small></div><b>→</b></button>
          <button onClick={() => onNavigate("access")}><span className="quick-icon">◎</span><div><strong>Phân quyền</strong><small>Vai trò, policy và phạm vi</small></div><b>→</b></button>
          <button onClick={() => onNavigate("files")}><span className="quick-icon">▱</span><div><strong>Kho tệp</strong><small>24.903 tệp · 84,2 GB</small></div><b>→</b></button>
        </aside>
      </div>

      <section className="panel environment-panel">
        <div><span className="environment-label">Môi trường</span><strong>core-production-vn</strong><small>Standard tier · Ho Chi Minh City</small></div>
        <div><span>Phiên bản Core</span><strong>1.0.0-rc.4</strong></div>
        <div><span>Database</span><strong><StatusDot /> PostgreSQL 17</strong></div>
        <div><span>Lần triển khai gần nhất</span><strong>14/08/2026 · 21:30</strong></div>
        <button className="ghost-button">Thông tin hệ thống</button>
      </section>
    </>
  );
}

function Modules() {
  return (
    <>
      <PageTitle eyebrow="Runtime composition" title="Modules" description="Theo dõi capability, phiên bản và trạng thái của các module đã đóng gói trong deployment." action={<button className="secondary-button">Kiểm tra tương thích</button>} />
      <div className="filter-row"><div className="search-field">⌕ <input aria-label="Tìm module" placeholder="Tìm theo tên hoặc capability..." /></div><button className="filter-chip active">Tất cả 8</button><button className="filter-chip">Đang bật 7</button><button className="filter-chip">Cần chú ý 1</button></div>
      <section className="module-grid">
        {modules.map((item) => <article className="module-card" key={item.key}>
          <div className="module-top"><div className="module-symbol">{item.name.slice(0, 2).toUpperCase()}</div><span className={`state ${item.tone}`}>{item.status}</span></div>
          <h3>{item.name}</h3><code>{item.key}</code><p>{item.detail}</p>
          <div className="module-meta"><span>v{item.version}</span><span>{item.events}</span></div>
          <button className="card-action">Mở chi tiết <span>→</span></button>
        </article>)}
      </section>
    </>
  );
}

function Resources() {
  const [query, setQuery] = useState("");
  const filtered = resources.filter((r) => `${r.name} ${r.module}`.toLowerCase().includes(query.toLowerCase()));
  return (
    <>
      <PageTitle eyebrow="Three-Plane Registry" title="Resources" description="Domain aggregate và Dynamic Resource cùng đăng ký capability nhưng giữ persistence độc lập." action={<button className="primary-button">＋ Tạo definition</button>} />
      <section className="panel table-panel">
        <div className="table-tools"><div className="search-field wide">⌕ <input value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Tìm resource" placeholder="Tìm resource, module..." /></div><button className="filter-chip active">Tất cả</button><button className="filter-chip">Domain</button><button className="filter-chip">Dynamic</button></div>
        <div className="data-table" role="table" aria-label="Danh sách resources">
          <div className="table-row table-head" role="row"><span>Tên resource</span><span>Storage mode</span><span>Owner module</span><span>Records</span><span>Schema</span><span>Cập nhật</span></div>
          {filtered.map((item) => <button className="table-row" role="row" key={item.name}>
            <span><b className="resource-glyph">◇</b><strong>{item.name}</strong></span><span><em className={`type-pill ${item.type.toLowerCase()}`}>{item.type}</em></span><span><code>{item.module}</code></span><span>{item.records}</span><span>{item.version}</span><span>{item.updated} <b>→</b></span>
          </button>)}
        </div>
      </section>
    </>
  );
}

function Access() {
  return (
    <>
      <PageTitle eyebrow="Identity & authorization" title="Truy cập" description="Quản lý vai trò, policy và phạm vi truy cập. Mọi quyết định không xác định đều bị từ chối." action={<button className="primary-button">＋ Tạo vai trò</button>} />
      <section className="access-summary"><div><span>Người dùng hoạt động</span><strong>197</strong><small>+12 trong 30 ngày</small></div><div><span>Vai trò</span><strong>14</strong><small>4 vai trò hệ thống</small></div><div><span>Policies</span><strong>33</strong><small>100% đã biên dịch</small></div><div><span>Yêu cầu bị từ chối</span><strong>28</strong><small>24 giờ gần nhất</small></div></section>
      <div className="role-grid">{roles.map((role) => <article className="panel role-card" key={role.name}><div className={`role-badge ${role.color}`}>{role.name.split(" ").map(x => x[0]).join("").slice(0,2)}</div><div className="role-copy"><h3>{role.name}</h3><p>{role.scope}</p></div><button aria-label={`Mở ${role.name}`}>•••</button><dl><div><dt>Người dùng</dt><dd>{role.users}</dd></div><div><dt>Policies</dt><dd>{role.policies}</dd></div></dl></article>)}</div>
      <section className="panel policy-banner"><div className="policy-icon">✓</div><div><h3>Permission engine đang ở chế độ fail-closed</h3><p>Policy không tồn tại hoặc evaluation lỗi sẽ trả về Deny. Revision hiện tại: <code>perm-r1842</code></p></div><button className="secondary-button">Mở Policy Explorer</button></section>
    </>
  );
}

function Activity() {
  return (
    <>
      <PageTitle eyebrow="Durable processing" title="Events & Jobs" description="Theo dõi outbox, consumer, background jobs, retry và dead-letter flow." action={<button className="secondary-button">↻ Làm mới</button>} />
      <section className="queue-grid"><div className="queue-card"><span>Outbox pending</span><strong>12</strong><div className="mini-bar"><i style={{width:"22%"}} /></div><small>Cũ nhất 48 giây</small></div><div className="queue-card"><span>Jobs running</span><strong>3</strong><div className="mini-bar blue"><i style={{width:"38%"}} /></div><small>7 workers sẵn sàng</small></div><div className="queue-card"><span>Retrying</span><strong>2</strong><div className="mini-bar amber"><i style={{width:"14%"}} /></div><small>Không có dead job</small></div><div className="queue-card"><span>Consumer lag</span><strong>0.8s</strong><div className="mini-bar violet"><i style={{width:"11%"}} /></div><small>Trong ngưỡng SLO</small></div></section>
      <section className="panel timeline-panel"><div className="panel-header"><div><h2>Live activity stream</h2><p>At-least-once delivery · idempotent consumers</p></div><span className="live-pill"><StatusDot /> Đang cập nhật</span></div>{activities.map((item) => <div className="activity-row expanded" key={item.title}><div className={`activity-icon ${item.type}`}>{item.type === "event" ? "↯" : "◷"}</div><div className="activity-main"><strong>{item.title}</strong><span>{item.meta}</span></div><span className={`state ${item.state.toLowerCase()}`}>{item.state}</span><time>{item.time}</time><button>Chi tiết</button></div>)}</section>
    </>
  );
}

function Files() {
  const fileRows = [
    ["architecture-standard-v1.1.pdf", "application/pdf", "2.4 MB", "Internal", "Active", "Hôm nay, 09:24"],
    ["customer-import-2026-08.csv", "text/csv", "18.7 MB", "Confidential", "Active", "Hôm qua, 16:08"],
    ["audit-checkpoint-20260814.sig", "application/octet-stream", "4 KB", "Restricted", "Active", "Hôm qua, 00:05"],
    ["module-manifest.yaml", "text/yaml", "12 KB", "Internal", "Quarantine", "12/08/2026"],
  ];
  return (
    <>
      <PageTitle eyebrow="Object storage" title="Tệp tin" description="Quản lý metadata, trạng thái quét, phân loại dữ liệu và liên kết resource." action={<button className="primary-button">↑ Tải tệp lên</button>} />
      <section className="storage-card"><div><span className="storage-icon">▱</span><div><strong>84.2 GB <em>/ 250 GB</em></strong><p>Dung lượng object storage đã sử dụng</p></div></div><div className="storage-progress"><i /></div><div className="storage-stats"><span>24,903 tệp</span><span>12 đang staging</span><span>1 quarantine</span><span>Checkpoint 4 phút trước</span></div></section>
      <section className="panel table-panel"><div className="table-tools"><div className="search-field wide">⌕ <input aria-label="Tìm tệp" placeholder="Tìm tên tệp, media type..." /></div><button className="filter-chip active">Tất cả</button><button className="filter-chip">Cần xử lý</button></div><div className="file-table"><div className="file-row file-head"><span>Tệp</span><span>Kích thước</span><span>Phân loại</span><span>Trạng thái</span><span>Cập nhật</span></div>{fileRows.map((f) => <button className="file-row" key={f[0]}><span><b>▤</b><span><strong>{f[0]}</strong><small>{f[1]}</small></span></span><span>{f[2]}</span><span>{f[3]}</span><span><em className={`state ${f[4].toLowerCase()}`}>{f[4]}</em></span><span>{f[5]}　→</span></button>)}</div></section>
    </>
  );
}

function Settings() {
  return (
    <>
      <PageTitle eyebrow="Deployment configuration" title="Cấu hình" description="Thông tin môi trường và các chính sách vận hành đang có hiệu lực." action={<button className="primary-button">Lưu thay đổi</button>} />
      <div className="settings-layout"><aside className="settings-nav"><button className="active">Tổng quát</button><button>Bảo mật</button><button>Tenant</button><button>Retention</button><button>Thông báo</button><button>Integrations</button></aside><section className="panel settings-form"><h2>Thông tin deployment</h2><p>Những thay đổi quan trọng sẽ được audit và có thể yêu cầu khởi động lại.</p><label>Tên môi trường<input defaultValue="core-production-vn" /></label><div className="form-grid"><label>Service tier<select defaultValue="standard"><option value="pilot">Pilot</option><option value="standard">Standard</option><option value="critical">Critical</option></select></label><label>Khu vực<input defaultValue="Ho Chi Minh City" /></label></div><label>Public base URL<input defaultValue="https://core.example.vn" /></label><div className="setting-toggle"><div><strong>Chế độ bảo trì</strong><span>Chặn public write và tạm dừng worker/outbox relay.</span></div><button role="switch" aria-checked="false" className="toggle"><i /></button></div><div className="setting-toggle"><div><strong>Buộc MFA cho quản trị viên</strong><span>Đang áp dụng cho tất cả tài khoản có quyền quản trị.</span></div><button role="switch" aria-checked="true" className="toggle on"><i /></button></div></section></div>
    </>
  );
}

export default function Home() {
  const [authenticated, setAuthenticated] = useState(false);
  const [authReady, setAuthReady] = useState(false);
  const [view, setView] = useState<View>("overview");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [commandOpen, setCommandOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [logoutOpen, setLogoutOpen] = useState(false);
  const [apiOnline, setApiOnline] = useState(false);

  useEffect(() => {
    const token = window.localStorage.getItem("core-access-token") || window.sessionStorage.getItem("core-access-token");
    if (!token) { setAuthReady(true); return; }
    fetch(`${API_URL}/api/v1/auth/me`, { headers: { Authorization: `Bearer ${token}` } }).then((response) => {
      if (!response.ok) throw new Error(); setAuthenticated(true); setApiOnline(true);
    }).catch(() => { window.localStorage.removeItem("core-access-token"); window.sessionStorage.removeItem("core-access-token"); }).finally(() => setAuthReady(true));
  }, []);

  useEffect(() => {
    if (!authenticated) return;
    const token = window.localStorage.getItem("core-access-token") || window.sessionStorage.getItem("core-access-token");
    fetch(`${API_URL}/api/v1/control-plane/bootstrap`, { headers: { Authorization: `Bearer ${token}` } }).then(r => { if (!r.ok) throw new Error(); return r.json(); }).then(() => setApiOnline(true)).catch(() => setApiOnline(false));
  }, [authenticated]);

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") { event.preventDefault(); setCommandOpen(true); }
      if (event.key === "Escape") { setCommandOpen(false); setNotificationsOpen(false); setProfileOpen(false); setLogoutOpen(false); setSidebarOpen(false); }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  const currentLabel = useMemo(() => navItems.find((item) => item.id === view)?.label ?? "Tổng quan", [view]);
  const navigate = (next: View) => { setView(next); setSidebarOpen(false); window.scrollTo({ top: 0, behavior: "smooth" }); };
  const signIn = (token: string, remember: boolean) => {
    (remember ? window.localStorage : window.sessionStorage).setItem("core-access-token", token);
    setApiOnline(true); setAuthenticated(true);
  };
  const signOut = async () => {
    const token = window.localStorage.getItem("core-access-token") || window.sessionStorage.getItem("core-access-token");
    if (token) await fetch(`${API_URL}/api/v1/auth/logout`, { method: "POST", headers: { Authorization: `Bearer ${token}` } }).catch(() => undefined);
    window.localStorage.removeItem("core-access-token"); window.sessionStorage.removeItem("core-access-token");
    setAuthenticated(false); setLogoutOpen(false); setProfileOpen(false); setView("overview");
  };

  if (!authReady) return <div className="auth-loading" aria-label="Đang kiểm tra phiên đăng nhập"><span /></div>;
  if (!authenticated) return <LoginScreen onAuthenticated={signIn} />;

  return (
    <div className="app-shell">
      <aside className={`sidebar ${sidebarOpen ? "open" : ""}`}>
        <div className="brand"><div className="brand-mark"><i /><i /><i /><i /></div><div><strong>Core</strong><span>Platform</span></div></div>
        <div className="environment-switcher"><span>CP</span><div><strong>Core Production</strong><small>core-production-vn</small></div><button aria-label="Đổi môi trường">⌄</button></div>
        <nav aria-label="Điều hướng chính">
          <p>Workspace</p>
          {navItems.slice(0, 6).map((item) => <button key={item.id} className={view === item.id ? "active" : ""} onClick={() => navigate(item.id)}><span className="nav-icon">{item.icon}</span><span>{item.label}</span>{item.badge && <em>{item.badge}</em>}</button>)}
          <p>System</p>
          {navItems.slice(6).map((item) => <button key={item.id} className={view === item.id ? "active" : ""} onClick={() => navigate(item.id)}><span className="nav-icon">{item.icon}</span><span>{item.label}</span></button>)}
        </nav>
        <div className="sidebar-status"><div><StatusDot tone={apiOnline ? "teal" : "amber"} /><strong>{apiOnline ? "Backend connected" : "Backend unavailable"}</strong></div><span>Core v1.0.0-rc.4</span></div>
      </aside>
      {sidebarOpen && <button className="sidebar-scrim" aria-label="Đóng menu" onClick={() => setSidebarOpen(false)} />}

      <div className="main-area">
        <header className="topbar">
          <button className="mobile-menu" aria-label="Mở menu" onClick={() => setSidebarOpen(true)}>☰</button>
          <div className="breadcrumb"><span>Core Platform</span><b>/</b><strong>{currentLabel}</strong></div>
          <button className="command-trigger" onClick={() => setCommandOpen(true)}><span>⌕</span> Tìm kiếm hoặc chạy lệnh... <kbd>⌘ K</kbd></button>
          <div className="top-actions"><button aria-label="Trợ giúp">?</button><button aria-label="Thông báo" className="notification-button" onClick={() => { setNotificationsOpen(!notificationsOpen); setProfileOpen(false); }}>♢<i /></button><button className="profile-button" aria-expanded={profileOpen} onClick={() => { setProfileOpen(!profileOpen); setNotificationsOpen(false); }}><span>MN</span><div><strong>Minh Nguyễn</strong><small>Platform Admin</small></div><b>⌄</b></button></div>
          {notificationsOpen && <div className="notification-popover"><div><strong>Thông báo</strong><button onClick={() => setNotificationsOpen(false)}>×</button></div><article><span className="notice amber">!</span><p><strong>Outbox có 12 sự kiện đang chờ</strong><small>Consumer webhook chậm hơn bình thường.</small></p><time>2 phút</time></article><article><span className="notice teal">✓</span><p><strong>Audit checkpoint hoàn tất</strong><small>12.400 records đã được xác minh.</small></p><time>4 phút</time></article></div>}
          {profileOpen && <div className="profile-popover"><div className="profile-summary"><span>MN</span><p><strong>Minh Nguyễn</strong><small>admin@core.local</small></p></div><div className="profile-role"><span>Platform Administrator</span><em>Production</em></div><button onClick={() => { navigate("settings"); setProfileOpen(false); }}><span>⚙</span> Hồ sơ & bảo mật</button><button onClick={() => setLogoutOpen(true)} className="logout-action"><span>↪</span> Đăng xuất</button></div>}
        </header>

        <main>
          {view === "overview" && <Overview onNavigate={navigate} />}
          {view === "modules" && <Modules />}
          {view === "resources" && <Resources />}
          {view === "access" && <Access />}
          {view === "activity" && <Activity />}
          {view === "files" && <Files />}
          {view === "settings" && <Settings />}
        </main>
      </div>

      {commandOpen && <div className="modal-backdrop" role="presentation" onMouseDown={() => setCommandOpen(false)}><section className="command-modal" role="dialog" aria-modal="true" aria-label="Command palette" onMouseDown={(e) => e.stopPropagation()}><div className="command-input"><span>⌕</span><input autoFocus placeholder="Tìm resource, module hoặc lệnh..." /><kbd>ESC</kbd></div><p>Đi tới</p>{navItems.slice(0,6).map((item) => <button key={item.id} onClick={() => { navigate(item.id); setCommandOpen(false); }}><span>{item.icon}</span>{item.label}<kbd>→</kbd></button>)}</section></div>}
      {logoutOpen && <div className="modal-backdrop logout-backdrop" role="presentation" onMouseDown={() => setLogoutOpen(false)}><section className="logout-modal" role="dialog" aria-modal="true" aria-labelledby="logout-title" onMouseDown={(e) => e.stopPropagation()}><span className="logout-icon">↪</span><h2 id="logout-title">Đăng xuất khỏi Core Platform?</h2><p>Phiên làm việc hiện tại sẽ kết thúc. Bạn cần xác thực lại để tiếp tục truy cập.</p><div><button className="secondary-button" onClick={() => setLogoutOpen(false)}>Ở lại</button><button className="danger-button" onClick={signOut}>Đăng xuất</button></div></section></div>}
    </div>
  );
}
