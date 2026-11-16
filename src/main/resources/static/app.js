/* ==========================================================
 * LOGIC GIAO DIỆN NGƯỜI DÙNG
 * Tìm tài khoản → kết bạn → được chấp nhận mới nhắn tin; có đã gửi / đã xem.
 * ========================================================== */
(() => {
  const $ = (s) => document.querySelector(s);
  const $$ = (s) => document.querySelectorAll(s);
  const CFG = window.CONFIG;

  const state = {
    me: null,
    active: null,          // người đang chat (phải là bạn bè)

    // Bạn bè / lời mời / tìm kiếm
    friends: [],           // [{id, username, displayName, unread, last}]
    friendIds: new Set(),
    requestsIn: [],        // lời mời nhận được
    requestsOut: [],       // lời mời đã gửi
    sideLoaded: false,
    sideTimer: null,
    sideSig: "",
    tab: "chats",          // chats | requests
    query: "",
    results: [],
    searching: false,
    searchSeq: 0,
    searchTimer: null,

    // Tin nhắn
    rendered: new Set(),
    els: new Map(),        // id tin nhắn -> phần tử DOM
    lastDay: "",
    timer: null,
    needRead: false,       // còn tin người kia gửi mà mình chưa đánh dấu đã xem
    attach: null,          // { file, kind, url }
    inflight: 0,           // số tin đang gửi (tránh hiện trùng khi tự tải)
    stick: true,           // đang ở cuối khung chat?
    media: new Map(),      // ảnh/video của cuộc trò chuyện đang mở
    mediaFilter: "ALL",
  };

  // Màu avatar dịu, chữ trắng vẫn đọc rõ
  const COLORS = [
    ["#4f7fa8", "#6c9cc4"], ["#4e9488", "#6fb3a6"], ["#b07a5e", "#cf9a7e"],
    ["#8a73b0", "#a893c9"], ["#6c8f4e", "#8aae6d"], ["#b9677a", "#d4889a"],
  ];

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
  const fold = (s) => String(s ?? "").normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/đ/g, "d").replace(/Đ/g, "D").toLowerCase();
  const initial = (name) => (name || "?").trim().split(/\s+/).pop().charAt(0);
  const colorOf = (key) => {
    let x = 0;
    for (const c of String(key)) x = (x * 31 + c.charCodeAt(0)) >>> 0;
    const [a, b] = COLORS[x % COLORS.length];
    return `linear-gradient(135deg, ${a}, ${b})`;
  };
  const setAvatar = (el, user) => {
    el.textContent = initial(user.displayName);
    el.style.background = colorOf(user.username || user.id);
  };
  const fmtTime = (iso) => new Date(iso).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
  const dayKey = (iso) => new Date(iso).toDateString();
  const fmtDay = (iso) => {
    const d = new Date(iso), t = new Date();
    if (d.toDateString() === t.toDateString()) return "Hôm nay";
    t.setDate(t.getDate() - 1);
    if (d.toDateString() === t.toDateString()) return "Hôm qua";
    return d.toLocaleDateString("vi-VN");
  };
  const fmtListTime = (iso) =>
    new Date(iso).toDateString() === new Date().toDateString()
      ? fmtTime(iso)
      : new Date(iso).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" });
  const fmtSize = (b) => (b < 1024 * 1024 ? Math.max(1, Math.round(b / 1024)) + " KB" : (b / 1024 / 1024).toFixed(1) + " MB");

  function toast(msg, type = "") {
    const el = document.createElement("div");
    el.className = "toast " + type;
    el.textContent = msg;
    $("#toasts").appendChild(el);
    setTimeout(() => el.remove(), 3700);
  }

  function ripple(e) {
    const btn = e.currentTarget;
    const r = btn.getBoundingClientRect();
    const s = Math.max(r.width, r.height);
    const span = document.createElement("span");
    span.className = "ripple";
    span.style.cssText = `width:${s}px;height:${s}px;left:${e.clientX - r.left - s / 2}px;top:${e.clientY - r.top - s / 2}px`;
    btn.appendChild(span);
    setTimeout(() => span.remove(), 600);
  }
  $$(".btn").forEach((b) => b.addEventListener("click", ripple));

  /* ---------- Chuyển màn hình ---------- */
  function showAuth() {
    clearInterval(state.timer);
    clearInterval(state.sideTimer);
    $("#app").classList.add("hidden");
    $("#auth").classList.remove("hidden");
    document.title = "ChatWave – Ứng dụng nhắn tin";
  }

  async function showApp() {
    state.me = Api.getUser();
    $("#auth").classList.add("hidden");
    $("#app").classList.remove("hidden");
    $("#meName").textContent = state.me.displayName;
    setAvatar($("#meAvatar"), state.me);

    Object.assign(state, { friends: [], friendIds: new Set(), requestsIn: [], requestsOut: [], sideLoaded: false, sideSig: "", tab: "chats", rooms: [] });
    $$("#sideTabs .side-tab").forEach((b) => b.classList.toggle("active", b.dataset.tab === "chats"));
    clearSearch();
    await refreshSide({ withSent: true, silent: false, animate: true });
    clearInterval(state.sideTimer);
    state.sideTimer = setInterval(() => refreshSide(), CFG.POLL_MS);
  }

  /* ---------- Tabs đăng nhập / đăng ký ---------- */
  $$(".tab").forEach((tab) =>
    tab.addEventListener("click", () => {
      const reg = tab.dataset.tab === "register";
      $$(".tab").forEach((t) => t.classList.toggle("active", t === tab));
      $(".tabs").classList.toggle("reg", reg);
      showForm(reg ? "register" : "login");
    })
  );

  function showForm(name) {
    $("#loginForm").classList.toggle("hidden", name !== "login");
    $("#registerForm").classList.toggle("hidden", name !== "register");
    $("#forgotForm").classList.toggle("hidden", name !== "forgot");
    $(".tabs").classList.toggle("hidden", name === "forgot");
    $("#authError").textContent = "";
    $("#authError").classList.remove("auth-ok");
  }

  function authError(msg) {
    const el = $("#authError");
    el.textContent = msg;
    el.classList.remove("shake");
    void el.offsetWidth;
    el.classList.add("shake");
  }
  const setLoading = (form, on) => form.querySelector(".btn").classList.toggle("loading", on);

  $("#loginForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    $("#authError").textContent = "";
    setLoading(e.target, true);
    try {
      await Api.login($("#loginUser").value.trim(), $("#loginPass").value);
      await showApp();
      toast("Đăng nhập thành công 🎉", "ok");
    } catch (err) {
      authError(err.message);
    } finally {
      setLoading(e.target, false);
    }
  });

  /* ---------- Quên mật khẩu ---------- */
  const fg = { step: 1 };
  function fgReset() {
    fg.step = 1;
    $("#forgotForm").reset();
    $("#fgStep2").classList.add("hidden");
    $("#fgEmail").readOnly = false;
    $("#fgResend").hidden = true;
    $("#fgBtnText").textContent = "Gửi mã xác nhận";
    $("#forgotNote").textContent = "Nhập email đã dùng khi đăng ký, chúng tôi sẽ gửi mã xác nhận 6 số.";
    document.querySelectorAll(".demo-code").forEach((n) => n.remove());
    ["#fgCode", "#fgPass"].forEach((i) => ($(i).required = false));
  }
  async function fgSendCode(btnForm) {
    const email = $("#fgEmail").value.trim();
    setLoading(btnForm, true);
    try {
      const r = await Api.forgotPassword(email);
      fg.step = 2;
      $("#fgEmail").readOnly = true;
      $("#fgStep2").classList.remove("hidden");
      ["#fgCode", "#fgPass"].forEach((i) => ($(i).required = true));
      $("#fgResend").hidden = false;
      $("#fgBtnText").textContent = "Đổi mật khẩu";
      $("#forgotNote").textContent = "Nếu email đúng, mã xác nhận đã được gửi (hiệu lực 10 phút). Nhập mã và mật khẩu mới.";
      document.querySelectorAll(".demo-code").forEach((n) => n.remove());
      if (r?.demoCode) {
        const d = document.createElement("div");
        d.className = "demo-code";
        d.innerHTML = `Chế độ demo (chưa gửi email thật). Mã của bạn: <b>${r.demoCode}</b>`;
        $("#forgotNote").after(d);
      }
      $("#fgCode").focus();
    } catch (err) {
      authError(err.message);
    } finally {
      setLoading(btnForm, false);
    }
  }
  $("#forgotLink").addEventListener("click", () => {
    fgReset();
    $("#fgEmail").value = "";
    showForm("forgot");
    $("#fgEmail").focus();
  });
  $("#fgBack").addEventListener("click", () => { showForm("login"); $("#loginUser").focus(); });
  $("#fgResend").addEventListener("click", () => { $("#authError").textContent = ""; fgSendCode($("#forgotForm")); });
  $("#forgotForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    $("#authError").textContent = "";
    if (fg.step === 1) return fgSendCode(e.target);
    if ($("#fgPass").value !== $("#fgPass2").value) return authError("Mật khẩu nhập lại không khớp");
    setLoading(e.target, true);
    try {
      await Api.resetPassword($("#fgEmail").value.trim(), $("#fgCode").value.trim(), $("#fgPass").value);
      toast("Đã đổi mật khẩu, hãy đăng nhập lại!", "ok");
      fgReset();
      showForm("login");
      $("#loginPass").value = "";
      $("#loginUser").focus();
    } catch (err) {
      authError(err.message);
    } finally {
      setLoading(e.target, false);
    }
  });

  if (window.CONFIG.DEMO_MODE) $('#sideTabs .side-tab[data-tab="groups"]').classList.add("hidden");

  /* ---------- Chạy thật: ẩn các chức năng backend chưa hỗ trợ ---------- */
  if (!window.CONFIG.DEMO_MODE) {
    const F = window.CONFIG.FEATURES || {};
    if (!F.forgotPassword) $("#forgotLink").classList.add("hidden");
    if (!F.displayName) { const n = $("#regName"); n.required = false; n.closest(".field").classList.add("hidden"); }
    $("#searchInput").placeholder = "Nhập đúng tên đăng nhập để tìm";
    $("#fileInput").accept = "image/*";
  }

  $("#registerForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    $("#authError").textContent = "";
    if ($("#regPass").value !== $("#regPass2").value) return authError("Mật khẩu nhập lại không khớp");
    setLoading(e.target, true);
    try {
      await Api.register($("#regUser").value.trim(), $("#regPass").value, $("#regName").value.trim(), $("#regEmail").value.trim());
      toast("Đăng ký thành công, hãy đăng nhập!", "ok");
      $("#loginUser").value = $("#regUser").value.trim();
      e.target.reset();
      $('.tab[data-tab="login"]').click();
      $("#loginPass").focus();
    } catch (err) {
      authError(err.message);
    } finally {
      setLoading(e.target, false);
    }
  });

  /* ==========================================================
   * THANH BÊN: bạn bè, lời mời, tìm kiếm
   * ========================================================== */
  const sortedFriends = () =>
    [...state.friends].sort(
      (a, b) => new Date(b.last?.at || 0) - new Date(a.last?.at || 0) || a.displayName.localeCompare(b.displayName, "vi")
    );

  const unreadOf = (f) => (state.active && String(state.active.id) === String(f.id) ? 0 : f.unread || 0);

  function updateBadges() {
    const total = state.friends.reduce((n, f) => n + unreadOf(f), 0);
    const req = state.requestsIn.length;
    const bc = $("#badgeChats"), br = $("#badgeReq");
    bc.textContent = total > 99 ? "99+" : total;
    bc.classList.toggle("hidden", !total);
    br.textContent = req > 99 ? "99+" : req;
    br.classList.toggle("hidden", !req);
    document.title = (total + req ? `(${total + req}) ` : "") + "ChatWave – Ứng dụng nhắn tin";
  }

  // Tải lại bạn bè + lời mời (chạy định kỳ). Người khác chấp nhận / gửi lời mời sẽ hiện ra ở đây.
  async function refreshSide({ withSent = false, silent = true, animate = false } = {}) {
    if (!state.me) return;
    try {
      const [friends, incoming, rooms] = await Promise.all([Api.getFriends(), Api.getRequests("received"), Api.getRooms().catch(() => state.rooms || [])]);
      const outgoing = withSent || state.tab === "requests" ? await Api.getRequests("sent") : state.requestsOut;
      if (!state.me) return; // đã đăng xuất trong lúc chờ

      if (state.sideLoaded) {
        friends.filter((f) => !state.friendIds.has(String(f.id)))
          .forEach((f) => toast(`Bạn và ${f.displayName} đã trở thành bạn bè 🎉`, "ok"));
        const old = new Set(state.requestsIn.map((r) => String(r.id)));
        incoming.filter((r) => !old.has(String(r.id)))
          .forEach((r) => toast(`${r.user.displayName} đã gửi lời mời kết bạn cho bạn`));
      }
      state.friends = friends;
      state.rooms = rooms;
      state.friendIds = new Set(friends.map((f) => String(f.id)));
      state.requestsIn = incoming;
      state.requestsOut = outgoing;
      state.sideLoaded = true;

      if (state.active && !state.active.isRoom && !state.friendIds.has(String(state.active.id))) {
        const name = state.active.displayName;
        closeChat();
        toast(`Bạn không còn là bạn bè với ${name}`, "error");
      }
      renderSide(false, animate);
    } catch (err) {
      if (!silent) toast(err.message, "error");
    }
  }

  function sideSignature() {
    return JSON.stringify([
      state.tab, state.query, state.searching, state.active?.id,
      state.friends.map((f) => [f.id, f.displayName, f.unread, f.last?.at, f.last?.text, f.last?.type]),
      state.requestsIn.map((r) => r.id),
      state.requestsOut.map((r) => r.id),
      state.results.map((r) => [r.id, r.relation, r.requestId]),
      (state.rooms || []).map((r) => [r.id, r.displayName, r.joined]),
    ]);
  }

  // force: vẽ lại dù dữ liệu không đổi; animate: chạy hiệu ứng trượt vào
  function renderSide(force = false, animate = false) {
    updateBadges();
    const sig = sideSignature();
    if (!force && sig === state.sideSig) return;
    state.sideSig = sig;

    const ul = $("#contactList");
    const top = ul.scrollTop;
    ul.innerHTML = "";
    const q = state.query.trim();
    if (q) renderSearch(ul, q, animate);
    else if (state.tab === "groups") renderGroups(ul, animate);
    else if (state.tab === "requests") renderRequests(ul, animate);
    else renderFriends(ul, animate);
    ul.scrollTop = top;
  }

  const secTitle = (text) => h("li", { class: "sec-title" }, text);
  const note = (text) => h("li", { class: "list-empty" }, text);

  function previewOf(f) {
    if (!f.last) return "Chưa có tin nhắn. Hãy gửi lời chào 👋";
    const mine = String(f.last.senderId) === String(state.me.id);
    const t = f.last.type === "IMAGE" ? "🖼️ Hình ảnh" : f.last.type === "VIDEO" ? "🎬 Video" : f.last.type === "AUDIO" ? "🎤 Ghi âm" : f.last.text;
    return (mine ? "Bạn: " : "") + (t || "");
  }

  function friendRow(f, i, animate) {
    const unread = unreadOf(f);
    const av = h("div", { class: "avatar" });
    setAvatar(av, f);
    const li = h("li", {
      class: "contact" + (animate ? " fresh" : "") + (state.active && String(state.active.id) === String(f.id) ? " active" : "") + (unread ? " unread" : ""),
      style: animate ? `animation-delay:${Math.min(i, 10) * 35}ms` : null,
    },
      av,
      h("div", { class: "c-info" },
        h("div", { class: "c-top" }, h("strong", {}, f.displayName), f.last?.at ? h("time", { class: "c-time" }, fmtListTime(f.last.at)) : null),
        h("div", { class: "c-bot" }, h("small", { class: "c-last" }, previewOf(f)), unread ? h("b", { class: "c-unread" }, unread > 99 ? "99+" : String(unread)) : null)
      )
    );
    li.addEventListener("click", () => openChat(f));
    return li;
  }

  function personRow(u, relation, requestId, animate, subtitle) {
    const av = h("div", { class: "avatar clickable", title: "Xem thông tin tài khoản" });
    setAvatar(av, u);
    av.addEventListener("click", () => showProfile(u, false, relation, requestId));
    return h("li", { class: "contact person" + (animate ? " fresh" : "") },
      av,
      h("div", { class: "c-info" }, h("strong", {}, u.displayName), h("small", { class: "c-last" }, subtitle || "@" + u.username)),
      h("div", { class: "c-actions" }, relationButtons(u, relation, requestId, "mini-btn"))
    );
  }

  function renderFriends(ul, animate) {
    const list = sortedFriends();
    if (!list.length) return ul.append(note("Bạn chưa có bạn bè nào. Hãy gõ tên vào ô tìm kiếm phía trên để tìm và kết bạn nhé 👋"));
    list.forEach((f, i) => ul.append(friendRow(f, i, animate)));
  }

  /* ----- Nhóm chat ----- */
  function roomRow(r, i, animate) {
    const av = h("div", { class: "avatar" });
    setAvatar(av, r);
    const li = h("li", {
      class: "contact" + (animate ? " fresh" : "") + (state.active && String(state.active.id) === String(r.id) ? " active" : ""),
      style: animate ? `animation-delay:${Math.min(i, 10) * 35}ms` : null,
    },
      av,
      h("div", { class: "c-info" },
        h("div", { class: "c-top" }, h("strong", {}, r.displayName)),
        h("div", { class: "c-bot" }, h("small", { class: "c-last" }, r.joined ? "Nhóm chat" : "Chưa tham gia"))
      ),
      r.joined ? null : h("div", { class: "c-actions" },
        h("button", { class: "mini-btn primary", type: "button", onclick: (e) => { e.stopPropagation(); joinGroup(r); } }, "Tham gia"))
    );
    if (r.joined) li.addEventListener("click", () => openChat(r));
    return li;
  }

  function renderGroups(ul, animate) {
    ul.append(h("li", { class: "group-new" },
      h("button", { class: "btn-soft primary", type: "button", onclick: createGroup }, "＋ Tạo nhóm mới")));
    const rooms = state.rooms || [];
    const mine = rooms.filter((r) => r.joined);
    const others = rooms.filter((r) => !r.joined);
    if (!rooms.length) return ul.append(note("Chưa có nhóm nào. Hãy tạo nhóm đầu tiên 👋"));
    if (mine.length) {
      ul.append(secTitle(`Nhóm của tôi (${mine.length})`));
      mine.forEach((r, i) => ul.append(roomRow(r, i, animate)));
    }
    if (others.length) {
      ul.append(secTitle(`Nhóm khác (${others.length})`));
      others.forEach((r, i) => ul.append(roomRow(r, i, animate)));
    }
  }

  async function createGroup() {
    const name = (prompt("Tên nhóm mới:") || "").trim();
    if (!name) return;
    try {
      const room = await Api.createRoom(name);
      toast(`Đã tạo nhóm "${name}"`, "ok");
      await refreshSide();
      openChat(state.rooms.find((r) => String(r.roomId) === String(room.roomId)) || room);
    } catch (err) {
      toast(err.message, "error");
    }
  }

  async function joinGroup(room) {
    try {
      await Api.joinRoom(room.roomId);
      toast(`Đã tham gia nhóm "${room.displayName}"`, "ok");
      await refreshSide();
      openChat(state.rooms.find((r) => String(r.roomId) === String(room.roomId)) || { ...room, joined: true });
    } catch (err) {
      toast(err.message, "error");
    }
  }

  async function showRoom(room) {
    let members = [];
    try { members = await Api.getRoomMembers(room.roomId); } catch (err) { return toast(err.message, "error"); }
    const avatar = h("div", { class: "avatar xl" }, initial(room.displayName));
    avatar.style.background = colorOf(room.username);
    const body = $("#profileBody");
    body.innerHTML = "";
    body.append(
      h("div", { class: "pf-cover" }),
      avatar,
      h("h3", { class: "pf-name" }, room.displayName),
      h("p", { class: "pf-user" }, `${members.length} thành viên`),
      h("div", { class: "pf-list" }, members.map((m) =>
        h("div", { class: "pf-row" }, h("span", {}, "Thành viên"), h("strong", {}, "@" + m + (m === state.me.username ? " (bạn)" : ""))))),
      h("div", { class: "pf-actions" })
    );
    $("#profileModal").classList.remove("hidden");
  }

  function renderRequests(ul, animate) {
    const inc = state.requestsIn, out = state.requestsOut;
    if (!inc.length && !out.length) return ul.append(note("Không có lời mời kết bạn nào."));
    if (inc.length) {
      ul.append(secTitle(`Lời mời đã nhận (${inc.length})`));
      inc.forEach((r) => ul.append(personRow(r.user, "PENDING_RECEIVED", r.id, animate, r.createdAt ? "Gửi lúc " + fmtListTime(r.createdAt) : null)));
    }
    if (out.length) {
      ul.append(secTitle(`Lời mời đã gửi (${out.length})`));
      out.forEach((r) => ul.append(personRow(r.user, "PENDING_SENT", r.id, animate, "Đang chờ chấp nhận")));
    }
  }

  // Khi có từ khóa: bạn bè khớp tên + tài khoản khác tìm được. Chưa gõ thì không hiện tài khoản nào.
  function renderSearch(ul, q, animate) {
    const min = CFG.MIN_SEARCH_CHARS || 2;
    const key = fold(q);
    const mine = sortedFriends().filter((f) => fold(f.displayName).includes(key) || fold(f.username).includes(key));
    if (mine.length) {
      ul.append(secTitle("Bạn bè"));
      mine.forEach((f, i) => ul.append(friendRow(f, i, animate)));
    }
    if (q.length < min) {
      if (!mine.length) ul.append(note(`Nhập ít nhất ${min} ký tự để tìm tài khoản mới`));
      return;
    }
    ul.append(secTitle("Tài khoản khác"));
    if (state.searching) return ul.append(note("Đang tìm..."));
    const others = state.results.filter((r) => r.relation !== "FRIEND");
    if (!others.length) ul.append(note(`Không tìm thấy tài khoản nào khớp với “${q}”`));
    else others.forEach((u) => ul.append(personRow(u, u.relation, u.requestId, animate)));
  }

  /* ----- Nút theo quan hệ: Kết bạn / Hủy lời mời / Chấp nhận / Từ chối / Nhắn tin ----- */
  function relationButtons(u, relation, requestId, base) {
    const btn = (label, cls, fn) =>
      h("button", { class: `${base} ${cls}`, type: "button", onclick: (e) => { e.stopPropagation(); fn(); } }, label);
    switch (relation) {
      case "FRIEND":
        return [btn("Nhắn tin", "primary", () => {
          closeModals();
          const f = state.friends.find((x) => String(x.id) === String(u.id)) || u;
          clearSearch();
          renderSide(true);
          openChat(f);
        })];
      case "PENDING_SENT":
        return [btn("Hủy lời mời", "ghost", () => doSocial(() => Api.cancelRequest(requestId), "Đã hủy lời mời"))];
      case "PENDING_RECEIVED":
        return [
          btn("Chấp nhận", "primary", () => doSocial(() => Api.acceptRequest(requestId), `Bạn và ${u.displayName} đã trở thành bạn bè 🎉`)),
          btn("Từ chối", "ghost", () => doSocial(() => Api.declineRequest(requestId), "Đã từ chối lời mời")),
        ];
      default:
        return [btn("Kết bạn", "primary", () => doSocial(() => Api.sendFriendRequest(u.id), `Đã gửi lời mời kết bạn tới ${u.displayName}`))];
    }
  }

  async function doSocial(fn, okMsg) {
    closeModals();
    try {
      await fn();
      if (okMsg) toast(okMsg, "ok");
    } catch (err) {
      toast(err.message, "error");
    }
    await Promise.all([state.query.trim().length >= (CFG.MIN_SEARCH_CHARS || 2) ? runSearch() : null, refreshSide({ withSent: true })]);
  }

  async function unfriend(u) {
    if (!confirm(`Hủy kết bạn với "${u.displayName}"? Hai bạn sẽ không nhắn tin được cho nhau nữa.`)) return;
    try {
      await Api.unfriend(u.id);
    } catch (err) {
      return toast(err.message, "error");
    }
    closeModals();
    if (state.active && String(state.active.id) === String(u.id)) closeChat();
    toast("Đã hủy kết bạn", "ok");
    await refreshSide({ withSent: true });
  }

  /* ----- Ô tìm kiếm ----- */
  function clearSearch() {
    $("#searchInput").value = "";
    state.query = "";
    state.results = [];
    state.searching = false;
    state.searchSeq++;
    clearTimeout(state.searchTimer);
    $("#searchClear").classList.add("hidden");
  }

  async function runSearch() {
    const q = state.query.trim();
    if (q.length < (CFG.MIN_SEARCH_CHARS || 2)) return;
    const seq = ++state.searchSeq;
    try {
      const res = await Api.searchUsers(q);
      if (seq !== state.searchSeq) return; // đã gõ tiếp, bỏ kết quả cũ
      state.results = res;
    } catch (err) {
      if (seq !== state.searchSeq) return;
      state.results = [];
      toast(err.message, "error");
    }
    state.searching = false;
    renderSide(true);
  }

  $("#searchInput").addEventListener("input", () => {
    state.query = $("#searchInput").value;
    $("#searchClear").classList.toggle("hidden", !state.query);
    clearTimeout(state.searchTimer);
    state.searchSeq++;
    state.results = [];
    const enough = state.query.trim().length >= (CFG.MIN_SEARCH_CHARS || 2);
    state.searching = enough;
    renderSide(true);
    if (enough) state.searchTimer = setTimeout(runSearch, 350);
  });
  $("#searchClear").addEventListener("click", () => { clearSearch(); renderSide(true); $("#searchInput").focus(); });

  $$("#sideTabs .side-tab").forEach((b) =>
    b.addEventListener("click", () => {
      state.tab = b.dataset.tab;
      $$("#sideTabs .side-tab").forEach((x) => x.classList.toggle("active", x === b));
      clearSearch();
      renderSide(true, true);
      if (state.tab === "requests") refreshSide({ withSent: true, animate: true });
      else if (state.tab === "groups") refreshSide({ animate: true });
    })
  );

  /* ==========================================================
   * CUỘC TRÒ CHUYỆN
   * ========================================================== */
  async function openChat(user) {
    if (!user.isRoom && !state.friendIds.has(String(user.id))) return toast("Hai bạn chưa là bạn bè nên chưa thể nhắn tin", "error");
    state.active = user;
    state.rendered.clear();
    state.els.clear();
    state.media.clear();
    state.mediaFilter = "ALL";
    state.lastDay = "";
    state.stick = true;
    state.needRead = false;
    clearAttachment();
    closeModals();
    $("#messages").innerHTML = "";
    $("#emptyState").classList.add("hidden");
    $("#chatBox").classList.remove("hidden");
    $("#chatName").textContent = user.displayName;
    $("#chatStatus").textContent = user.isRoom ? "Nhóm chat" : "@" + user.username;
    if (user.isRoom) {
      Api.getRoomMembers(user.roomId)
        .then((m) => { if (state.active === user) $("#chatStatus").textContent = `Nhóm chat · ${m.length} thành viên`; })
        .catch(() => {});
    }
    setAvatar($("#chatAvatar"), user);
    $("#app").classList.add("has-chat", "chat-open");
    if (window.innerWidth <= 800) applyInfo(false, false); // điện thoại: vào chat thì hiện tin nhắn trước
    renderSide(true);
    renderInfo();

    await fetchMessages(true);
    $("#msgInput").focus();

    clearInterval(state.timer);
    state.timer = setInterval(() => fetchMessages(false), CFG.POLL_MS);
  }

  function closeChat() {
    clearInterval(state.timer);
    state.active = null;
    state.rendered.clear();
    state.els.clear();
    state.media.clear();
    state.needRead = false;
    clearAttachment();
    closeModals();
    $("#chatBox").classList.add("hidden");
    $("#emptyState").classList.remove("hidden");
    $("#app").classList.remove("chat-open", "has-chat");
    renderSide(true);
  }

  async function fetchMessages(first) {
    const target = state.active;
    if (!target) return;
    if (!first && state.inflight > 0) return; // đang gửi: đợi gửi xong rồi mới tự tải
    try {
      const msgs = await Api.getMessages(target.id);
      if (!state.active || state.active.id !== target.id) return; // đã chuyển người khác
      let added = false;
      msgs.forEach((m) => {
        const key = String(m.id);
        if (state.rendered.has(key)) {
          // tin cũ: chỉ cập nhật trạng thái "đã xem"
          const el = state.els.get(key);
          if (el) { el.dataset.seen = m.seen ? "1" : "0"; el.dataset.seenAt = m.readAt || ""; }
          return;
        }
        addMessage(m);
        added = true;
      });
      if (msgs.some((m) => String(m.senderId) !== String(state.me.id) && !m.seen)) state.needRead = true;
      if (added) {
        if (first || state.stick) scrollBottom();
        renderInfo();
      }
      updateReceipts();
      if (state.needRead) markReadNow();
    } catch (err) {
      if (first) toast(err.message, "error");
    }
  }

  // Đánh dấu "đã xem" các tin người kia gửi, chỉ khi mình đang thật sự nhìn thấy cửa sổ này
  async function markReadNow() {
    const u = state.active;
    if (!u || document.visibilityState !== "visible") return;
    try {
      await Api.markRead(u.id);
      if (state.active && state.active.id === u.id) state.needRead = false;
      const f = state.friends.find((x) => String(x.id) === String(u.id));
      if (f) f.unread = 0;
      renderSide();
    } catch { /* thử lại ở lần tự tải sau */ }
  }
  document.addEventListener("visibilitychange", () => { if (document.visibilityState === "visible" && state.needRead) markReadNow(); });

  function addMessage(m, pending = false) {
    const box = $("#messages");
    const day = dayKey(m.createdAt);
    if (day !== state.lastDay) {
      const d = document.createElement("div");
      d.className = "day-divider";
      d.textContent = fmtDay(m.createdAt);
      box.appendChild(d);
      state.lastDay = day;
    }
    const mine = String(m.senderId) === String(state.me.id);
    const el = document.createElement("div");
    el.className = "msg " + (mine ? "mine" : "theirs") + (pending ? " pending" : "");
    el.dataset.id = m.id;
    el.dataset.seen = m.seen ? "1" : "0";
    el.dataset.seenAt = m.readAt || "";

    const b = document.createElement("div");
    const type = m.type || "TEXT";
    b.className = "bubble" + (type !== "TEXT" ? " media" : "");

    if (type === "IMAGE" && m.fileUrl) {
      const img = new Image();
      img.className = "chat-img";
      img.alt = m.fileName || "Hình ảnh";
      img.src = m.fileUrl;
      img.addEventListener("load", () => { if (state.stick) scrollBottom(); });
      img.addEventListener("click", () => openLightbox(m.fileUrl, m.fileName, "IMAGE"));
      b.appendChild(img);
    } else if (type === "VIDEO" && m.fileUrl) {
      const v = document.createElement("video");
      v.className = "chat-video";
      v.src = m.fileUrl;
      v.controls = true;
      v.preload = "metadata";
      v.playsInline = true;
      v.addEventListener("loadedmetadata", () => { if (state.stick) scrollBottom(); });
      b.appendChild(v);
    } else if (type === "AUDIO" && m.fileUrl) {
      b.classList.remove("media");
      const au = document.createElement("audio");
      au.className = "chat-audio";
      au.src = m.fileUrl;
      au.controls = true;
      au.preload = "metadata";
      b.appendChild(au);
    } else if (type !== "TEXT") {
      b.classList.remove("media");
      b.textContent = "[Không tải được tệp đính kèm]";
    }

    if (m.content) {
      const p = document.createElement("div");
      if (type !== "TEXT") p.className = "caption";
      p.textContent = m.content; // textContent -> chống XSS
      b.appendChild(p);
    }

    if (state.active?.isRoom && !mine && m.senderId) {
      const sn = document.createElement("div");
      sn.className = "sender-name";
      sn.textContent = m.senderId;
      b.prepend(sn);
    }

    const meta = h("div", { class: "meta" }, h("time", {}, fmtTime(m.createdAt)));
    if (mine) meta.append(h("span", { class: "receipt" }));
    el.append(b, meta);
    box.appendChild(el);

    if (!pending) {
      state.rendered.add(String(m.id));
      state.els.set(String(m.id), el);
      if ((type === "IMAGE" || type === "VIDEO") && m.fileUrl) {
        state.media.set(String(m.id), { id: m.id, type, fileUrl: m.fileUrl, fileName: m.fileName, createdAt: m.createdAt });
      }
    }
    return { el, bubble: b };
  }

  // Chỉ tin cuối cùng mình gửi mới hiện trạng thái (giống Zalo): Đang gửi / Đã gửi / Đã xem
  function updateReceipts() {
    if (state.active?.isRoom) return;
    const box = $("#messages");
    box.querySelectorAll(".receipt").forEach((r) => { r.textContent = ""; r.className = "receipt"; });
    const mine = box.querySelectorAll(".msg.mine");
    const last = mine[mine.length - 1];
    if (!last) return;
    const r = last.querySelector(".receipt");
    if (!r) return;
    if (last.classList.contains("failed")) { r.textContent = "Gửi lỗi"; r.classList.add("err"); }
    else if (last.classList.contains("pending")) r.textContent = "Đang gửi…";
    else if (last.dataset.seen === "1") {
      r.textContent = "✓✓ Đã xem" + (last.dataset.seenAt ? " " + fmtTime(last.dataset.seenAt) : "");
      r.classList.add("seen");
    } else r.textContent = "✓ Đã gửi";
  }

  const scrollBottom = () => {
    const box = $("#messages");
    box.scrollTop = box.scrollHeight;
  };
  $("#messages").addEventListener("scroll", () => {
    const b = $("#messages");
    state.stick = b.scrollHeight - b.scrollTop - b.clientHeight < 140;
  });

  /* ---------- Cột thông tin hội thoại (bên phải) ---------- */
  function applyInfo(open, persist = true) {
    $("#app").classList.toggle("info-open", open);
    if (persist) localStorage.setItem("cw_info", open ? "1" : "0");
  }
  $("#infoBtn").addEventListener("click", () => applyInfo(!$("#app").classList.contains("info-open")));
  $("#infoClose").addEventListener("click", () => applyInfo(false));
  $("#mediaToggle").addEventListener("click", () => $("#mediaSection").classList.toggle("collapsed"));

  // Mặc định mở trên màn hình rộng, đóng trên màn hình hẹp / điện thoại
  (() => {
    const saved = localStorage.getItem("cw_info");
    const open = window.innerWidth <= 800 ? false : saved === null ? window.innerWidth >= 1100 : saved === "1";
    applyInfo(open, false);
  })();

  const sortedMedia = () => [...state.media.values()].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  function mediaThumb(it) {
    const btn = h("button", { class: "m-thumb", type: "button", title: it.fileName || (it.type === "IMAGE" ? "Hình ảnh" : "Video") });
    if (it.type === "IMAGE") {
      const img = new Image();
      img.alt = "";
      img.loading = "lazy";
      img.src = it.fileUrl;
      btn.appendChild(img);
    } else {
      const v = document.createElement("video");
      v.preload = "metadata";
      v.muted = true;
      v.playsInline = true;
      v.src = it.fileUrl + "#t=0.1"; // hiện khung hình đầu tiên làm ảnh bìa
      btn.append(v, h("span", { class: "play" }, "▶"));
    }
    btn.addEventListener("click", () => openLightbox(it.fileUrl, it.fileName, it.type));
    return btn;
  }

  function renderInfo() {
    const u = state.active;
    if (!u) return;
    setAvatar($("#infoAvatar"), u);
    $("#infoName").textContent = u.displayName;
    $("#infoUser").textContent = u.isRoom ? "Nhóm chat" : "@" + u.username;

    const items = sortedMedia();
    $("#mediaCount").textContent = items.length ? `(${items.length})` : "";
    $("#mediaEmpty").classList.toggle("hidden", items.length > 0);
    $("#mediaAllBtn").classList.toggle("hidden", items.length === 0);
    $("#mediaAllBtn").textContent = `Xem tất cả (${items.length})`;
    const grid = $("#mediaGrid");
    grid.innerHTML = "";
    items.slice(0, 6).forEach((it) => grid.appendChild(mediaThumb(it)));

    if (!$("#mediaModal").classList.contains("hidden")) renderMediaModal();
  }

  /* ---------- Thư viện Ảnh/Video (Xem tất cả) ---------- */
  function renderMediaModal() {
    const all = sortedMedia();
    const items = state.mediaFilter === "ALL" ? all : all.filter((x) => x.type === state.mediaFilter);
    $("#mediaModalTitle").textContent = `Ảnh/Video · ${state.active?.displayName || ""}`;
    $$("#mediaTabs .seg-btn").forEach((b) => b.classList.toggle("active", b.dataset.filter === state.mediaFilter));
    const grid = $("#mediaModalGrid");
    grid.innerHTML = "";
    items.forEach((it) => grid.appendChild(mediaThumb(it)));
    $("#mediaModalEmpty").classList.toggle("hidden", items.length > 0);
  }
  function openMediaModal() {
    renderMediaModal();
    $("#mediaModal").classList.remove("hidden");
  }
  $("#mediaAllBtn").addEventListener("click", openMediaModal);
  $("#mediaModalClose").addEventListener("click", () => $("#mediaModal").classList.add("hidden"));
  $$("#mediaTabs .seg-btn").forEach((b) =>
    b.addEventListener("click", () => { state.mediaFilter = b.dataset.filter; renderMediaModal(); })
  );

  /* ---------- Thông tin tài khoản ---------- */
  // relation: FRIEND | NONE | PENDING_SENT | PENDING_RECEIVED (quyết định các nút hiện ra)
  function showProfile(user, isMe, relation = "FRIEND", requestId = null) {
    if (!user) return;
    if (user.isRoom) return showRoom(user);
    const avatar = h("div", { class: "avatar xl" }, initial(user.displayName));
    avatar.style.background = colorOf(user.username || user.id);

    const isActive = state.active && String(state.active.id) === String(user.id);
    const rows = [
      ["Tên hiển thị", user.displayName],
      ["Tên đăng nhập", "@" + user.username],
      ["Vai trò", user.role === "ADMIN" ? "Quản trị viên" : "Người dùng"],
      ["Ngày tham gia", user.createdAt ? new Date(user.createdAt).toLocaleDateString("vi-VN") : "Chưa có thông tin"],
    ];
    if (isMe) {
      rows.push(["Email", user.email || "Chưa có"]);
      rows.push(["Trạng thái", "Đang hoạt động"]);
    }
    else {
      const label = { FRIEND: "Bạn bè", NONE: "Chưa kết bạn", PENDING_SENT: "Đã gửi lời mời", PENDING_RECEIVED: "Đang chờ bạn chấp nhận" }[relation];
      rows.push(["Quan hệ", label]);
      if (relation === "FRIEND" && isActive) rows.push(["Ảnh/Video đã chia sẻ", String(state.media.size)]);
    }

    const actions = h("div", { class: "pf-actions" });
    if (isMe) {
      const form = h("form", { class: "pf-email hidden", autocomplete: "off" });
      const inp = h("input", { type: "email", required: "", placeholder: "Nhập địa chỉ email (ví dụ ten@gmail.com)" });
      inp.value = user.email || "";
      form.append(inp, h("button", { class: "btn-soft primary", type: "submit" }, "Lưu email"));
      form.addEventListener("submit", async (e) => {
        e.preventDefault();
        try {
          state.me = await Api.updateEmail(inp.value.trim());
          toast("Đã lưu email 📧", "ok");
          showProfile(state.me, true);
        } catch (err) {
          toast(err.message, "error");
        }
      });
      if (window.CONFIG.DEMO_MODE || (window.CONFIG.FEATURES || {}).editEmail) actions.append(h("button", {
        class: "btn-soft", type: "button",
        onclick: () => { form.classList.toggle("hidden"); inp.focus(); },
      }, user.email ? "✉️ Đổi email" : "✉️ Thêm email (để lấy lại mật khẩu)"), form);
      actions.append(h("button", {
        class: "btn-soft danger", type: "button",
        onclick: () => confirmLogout(() => { closeModals(); logout(); toast("Đã đăng xuất"); }),
      }, "Đăng xuất"));
    } else if (relation === "FRIEND") {
      if (isActive) actions.append(h("button", { class: "btn-soft", type: "button", onclick: () => { closeModals(); openMediaModal(); } }, "🖼️ Xem ảnh/video đã chia sẻ"));
      else actions.append(...relationButtons(user, "FRIEND", null, "btn-soft"));
      actions.append(h("button", { class: "btn-soft danger", type: "button", onclick: () => unfriend(user) }, "Hủy kết bạn"));
    } else {
      actions.append(...relationButtons(user, relation, requestId, "btn-soft"));
    }

    const body = $("#profileBody");
    body.innerHTML = "";
    body.append(
      h("div", { class: "pf-cover" }),
      avatar,
      h("h3", { class: "pf-name" }, user.displayName),
      h("p", { class: "pf-user" }, "@" + user.username),
      h("div", { class: "pf-list" }, rows.map(([k, v]) => h("div", { class: "pf-row" }, h("span", {}, k), h("strong", {}, v)))),
      actions
    );
    $("#profileModal").classList.remove("hidden");
  }

  // Bấm avatar / tên của mình -> thông tin tài khoản của mình
  $(".me").addEventListener("click", () => showProfile(state.me, true));
  // Bấm avatar / tên người đang chat -> thông tin của họ
  $("#chatAvatar").addEventListener("click", () => showProfile(state.active, false));
  $("#chatTitle").addEventListener("click", () => showProfile(state.active, false));
  $("#infoAvatar").addEventListener("click", () => showProfile(state.active, false));
  $("#profileClose").addEventListener("click", () => $("#profileModal").classList.add("hidden"));

  function closeModals() {
    $("#mediaModal").classList.add("hidden");
    $("#profileModal").classList.add("hidden");
  }
  ["mediaModal", "profileModal"].forEach((id) =>
    $("#" + id).addEventListener("click", (e) => { if (e.target.id === id) $("#" + id).classList.add("hidden"); })
  );

  /* ---------- Tệp đính kèm (ảnh / video) ---------- */
  function setAttachment(file) {
    if (!file) return;
    if (!state.active) return toast("Hãy chọn một người bạn để nhắn tin trước", "error");
    const v = Api.validateFile(file);
    if (v.error) return toast(v.error, "error");

    clearAttachment();
    const url = URL.createObjectURL(file);
    state.attach = { file, kind: v.kind, url };

    const thumb = $("#attachThumb");
    thumb.innerHTML = "";
    let el;
    if (v.kind === "IMAGE") {
      el = new Image();
      el.alt = "";
      el.src = url;
    } else if (v.kind === "AUDIO") {
      el = document.createElement("span");
      el.textContent = "🎤";
      el.style.cssText = "font-size:28px;display:grid;place-items:center;width:100%;height:100%";
    } else {
      el = document.createElement("video");
      el.muted = true;
      el.preload = "metadata";
      el.src = url;
    }
    thumb.appendChild(el);

    $("#attachName").textContent = file.name;
    $("#attachMeta").textContent = (v.kind === "IMAGE" ? "Hình ảnh" : v.kind === "AUDIO" ? "Ghi âm" : "Video") + " · " + fmtSize(file.size);
    $("#attachPreview").classList.remove("hidden");
    $("#msgInput").placeholder = "Thêm chú thích (không bắt buộc)...";
    $("#msgInput").focus();
  }

  function clearAttachment(keepUrl = false) {
    if (state.attach && !keepUrl) URL.revokeObjectURL(state.attach.url);
    state.attach = null;
    $("#attachPreview").classList.add("hidden");
    $("#attachThumb").innerHTML = "";
    $("#msgInput").placeholder = "Nhập tin nhắn...";
    $("#fileInput").value = "";
  }

  $("#attachBtn").addEventListener("click", () => $("#fileInput").click());
  $("#fileInput").addEventListener("change", (e) => setAttachment(e.target.files[0]));

  /* ---------- Ghi âm (bấm 🎤 để bắt đầu, bấm lại để dừng rồi bấm Gửi) ---------- */
  const micBtn = $("#micBtn");
  let rec = null;
  const fmtDur = (ms) => { const s = Math.floor(ms / 1000); return Math.floor(s / 60) + ":" + String(s % 60).padStart(2, "0"); };
  async function toggleRecord() {
    if (rec) { rec.stop(); return; }
    if (!state.active) return toast("Hãy chọn một người bạn để nhắn tin trước", "error");
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) return toast("Trình duyệt không hỗ trợ ghi âm", "error");
    let stream;
    try { stream = await navigator.mediaDevices.getUserMedia({ audio: true }); }
    catch { return toast("Không dùng được micro. Hãy cho phép quyền micro cho trang này.", "error"); }
    const chunks = [];
    const mr = new MediaRecorder(stream);
    const t0 = Date.now();
    const tick = setInterval(() => { micBtn.textContent = "⏹ " + fmtDur(Date.now() - t0); }, 500);
    mr.ondataavailable = (e) => { if (e.data.size) chunks.push(e.data); };
    mr.onstop = () => {
      clearInterval(tick);
      stream.getTracks().forEach((t) => t.stop());
      rec = null;
      micBtn.classList.remove("recording");
      micBtn.textContent = "🎤";
      if (!chunks.length) return;
      const mime = mr.mimeType || "audio/webm";
      const ext = mime.includes("ogg") ? "ogg" : mime.includes("mp4") ? "m4a" : "webm";
      setAttachment(new File(chunks, `ghi-am-${Date.now()}.${ext}`, { type: mime }));
    };
    mr.start();
    rec = mr;
    micBtn.classList.add("recording");
    micBtn.textContent = "⏹ 0:00";
  }
  micBtn.addEventListener("click", toggleRecord);
  $("#attachRemove").addEventListener("click", () => clearAttachment());

  // Dán ảnh từ clipboard (Ctrl+V)
  $("#msgInput").addEventListener("paste", (e) => {
    const f = [...(e.clipboardData?.files || [])].find((x) => x.type.startsWith("image/"));
    if (f) { e.preventDefault(); setAttachment(f); }
  });

  // Kéo thả tệp vào khung chat
  (() => {
    const box = $("#chatBox");
    const overlay = $("#dropOverlay");
    let depth = 0;
    const hasFiles = (e) => Array.from(e.dataTransfer?.types || []).includes("Files");
    box.addEventListener("dragenter", (e) => { if (!hasFiles(e)) return; e.preventDefault(); depth++; overlay.classList.remove("hidden"); });
    box.addEventListener("dragover", (e) => { if (hasFiles(e)) e.preventDefault(); });
    box.addEventListener("dragleave", () => { depth = Math.max(0, depth - 1); if (!depth) overlay.classList.add("hidden"); });
    box.addEventListener("drop", (e) => {
      if (!hasFiles(e)) return;
      e.preventDefault();
      depth = 0;
      overlay.classList.add("hidden");
      setAttachment(e.dataTransfer.files[0]);
    });
  })();

  /* ---------- Xem ảnh phóng to / phát video ---------- */
  function openLightbox(url, name, type = "IMAGE") {
    const img = $("#lbImg"), vid = $("#lbVideo");
    if (type === "VIDEO") {
      img.classList.add("hidden");
      img.removeAttribute("src");
      vid.classList.remove("hidden");
      vid.src = url;
      try { const p = vid.play(); if (p && p.catch) p.catch(() => {}); } catch { /* trình duyệt chặn tự phát */ }
    } else {
      vid.classList.add("hidden");
      try { vid.pause(); } catch { /* bỏ qua */ }
      vid.removeAttribute("src");
      img.classList.remove("hidden");
      img.src = url;
    }
    const a = $("#lbDownload");
    a.href = url;
    a.download = name || (type === "VIDEO" ? "video" : "hinh-anh");
    $("#lightbox").classList.remove("hidden");
  }
  function closeLightbox() {
    const vid = $("#lbVideo");
    try { vid.pause(); } catch { /* bỏ qua */ }
    vid.removeAttribute("src");
    $("#lbImg").removeAttribute("src");
    $("#lightbox").classList.add("hidden");
  }
  $("#lbClose").addEventListener("click", closeLightbox);
  $("#lightbox").addEventListener("click", (e) => { if (e.target.id === "lightbox") closeLightbox(); });
  document.addEventListener("keydown", (e) => {
    if (e.key !== "Escape") return;
    if (!$("#lightbox").classList.contains("hidden")) closeLightbox();
    else closeModals();
  });

  /* ---------- Gửi tin nhắn ---------- */
  function flySend() {
    const btn = $("#sendBtn");
    btn.classList.remove("fly");
    void btn.offsetWidth;
    btn.classList.add("fly");
  }

  async function send() {
    const input = $("#msgInput");
    const content = input.value.trim();
    const target = state.active;
    if (!target) return;
    if (!state.attach && !content) return;

    input.value = "";
    $("#emojiPanel").classList.add("hidden");
    flySend();
    state.stick = true;

    if (state.attach) return sendAttachment(target, content);

    const temp = { id: "tmp" + Date.now(), senderId: state.me.id, type: "TEXT", content, createdAt: new Date().toISOString() };
    const { el } = addMessage(temp, true);
    updateReceipts();
    scrollBottom();

    state.inflight++;
    try {
      const saved = await Api.sendMessage(target.id, content);
      el.classList.remove("pending");
      el.dataset.id = saved.id;
      state.rendered.add(String(saved.id));
      state.els.set(String(saved.id), el);
    } catch (err) {
      el.classList.remove("pending");
      el.classList.add("failed");
      toast("Gửi thất bại: " + err.message, "error");
      refreshSide(); // có thể hai bạn vừa hủy kết bạn
    } finally {
      state.inflight--;
      updateReceipts();
    }
    refreshSide();
  }

  async function sendAttachment(target, content) {
    const { file, kind, url } = state.attach;
    clearAttachment(true); // giữ lại url để hiển thị tin đang gửi

    const temp = {
      id: "tmp" + Date.now(), senderId: state.me.id, type: kind, content,
      fileUrl: url, fileName: file.name, createdAt: new Date().toISOString(),
    };
    const { el, bubble } = addMessage(temp, true);
    const bar = document.createElement("div");
    bar.className = "upload-progress";
    bar.innerHTML = "<i></i>";
    bubble.appendChild(bar);
    updateReceipts();
    scrollBottom();

    state.inflight++;
    try {
      const saved = await Api.sendFile(target.id, file, content, (p) => {
        bar.firstChild.style.width = Math.round(p * 100) + "%";
      });
      bar.remove();
      el.classList.remove("pending");
      el.dataset.id = saved.id;
      state.rendered.add(String(saved.id));
      state.els.set(String(saved.id), el);
      if (state.active && state.active.id === target.id && kind !== "AUDIO") {
        state.media.set(String(saved.id), { id: saved.id, type: kind, fileUrl: url, fileName: file.name, createdAt: saved.createdAt || temp.createdAt });
        renderInfo();
      }
    } catch (err) {
      bar.remove();
      el.classList.remove("pending");
      el.classList.add("failed");
      toast("Gửi thất bại: " + err.message, "error");
      refreshSide();
    } finally {
      state.inflight--;
      updateReceipts();
    }
    refreshSide();
  }

  $("#sendBtn").addEventListener("click", send);
  $("#msgInput").addEventListener("keydown", (e) => {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); send(); }
  });

  /* ---------- Emoji ---------- */
  const EMOJIS = "😀 😂 🤣 😊 😍 😘 😎 🤔 😢 😭 😡 👍 👏 🙏 🔥 ❤️ 🎉 💯 ✅ 🚀 💡 🌟 ☕ 🍕".split(" ");
  const panel = $("#emojiPanel");
  EMOJIS.forEach((em) => {
    const b = document.createElement("button");
    b.type = "button";
    b.textContent = em;
    b.addEventListener("click", () => { $("#msgInput").value += em; $("#msgInput").focus(); });
    panel.appendChild(b);
  });
  $("#emojiBtn").addEventListener("click", (e) => { e.stopPropagation(); panel.classList.toggle("hidden"); });
  document.addEventListener("click", (e) => { if (!panel.contains(e.target)) panel.classList.add("hidden"); });

  /* ---------- Nút khác ---------- */
  $("#refreshBtn").addEventListener("click", () => { fetchMessages(false); refreshSide(); toast("Đã tải lại tin nhắn"); });
  $("#backBtn").addEventListener("click", () => { $("#app").classList.remove("chat-open"); });

  $("#themeBtn").addEventListener("click", () => {
    document.body.classList.toggle("dark");
    localStorage.setItem("cw_theme", document.body.classList.contains("dark") ? "dark" : "light");
  });
  if (localStorage.getItem("cw_theme") === "dark") document.body.classList.add("dark");

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
    Api.clearSession();
    clearAttachment();
    closeModals();
    closeLightbox();
    clearInterval(state.timer);
    clearInterval(state.sideTimer);
    clearSearch();
    Object.assign(state, { me: null, active: null, friends: [], friendIds: new Set(), requestsIn: [], requestsOut: [], sideLoaded: false, sideSig: "", rooms: [] });
    state.media.clear();
    state.els.clear();
    $("#chatBox").classList.add("hidden");
    $("#emptyState").classList.remove("hidden");
    $("#app").classList.remove("chat-open", "has-chat");
    showAuth();
  }
  window.addEventListener("session-expired", (e) => {
    if (e.detail === "admin") return;
    logout();
    toast("Phiên đăng nhập đã hết hạn", "error");
  });

  /* ---------- Khởi động ---------- */
  if (Api.getToken() && Api.getUser()) showApp();
  else showAuth();
})();
