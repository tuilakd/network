/* ==========================================================
 * LỚP GỌI API
 * - DEMO_MODE = true  : dùng dữ liệu giả (localStorage + IndexedDB), nhiều tài khoản thật trong cùng trình duyệt
 * - DEMO_MODE = false : gọi RESTful API Spring Boot, gửi kèm JWT
 * Phiên đăng nhập người dùng và quản trị viên được lưu riêng.
 * ========================================================== */
const Api = (() => {
  const C = window.CONFIG;
  const E = C.ENDPOINTS;
  const D = C.DEMO_MODE;
  const NET_ERR = "Không kết nối được máy chủ. Kiểm tra backend đã chạy và đã bật CORS chưa.";

  /* ---------- Phiên đăng nhập (user / admin tách riêng) ---------- */
  const sessionStore = () => (C.SESSION_SCOPE === "browser" ? localStorage : sessionStorage);
  const makeSession = (k) => ({
    getToken: () => sessionStore().getItem(k.token),
    getUser: () => { try { return JSON.parse(sessionStore().getItem(k.user)); } catch { return null; } },
    save: (t, u) => { sessionStore().setItem(k.token, t); sessionStore().setItem(k.user, JSON.stringify(u)); },
    clear: () => { sessionStore().removeItem(k.token); sessionStore().removeItem(k.user); },
  });
  const SES = {
    user:  makeSession({ token: "cw_token",       user: "cw_user" }),
    admin: makeSession({ token: "cw_admin_token", user: "cw_admin_user" }),
  };

  /* ---------- Chuẩn hoá dữ liệu từ backend ---------- */
  const arr = (d) => (Array.isArray(d) ? d : d?.content ?? []);
  const same = (a, b) => String(a) === String(b);

  const normRole = (u) => {
    let r = u.role ?? (Array.isArray(u.roles) ? u.roles[0] : u.roles) ?? u.authorities?.[0] ?? "USER";
    if (r && typeof r === "object") r = r.name ?? r.authority ?? "USER";
    return String(r).replace(/^ROLE_/i, "").toUpperCase();
  };
  const normUser = (u) => ({
    id: D ? (u.id ?? u.userId) : (u.username ?? u.userName ?? u.friendUsername ?? u.id),
    username: u.username ?? u.userName ?? "",
    displayName: u.displayName ?? u.fullName ?? u.name ?? u.username ?? "Người dùng",
    role: normRole(u),
    email: u.email ?? "",
    locked: u.locked ?? (u.enabled === false),
    createdAt: u.createdAt ?? u.created ?? null,
    messageCount: u.messageCount ?? null,
  });

  const resolveUrl = (u) => {
    if (!u) return "";
    if (/^(https?:|blob:|data:)/i.test(u)) return u;
    return C.FILE_BASE + (u.startsWith("/") ? "" : "/") + u;
  };
  const guessType = (m) => {
    const t = String(m.type ?? m.messageType ?? m.attachmentType ?? "").toUpperCase();
    if (t.includes("IMAGE")) return "IMAGE";
    if (t.includes("AUDIO")) return "AUDIO";
    if (t.includes("VIDEO")) return "VIDEO";
    if (t === "TEXT") return "TEXT";
    const f = String(m.fileUrl ?? m.mediaUrl ?? m.url ?? m.attachmentUrl ?? "").toLowerCase();
    if (/\.(png|jpe?g|gif|webp|bmp|svg)(\?|$)/.test(f)) return "IMAGE";
    if (/\.(mp4|webm|ogg|mov|m4v)(\?|$)/.test(f)) return "VIDEO";
    return "TEXT";
  };
  const normMsg = (m) => {
    const readAt = m.readAt ?? m.seenAt ?? null;
    const seen = !!(m.seen ?? m.read ?? m.isRead ?? readAt);
    return {
      id: m.id ?? m.messageId ?? `${Date.now()}${Math.random()}`,
      senderId: m.senderId ?? (typeof m.sender === "string" ? m.sender : m.sender?.id) ?? m.fromUserId,
      receiverId: m.receiverId ?? (typeof m.recipient === "string" ? m.recipient : m.receiver?.id) ?? m.toUserId,
      senderName: m.senderName ?? m.sender?.displayName ?? m.sender?.username ?? "",
      receiverName: m.receiverName ?? m.receiver?.displayName ?? m.receiver?.username ?? "",
      content: m.content ?? m.message ?? m.text ?? "",
      type: guessType(m),
      fileUrl: resolveUrl(m.fileUrl ?? m.mediaUrl ?? m.url ?? m.attachmentUrl ?? ""),
      fileName: m.fileName ?? m.originalName ?? String(m.attachmentUrl ?? "").split("/").pop(),
      createdAt: m.createdAt ?? m.sentAt ?? m.timestamp ?? m.time ?? new Date().toISOString(),
      seen,           // người nhận đã xem chưa
      readAt,         // xem lúc nào (nếu có)
    };
  };

  // Bạn bè: người dùng + số tin chưa đọc + tin cuối  { unread, last:{ at, text, type, senderId } }
  const normFriend = (f) => {
    const u = f.user ?? f.friend ?? f;
    const lm = f.lastMessage ?? u.lastMessage ?? f.last ?? null;
    let last = null;
    if (lm) {
      const m = normMsg(lm);
      last = { id: m.id, at: lm.at ?? m.createdAt, text: lm.text ?? m.content, type: m.type, senderId: m.senderId };
    }
    return { ...normUser(u), unread: f.unread ?? f.unreadCount ?? u.unreadCount ?? 0, last };
  };

  // Lời mời kết bạn  { id (mã lời mời), user, createdAt }
  const normReq = (r) => ({
    id: r.id ?? r.requestId,
    user: normUser(r.user ?? r.from ?? r.sender ?? r.requester ?? r.receiver ?? r.addressee ?? r.to ?? r),
    createdAt: r.createdAt ?? r.time ?? null,
  });

  // Kết quả tìm kiếm  { ...user, relation: NONE | FRIEND | PENDING_SENT | PENDING_RECEIVED, requestId }
  const REL = {
    FRIEND: "FRIEND", FRIENDS: "FRIEND", NONE: "NONE",
    PENDING_SENT: "PENDING_SENT", SENT: "PENDING_SENT", REQUEST_SENT: "PENDING_SENT",
    PENDING_RECEIVED: "PENDING_RECEIVED", RECEIVED: "PENDING_RECEIVED", REQUEST_RECEIVED: "PENDING_RECEIVED",
  };
  const normSearch = (u) => ({
    ...normUser(u),
    relation: REL[String(u.relation ?? u.relationship ?? u.status ?? "NONE").toUpperCase()] ?? "NONE",
    requestId: u.requestId ?? u.friendRequestId ?? null,
  });

  // Phòng chat nhóm. id giao diện = "room:<id>" để không trùng với username của bạn bè
  const isRoomId = (id) => String(id).startsWith("room:");
  const rid = (id) => String(id).slice(5);
  const normRoom = (r, joined) => ({
    id: "room:" + r.id, roomId: r.id, username: "nhom" + r.id,
    displayName: r.name ?? r.roomName ?? ("Nhóm " + r.id),
    role: "USER", email: "", locked: false, createdAt: r.createdAt ?? null, messageCount: null,
    isRoom: true, joined, unread: 0, last: null,
  });
  const normRoomMsg = (m) => normMsg({ ...m, sender: m.sender ?? m.senderUsername ?? m.username ?? m.senderName, read: true });

  const normLog = (l) => ({
    id: l.id ?? `${l.time}${Math.random()}`,
    time: l.time ?? l.createdAt ?? l.timestamp ?? "",
    method: l.method ?? "",
    path: l.path ?? l.uri ?? l.url ?? "",
    status: l.status ?? l.statusCode ?? 0,
    username: l.username ?? l.user ?? "-",
    ip: l.ip ?? l.remoteAddr ?? "",
    durationMs: l.durationMs ?? l.duration ?? null,
  });

  const parseLogin = (data, username) => {
    const token = data.token ?? data.accessToken ?? data.jwt;
    if (!token) throw new Error("Backend không trả về token (token / accessToken / jwt).");
    const user = data.user
      ? normUser(data.user)
      : normUser({ id: data.id ?? data.userId, username: data.username ?? username, displayName: data.displayName ?? data.fullName, role: data.role ?? data.roles });
    return { token, user };
  };

  /* ---------- Gọi HTTP thật ---------- */
  function expired(scope, ses) {
    ses.clear();
    window.dispatchEvent(new CustomEvent("session-expired", { detail: scope }));
  }

  async function http(path, { method = "GET", body, auth = true, scope = "user" } = {}) {
    const ses = SES[scope];
    const headers = { "Content-Type": "application/json" };
    if (auth && ses.getToken()) headers["Authorization"] = "Bearer " + ses.getToken();

    let res;
    try {
      res = await fetch(C.API_BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
    } catch {
      throw new Error(NET_ERR);
    }

    let data = null;
    const text = await res.text();
    if (text) { try { data = JSON.parse(text); } catch { data = text; } }

    if (res.status === 401 && auth) expired(scope, ses);
    if (!res.ok) throw new Error((data && (data.message || data.error)) || `Lỗi ${res.status}`);
    return data;
  }

  // Tải tệp lên (multipart) có báo tiến trình
  function upload(path, formData, onProgress) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("POST", C.API_BASE + path);
      const t = SES.user.getToken();
      if (t) xhr.setRequestHeader("Authorization", "Bearer " + t);
      xhr.upload.onprogress = (e) => { if (e.lengthComputable && onProgress) onProgress(e.loaded / e.total); };
      xhr.onload = () => {
        let data = null;
        try { data = JSON.parse(xhr.responseText); } catch { /* không phải JSON */ }
        if (xhr.status === 401) expired("user", SES.user);
        if (xhr.status >= 200 && xhr.status < 300) resolve(data);
        else reject(new Error((data && (data.message || data.error)) || `Lỗi ${xhr.status}`));
      };
      xhr.onerror = () => reject(new Error(NET_ERR));
      xhr.send(formData);
    });
  }

  /* ---------- Kiểm tra tệp trước khi gửi ---------- */
  function validateFile(file) {
    let kind = null;
    if (file.type.startsWith("image/")) kind = "IMAGE";
    else if (file.type.startsWith("audio/")) kind = "AUDIO";
    else if (file.type.startsWith("video/")) {
      if (!D) return { error: "Backend hiện chỉ nhận hình ảnh và ghi âm, chưa nhận video" };
      kind = "VIDEO";
    }
    if (!kind) return { error: D ? "Chỉ hỗ trợ gửi hình ảnh, video hoặc ghi âm" : "Chỉ hỗ trợ gửi hình ảnh hoặc ghi âm" };
    const max = kind === "IMAGE" ? C.MAX_IMAGE_MB : kind === "AUDIO" ? (C.MAX_AUDIO_MB || 10) : C.MAX_VIDEO_MB;
    if (file.size > max * 1024 * 1024)
      return { error: `${kind === "IMAGE" ? "Ảnh" : kind === "AUDIO" ? "Ghi âm" : "Video"} quá lớn (tối đa ${max} MB)` };
    return { kind };
  }

  /* ==========================================================
   * DEMO MODE (dữ liệu giả, dùng chung giữa các tab cùng trình duyệt)
   * ========================================================== */
  const DB_USERS = "cw_demo_users";
  const DB_MSGS = "cw_demo_msgs";
  const DB_LOGS = "cw_demo_logs";
  const DB_FRIENDS = "cw_demo_friends";
  const DB_FLAG = "cw_demo_v2";
  const load = (k, d) => { try { return JSON.parse(localStorage.getItem(k)) ?? d; } catch { return d; } };
  const store = (k, v) => localStorage.setItem(k, JSON.stringify(v));
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const typeOf = (m) => m.type || "TEXT";
  const fold = (s) => String(s || "").normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/đ/g, "d").replace(/Đ/g, "D").toLowerCase();
  const newId = () => Date.now() * 1000 + Math.floor(Math.random() * 1000);

  const ADMIN = { id: 9000, username: "admin", displayName: "Quản trị viên", password: "admin123", role: "ADMIN" };
  const BOTS = [
    { id: 9001, username: "an",   displayName: "Nguyễn Văn An" },
    { id: 9002, username: "binh", displayName: "Trần Thị Bình" },
    { id: 9003, username: "chi",  displayName: "Lê Minh Chi" },
    { id: 9004, username: "dung", displayName: "Phạm Quốc Dũng" },
  ];
  const BOT_REPLIES = [
    "Chào bạn! Mình đang online đây 😊", "Ok, mình hiểu rồi nhé 👍", "Hay đó! Kể mình nghe thêm đi.",
    "Để mình xem lại rồi báo bạn sau nha.", "Haha, đúng rồi đó 😂", "API chạy ngon chưa bạn? 🚀",
  ];
  const BOT_IMG = ["Ảnh đẹp quá! 😍", "Chụp ở đâu vậy bạn?", "Mình lưu ảnh này nhé 👍"];
  const BOT_VID = ["Video hay quá! 🎬", "Xem xong rồi, vui ghê 😄", "Gửi thêm video nữa đi bạn!"];
  const pick = (a) => a[Math.floor(Math.random() * a.length)];
  const isBot = (id) => BOTS.some((b) => same(b.id, id));
  const pairOf = (f, x, y) => (same(f.a, x) && same(f.b, y)) || (same(f.a, y) && same(f.b, x));

  // Danh sách tài khoản demo (tài khoản máy chỉ có khi bật DEMO_BOTS)
  function demoUsers() {
    let users = load(DB_USERS, []);
    const now = new Date().toISOString();
    const seeds = [ADMIN, ...(C.DEMO_BOTS ? BOTS.map((b) => ({ ...b, password: "bot", role: "USER" })) : [])];
    let changed = false;
    seeds.forEach((s) => {
      if (!users.find((u) => same(u.id, s.id))) { users.push({ ...s, locked: false, createdAt: now }); changed = true; }
    });
    if (changed) store(DB_USERS, users);
    return C.DEMO_BOTS ? users : users.filter((u) => !isBot(u.id));
  }

  // Dữ liệu demo bản cũ: bỏ tài khoản máy cũ, và coi các cuộc trò chuyện đã có là bạn bè (để không mất tin nhắn)
  function migrateDemo() {
    if (localStorage.getItem(DB_FLAG)) return;
    store(DB_USERS, load(DB_USERS, []).filter((u) => !isBot(u.id)));
    const msgs = load(DB_MSGS, []).filter((m) => !isBot(m.senderId) && !isBot(m.receiverId));
    store(DB_MSGS, msgs);
    const fr = load(DB_FRIENDS, []);
    msgs.forEach((m) => {
      if (!fr.find((f) => pairOf(f, m.senderId, m.receiverId)))
        fr.push({ id: newId(), a: m.senderId, b: m.receiverId, status: "ACCEPTED", createdAt: m.createdAt });
    });
    store(DB_FRIENDS, fr);
    localStorage.setItem(DB_FLAG, "1");
  }
  if (D) migrateDemo();

  function logDemo(method, path, status, username) {
    const logs = load(DB_LOGS, []);
    logs.push({ id: Date.now() + Math.random(), time: new Date().toISOString(), method, path, status, username: username || "-", ip: "127.0.0.1" });
    store(DB_LOGS, logs.slice(-300));
  }

  // Kho tệp demo: IndexedDB (lưu được video lớn, không bị giới hạn 5MB của localStorage)
  const idb = (() => {
    let dbp;
    const open = () => (dbp ||= new Promise((res, rej) => {
      const r = indexedDB.open("cw_demo_files", 1);
      r.onupgradeneeded = () => r.result.createObjectStore("files");
      r.onsuccess = () => res(r.result);
      r.onerror = () => rej(r.error);
    }));
    const tx = async (mode, fn) => {
      const db = await open();
      return new Promise((res, rej) => {
        const t = db.transaction("files", mode);
        const req = fn(t.objectStore("files"));
        t.oncomplete = () => res(req?.result);
        t.onerror = () => rej(t.error);
      });
    };
    return {
      put: (k, b) => tx("readwrite", (s) => s.put(b, k)),
      get: (k) => tx("readonly", (s) => s.get(k)),
      del: (k) => tx("readwrite", (s) => s.delete(k)),
    };
  })();
  const urlCache = new Map();
  async function fileUrlFor(key) {
    if (urlCache.has(key)) return urlCache.get(key);
    try {
      const blob = await idb.get(key);
      if (blob) { const u = URL.createObjectURL(blob); urlCache.set(key, u); return u; }
    } catch { /* bỏ qua */ }
    return "";
  }

  const pushMsg = (msg) => { const all = load(DB_MSGS, []); all.push(msg); store(DB_MSGS, all); };
  const me = () => SES.user.getUser();
  const findFriendship = (x, y) => load(DB_FRIENDS, []).find((f) => pairOf(f, x, y));
  function requireFriends(otherId) {
    const f = findFriendship(me().id, otherId);
    if (!f || f.status !== "ACCEPTED") throw new Error("Hai bạn chưa là bạn bè nên chưa thể nhắn tin");
  }
  const conversation = (x, y) =>
    load(DB_MSGS, [])
      .filter((m) => (same(m.senderId, x) && same(m.receiverId, y)) || (same(m.senderId, y) && same(m.receiverId, x)))
      .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));

  // Tài khoản máy (DEMO_BOTS): tự chấp nhận kết bạn, tự xem và trả lời tin nhắn
  function botAcceptRequest(botId, myId) {
    setTimeout(() => {
      const fr = load(DB_FRIENDS, []);
      const f = fr.find((x) => x.status === "PENDING" && same(x.a, myId) && same(x.b, botId));
      if (f) { f.status = "ACCEPTED"; f.updatedAt = new Date().toISOString(); store(DB_FRIENDS, fr); }
    }, 1200);
  }
  function botRespond(botId, myId, replies) {
    setTimeout(() => {
      const all = load(DB_MSGS, []);
      const now = new Date().toISOString();
      all.forEach((m) => { if (same(m.senderId, myId) && same(m.receiverId, botId) && !m.readAt) m.readAt = now; });
      store(DB_MSGS, all);
    }, 900);
    setTimeout(() => {
      pushMsg({ id: Date.now() + 1, senderId: botId, receiverId: myId, type: "TEXT", content: pick(replies), readAt: null, createdAt: new Date().toISOString() });
    }, 1800 + Math.random() * 1500);
  }

  const demo = {
    async register({ username, password, displayName, email }) {
      await sleep(500);
      const users = load(DB_USERS, []);
      if (email && users.find((u) => u.email && u.email.toLowerCase() === email.toLowerCase())) {
        logDemo("POST", "/api/auth/register", 409, username);
        throw new Error("Email này đã được dùng cho tài khoản khác");
      }
      if (demoUsers().find((u) => u.username.toLowerCase() === username.toLowerCase())) {
        logDemo("POST", "/api/auth/register", 409, username);
        throw new Error("Tên đăng nhập đã tồn tại");
      }
      users.push({ id: Date.now(), username, password, displayName, email: email || "", role: "USER", locked: false, createdAt: new Date().toISOString() });
      store(DB_USERS, users);
      logDemo("POST", "/api/auth/register", 201, username);
      return { message: "ok" };
    },
    // Quên mật khẩu (demo): không có máy chủ gửi email nên trả mã ra để hiển thị trên màn hình
    async setEmail(email) {
      await sleep(400);
      const my = me();
      const users = load(DB_USERS, []);
      if (users.find((u) => u.email && u.email.toLowerCase() === email.toLowerCase() && !same(u.id, my.id)))
        throw new Error("Email này đã được dùng cho tài khoản khác");
      const u = users.find((x) => same(x.id, my.id));
      if (!u) throw new Error("Không tìm thấy tài khoản");
      u.email = email;
      store(DB_USERS, users);
      logDemo("PUT", "/api/users/me/email", 200, u.username);
      return { email };
    },
    async forgot(email) {
      await sleep(500);
      const users = load(DB_USERS, []);
      const u = users.find((x) => x.email && x.email.toLowerCase() === email.toLowerCase() && x.role !== "ADMIN");
      logDemo("POST", "/api/auth/forgot-password", 200, u?.username);
      if (!u) return { message: "ok" }; // không tiết lộ email có tồn tại hay không
      const code = String(Math.floor(100000 + Math.random() * 900000));
      const resets = load("cw_demo_resets", {});
      resets[email.toLowerCase()] = { code, expires: Date.now() + 10 * 60 * 1000, tries: 0 };
      store("cw_demo_resets", resets);
      return { message: "ok", demoCode: code };
    },
    async reset(email, code, newPassword) {
      await sleep(500);
      const key = email.toLowerCase();
      const resets = load("cw_demo_resets", {});
      const r = resets[key];
      if (!r || r.expires < Date.now()) throw new Error("Mã không hợp lệ hoặc đã hết hạn. Hãy gửi lại mã mới.");
      if (r.tries >= 5) { delete resets[key]; store("cw_demo_resets", resets); throw new Error("Nhập sai quá nhiều lần. Hãy gửi lại mã mới."); }
      if (r.code !== String(code).trim()) {
        r.tries++; store("cw_demo_resets", resets);
        throw new Error("Mã xác nhận không đúng");
      }
      const users = load(DB_USERS, []);
      const u = users.find((x) => x.email && x.email.toLowerCase() === key);
      if (!u) throw new Error("Không tìm thấy tài khoản");
      u.password = newPassword;
      store(DB_USERS, users);
      delete resets[key]; store("cw_demo_resets", resets);
      logDemo("POST", "/api/auth/reset-password", 200, u.username);
      return { message: "ok" };
    },
    async login({ username, password }) {
      await sleep(600);
      const key = username.trim().toLowerCase(); // đăng nhập bằng tên đăng nhập HOẶC email
      const u = demoUsers().find((x) => (x.username.toLowerCase() === key || (x.email && x.email.toLowerCase() === key)) && x.password === password);
      if (!u) { logDemo("POST", "/api/auth/login", 401, username); throw new Error("Sai tên đăng nhập hoặc mật khẩu"); }
      if (u.locked) { logDemo("POST", "/api/auth/login", 403, username); throw new Error("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên."); }
      logDemo("POST", "/api/auth/login", 200, username);
      return { token: `demo.${u.id}.${Date.now()}`, user: normUser(u) };
    },

    /* ----- Tìm kiếm & bạn bè ----- */
    async search(q) {
      await sleep(200);
      const k = fold(q.trim());
      if (!k) return [];
      const my = me();
      const fr = load(DB_FRIENDS, []);
      return demoUsers()
        .filter((u) => u.role !== "ADMIN" && !u.locked && !same(u.id, my.id) && (fold(u.displayName).includes(k) || fold(u.username).includes(k)))
        .slice(0, 20)
        .map((u) => {
          const f = fr.find((x) => pairOf(x, my.id, u.id));
          let relation = "NONE", requestId = null;
          if (f?.status === "ACCEPTED") relation = "FRIEND";
          else if (f) { relation = same(f.a, my.id) ? "PENDING_SENT" : "PENDING_RECEIVED"; requestId = f.id; }
          return normSearch({ ...u, relation, requestId });
        });
    },
    async friends() {
      const my = me();
      const users = demoUsers();
      return load(DB_FRIENDS, [])
        .filter((f) => f.status === "ACCEPTED" && (same(f.a, my.id) || same(f.b, my.id)))
        .map((f) => {
          const otherId = same(f.a, my.id) ? f.b : f.a;
          const u = users.find((x) => same(x.id, otherId));
          if (!u || u.locked) return null;
          const conv = conversation(my.id, otherId);
          return normFriend({
            ...u,
            unreadCount: conv.filter((m) => same(m.senderId, otherId) && !m.readAt).length,
            lastMessage: conv[conv.length - 1] || null,
          });
        })
        .filter(Boolean);
    },
    async requests(kind) {
      const my = me();
      const users = demoUsers();
      const pending = load(DB_FRIENDS, []).filter((f) => f.status === "PENDING");
      const mine = kind === "sent" ? pending.filter((f) => same(f.a, my.id)) : pending.filter((f) => same(f.b, my.id));
      return mine
        .map((f) => {
          const u = users.find((x) => same(x.id, kind === "sent" ? f.b : f.a));
          return u && !u.locked ? normReq({ id: f.id, user: u, createdAt: f.createdAt }) : null;
        })
        .filter(Boolean);
    },
    async sendRequest(toId) {
      await sleep(150);
      const my = me();
      if (same(toId, my.id)) throw new Error("Không thể kết bạn với chính mình");
      const target = demoUsers().find((u) => same(u.id, toId));
      if (!target || target.role === "ADMIN" || target.locked) throw new Error("Không tìm thấy người dùng");
      const fr = load(DB_FRIENDS, []);
      const ex = fr.find((f) => pairOf(f, my.id, toId));
      const now = new Date().toISOString();
      if (ex) {
        if (ex.status === "ACCEPTED") throw new Error("Hai bạn đã là bạn bè");
        if (same(ex.a, my.id)) throw new Error("Bạn đã gửi lời mời cho người này rồi");
        ex.status = "ACCEPTED"; // họ đã gửi lời mời cho mình từ trước -> tự động chấp nhận
        ex.updatedAt = now;
      } else {
        fr.push({ id: newId(), a: my.id, b: toId, status: "PENDING", createdAt: now });
      }
      store(DB_FRIENDS, fr);
      logDemo("POST", "/api/friends/requests", 201, my.username);
      if (C.DEMO_BOTS && isBot(toId)) botAcceptRequest(toId, my.id);
    },
    async accept(requestId) {
      await sleep(150);
      const my = me();
      const fr = load(DB_FRIENDS, []);
      const ex = fr.find((f) => same(f.id, requestId) && f.status === "PENDING" && same(f.b, my.id));
      if (!ex) throw new Error("Không tìm thấy lời mời kết bạn");
      ex.status = "ACCEPTED";
      ex.updatedAt = new Date().toISOString();
      store(DB_FRIENDS, fr);
      logDemo("POST", `/api/friends/requests/${requestId}/accept`, 200, my.username);
    },
    async decline(requestId) { // từ chối lời mời nhận được
      await sleep(100);
      const my = me();
      store(DB_FRIENDS, load(DB_FRIENDS, []).filter((f) => !(same(f.id, requestId) && f.status === "PENDING" && same(f.b, my.id))));
      logDemo("POST", `/api/friends/requests/${requestId}/decline`, 200, my.username);
    },
    async cancel(requestId) { // hủy lời mời mình đã gửi
      await sleep(100);
      const my = me();
      store(DB_FRIENDS, load(DB_FRIENDS, []).filter((f) => !(same(f.id, requestId) && f.status === "PENDING" && same(f.a, my.id))));
      logDemo("DELETE", `/api/friends/requests/${requestId}`, 204, my.username);
    },
    async unfriend(userId) {
      await sleep(100);
      const my = me();
      store(DB_FRIENDS, load(DB_FRIENDS, []).filter((f) => !(f.status === "ACCEPTED" && pairOf(f, my.id, userId))));
      logDemo("DELETE", `/api/friends/${userId}`, 204, my.username);
    },

    /* ----- Tin nhắn ----- */
    async messages(otherId) {
      requireFriends(otherId);
      const list = conversation(me().id, otherId);
      return Promise.all(list.map(async (m) => normMsg({ ...m, fileUrl: m.fileKey ? await fileUrlFor(m.fileKey) : "" })));
    },
    async markRead(otherId) {
      requireFriends(otherId);
      const my = me();
      const all = load(DB_MSGS, []);
      const now = new Date().toISOString();
      let n = 0;
      all.forEach((m) => { if (same(m.senderId, otherId) && same(m.receiverId, my.id) && !m.readAt) { m.readAt = now; n++; } });
      if (n) { store(DB_MSGS, all); logDemo("POST", `/api/messages/${otherId}/read`, 200, my.username); }
      return { updated: n };
    },
    async send(receiverId, content) {
      await sleep(200);
      requireFriends(receiverId);
      const my = me();
      const msg = { id: Date.now(), senderId: my.id, receiverId, type: "TEXT", content, readAt: null, createdAt: new Date().toISOString() };
      pushMsg(msg);
      logDemo("POST", "/api/messages", 201, my.username);
      if (C.DEMO_BOTS && isBot(receiverId)) botRespond(receiverId, my.id, BOT_REPLIES);
      return normMsg(msg);
    },
    async sendFile(receiverId, file, content, onProgress) {
      requireFriends(receiverId);
      const my = me();
      const kind = file.type.startsWith("video/") ? "VIDEO" : "IMAGE";
      for (let i = 1; i <= 5; i++) { await sleep(120); onProgress && onProgress(i / 5); }
      const key = "f" + Date.now();
      await idb.put(key, file);
      urlCache.set(key, URL.createObjectURL(file));
      const msg = {
        id: Date.now(), senderId: my.id, receiverId, type: kind, content: content || "", readAt: null,
        fileKey: key, fileName: file.name, fileSize: file.size, createdAt: new Date().toISOString(),
      };
      pushMsg(msg);
      logDemo("POST", "/api/messages/upload", 201, my.username);
      if (C.DEMO_BOTS && isBot(receiverId)) botRespond(receiverId, my.id, kind === "IMAGE" ? BOT_IMG : BOT_VID);
      return normMsg({ ...msg, fileUrl: urlCache.get(key) });
    },
  };

  const demoAdmin = {
    async stats() {
      await sleep(200);
      const users = demoUsers().filter((u) => u.role !== "ADMIN");
      const msgs = load(DB_MSGS, []);
      return {
        totalUsers: users.length,
        lockedUsers: users.filter((u) => u.locked).length,
        totalMessages: msgs.length,
        totalImages: msgs.filter((m) => typeOf(m) === "IMAGE").length,
        totalVideos: msgs.filter((m) => typeOf(m) === "VIDEO").length,
      };
    },
    async users() {
      await sleep(200);
      const msgs = load(DB_MSGS, []);
      return demoUsers()
        .filter((u) => u.role !== "ADMIN")
        .map((u) => normUser({ ...u, messageCount: msgs.filter((m) => same(m.senderId, u.id)).length }));
    },
    async setLocked(id, locked) {
      const users = load(DB_USERS, []);
      const u = users.find((x) => same(x.id, id));
      if (!u) throw new Error("Không tìm thấy người dùng");
      u.locked = locked;
      store(DB_USERS, users);
      logDemo("PUT", `/api/admin/users/${id}/lock`, 200, "admin");
    },
    async setRole(id, role) {
      const users = load(DB_USERS, []);
      const u = users.find((x) => same(x.id, id));
      if (!u) throw new Error("Không tìm thấy người dùng");
      u.role = role;
      store(DB_USERS, users);
    },
    async deleteUser(id) {
      store(DB_USERS, load(DB_USERS, []).filter((u) => !same(u.id, id)));
      store(DB_FRIENDS, load(DB_FRIENDS, []).filter((f) => !same(f.a, id) && !same(f.b, id)));
      const msgs = load(DB_MSGS, []);
      const gone = msgs.filter((m) => same(m.senderId, id) || same(m.receiverId, id));
      store(DB_MSGS, msgs.filter((m) => !gone.includes(m)));
      for (const m of gone) if (m.fileKey) { try { await idb.del(m.fileKey); } catch { /* bỏ qua */ } }
      logDemo("DELETE", `/api/admin/users/${id}`, 204, "admin");
    },
    async logs() {
      await sleep(120);
      return load(DB_LOGS, []).slice().reverse().map(normLog);
    },
  };

  const msgCache = new Map(); // username -> danh sách tin vừa tải (dùng cho markRead)
  const bad = (m) => { throw new Error(m); };

  /* ==========================================================
   * API công khai
   * ========================================================== */
  return {
    getToken: SES.user.getToken,
    getUser: SES.user.getUser,
    clearSession: SES.user.clear,
    isDemo: () => D,
    validateFile,

    async login(username, password) {
      const data = D
        ? await demo.login({ username, password })
        : await http(E.login, { method: "POST", body: { username, password }, auth: false });
      const { token, user } = parseLogin(data, username);
      if (user.role === "ADMIN") throw new Error("Tài khoản quản trị vui lòng đăng nhập tại trang quản trị.");
      SES.user.save(token, user);
      return user;
    },

    async register(username, password, displayName, email) {
      if (D) return demo.register({ username, password, displayName, email });
      return http(E.register, { method: "POST", body: { username, email, password }, auth: false });
    },

    // Thêm / đổi email của tài khoản đang đăng nhập
    async updateEmail(email) {
      if (D) await demo.setEmail(email);
      else await http(E.myEmail, { method: "PUT", body: { email } });
      const u = { ...SES.user.getUser(), email };
      SES.user.save(SES.user.getToken(), u);
      return u;
    },

    // Bước 1: gửi mã xác nhận tới email. Demo trả thêm { demoCode } để hiện trên màn hình
    async forgotPassword(email) {
      if (D) return demo.forgot(email);
      return http(E.forgotPassword, { method: "POST", body: { email }, auth: false });
    },
    // Bước 2: nhập mã + mật khẩu mới
    async resetPassword(email, code, newPassword) {
      if (D) return demo.reset(email, code, newPassword);
      return http(E.resetPassword, { method: "POST", body: { email, code, newPassword }, auth: false });
    },

    /* ----- Tìm kiếm & kết bạn ----- */
    // Chỉ tìm khi có từ khóa; không bao giờ trả về toàn bộ người dùng
    async searchUsers(q) {
      if (D) return demo.search(q);
      // Backend chưa có tìm gần đúng: tìm CHÍNH XÁC theo tên đăng nhập qua /profile/{username}
      const my = SES.user.getUser();
      const name = q.trim();
      if (!name || same(name, my?.username)) return [];
      let p;
      try { p = await http(`${E.profile}/${encodeURIComponent(name)}`); } catch { return []; }
      if (!p || !p.username || same(p.username, my?.username)) return [];
      const [fr, pend] = await Promise.all([http(E.friends), http(E.friendsPending)]);
      let relation = "NONE", requestId = null;
      if (arr(fr).some((f) => same(f.friendUsername ?? (same(f.requester, my?.username) ? f.addressee : f.requester), p.username))) relation = "FRIEND";
      else {
        const rq = arr(pend).find((r) => same(r.requester, p.username));
        if (rq) { relation = "PENDING_RECEIVED"; requestId = rq.id; }
      }
      return [{ ...normUser({ username: p.username, fullName: p.fullName, email: p.email }), relation, requestId }];
    },

    async getFriends() {
      if (D) return demo.friends();
      // Ghép /friends (ai là bạn) với /direct-messages/conversations (tin cuối, chưa đọc)
      const me = SES.user.getUser()?.username;
      const [fr, conv] = await Promise.all([http(E.friends), http(E.conversations)]);
      const map = new Map();
      arr(fr).forEach((f) => {
        if (f.status && String(f.status).toUpperCase() !== "ACCEPTED") return;
        const name = f.friendUsername ?? (same(f.requester, me) ? f.addressee : f.requester);
        if (name) map.set(name, { unread: 0, last: null });
      });
      arr(conv).forEach((c) => {
        const e = map.get(c.friendUsername);
        if (!e) return;
        e.unread = c.unreadCount ?? 0;
        e.last = { id: `${c.friendUsername}|${c.lastSentAt}`, at: c.lastSentAt, text: c.lastMessage ?? "", type: "TEXT", senderId: c.lastSender };
      });
      return [...map].map(([name, e]) => ({ ...normUser({ username: name }), unread: e.unread, last: e.last }));
    },

    // kind = "received" (lời mời gửi tới mình) | "sent" (lời mời mình đã gửi); mỗi mục { id, user, createdAt }
    async getRequests(kind = "received") {
      if (D) return demo.requests(kind);
      if (kind === "sent") return []; // backend chưa có danh sách lời mời đã gửi
      return arr(await http(E.friendsPending))
        .map((r) => normReq({ id: r.id, user: { username: r.requester }, createdAt: r.createdAt }));
    },

    async sendFriendRequest(userId) {
      if (D) return demo.sendRequest(userId);
      return http(E.friendRequestSend, { method: "POST", body: { targetUsername: userId } });
    },
    async acceptRequest(requestId) {
      if (D) return demo.accept(requestId);
      return http(`${E.friendRequests}/${requestId}/accept`, { method: "POST" });
    },
    async declineRequest(requestId) {
      if (D) return demo.decline(requestId);
      return http(`${E.friendRequests}/${requestId}/decline`, { method: "POST" });
    },
    async cancelRequest(requestId) {
      if (D) return demo.cancel(requestId);
      return bad("Backend chưa hỗ trợ hủy lời mời đã gửi");
    },
    async unfriend(userId) {
      if (D) return demo.unfriend(userId);
      return http(`${E.friends}/${encodeURIComponent(userId)}`, { method: "DELETE" });
    },

    /* ----- Phòng chat nhóm ----- */
    async getRooms() {
      if (D) return [];
      const [all, mine] = await Promise.all([http(E.rooms), http(`${E.rooms}/mine`)]);
      const mineIds = new Set(arr(mine).map((r) => String(r.id)));
      return arr(all).map((r) => normRoom(r, mineIds.has(String(r.id))));
    },
    async createRoom(name) {
      if (D) throw new Error("Chế độ demo chưa có nhóm chat");
      return normRoom(await http(E.rooms, { method: "POST", body: { name } }), true);
    },
    async joinRoom(roomId) {
      return http(`${E.rooms}/${roomId}/join`, { method: "POST" });
    },
    async getRoomMembers(roomId) {
      return arr(await http(`${E.rooms}/${roomId}/members`)).map((m) => (typeof m === "string" ? m : m.username ?? m.name ?? String(m)));
    },

    /* ----- Tin nhắn ----- */
    async getMessages(otherId) {
      if (D) return demo.messages(otherId);
      if (isRoomId(otherId)) {
        const rl = arr(await http(`${E.rooms}/${rid(otherId)}/messages?page=0&size=200`)).map(normRoomMsg);
        rl.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt) || Number(a.id) - Number(b.id));
        return rl;
      }
      const list = arr(await http(`${E.messages}/${encodeURIComponent(otherId)}?page=0&size=200`)).map(normMsg);
      list.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt) || Number(a.id) - Number(b.id));
      msgCache.set(String(otherId), list);
      return list;
    },

    // Đánh dấu mọi tin của người kia gửi cho mình là đã xem
    async markRead(otherId) {
      if (D) return demo.markRead(otherId);
      if (isRoomId(otherId)) return; // chat nhóm không có trạng thái đã xem
      // Backend đánh dấu đã đọc từng tin (PUT /direct-messages/{id}/read)
      const me = SES.user.getUser()?.username;
      const todo = (msgCache.get(String(otherId)) || []).filter((m) => !m.seen && same(m.receiverId, me));
      await Promise.all(todo.map((m) =>
        http(`${E.messages}/${m.id}/read`, { method: "PUT" }).then(() => { m.seen = true; }).catch(() => {})));
    },

    async sendMessage(receiverId, content) {
      if (D) return demo.send(receiverId, content);
      const data = isRoomId(receiverId)
        ? await http(`${E.rooms}/${rid(receiverId)}/messages`, { method: "POST", body: { content } })
        : await http(E.messages, { method: "POST", body: { recipient: receiverId, content } });
      return normMsg(data && typeof data === "object" ? data : { senderId: SES.user.getUser()?.id, receiverId, content });
    },

    // Gửi hình ảnh / video (kèm chú thích tùy chọn)
    async sendFile(receiverId, file, content, onProgress) {
      if (D) return demo.sendFile(receiverId, file, content, onProgress);
      const fd = new FormData();
      fd.append("file", file);
      const up = (await upload(E.upload, fd, onProgress)) || {};
      const url = up.url ?? up.fileUrl ?? up.fileDownloadUri ?? up.downloadUrl ?? up.attachmentUrl ?? (up.fileName ? `/api/files/${up.fileName}` : "");
      if (!url) throw new Error("Backend không trả về đường dẫn tệp đã tải lên.");
      const kind = up.fileType ?? (file.type.startsWith("audio/") ? "AUDIO" : "IMAGE"); // backend: IMAGE hoặc AUDIO
      const room = isRoomId(receiverId);
      const data = await http(room ? `${E.rooms}/${rid(receiverId)}/messages` : E.messages, {
        method: "POST",
        body: room ? { content: content || "", attachmentUrl: url, attachmentType: kind }
                   : { recipient: receiverId, content: content || "", attachmentUrl: url, attachmentType: kind },
      });
      return normMsg(data && typeof data === "object" ? data : { senderId: SES.user.getUser()?.id, receiverId, content });
    },

    /* ---------- Khu vực quản trị ---------- */
    admin: {
      getToken: SES.admin.getToken,
      getUser: SES.admin.getUser,
      clear: SES.admin.clear,
      isDemo: () => D,

      async login(username, password) {
        const data = D
          ? await demo.login({ username, password })
          : await http(E.adminLogin, { method: "POST", body: { username, password }, auth: false, scope: "admin" });
        const { token, user } = parseLogin(data, username);
        if (user.role !== "ADMIN") throw new Error("Tài khoản này không có quyền quản trị.");
        SES.admin.save(token, user);
        return user;
      },

      async stats() {
        if (D) return demoAdmin.stats();
        const d = (await http(E.adminStats, { scope: "admin" })) || {};
        return {
          totalUsers: d.totalUsers ?? 0, lockedUsers: d.lockedUsers ?? 0,
          totalMessages: d.totalMessages ?? 0, totalImages: d.totalImages ?? 0, totalVideos: d.totalVideos ?? 0,
        };
      },

      async users() {
        if (D) return demoAdmin.users();
        return arr(await http(E.adminUsers, { scope: "admin" })).map((u) => ({ ...normUser(u), id: u.id }));
      },

      async setRole(id, role) {
        if (D) return demoAdmin.setRole(id, role);
        return http(`${E.adminUsers}/${id}/role`, { method: "PUT", body: { role }, scope: "admin" });
      },

      async setLocked(id, locked) {
        if (D) return demoAdmin.setLocked(id, locked);
        return http(`${E.adminUsers}/${id}/lock`, { method: "PUT", body: { locked }, scope: "admin" });
      },

      async deleteUser(id) {
        if (D) return demoAdmin.deleteUser(id);
        return http(`${E.adminUsers}/${id}`, { method: "DELETE", scope: "admin" });
      },

      async logs() {
        if (D) return demoAdmin.logs();
        return arr(await http(`${E.adminLogs}?size=300`, { scope: "admin" })).map(normLog);
      },
    },
  };
})();
