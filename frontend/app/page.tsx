"use client";

import { useEffect, useMemo, useState } from "react";

type View = "business-home" | "approvals" | "overview" | "modules" | "resources" | "access" | "activity" | "files" | "settings";
type AuthStep = "login" | "mfa";
const API_URL = process.env.NEXT_PUBLIC_CORE_API_URL ?? "https://api.corejava.sgodata.com";
type UserInfo = { id: string; email: string; displayName: string; role: string };
type NavigationItem = { key:string; parentKey:string; ownerModule:string; label:string; labelKey:string; icon:string; type:"GROUP"|"PAGE"; viewKey:string; route:string; sortOrder:number; keywords:string[] };
type NavigationWorkspace = { key:string; label:string; labelKey:string; icon:string; category:"BUSINESS"|"ADMIN"; sortOrder:number; items:NavigationItem[] };
type NavigationModel = { revision:string; defaultWorkspaceKey:string; currentWorkspaceKey:string; workspaces:NavigationWorkspace[]; favoriteKeys:string[]; recentKeys:string[] };
type ModuleItem = { id: string; name: string; moduleKey: string; version: string; status: string; description: string; metric: string };
type ResourceItem = { id: string; name: string; storageMode: string; ownerModule: string; records: number; schemaVersion: string; updatedAt: string };
type ActivityItem = { id: string; kind: string; name: string; metadata: string; status: string; occurredAt: string };
type RoleItem = { id: string; name: string; users: number; policies: number; scope: string };
type FileItem = { id: string; name: string; mediaType: string; sizeBytes: number; classification: string; status: string; updatedAt: string };
type AuditItem = { id: string; actorEmail: string; action: string; resourceType?: string; resourceId?: string; result: string; correlationId: string; occurredAt: string };
type DynamicDefinition = { id:string; resourceKey:string; name:string; version:number; schema:{fields:Array<{key:string;type:string;required?:boolean}>}; status:string; updatedAt:string };
type BootstrapData = {
  summary: { resources: number; modules: number; pendingOutbox: number; runningJobs: number; files: number; storageGb: number; coreVersion: string; environment: string };
  modules: ModuleItem[]; resources: ResourceItem[]; activities: ActivityItem[]; roles: RoleItem[]; files: FileItem[]; audit: AuditItem[]; settings: Record<string,string>;
};

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
      const response = await fetch(`${API_URL}/api/v1/auth/login`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password, remember }) });
      const body = await response.json();
      if (!response.ok) throw new Error(body.detail ?? "Không thể đăng nhập.");
      if (body.mfaRequired === false && body.session?.accessToken) {
        onAuthenticated(body.session.accessToken, remember);
      } else if (body.challengeId) {
        setChallengeId(body.challengeId); setStep("mfa");
      } else {
        throw new Error("Phản hồi đăng nhập không hợp lệ.");
      }
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

function Overview({ onNavigate, data, displayName }: { onNavigate: (view: View) => void; data: BootstrapData; displayName?: string }) {
  const { summary, activities } = data;
  return (
    <>
      <PageTitle
        eyebrow="Core Control Plane"
        title={`Xin chào, ${displayName || "Platform Administrator"}`}
        description={`Hệ thống đang hoạt động. Có ${summary.pendingOutbox} sự kiện outbox và ${summary.runningJobs} background job đang xử lý.`}
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
          <strong className="metric-value">{summary.resources.toLocaleString("vi-VN")}</strong><span className="metric-trend positive">Live</span><small>dữ liệu từ PostgreSQL</small>
        </article>
        <article className="metric-card">
          <div className="metric-icon violet">◫</div><span className="metric-label">Modules hoạt động</span>
          <strong className="metric-value">{data.modules.filter(x => x.status !== "DISABLED").length} <em>/ {summary.modules}</em></strong><span className="metric-trend neutral">Đã đăng ký</span><small>module runtime</small>
        </article>
        <article className="metric-card">
          <div className="metric-icon amber">↯</div><span className="metric-label">Outbox đang chờ</span>
          <strong className="metric-value">{summary.pendingOutbox}</strong><span className="metric-trend warning">Pending</span><small>outbox chờ xử lý</small>
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
              <div className="activity-row" key={item.id}>
                <div className={`activity-icon ${item.kind.toLowerCase()}`}>{item.kind === "EVENT" ? "↯" : "◷"}</div>
                <div className="activity-main"><strong>{item.name}</strong><span>{item.metadata}</span></div>
                <span className={`state ${item.status.toLowerCase()}`}>{item.status}</span>
                <time>{new Date(item.occurredAt).toLocaleTimeString("vi-VN")}</time>
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

function Modules({ items, onStatus }: { items: ModuleItem[]; onStatus: (item: ModuleItem) => void }) {
  return (
    <>
      <PageTitle eyebrow="Runtime composition" title="Modules" description="Theo dõi capability, phiên bản và trạng thái của các module đã đóng gói trong deployment." action={<button className="secondary-button">Kiểm tra tương thích</button>} />
      <div className="filter-row"><div className="search-field">⌕ <input aria-label="Tìm module" placeholder="Tìm theo tên hoặc capability..." /></div><button className="filter-chip active">Tất cả 8</button><button className="filter-chip">Đang bật 7</button><button className="filter-chip">Cần chú ý 1</button></div>
      <section className="module-grid">
        {items.map((item) => <article className="module-card" key={item.id}>
          <div className="module-top"><div className="module-symbol">{item.name.slice(0, 2).toUpperCase()}</div><span className={`state ${item.status === "HEALTHY" ? "teal" : item.status === "ATTENTION" ? "amber" : "gray"}`}>{item.status}</span></div>
          <h3>{item.name}</h3><code>{item.moduleKey}</code><p>{item.description}</p>
          <div className="module-meta"><span>v{item.version}</span><span>{item.metric}</span></div>
          <button className="card-action" onClick={() => onStatus(item)}>{item.status === "DISABLED" ? "Bật module" : "Tắt module"} <span>→</span></button>
        </article>)}
      </section>
    </>
  );
}

function DynamicConsole({ onChanged }: { onChanged: () => Promise<void> }) {
  const [definitions,setDefinitions]=useState<DynamicDefinition[]>([]);const [selected,setSelected]=useState("");const [records,setRecords]=useState<Array<{id:string;data:Record<string,unknown>;version:number;updatedAt:string}>>([]);
  const [key,setKey]=useState("");const [name,setName]=useState("");const [classification,setClassification]=useState("INTERNAL");const [schema,setSchema]=useState('{"fields":[{"key":"name","type":"string","required":true}]}');const [recordJson,setRecordJson]=useState('{"name":"Bản ghi mới"}');const [message,setMessage]=useState("");
  const authHeaders=()=>({Authorization:`Bearer ${window.localStorage.getItem("core-access-token")||window.sessionStorage.getItem("core-access-token")||""}`});
  const loadDefinitions=async()=>{const r=await fetch(`${API_URL}/api/v1/dynamic/definitions`,{headers:authHeaders()});if(!r.ok)throw new Error("Không thể tải definitions");const body=await r.json();setDefinitions(body);if(!selected&&body.length)setSelected(body[0].resourceKey);};
  const loadRecords=async(resourceKey:string)=>{if(!resourceKey){setRecords([]);return;}const r=await fetch(`${API_URL}/api/v1/dynamic/${resourceKey}/records?page=0&size=50`,{headers:authHeaders()});if(!r.ok)throw new Error("Không thể tải records");setRecords((await r.json()).items);};
  useEffect(()=>{loadDefinitions().catch(e=>setMessage(e.message));},[]);
  useEffect(()=>{loadRecords(selected).catch(e=>setMessage(e.message));},[selected]);
  const createDefinition=async()=>{try{const r=await fetch(`${API_URL}/api/v1/dynamic/definitions`,{method:"POST",headers:{...authHeaders(),"Content-Type":"application/json"},body:JSON.stringify({resourceKey:key,name,schema:JSON.parse(schema),classification})});if(!r.ok)throw new Error((await r.json()).detail||"Tạo definition thất bại");setKey("");setName("");setMessage("Đã tạo definition");await loadDefinitions();await onChanged();}catch(e){setMessage(e instanceof Error?e.message:"Dữ liệu không hợp lệ");}};
  const createRecord=async()=>{try{const r=await fetch(`${API_URL}/api/v1/dynamic/${selected}/records`,{method:"POST",headers:{...authHeaders(),"Content-Type":"application/json"},body:JSON.stringify(JSON.parse(recordJson))});if(!r.ok)throw new Error((await r.json()).detail||"Tạo record thất bại");setMessage("Đã tạo record và revision");await loadRecords(selected);await onChanged();}catch(e){setMessage(e instanceof Error?e.message:"JSON không hợp lệ");}};
  const exportCsv=async()=>{const r=await fetch(`${API_URL}/api/v1/dynamic/${selected}/export.csv`,{headers:authHeaders()});if(!r.ok)return setMessage("Export thất bại");const url=URL.createObjectURL(await r.blob());const a=document.createElement("a");a.href=url;a.download=`${selected}.csv`;a.click();URL.revokeObjectURL(url);};
  const importCsv=async(file:File)=>{const form=new FormData();form.append("file",file);const r=await fetch(`${API_URL}/api/v1/dynamic/${selected}/import.csv`,{method:"POST",headers:authHeaders(),body:form});const body=await r.json();if(!r.ok)return setMessage(body.detail||"Import thất bại");setMessage(`Import thành công ${body.imported}, lỗi ${body.failed}`);await loadRecords(selected);await onChanged();};
  return <section className="panel settings-form"><div className="panel-header"><div><h2>Dynamic Resource Console</h2><p>Definition, schema validation, Generic CRUD, history và CSV.</p></div><span className="live-pill"><StatusDot /> API thật</span></div>
    {message&&<p className="auth-error" role="status">{message}</p>}
    <div className="form-grid"><label>Resource key<input value={key} onChange={e=>setKey(e.target.value)} placeholder="customer-profile" /></label><label>Tên definition<input value={name} onChange={e=>setName(e.target.value)} placeholder="Customer Profile" /></label><label>Phân loại dữ liệu<select value={classification} onChange={e=>setClassification(e.target.value)}><option value="INTERNAL">Internal</option><option value="CONFIDENTIAL">Confidential</option><option value="RESTRICTED">Restricted</option><option value="PUBLIC">Public</option><option value="">Chưa phân loại (cần duyệt)</option></select></label></div>
    <label>JSON Schema<textarea rows={4} value={schema} onChange={e=>setSchema(e.target.value)} /></label><button className="secondary-button" onClick={createDefinition}>Tạo definition</button>
    <hr/><label>Definition<select value={selected} onChange={e=>setSelected(e.target.value)}><option value="">Chọn definition</option>{definitions.map(d=><option key={d.id} value={d.resourceKey}>{d.name} · v{d.version}</option>)}</select></label>
    {selected&&<><label>Record JSON<textarea rows={4} value={recordJson} onChange={e=>setRecordJson(e.target.value)} /></label><div className="filter-row"><button className="primary-button" onClick={createRecord}>Tạo record</button><button className="secondary-button" onClick={exportCsv}>Export CSV</button><label className="secondary-button">Import CSV<input hidden type="file" accept=".csv,text/csv" onChange={e=>e.target.files?.[0]&&importCsv(e.target.files[0])}/></label></div>
    <div className="data-table"><div className="table-row table-head"><span>ID</span><span>Data</span><span>Version</span><span>Cập nhật</span><span></span><span></span></div>{records.map(r=><div className="table-row" key={r.id}><span><code>{r.id.slice(0,8)}</code></span><span><code>{JSON.stringify(r.data)}</code></span><span>v{r.version}</span><span>{new Date(r.updatedAt).toLocaleString("vi-VN")}</span><span></span><span></span></div>)}</div></>}
  </section>;
}

function Resources({ items, onChanged }: { items: ResourceItem[]; onChanged: () => Promise<void> }) {
  const [query, setQuery] = useState("");
  const filtered = items.filter((r) => `${r.name} ${r.ownerModule}`.toLowerCase().includes(query.toLowerCase()));
  return (
    <>
      <PageTitle eyebrow="Three-Plane Registry" title="Resources" description="Domain aggregate và Dynamic Resource cùng đăng ký capability nhưng giữ persistence độc lập." />
      <section className="panel table-panel">
        <div className="table-tools"><div className="search-field wide">⌕ <input value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Tìm resource" placeholder="Tìm resource, module..." /></div><button className="filter-chip active">Tất cả</button><button className="filter-chip">Domain</button><button className="filter-chip">Dynamic</button></div>
        <div className="data-table" role="table" aria-label="Danh sách resources">
          <div className="table-row table-head" role="row"><span>Tên resource</span><span>Storage mode</span><span>Owner module</span><span>Records</span><span>Schema</span><span>Cập nhật</span></div>
          {filtered.map((item) => <button className="table-row" role="row" key={item.name}>
            <span><b className="resource-glyph">◇</b><strong>{item.name}</strong></span><span><em className={`type-pill ${item.storageMode.toLowerCase()}`}>{item.storageMode}</em></span><span><code>{item.ownerModule}</code></span><span>{item.records.toLocaleString("vi-VN")}</span><span>{item.schemaVersion}</span><span>{new Date(item.updatedAt).toLocaleString("vi-VN")} <b>→</b></span>
          </button>)}
        </div>
      </section>
      <DynamicConsole onChanged={onChanged} />
    </>
  );
}

function Access({ items, onCreate }: { items: RoleItem[]; onCreate: () => void }) {
  return (
    <>
      <PageTitle eyebrow="Identity & authorization" title="Truy cập" description="Quản lý vai trò, policy và phạm vi truy cập. Mọi quyết định không xác định đều bị từ chối." action={<button className="primary-button" onClick={onCreate}>＋ Tạo vai trò</button>} />
      <section className="access-summary"><div><span>Người dùng hoạt động</span><strong>197</strong><small>+12 trong 30 ngày</small></div><div><span>Vai trò</span><strong>14</strong><small>4 vai trò hệ thống</small></div><div><span>Policies</span><strong>33</strong><small>100% đã biên dịch</small></div><div><span>Yêu cầu bị từ chối</span><strong>28</strong><small>24 giờ gần nhất</small></div></section>
      <div className="role-grid">{items.map((role, index) => <article className="panel role-card" key={role.id}><div className={`role-badge ${["violet","blue","amber","teal"][index%4]}`}>{role.name.split(" ").map(x => x[0]).join("").slice(0,2)}</div><div className="role-copy"><h3>{role.name}</h3><p>{role.scope}</p></div><button aria-label={`Mở ${role.name}`}>•••</button><dl><div><dt>Người dùng</dt><dd>{role.users}</dd></div><div><dt>Policies</dt><dd>{role.policies}</dd></div></dl></article>)}</div>
      <section className="panel policy-banner"><div className="policy-icon">✓</div><div><h3>Permission engine đang ở chế độ fail-closed</h3><p>Policy không tồn tại hoặc evaluation lỗi sẽ trả về Deny. Revision hiện tại: <code>perm-r1842</code></p></div><button className="secondary-button">Mở Policy Explorer</button></section>
    </>
  );
}

function Activity({ items }: { items: ActivityItem[] }) {
  return (
    <>
      <PageTitle eyebrow="Durable processing" title="Events & Jobs" description="Theo dõi outbox, consumer, background jobs, retry và dead-letter flow." action={<button className="secondary-button">↻ Làm mới</button>} />
      <section className="queue-grid"><div className="queue-card"><span>Outbox pending</span><strong>12</strong><div className="mini-bar"><i style={{width:"22%"}} /></div><small>Cũ nhất 48 giây</small></div><div className="queue-card"><span>Jobs running</span><strong>3</strong><div className="mini-bar blue"><i style={{width:"38%"}} /></div><small>7 workers sẵn sàng</small></div><div className="queue-card"><span>Retrying</span><strong>2</strong><div className="mini-bar amber"><i style={{width:"14%"}} /></div><small>Không có dead job</small></div><div className="queue-card"><span>Consumer lag</span><strong>0.8s</strong><div className="mini-bar violet"><i style={{width:"11%"}} /></div><small>Trong ngưỡng SLO</small></div></section>
      <section className="panel timeline-panel"><div className="panel-header"><div><h2>Live activity stream</h2><p>At-least-once delivery · idempotent consumers</p></div><span className="live-pill"><StatusDot /> Dữ liệu thật</span></div>{items.map((item) => <div className="activity-row expanded" key={item.id}><div className={`activity-icon ${item.kind.toLowerCase()}`}>{item.kind === "EVENT" ? "↯" : "◷"}</div><div className="activity-main"><strong>{item.name}</strong><span>{item.metadata}</span></div><span className={`state ${item.status.toLowerCase()}`}>{item.status}</span><time>{new Date(item.occurredAt).toLocaleString("vi-VN")}</time><button>Chi tiết</button></div>)}</section>
    </>
  );
}

function Files({ items, storageGb, onUpload, onDownload }: { items: FileItem[]; storageGb: number; onUpload:(file:File)=>Promise<void>; onDownload:(item:FileItem)=>Promise<void> }) {
  return (
    <>
      <PageTitle eyebrow="Object storage" title="Tệp tin" description="Quản lý metadata, checksum, phân loại và nội dung file theo tenant." action={<label className="primary-button">↑ Tải tệp lên<input hidden type="file" onChange={e=>e.target.files?.[0]&&onUpload(e.target.files[0])}/></label>} />
      <section className="storage-card"><div><span className="storage-icon">▱</span><div><strong>{storageGb.toFixed(2)} GB</strong><p>Dung lượng metadata file đã ghi nhận</p></div></div><div className="storage-progress"><i /></div><div className="storage-stats"><span>{items.length.toLocaleString("vi-VN")} tệp</span><span>{items.filter(x=>x.status === "QUARANTINE").length} quarantine</span><span>Dữ liệu từ PostgreSQL</span></div></section>
      <section className="panel table-panel"><div className="table-tools"><div className="search-field wide">⌕ <input aria-label="Tìm tệp" placeholder="Tìm tên tệp, media type..." /></div><button className="filter-chip active">Tất cả</button><button className="filter-chip">Cần xử lý</button></div><div className="file-table"><div className="file-row file-head"><span>Tệp</span><span>Kích thước</span><span>Phân loại</span><span>Trạng thái</span><span>Cập nhật</span></div>{items.map((f) => <button className="file-row" key={f.id} onClick={()=>onDownload(f)}><span><b>▤</b><span><strong>{f.name}</strong><small>{f.mediaType}</small></span></span><span>{(f.sizeBytes/1024/1024).toFixed(2)} MB</span><span>{f.classification}</span><span><em className={`state ${f.status.toLowerCase()}`}>{f.status}</em></span><span>{new Date(f.updatedAt).toLocaleString("vi-VN")}　↓</span></button>)}</div></section>
    </>
  );
}

function Settings({ values, onSave }: { values: Record<string,string>; onSave: (items: Array<{key:string;value:string}>) => Promise<void> }) {
  const [name,setName]=useState(values["environment.name"] ?? "core-production-vn");
  const [tier,setTier]=useState(values["environment.tier"] ?? "standard");
  const [region,setRegion]=useState(values["environment.region"] ?? "Ho Chi Minh City");
  const [url,setUrl]=useState(values["environment.publicUrl"] ?? "https://corejava.sgodata.com");
  return (
    <>
      <PageTitle eyebrow="Deployment configuration" title="Cấu hình" description="Thông tin môi trường và các chính sách vận hành đang có hiệu lực." action={<button className="primary-button" onClick={() => onSave([{key:"environment.name",value:name},{key:"environment.tier",value:tier},{key:"environment.region",value:region},{key:"environment.publicUrl",value:url}])}>Lưu thay đổi</button>} />
      <div className="settings-layout"><aside className="settings-nav"><button className="active">Tổng quát</button><button>Bảo mật</button><button>Tenant</button><button>Retention</button><button>Thông báo</button><button>Integrations</button></aside><section className="panel settings-form"><h2>Thông tin deployment</h2><p>Thay đổi được lưu vào PostgreSQL và ghi audit.</p><label>Tên môi trường<input value={name} onChange={e=>setName(e.target.value)} /></label><div className="form-grid"><label>Service tier<select value={tier} onChange={e=>setTier(e.target.value)}><option value="pilot">Pilot</option><option value="standard">Standard</option><option value="critical">Critical</option></select></label><label>Khu vực<input value={region} onChange={e=>setRegion(e.target.value)} /></label></div><label>Public base URL<input value={url} onChange={e=>setUrl(e.target.value)} /></label></section></div>
    </>
  );
}

function BusinessHome({ workspace, onOpen }: { workspace: NavigationWorkspace; onOpen: (item: NavigationItem) => void }) {
  const pages = workspace.items.filter(item => item.type === "PAGE" && item.viewKey !== "business-home");
  return <>
    <PageTitle eyebrow="Business Workspace" title="Không gian nghiệp vụ" description="Các phân hệ được tự động đăng ký từ module đang bật và chỉ hiển thị theo quyền của tài khoản." />
    <section className="business-hero panel"><div><span className="business-hero-icon">▦</span><p className="eyebrow">Workspace động</p><h2>Chọn phân hệ để bắt đầu</h2><p>Menu nghiệp vụ được tách khỏi Control Plane. Khi cài thêm module, chức năng được đưa vào đúng Workspace mà không sửa Core shell.</p></div><strong>{pages.length}<small>phân hệ được cấp quyền</small></strong></section>
    <section className="business-module-grid" aria-label="Phân hệ nghiệp vụ">
      {pages.map(item => <button key={item.key} className="business-module-card" onClick={() => onOpen(item)}><span>{item.icon}</span><div><small>{item.ownerModule}</small><strong>{item.label}</strong><p>{item.keywords.slice(0,3).join(" · ")}</p></div><b>→</b></button>)}
      {pages.length === 0 && <div className="empty-workspace"><span>◇</span><h2>Chưa có phân hệ được cấp quyền</h2><p>Liên hệ quản trị viên để bật module hoặc gán policy phù hợp.</p></div>}
    </section>
  </>;
}

type ApprovalItem = { id:string; title:string; description:string; status:string; priority:string; amount?:number; version:number; updatedAt:string };
function ApprovalWorkspace() {
  const [items,setItems]=useState<ApprovalItem[]>([]);const [status,setStatus]=useState("");const [query,setQuery]=useState("");
  const [title,setTitle]=useState("");const [priority,setPriority]=useState("MEDIUM");const [message,setMessage]=useState("");const [loading,setLoading]=useState(false);
  const authHeaders=()=>({Authorization:`Bearer ${window.localStorage.getItem("core-access-token")||window.sessionStorage.getItem("core-access-token")||""}`});
  const load=async()=>{setLoading(true);try{const params=new URLSearchParams();if(status)params.set("status",status);if(query)params.set("q",query);const r=await fetch(`${API_URL}/api/v1/approvals?${params}`,{headers:authHeaders()});if(!r.ok)throw new Error((await r.json().catch(()=>({}))).detail||"Không thể tải đề nghị");setItems(await r.json());setMessage("");}catch(e){setMessage(e instanceof Error?e.message:"Không thể tải dữ liệu");}finally{setLoading(false);}};
  useEffect(()=>{load();},[status]);
  const create=async()=>{if(!title.trim())return setMessage("Vui lòng nhập tiêu đề đề nghị.");const r=await fetch(`${API_URL}/api/v1/approvals`,{method:"POST",headers:{...authHeaders(),"Content-Type":"application/json"},body:JSON.stringify({title:title.trim(),description:"",priority})});if(!r.ok)return setMessage((await r.json().catch(()=>({}))).detail||"Không thể tạo đề nghị");setTitle("");setMessage("Đã tạo đề nghị mới.");await load();};
  const transition=async(item:ApprovalItem,action:"submit"|"approve"|"reject"|"cancel")=>{const note=action==="approve"||action==="reject"?window.prompt("Nhập nội dung quyết định")||"":"";if((action==="approve"||action==="reject")&&!note)return;const r=await fetch(`${API_URL}/api/v1/approvals/${item.id}/${action}`,{method:"POST",headers:{...authHeaders(),"Content-Type":"application/json"},body:JSON.stringify(action==="approve"||action==="reject"?{note}:{})});if(!r.ok)return setMessage((await r.json().catch(()=>({}))).detail||"Thao tác thất bại");setMessage("Đã cập nhật trạng thái đề nghị.");await load();};
  return <>
    <PageTitle eyebrow="Approval Domain" title="Đề nghị phê duyệt" description="Module nghiệp vụ code-first với state machine, permission, audit và transactional outbox." />
    <section className="panel approval-create"><div><h2>Tạo đề nghị</h2><p>Dữ liệu được kiểm tra theo domain invariant trước khi ghi.</p></div><input aria-label="Tiêu đề đề nghị" value={title} onChange={e=>setTitle(e.target.value)} placeholder="Ví dụ: Phê duyệt mua thiết bị" /><select value={priority} onChange={e=>setPriority(e.target.value)} aria-label="Mức ưu tiên"><option value="LOW">Thấp</option><option value="MEDIUM">Trung bình</option><option value="HIGH">Cao</option><option value="URGENT">Khẩn cấp</option></select><button className="primary-button" onClick={create}>＋ Tạo đề nghị</button></section>
    <div className="filter-row"><div className="search-field"><span>⌕</span><input value={query} onChange={e=>setQuery(e.target.value)} onKeyDown={e=>e.key==="Enter"&&load()} placeholder="Tìm đề nghị..." /></div>{["","DRAFT","SUBMITTED","APPROVED","REJECTED"].map(value=><button key={value||"all"} className={`filter-chip ${status===value?"active":""}`} onClick={()=>setStatus(value)}>{value||"Tất cả"}</button>)}</div>
    {message&&<p className="operation-message">{message}</p>}
    <section className="panel approval-table"><div className="approval-row approval-head"><span>Đề nghị</span><span>Ưu tiên</span><span>Trạng thái</span><span>Cập nhật</span><span>Thao tác</span></div>{loading?<div className="empty-workspace"><p>Đang tải...</p></div>:items.map(item=><div className="approval-row" key={item.id}><span><strong>{item.title}</strong><small>v{item.version} · {item.id.slice(0,8)}</small></span><span><em className={`priority ${item.priority.toLowerCase()}`}>{item.priority}</em></span><span><em className={`state ${item.status.toLowerCase()}`}>{item.status}</em></span><time>{new Date(item.updatedAt).toLocaleString("vi-VN")}</time><span className="approval-actions">{item.status==="DRAFT"&&<button onClick={()=>transition(item,"submit")}>Gửi duyệt</button>}{item.status==="SUBMITTED"&&<><button onClick={()=>transition(item,"approve")}>Duyệt</button><button onClick={()=>transition(item,"reject")}>Từ chối</button></>}</span></div>)}{!loading&&items.length===0&&<div className="empty-workspace"><span>✓</span><h2>Chưa có đề nghị</h2><p>Tạo đề nghị đầu tiên trong biểu mẫu phía trên.</p></div>}</section>
  </>;
}

export default function Home() {
  const [authenticated,setAuthenticated]=useState(false);const [authReady,setAuthReady]=useState(false);const [user,setUser]=useState<UserInfo|null>(null);
  const [navigation,setNavigation]=useState<NavigationModel|null>(null);const [workspaceKey,setWorkspaceKey]=useState("");const [expandedGroup,setExpandedGroup]=useState("");const [view,setView]=useState<View>("business-home");
  const [sidebarOpen,setSidebarOpen]=useState(false);const [commandOpen,setCommandOpen]=useState(false);const [commandQuery,setCommandQuery]=useState("");const [notificationsOpen,setNotificationsOpen]=useState(false);const [profileOpen,setProfileOpen]=useState(false);const [logoutOpen,setLogoutOpen]=useState(false);
  const [apiOnline,setApiOnline]=useState(false);const [data,setData]=useState<BootstrapData|null>(null);const [operationError,setOperationError]=useState("");
  const token=()=>window.localStorage.getItem("core-access-token")||window.sessionStorage.getItem("core-access-token")||"";
  const authHeaders=()=>({Authorization:`Bearer ${token()}`});

  useEffect(()=>{const existing=token();if(!existing){setAuthReady(true);return;}fetch(`${API_URL}/api/v1/auth/me`,{headers:{Authorization:`Bearer ${existing}`}}).then(async r=>{if(!r.ok)throw new Error();setUser(await r.json());setAuthenticated(true);setApiOnline(true);}).catch(()=>{window.localStorage.removeItem("core-access-token");window.sessionStorage.removeItem("core-access-token");}).finally(()=>setAuthReady(true));},[]);

  const selectInitialNavigation=(model:NavigationModel)=>{const pages=model.workspaces.flatMap(workspace=>workspace.items.filter(item=>item.type==="PAGE").map(item=>({workspace,item})));const hashRoute=`/${window.location.hash}`;const fromRoute=pages.find(entry=>entry.item.route===hashRoute);const workspace=model.workspaces.find(item=>item.key===(fromRoute?.workspace.key||model.currentWorkspaceKey||model.defaultWorkspaceKey))||model.workspaces[0];const item=fromRoute?.item||workspace?.items.find(entry=>entry.type==="PAGE");if(workspace)setWorkspaceKey(workspace.key);if(item){setView(item.viewKey as View);setExpandedGroup(item.parentKey);}};
  const loadNavigation=async()=>{const response=await fetch(`${API_URL}/api/v1/navigation/me`,{headers:authHeaders()});if(!response.ok)throw new Error("Không thể tải Navigation Registry");const model:NavigationModel=await response.json();setNavigation(model);selectInitialNavigation(model);return model;};
  const refresh=async()=>{const response=await fetch(`${API_URL}/api/v1/control-plane/bootstrap`,{headers:authHeaders()});if(!response.ok)throw new Error("Không thể tải dữ liệu Control Plane");setData(await response.json());};

  useEffect(()=>{if(!authenticated)return;(async()=>{try{const [me,model]=await Promise.all([fetch(`${API_URL}/api/v1/auth/me`,{headers:authHeaders()}).then(r=>{if(!r.ok)throw new Error();return r.json();}),loadNavigation()]);setUser(me);if(model.workspaces.some(workspace=>workspace.category==="ADMIN"))await refresh();setApiOnline(true);}catch{setApiOnline(false);setOperationError("Không thể khởi tạo Workspace. Vui lòng đăng nhập lại hoặc kiểm tra backend.");}})();},[authenticated]);
  useEffect(()=>{const handler=(event:KeyboardEvent)=>{if((event.ctrlKey||event.metaKey)&&event.key.toLowerCase()==="k"){event.preventDefault();setCommandOpen(true);}if(event.key==="Escape"){setCommandOpen(false);setNotificationsOpen(false);setProfileOpen(false);setLogoutOpen(false);setSidebarOpen(false);}};window.addEventListener("keydown",handler);return()=>window.removeEventListener("keydown",handler);},[]);

  const workspaces=navigation?.workspaces??[];const allPages=useMemo(()=>workspaces.flatMap(workspace=>workspace.items.filter(item=>item.type==="PAGE").map(item=>({workspace,item}))),[workspaces]);
  const activeWorkspace=workspaces.find(item=>item.key===workspaceKey)||workspaces[0];const currentEntry=allPages.find(entry=>entry.item.viewKey===view);const currentLabel=currentEntry?.item.label??"Workspace";
  const favoriteEntries=(navigation?.favoriteKeys??[]).map(key=>allPages.find(entry=>entry.item.key===key)).filter((entry):entry is {workspace:NavigationWorkspace;item:NavigationItem}=>Boolean(entry));
  const commandEntries=allPages.filter(entry=>{const text=`${entry.item.label} ${entry.item.keywords.join(" ")} ${entry.workspace.label}`.toLowerCase();return text.includes(commandQuery.toLowerCase());}).sort((a,b)=>{const recent=navigation?.recentKeys??[];const ai=recent.indexOf(a.item.key),bi=recent.indexOf(b.item.key);return(ai<0?999:ai)-(bi<0?999:bi)||a.item.sortOrder-b.item.sortOrder;});

  const savePreferences=async(favoriteKeys:string[],recentKeys:string[],lastWorkspaceKey:string)=>{const response=await fetch(`${API_URL}/api/v1/navigation/me/preferences`,{method:"PUT",headers:{...authHeaders(),"Content-Type":"application/json"},body:JSON.stringify({favoriteKeys,recentKeys,lastWorkspaceKey})});if(response.ok)setNavigation(await response.json());};
  const openItem=(item:NavigationItem)=>{if(item.type!=="PAGE")return;const owner=workspaces.find(workspace=>workspace.items.some(candidate=>candidate.key===item.key));if(owner)setWorkspaceKey(owner.key);setView(item.viewKey as View);setExpandedGroup(item.parentKey);setSidebarOpen(false);setCommandOpen(false);setCommandQuery("");window.history.replaceState(null,"",item.route);window.scrollTo({top:0,behavior:"smooth"});if(navigation){const recent=[item.key,...navigation.recentKeys.filter(key=>key!==item.key)].slice(0,10);void savePreferences(navigation.favoriteKeys,recent,owner?.key||workspaceKey);}};
  const navigate=(next:View)=>{const entry=allPages.find(candidate=>candidate.item.viewKey===next);if(entry)openItem(entry.item);};
  const changeWorkspace=(key:string)=>{const workspace=workspaces.find(item=>item.key===key);const preferred=(navigation?.recentKeys??[]).map(recent=>workspace?.items.find(item=>item.key===recent&&item.type==="PAGE")).find(Boolean);const first=preferred||workspace?.items.find(item=>item.type==="PAGE");if(first)openItem(first);else setWorkspaceKey(key);};
  const toggleFavorite=(item:NavigationItem)=>{if(!navigation)return;const favoriteKeys=navigation.favoriteKeys.includes(item.key)?navigation.favoriteKeys.filter(key=>key!==item.key):[...navigation.favoriteKeys,item.key].slice(0,20);void savePreferences(favoriteKeys,navigation.recentKeys,workspaceKey);};
  const mutate=async(path:string,method:string,body?:unknown)=>{setOperationError("");const response=await fetch(`${API_URL}${path}`,{method,headers:{...authHeaders(),"Content-Type":"application/json"},body:body===undefined?undefined:JSON.stringify(body)});if(!response.ok){const problem=await response.json().catch(()=>({}));const message=problem.detail||"Thao tác thất bại";setOperationError(message);throw new Error(message);}await refresh();};
  const changeModuleStatus=async(item:ModuleItem)=>{try{await mutate(`/api/v1/control-plane/modules/${item.id}/status`,"PATCH",{status:item.status==="DISABLED"?"HEALTHY":"DISABLED"});await loadNavigation();}catch{}};
  const createRole=()=>{const name=window.prompt("Tên vai trò");if(name)mutate("/api/v1/control-plane/roles","POST",{name,scope:"Toàn deployment"}).catch(()=>undefined);};
  const uploadFile=async(file:File)=>{setOperationError("");const form=new FormData();form.append("file",file);const response=await fetch(`${API_URL}/api/v1/files?classification=INTERNAL`,{method:"POST",headers:authHeaders(),body:form});if(!response.ok){const p=await response.json().catch(()=>({}));setOperationError(p.detail||"Upload thất bại");return;}await refresh();};
  const downloadFile=async(item:FileItem)=>{const response=await fetch(`${API_URL}/api/v1/files/${item.id}/content`,{headers:authHeaders()});if(!response.ok){const p=await response.json().catch(()=>({}));setOperationError(p.detail||"Nội dung file chưa sẵn sàng");return;}const url=URL.createObjectURL(await response.blob());const a=document.createElement("a");a.href=url;a.download=item.name;a.click();URL.revokeObjectURL(url);};
  const signIn=(accessToken:string,remember:boolean)=>{(remember?window.localStorage:window.sessionStorage).setItem("core-access-token",accessToken);setNavigation(null);setApiOnline(true);setAuthenticated(true);};
  const signOut=async()=>{const accessToken=token();if(accessToken)await fetch(`${API_URL}/api/v1/auth/logout`,{method:"POST",headers:{Authorization:`Bearer ${accessToken}`}}).catch(()=>undefined);window.localStorage.removeItem("core-access-token");window.sessionStorage.removeItem("core-access-token");setAuthenticated(false);setNavigation(null);setData(null);setUser(null);setLogoutOpen(false);setProfileOpen(false);setView("business-home");};
  const initials=(user?.displayName||user?.email||"CP").split(/\s+/).map(part=>part[0]).join("").slice(0,2).toUpperCase();const settingsItem=allPages.find(entry=>entry.item.viewKey==="settings")?.item;
  const renderPageButton=(item:NavigationItem,compact=false)=><div className={`nav-entry ${compact?"compact":""}`} key={item.key}><button className={view===item.viewKey?"active":""} onClick={()=>openItem(item)}><span className="nav-icon">{item.icon}</span><span>{item.label}</span></button><button className={`favorite-toggle ${navigation?.favoriteKeys.includes(item.key)?"selected":""}`} aria-label={`${navigation?.favoriteKeys.includes(item.key)?"Bỏ":"Thêm"} yêu thích ${item.label}`} onClick={()=>toggleFavorite(item)}>☆</button></div>;

  if(!authReady)return <div className="auth-loading" aria-label="Đang kiểm tra phiên đăng nhập"><span/></div>;
  if(!authenticated)return <LoginScreen onAuthenticated={signIn}/>;
  if(!navigation)return <div className="auth-loading" aria-label="Đang tải Navigation Registry"><span/></div>;

  const topLevel=activeWorkspace?.items.filter(item=>!item.parentKey)??[];
  return <div className="app-shell">
    <aside className={`sidebar ${sidebarOpen?"open":""}`}>
      <div className="brand"><div className="brand-mark"><i/><i/><i/><i/></div><div><strong>Core</strong><span>Platform</span></div></div>
      <label className="workspace-switcher"><span>{activeWorkspace?.icon||"▦"}</span><div><small>Workspace</small><select value={activeWorkspace?.key||""} onChange={event=>changeWorkspace(event.target.value)} aria-label="Chọn Workspace">{workspaces.map(workspace=><option key={workspace.key} value={workspace.key}>{workspace.label}</option>)}</select></div><b>⌄</b></label>
      <nav aria-label="Điều hướng Workspace">
        {favoriteEntries.length>0&&<><p>Yêu thích</p>{favoriteEntries.slice(0,5).map(entry=>renderPageButton(entry.item,true))}</>}
        <p>{activeWorkspace?.category==="ADMIN"?"Quản trị hệ thống":"Phân hệ nghiệp vụ"}</p>
        {topLevel.map(item=>item.type==="GROUP"?<div className={`nav-group ${expandedGroup===item.key?"open":""}`} key={item.key}><button className="nav-group-trigger" onClick={()=>setExpandedGroup(expandedGroup===item.key?"":item.key)}><span className="nav-icon">{item.icon}</span><span>{item.label}</span><b>⌄</b></button>{expandedGroup===item.key&&<div className="nav-children">{activeWorkspace?.items.filter(child=>child.parentKey===item.key&&child.type==="PAGE").map(child=>renderPageButton(child))}</div>}</div>:renderPageButton(item))}
      </nav>
      <div className="sidebar-status"><div><StatusDot tone={apiOnline?"teal":"amber"}/><strong>{apiOnline?"Backend connected":"Backend unavailable"}</strong></div><span>Navigation {navigation.revision}</span></div>
    </aside>
    {sidebarOpen&&<button className="sidebar-scrim" aria-label="Đóng menu" onClick={()=>setSidebarOpen(false)}/>}
    <div className="main-area"><header className="topbar"><button className="mobile-menu" aria-label="Mở menu" onClick={()=>setSidebarOpen(true)}>☰</button><div className="breadcrumb"><span>{activeWorkspace?.label||"Workspace"}</span><b>/</b><strong>{currentLabel}</strong></div><button className="command-trigger" onClick={()=>setCommandOpen(true)}><span>⌕</span> Tìm module hoặc chức năng... <kbd>⌘ K</kbd></button><div className="top-actions"><button aria-label="Trợ giúp">?</button><button aria-label="Thông báo" className="notification-button" onClick={()=>{setNotificationsOpen(!notificationsOpen);setProfileOpen(false);}}>♢<i/></button><button className="profile-button" aria-expanded={profileOpen} onClick={()=>{setProfileOpen(!profileOpen);setNotificationsOpen(false);}}><span>{initials}</span><div><strong>{user?.displayName||"Người dùng"}</strong><small>{user?.role==="PLATFORM_ADMIN"?"Platform Admin":"Application User"}</small></div><b>⌄</b></button></div>
      {notificationsOpen&&<div className="notification-popover"><div><strong>Thông báo</strong><button onClick={()=>setNotificationsOpen(false)}>×</button></div><article><span className="notice teal">✓</span><p><strong>Navigation Registry đã đồng bộ</strong><small>Menu được lọc theo module và quyền hiện tại.</small></p><time>Live</time></article></div>}
      {profileOpen&&<div className="profile-popover"><div className="profile-summary"><span>{initials}</span><p><strong>{user?.displayName||"Người dùng"}</strong><small>{user?.email}</small></p></div><div className="profile-role"><span>{user?.role}</span><em>{activeWorkspace?.label}</em></div>{settingsItem&&<button onClick={()=>{openItem(settingsItem);setProfileOpen(false);}}><span>⚙</span> Hồ sơ & bảo mật</button>}<button onClick={()=>setLogoutOpen(true)} className="logout-action"><span>↪</span> Đăng xuất</button></div>}
    </header><main>{operationError&&<p className="auth-error" role="alert">{operationError}</p>}{view==="business-home"&&activeWorkspace&&<BusinessHome workspace={activeWorkspace} onOpen={openItem}/>} {view==="approvals"&&<ApprovalWorkspace/>}{data&&view==="overview"&&<Overview onNavigate={navigate} data={data} displayName={user?.displayName}/>} {data&&view==="modules"&&<Modules items={data.modules} onStatus={changeModuleStatus}/>} {data&&view==="resources"&&<Resources items={data.resources} onChanged={refresh}/>} {data&&view==="access"&&<Access items={data.roles} onCreate={createRole}/>} {data&&view==="activity"&&<Activity items={data.activities}/>} {data&&view==="files"&&<Files items={data.files} storageGb={data.summary.storageGb} onUpload={uploadFile} onDownload={downloadFile}/>} {data&&view==="settings"&&<Settings values={data.settings} onSave={items=>mutate("/api/v1/control-plane/settings","PUT",items)}/>} {activeWorkspace?.category==="ADMIN"&&!data&&<div className="auth-loading" aria-label="Đang tải Control Plane"><span/></div>}</main></div>
    {commandOpen&&<div className="modal-backdrop" role="presentation" onMouseDown={()=>setCommandOpen(false)}><section className="command-modal" role="dialog" aria-modal="true" aria-label="Command palette" onMouseDown={event=>event.stopPropagation()}><div className="command-input"><span>⌕</span><input autoFocus value={commandQuery} onChange={event=>setCommandQuery(event.target.value)} placeholder="Tìm Workspace, module hoặc chức năng..."/><kbd>ESC</kbd></div><p>{commandQuery?"Kết quả":"Gần đây và chức năng được cấp quyền"}</p>{commandEntries.slice(0,12).map(entry=><button key={entry.item.key} onClick={()=>openItem(entry.item)}><span>{entry.item.icon}</span><div><strong>{entry.item.label}</strong><small>{entry.workspace.label} · {entry.item.ownerModule}</small></div><kbd>→</kbd></button>)}{commandEntries.length===0&&<div className="command-empty">Không tìm thấy chức năng phù hợp với quyền hiện tại.</div>}</section></div>}
    {logoutOpen&&<div className="modal-backdrop logout-backdrop" role="presentation" onMouseDown={()=>setLogoutOpen(false)}><section className="logout-modal" role="dialog" aria-modal="true" aria-labelledby="logout-title" onMouseDown={event=>event.stopPropagation()}><span className="logout-icon">↪</span><h2 id="logout-title">Đăng xuất khỏi Core Platform?</h2><p>Phiên làm việc hiện tại sẽ kết thúc. Bạn cần xác thực lại để tiếp tục truy cập.</p><div><button className="secondary-button" onClick={()=>setLogoutOpen(false)}>Ở lại</button><button className="danger-button" onClick={signOut}>Đăng xuất</button></div></section></div>}
  </div>;
}
