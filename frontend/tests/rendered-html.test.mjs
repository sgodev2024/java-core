import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import test from "node:test";
import { fileURLToPath } from "node:url";

const PORT = 3311;
const ROOT = fileURLToPath(new URL("..", import.meta.url));
const BASE_URL = `http://127.0.0.1:${PORT}`;

async function waitForServer(attempts = 60) {
  for (let i = 0; i < attempts; i++) {
    try {
      const response = await fetch(BASE_URL, { headers: { accept: "text/html" } });
      if (response.status < 500) return;
    } catch {
      // server chưa sẵn sàng, thử lại
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`standalone server not ready at ${BASE_URL}`);
}

test("control plane console server-renders the login shell", async () => {
  const server = spawn(process.execPath, ["dist/standalone/server.js"], {
    cwd: ROOT,
    env: { ...process.env, PORT: String(PORT), HOST: "127.0.0.1" },
    stdio: "ignore",
  });
  try {
    await waitForServer();
    const response = await fetch(BASE_URL, { headers: { accept: "text/html" } });
    assert.equal(response.status, 200);
    assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

    const html = await response.text();
    assert.match(html, /<title>Core Platform — Control Plane<\/title>/i);
    assert.match(html, /<html lang="vi">/);
    assert.match(html, /Đang kiểm tra phiên đăng nhập/);
    assert.match(html, /auth-loading/);
    assert.doesNotMatch(html, /Your site is taking shape|react-loading-skeleton|codex-preview/i);
  } finally {
    server.kill();
  }
});
