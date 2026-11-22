/* ==========================================================
 * LOGIC TRANG QUẢN TRỊ
 * ========================================================== */
(() => {
  const $ = (s) => document.querySelector(s);
  const $$ = (s) => document.querySelectorAll(s);

  const TITLES = { dashboard: "Tổng quan", users: "Người dùng", logs: "Nhật ký request" };
  const state = { view: "users", users: [], logs: [], logTimer: null };

  /* ---------- Tiện ích ---------- */
  // Tạo phần tử DOM an toàn (không dùng innerHTML với dữ liệu từ server)
  const h = (tag, attrs = {}, ...kids) => {
    const el = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs)) {
      if (k === "class") el.className = v;
      else if (k.startsWith("on")) el.addEventListener(k.slice(2), v);
      else if (v !== false && v != null) el.setAttribute(k, v);
    }
    kids.flat().forEach((c) => el.append(c && c.nodeType ? c : document.createTextNode(c ?? "")));
    return el;
  };

  const COLORS = [
    ["#4f7fa8", "#6c9cc4"], ["#4e9488", "#6fb3a6"], ["#b07a5e", "#cf9a7e"],
    ["#8a73b0", "#a893c9"], ["#6c8f4e", "#8aae6d"], ["#b9677a", "#d4889a"],
  ];
  const colorOf = (key) => {
    let x = 0;
    for (const c of String(key)) x = (x * 31 + c.charCodeAt(0)) >>> 0;
    const [a, b] = COLORS[x % COLORS.length];
    return `linear-gradient(135deg, ${a}, ${b})`;
  };
  const initial = (n) => (n || "?").trim().split(/\s+/).pop().charAt(0);
  const avatar = (u) => {
    const el = h("div", { class: "avatar sm" }, initial(u.displayName));
    el.style.background = colorOf(u.username || u.id);
    return el;
  };
  const fmtDT = (iso) => (iso ? new Date(iso).toLocaleString("vi-VN") : "–");
  const emptyRow = (cols, text) => h("tr", {}, h("td", { class: "empty-row", colspan: cols }, text));

  function toast(msg, type = "") {
    const el = h("div", { class: "toast " + type }, msg);
    $("#toasts").appendChild(el);
    setTimeout(() => el.remove(), 3700);
  }

  /* ---------- Đăng nhập quản trị ---------- */
  function showLogin() {
    clearInterval(state.logTimer);
    $("#adminApp").classList.add("hidden");
    $("#adminAuth").classList.remove("hidden");
  }

  function showAdmin() {
    const me = Api.admin.getUser();
    $("#adminAuth").classList.add("hidden");
    $("#adminApp").classList.remove("hidden");
    $("#adminName").textContent = me.displayName;
    $("#adminAvatar").textContent = initial(me.displayName);
    $("#adminAvatar").style.background = colorOf(me.username);
    switchView("users");
  }

  function authError(msg) {
    const el = $("#adminAuthError");
    el.textContent = msg;
    el.classList.remove("shake");
    void el.offsetWidth;
    el.classList.add("shake");
  }

  $("#adminLoginForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    $("#adminAuthError").textContent = "";
    const btn = e.target.querySelector(".btn");
    btn.classList.add("loading");
    try {
      await Api.admin.login($("#adminUser").value.trim(), $("#adminPass").value);
      showAdmin();
      toast("Đăng nhập quản trị thành công", "ok");
    } catch (err) {
      authError(err.message);
    } finally {
      btn.classList.remove("loading");
    }
  });

  /* ---------- Hộp xác nhận đăng xuất (hiện giữa màn hình) ---------- */
  function confirmLogout(onYes) {
    if (document.querySelector(".confirm-overlay")) return;
    const close = () => { document.removeEventListener("keydown", onKey, true); ov.remove(); };
    const onKey = (e) => { if (e.key === "Escape") { e.stopPropagation(); close(); } };
    const no = document.createElement("button");
    no.type = "button"; no.textContent = "Ở lại"; no.addEventListener("click", close);
    const yes = document.createElement("button");
    yes.type = "button"; yes.className = "danger"; yes.textContent = "Đăng xuất";
    yes.addEventListener("click", () => { close(); onYes(); });
    const box = document.createElement("div");
    box.className = "confirm-box";
    box.setAttribute("role", "alertdialog");
    box.innerHTML = '<div class="confirm-ico">👋</div><h3>Bạn có chắc chắn muốn đăng xuất không?</h3><p>Bạn sẽ cần đăng nhập lại để tiếp tục sử dụng.</p>';
    const act = document.createElement("div");
    act.className = "confirm-actions";
    act.append(no, yes);
    box.append(act);
    const ov = document.createElement("div");
    ov.className = "confirm-overlay";
    ov.append(box);
    ov.addEventListener("click", (e) => { if (e.target === ov) close(); });
    document.addEventListener("keydown", onKey, true);
    document.body.append(ov);
    no.focus();
  }

  function logout() {
    Api.admin.clear();
    showLogin();
  }
  $("#adminLogout").addEventListener("click", () => confirmLogout(() => { logout(); toast("Đã đăng xuất"); }));
  window.addEventListener("session-expired", (e) => {
    if (e.detail !== "admin") return;
    logout();
    toast("Phiên quản trị đã hết hạn, hãy đăng nhập lại", "error");
  });

  /* ---------- Chuyển tab ---------- */
  function switchView(view) {
    state.view = view;
    $$(".nav-btn").forEach((b) => b.classList.toggle("active", b.dataset.view === view));
    $$(".view").forEach((v) => v.classList.toggle("hidden", v.id !== "view-" + view));
    $("#viewTitle").textContent = TITLES[view];
    clearInterval(state.logTimer);
    loadView();
  }
  $$(".nav-btn").forEach((b) => b.addEventListener("click", () => switchView(b.dataset.view)));
  $("#adminRefresh").addEventListener("click", () => { loadView(); toast("Đã tải lại"); });

  function loadView() {
    ({ dashboard: loadDashboard, users: loadUsers, logs: loadLogs })[state.view]();
  }

  // Bắt lỗi chung khi gọi API
  const guard = async (fn) => {
    try { return await fn(); } catch (err) { toast(err.message, "error"); return null; }
  };

  /* ---------- Tổng quan ---------- */
  async function loadDashboard() {
    const s = await guard(() => Api.admin.stats());
    if (!s) return;
    $("#stUsers").textContent = s.totalUsers;
    $("#stLocked").textContent = s.lockedUsers;
    $("#stMessages").textContent = s.totalMessages;
    $("#stImages").textContent = s.totalImages;
    $("#stVideos").textContent = s.totalVideos;

    const total = s.totalMessages || 1;
    const text = Math.max(0, s.totalMessages - s.totalImages - s.totalVideos);
    const pct = (n) => ((n / total) * 100).toFixed(1) + "%";
    $("#ratioText").style.width = pct(text);
    $("#ratioImg").style.width = pct(s.totalImages);
    $("#ratioVid").style.width = pct(s.totalVideos);
  }

  /* ---------- Người dùng ---------- */
  async function loadUsers() {
    const list = await guard(() => Api.admin.users());
    if (!list) return;
    state.users = list;
    renderUsers();
  }

  function renderUsers() {
    const q = $("#userSearch").value.trim().toLowerCase();
    const list = state.users.filter((u) => u.displayName.toLowerCase().includes(q) || u.username.toLowerCase().includes(q));
    const body = $("#usersBody");
    body.innerHTML = "";
    if (!list.length) return body.appendChild(emptyRow(4, "Không có người dùng nào"));

    list.forEach((u) => {
      const role = u.role === "ADMIN" ? h("span", { class: "badge info" }, "Quản trị viên") : h("span", { class: "badge ok" }, "Người dùng");
      body.appendChild(
        h("tr", {},
          h("td", {}, h("div", { class: "user-cell" }, avatar(u), h("strong", {}, "@" + u.username))),
          h("td", { class: "muted" }, u.email || "–"),
          h("td", {}, role),
          h("td", {}, h("div", { class: "row-actions" },
            h("button", { class: "btn-sm", type: "button", onclick: () => changeRole(u) }, u.role === "ADMIN" ? "⬇ Hạ xuống Người dùng" : "⬆ Nâng lên Admin"),
            h("button", { class: "btn-sm danger", type: "button", onclick: () => removeUser(u) }, "🗑 Xóa")
          ))
        )
      );
    });
  }
  $("#userSearch").addEventListener("input", renderUsers);

  async function changeRole(u) {
    const next = u.role === "ADMIN" ? "USER" : "ADMIN";
    if (!confirm(`Đổi quyền của "${u.username}" thành ${next === "ADMIN" ? "Quản trị viên" : "Người dùng"}?`)) return;
    const ok = await guard(async () => { await Api.admin.setRole(u.id, next); return true; });
    if (ok) { toast("Đã đổi quyền", "ok"); loadUsers(); }
  }

  async function removeUser(u) {
    if (!confirm(`Xóa vĩnh viễn tài khoản "${u.displayName}" và toàn bộ tin nhắn của họ? Hành động này không thể hoàn tác.`)) return;
    const ok = await guard(async () => { await Api.admin.deleteUser(u.id); return true; });
    if (ok) { toast("Đã xóa tài khoản", "ok"); loadUsers(); }
  }

  /* ---------- Nhật ký request ---------- */
  const METHOD_CLASS = { GET: "info", POST: "ok", PUT: "warn", PATCH: "warn", DELETE: "danger" };
  const statusClass = (s) => (s >= 500 ? "danger" : s >= 400 ? "warn" : "ok");

  async function loadLogs() {
    const list = await guard(() => Api.admin.logs());
    if (!list) return;
    state.logs = list;
    renderLogs();
    clearInterval(state.logTimer);
    if ($("#logAuto").checked) {
      state.logTimer = setInterval(async () => {
        if (state.view !== "logs") return;
        const l = await Api.admin.logs().catch(() => null);
        if (l) { state.logs = l; renderLogs(); }
      }, 5000);
    }
  }

  function renderLogs() {
    const q = $("#logSearch").value.trim().toLowerCase();
    const list = state.logs.filter((l) =>
      !q || `${l.method} ${l.path} ${l.status} ${l.username} ${l.ip}`.toLowerCase().includes(q));
    const body = $("#logBody");
    body.innerHTML = "";
    if (!list.length) return body.appendChild(emptyRow(7, "Chưa có nhật ký nào"));

    list.slice(0, 300).forEach((l) => {
      body.appendChild(
        h("tr", {},
          h("td", { class: "muted" }, fmtDT(l.time)),
          h("td", {}, h("span", { class: "badge " + (METHOD_CLASS[l.method] || "info") }, l.method)),
          h("td", { class: "mono" }, l.path),
          h("td", {}, h("span", { class: "badge " + statusClass(l.status) }, l.status)),
          h("td", {}, l.username),
          h("td", { class: "muted" }, l.ip),
          h("td", { class: "muted" }, l.durationMs != null ? l.durationMs + " ms" : "–")
        )
      );
    });
  }
  $("#logSearch").addEventListener("input", renderLogs);
  $("#logAuto").addEventListener("change", loadLogs);

  /* ---------- Giao diện sáng/tối (dùng chung với trang chat) ---------- */
  $("#adminTheme").addEventListener("click", () => {
    document.body.classList.toggle("dark");
    localStorage.setItem("cw_theme", document.body.classList.contains("dark") ? "dark" : "light");
  });
  if (localStorage.getItem("cw_theme") === "dark") document.body.classList.add("dark");

  /* ---------- Hiệu ứng ripple cho nút ---------- */
  $$(".btn").forEach((b) =>
    b.addEventListener("click", (e) => {
      const r = b.getBoundingClientRect();
      const s = Math.max(r.width, r.height);
      const span = document.createElement("span");
      span.className = "ripple";
      span.style.cssText = `width:${s}px;height:${s}px;left:${e.clientX - r.left - s / 2}px;top:${e.clientY - r.top - s / 2}px`;
      b.appendChild(span);
      setTimeout(() => span.remove(), 600);
    })
  );

  /* ---------- Khởi động ---------- */
  const au = Api.admin.getUser();
  if (Api.admin.getToken() && au && au.role === "ADMIN") showAdmin();
  else showLogin();
})();
