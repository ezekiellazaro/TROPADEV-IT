'use strict';
/* Uy May Sira! - web page. Talks to the Java server with JSON (fetch). */

// ---------- small helpers ----------
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => 'PHP ' + Number(n).toLocaleString('en-US', { maximumFractionDigits: 2 });

const FACILITY_TYPES = ['Barangay Hall', 'Health Center', 'Day Care Center', 'Public School', 'Barangay Road', 'Bridge', 'Drainage System', 'Street Light', 'Waiting Shed', 'Basketball Court', 'Public Comfort Room', 'Other'];
const DAMAGE_TYPES = ['Flood Damage', 'Structural Damage', 'Electrical Damage', 'Fire Damage', 'Road Damage', 'Water Damage', 'Broken Equipment', 'Other'];
const SEVERITIES = ['LOW', 'MODERATE', 'HIGH', 'CRITICAL'];
const STATUSES = ['REPORTED', 'UNDER REVIEW', 'IN PROGRESS', 'REPAIRED', 'CLOSED'];
const CRITERIA = ['All Fields', 'Report ID', 'Facility Name', 'Barangay', 'Facility Type', 'Severity', 'Status'];
const SUB = 'Uy May Sira! - Facility Damage Reporting and Monitoring System';
const ICON = { home: '🏠', doc: '📄', plus: '➕', search: '🔍', phone: '📞', user: '👤', gear: '⚙️', logout: '🚪', bell: '🔔', clock: '🕒', alert: '⚠️', check: '✅', exclaim: '❗', camera: '📷', chart: '📊', pencil: '✏️', trash: '🗑️', send: '📨', cloud: '☁️', users: '👥' };
const PILLS = {
  CRITICAL: ['#ffdada', '#be1414'], HIGH: ['#ffe8e8', '#d63737'], MODERATE: ['#fff0d2', '#d67d0a'], LOW: ['#def6e8', '#1c8c50'],
  REPORTED: ['#dcebff', '#1e64d2'], 'UNDER REVIEW': ['#ffeccd', '#cd730a'], 'IN PROGRESS': ['#dcf5e4', '#1c8c50'], REPAIRED: ['#ebe2ff', '#6e46c8'], CLOSED: ['#e5e9ee', '#5a6473']
};
const pill = v => { const c = PILLS[v] || ['#e5e9ee', '#5a6473']; return `<span class="pill" style="background:${c[0]};color:${c[1]}">${esc(v)}</span>`; };

// ---------- state ----------
let me = null, pollTimer = null, clockTimer = null, currentKey = 'DASHBOARD', refreshCurrent = null;
let lastTotal = null, lastCritical = 0, lastHigh = 0;
let soundOn = localStorage.getItem('uymaysira_sound') !== 'off';
let audioCtx = null;

// ---------- server calls ----------
async function api(path, opts = {}) {
  const res = await fetch(path, {
    method: opts.method || 'GET',
    headers: opts.body ? { 'Content-Type': 'application/json' } : {},
    body: opts.body ? JSON.stringify(opts.body) : undefined,
    credentials: 'same-origin'
  });
  let data = null;
  try { data = await res.json(); } catch (e) { data = null; }
  if (!res.ok) {
    if (res.status === 401 && me) { me = null; showLogin(); }
    throw new Error((data && data.error) || 'Request failed (' + res.status + ')');
  }
  return data;
}

// ---------- toast, modal, sound ----------
function toast(msg, type = '') {
  const t = document.createElement('div');
  t.className = 'toast ' + type; t.textContent = msg;
  $('#toast-host').appendChild(t);
  setTimeout(() => t.remove(), type === 'alert' ? 8000 : 4500);
}
function openModal(html, wide = false) {
  const host = $('#modal-host');
  host.innerHTML = `<div class="overlay"><div class="modal ${wide ? 'wide' : ''}">${html}</div></div>`;
  const close = () => { host.innerHTML = ''; document.removeEventListener('keydown', onKey); };
  const onKey = e => { if (e.key === 'Escape') close(); };
  document.addEventListener('keydown', onKey);
  $('.overlay', host).addEventListener('mousedown', e => { if (e.target.classList.contains('overlay')) close(); });
  return { el: $('.modal', host), close };
}
function confirmBox(title, text, okLabel, onOk) {
  const m = openModal(`<h2>${esc(title)}</h2><p>${esc(text)}</p><div class="actions"><button class="btn gray" data-x="no">Cancel</button><button class="btn red" data-x="yes">${esc(okLabel)}</button></div>`);
  $('[data-x=no]', m.el).onclick = m.close;
  $('[data-x=yes]', m.el).onclick = () => { m.close(); onOk(); };
}
function unlockAudio() {
  try { audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)(); audioCtx.resume(); }
  catch (e) { console.warn('Sound not available:', e.message); }
}
/** Siren made with the browser's Web Audio (no sound file). CRITICAL/HIGH = long siren, others = short beep. */
function playAlarm(severity, force = false) {
  if (!soundOn && !force) return;
  try {
    unlockAudio();
    const urgent = severity === 'CRITICAL' || severity === 'HIGH';
    const notes = urgent ? [880, 660, 880, 660, 880, 660] : [880, 1100];
    const len = urgent ? 0.28 : 0.15;
    let t = audioCtx.currentTime;
    notes.forEach(freq => {
      const osc = audioCtx.createOscillator(), gain = audioCtx.createGain();
      osc.type = 'sine'; osc.frequency.value = freq;
      gain.gain.setValueAtTime(0, t); gain.gain.linearRampToValueAtTime(0.4, t + 0.02); gain.gain.linearRampToValueAtTime(0, t + len);
      osc.connect(gain); gain.connect(audioCtx.destination); osc.start(t); osc.stop(t + len); t += len;
    });
  } catch (e) { console.warn('Sound not available:', e.message); }
}

// ---------- pictures drawn with SVG ----------
function sceneSvg() {
  let houses = '';
  for (let i = 0; i < 34; i++) {
    const x = 20 + i * 34, y = 104 + (i * 7) % 9;
    houses += `<rect x="${x}" y="${y}" width="12" height="8" fill="${i % 2 ? '#ebe1d2' : '#e6cdaa'}"/><polygon points="${x - 2},${y} ${x + 6},${y - 6} ${x + 14},${y}" fill="${i % 3 ? '#78463c' : '#be503c'}"/>`;
  }
  return `<svg class="scene" viewBox="0 0 1200 130" preserveAspectRatio="none" aria-hidden="true"><defs><linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#407cba"/><stop offset="1" stop-color="#c4e2f6"/></linearGradient></defs>
  <rect width="1200" height="130" fill="url(#sky)"/><polygon points="0,80 120,45 290,72 440,24 600,68 770,36 940,76 1080,44 1200,78 1200,130 0,130" fill="#56849c"/>
  <polygon points="0,96 220,76 480,94 740,78 1020,96 1200,86 1200,130 0,130" fill="#28644e"/>${houses}<rect y="120" width="1200" height="10" fill="#1e553c"/></svg>`;
}
function hallSvg() {
  return `<svg viewBox="0 0 240 205" aria-hidden="true"><polygon points="0,120 50,80 110,110 170,70 240,105 240,160 0,160" fill="#18467a"/><rect y="140" width="240" height="22" fill="#247858"/>
  <rect x="28" y="62" width="3" height="100" fill="#dce1eb"/><rect x="31" y="62" width="26" height="9" fill="#0038a8"/><rect x="31" y="71" width="26" height="9" fill="#ce1126"/><polygon points="31,62 31,80 43,71" fill="#fff"/>
  <rect x="62" y="108" width="140" height="42" fill="#f5f0e1"/><polygon points="54,108 132,82 210,108" fill="#284682"/><rect x="62" y="98" width="140" height="10" fill="#d2cdbe"/><rect x="92" y="100" width="80" height="14" fill="#fff"/>
  <text x="132" y="110" text-anchor="middle" font-size="9" font-weight="700" fill="#143270">BARANGAY HALL</text>
  <g fill="#5a96d2"><rect x="70" y="124" width="8" height="14"/><rect x="82" y="124" width="8" height="14"/><rect x="94" y="124" width="8" height="14"/><rect x="164" y="124" width="8" height="14"/><rect x="175" y="124" width="8" height="14"/><rect x="186" y="124" width="8" height="14"/></g>
  <rect x="121" y="128" width="22" height="22" fill="#6e4632"/>
  <g transform="rotate(-7 120 172)" fill="#fff" font-style="italic" font-weight="700" font-size="14" text-anchor="middle"><text x="120" y="170">Ligtas na Barangay,</text><text x="120" y="188">Mas Matatag na Bukas!</text></g></svg>`;
}

// ---------- menus and pages ----------
const MENUS = {
  admin: [['DASHBOARD', 'Dashboard', 'home'], ['MANAGE', 'Manage Reports', 'doc'], ['VIEWALL', 'View All Reports', 'doc'], ['SEARCH', 'Search Reports', 'search'], ['CONTACTS', 'Emergency Contacts', 'phone'], ['USERS', 'User Management', 'user'], ['PROFILE', 'Settings', 'gear'], ['LOGOUT', 'Logout', 'logout']],
  reporter: [['DASHBOARD', 'Dashboard', 'home'], ['REPORT_FORM', 'Report Damage', 'plus'], ['VIEWALL', 'View My Reports', 'doc'], ['SEARCH', 'Search Reports', 'search'], ['CONTACTS', 'Emergency Contacts', 'phone'], ['PROFILE', 'My Profile', 'user'], ['LOGOUT', 'Logout', 'logout']]
};
function titles(key) {
  switch (key) {
    case 'DASHBOARD': return [me.title, me.subtitle];
    case 'MANAGE': return ['Manage Reports', SUB];
    case 'VIEWALL': return [me.manage ? 'View All Reports' : 'My Reports', SUB];
    case 'SEARCH': return ['Search Reports', SUB];
    case 'REPORT_FORM': return ['Report Damage', SUB];
    case 'CONTACTS': return ['Emergency Contacts', SUB];
    case 'USERS': return ['User Management', SUB];
    case 'PROFILE': return [me.manage ? 'Settings' : 'My Profile', SUB];
    default: return ['', ''];
  }
}
const PAGES = {
  DASHBOARD: el => (me.manage ? pageAdminDashboard(el) : pageReporterDashboard(el)),
  MANAGE: el => tablePage(el, { title: 'Manage Reports', priority: true, actions: 'manage', statusButtons: true, advanced: true, size: 8 }),
  VIEWALL: el => tablePage(el, { title: me.manage ? 'All Damage Reports' : 'My Reports', priority: true, actions: 'view', size: 8 }),
  SEARCH: el => tablePage(el, { title: 'Search Reports', priority: true, actions: 'view', advanced: true, size: 8 }),
  REPORT_FORM: el => { el.innerHTML = '<section class="card"></section>'; reportForm($('section', el), { onSaved: () => go(me.manage ? 'MANAGE' : 'VIEWALL') }); },
  CONTACTS: pageContacts, USERS: pageUsers, PROFILE: pageProfile
};

async function go(key) {
  if (key === 'LOGOUT') { logout(); return; }
  currentKey = key; refreshCurrent = null;
  $$('.nav-item').forEach(b => b.classList.toggle('active', b.dataset.go === key));
  const t = titles(key);
  $('#banner h1').textContent = t[0]; $('#banner p').textContent = t[1];
  const page = $('#page');
  page.innerHTML = '<div class="empty">Loading...</div>';
  try { await PAGES[key](page); } catch (e) { page.innerHTML = `<div class="card form-error">${esc(e.message)}</div>`; }
  window.scrollTo(0, 0);
}
document.addEventListener('click', e => {
  const t = e.target.closest('[data-go]');
  if (t && !t.classList.contains('nav-item')) go(t.dataset.go);
});

function tablePage(el, opts) {
  el.innerHTML = '<section class="card"></section>';
  const table = reportTable($('section', el), opts);
  refreshCurrent = () => table.load();
  return table.load();
}

// ---------- the reports table (search, filters, badges, buttons, pages) ----------
function reportTable(box, o) {
  const st = { page: 1, q: '', criteria: 'All Fields', severity: 'All', status: 'All Status', selected: null };
  const size = o.size || 5;
  const cols = o.compact ? ['ID', 'Facility', 'Damage', 'Severity', 'Status']
    : ['ID', 'Reporter', 'Facility', 'Facility Type', 'Damage Type', 'Severity'].concat(o.priority ? ['Priority'] : [], ['Estimated Cost', 'Date Reported', 'Status']);
  if (o.actions !== 'none') cols.push('Action');
  const opt = (list, cur) => list.map(v => `<option${v === cur ? ' selected' : ''}>${esc(v)}</option>`).join('');
  const search = '<button class="btn primary" data-a="search">Search</button>';
  let controls;
  if (o.advanced) {
    controls = `<span>Search by:</span><select data-f="criteria">${opt(CRITERIA)}</select><input type="search" data-f="q" placeholder="Search by report ID, facility, or barangay...">
      <span>Severity:</span><select data-f="severity">${opt(['All', 'Low', 'Moderate', 'High', 'Critical'])}</select><select data-f="status">${opt(['All Status'].concat(STATUSES))}</select>${search}<button class="btn gray" data-a="reset">Reset</button>`;
  } else if (o.compact) {
    controls = '<input type="search" data-f="q" placeholder="Search reports...">';
  } else {
    controls = `<input type="search" data-f="q" placeholder="Search by report ID, facility, or barangay..."><select data-f="status">${opt(['All Status'].concat(STATUSES))}</select>${search}`;
  }
  box.innerHTML = `<div class="tbl-head"><h3>${ICON.doc} ${esc(o.title)}</h3><div class="tbl-controls">${controls}</div></div>
    <div class="tbl-wrap"><table><thead><tr>${cols.map(c => `<th>${esc(c === 'Type' ? 'Type' : c)}</th>`).join('')}</tr></thead><tbody></tbody></table></div>
    <div class="tbl-foot"><div class="left">${o.statusButtons ? '<button class="btn green" data-a="status">UPDATE STATUS</button><button class="btn orange" data-a="approve">APPROVE</button>' : ''}<button class="btn gray" data-a="refresh">REFRESH</button><span data-r="info"></span></div><div class="pager" data-r="pager"></div></div>`;

  const cell = (r, c) => {
    switch (c) {
      case 'ID': return `<td class="id">${esc(r.id)}</td>`;
      case 'Reporter': return `<td>${esc(r.reporter)}</td>`;
      case 'Facility': return `<td>${esc(r.facilityName)}</td>`;
      case 'Facility Type': return `<td>${esc(r.facilityType)}</td>`;
      case 'Damage Type': case 'Damage': return `<td>${esc(r.damageType)}</td>`;
      case 'Severity': return `<td>${pill(r.severity)}</td>`;
      case 'Priority': return `<td><b>${esc(r.priority)}</b></td>`;
      case 'Estimated Cost': return `<td>${money(r.cost)}</td>`;
      case 'Date Reported': return `<td>${esc(r.date)}</td>`;
      case 'Status': return `<td>${pill(r.status)}</td>`;
      default: {
        const manage = o.actions === 'manage';
        return `<td class="act"><button class="btn small primary" data-a="view" data-id="${esc(r.id)}">View</button>${manage ? `<button class="btn small green" data-a="edit" data-id="${esc(r.id)}">Edit</button><button class="btn small red" data-a="delete" data-id="${esc(r.id)}">Delete</button>` : ''}</td>`;
      }
    }
  };

  async function load() {
    const qs = new URLSearchParams({ page: st.page, size, q: st.q, criteria: st.criteria, severity: st.severity, status: st.status });
    const d = await api('/api/reports?' + qs);
    st.page = d.page;
    $('tbody', box).innerHTML = d.items.length
      ? d.items.map(r => `<tr data-id="${esc(r.id)}" class="${r.id === st.selected ? 'sel' : ''}">${cols.map(c => cell(r, c)).join('')}</tr>`).join('')
      : `<tr><td class="empty" colspan="${cols.length}">No reports found</td></tr>`;
    const from = (d.page - 1) * d.size;
    $('[data-r=info]', box).textContent = d.total ? `Showing ${from + 1} to ${from + d.items.length} of ${d.total} reports` : 'No reports found';
    const nums = [];
    for (let p = Math.max(1, Math.min(d.page - 2, d.pages - 4)); p <= Math.min(d.pages, Math.max(1, Math.min(d.page - 2, d.pages - 4)) + 4); p++) nums.push(p);
    $('[data-r=pager]', box).innerHTML = `<button data-p="${d.page - 1}" ${d.page > 1 ? '' : 'disabled'}>&lt;</button>${nums.map(p => `<button data-p="${p}" class="${p === d.page ? 'on' : ''}">${p}</button>`).join('')}<button data-p="${d.page + 1}" ${d.page < d.pages ? '' : 'disabled'}>&gt;</button>`;
  }
  const changed = async () => { await load(); if (o.onChange) o.onChange(); };
  const run = fn => async () => { try { await fn(); } catch (e) { toast(e.message, 'error'); } };

  function readFilters() {
    $$('[data-f]', box).forEach(inp => { st[inp.dataset.f] = inp.value; });
    st.page = 1;
  }
  box.addEventListener('keydown', e => { if (e.key === 'Enter' && e.target.matches('[data-f=q]')) { readFilters(); run(load)(); } });
  box.addEventListener('change', e => { if (e.target.matches('select[data-f]')) { readFilters(); run(load)(); } });
  box.addEventListener('click', e => {
    const pg = e.target.closest('[data-p]');
    if (pg && !pg.disabled) { st.page = Number(pg.dataset.p); run(load)(); return; }
    const btn = e.target.closest('[data-a]');
    const row = e.target.closest('tr[data-id]');
    if (!btn) {
      if (row) { st.selected = row.dataset.id; $$('tbody tr', box).forEach(r => r.classList.toggle('sel', r === row)); }
      return;
    }
    const id = btn.dataset.id || st.selected;
    switch (btn.dataset.a) {
      case 'search': readFilters(); run(load)(); break;
      case 'reset': $$('[data-f]', box).forEach(inp => { inp.value = inp.tagName === 'SELECT' ? inp.options[0].value : ''; }); readFilters(); run(load)(); break;
      case 'refresh': run(load)(); break;
      case 'view': run(async () => showDetails(await api('/api/reports/' + id)))(); break;
      case 'edit': run(async () => editReport(await api('/api/reports/' + id), changed))(); break;
      case 'delete': confirmBox('Delete report', `Delete report ${id}? This cannot be undone.`, 'Delete', run(async () => { await api('/api/reports/' + id, { method: 'DELETE' }); toast('Report ' + id + ' was deleted.', 'ok'); st.selected = null; await changed(); })); break;
      case 'status': if (!id) { toast('Please select a report from the table first.'); break; } run(async () => statusDialog(await api('/api/reports/' + id), changed))(); break;
      case 'approve': if (!id) { toast('Please select a report from the table first.'); break; }
        run(async () => { await api('/api/reports/' + id + '/status', { method: 'POST', body: { status: 'UNDER REVIEW' } }); toast('Report ' + id + ' approved and moved to UNDER REVIEW.', 'ok'); await changed(); })(); break;
      default: break;
    }
  });
  return { load };
}

function showDetails(r) {
  const m = openModal(`<h2>Report ${esc(r.id)}</h2><dl class="detail">
    <dt>Reporter</dt><dd>${esc(r.reporter)}</dd><dt>Contact</dt><dd>${esc(r.contact)}</dd><dt>Barangay</dt><dd>${esc(r.barangay)}</dd>
    <dt>Facility</dt><dd>${esc(r.facilityName)} (${esc(r.facilityType)})</dd><dt>Damage type</dt><dd>${esc(r.damageType)}</dd>
    <dt>Severity</dt><dd>${pill(r.severity)} &nbsp;Priority: <b>${esc(r.priority)}</b></dd><dt>Estimated cost</dt><dd>${money(r.cost)}</dd>
    <dt>Date reported</dt><dd>${esc(r.date)}</dd><dt>Status</dt><dd>${pill(r.status)}</dd><dt>Description</dt><dd>${esc(r.description)}</dd></dl>
    ${r.photo ? `<img class="detail-photo" src="${esc(r.photo)}" alt="Photo evidence">` : ''}
    <div class="actions"><button class="btn primary" data-x="ok">Close</button></div>`);
  $('[data-x=ok]', m.el).onclick = m.close;
}
function editReport(r, done) {
  const m = openModal('<div id="edit-box"></div>', true);
  reportForm($('#edit-box', m.el), { report: r, onSaved: () => { m.close(); done(); } });
}
function statusDialog(r, done) {
  const m = openModal(`<h2>Update status - ${esc(r.id)}</h2><p>Current status: ${pill(r.status)}</p>
    <label>New status<select id="new-status">${STATUSES.map(s => `<option${s === r.status ? ' selected' : ''}>${s}</option>`).join('')}</select></label>
    <p class="muted">Status can only move one step forward: REPORTED, UNDER REVIEW, IN PROGRESS, REPAIRED, CLOSED.</p>
    <div class="actions"><button class="btn gray" data-x="no">Cancel</button><button class="btn green" data-x="yes">Update</button></div>`);
  $('[data-x=no]', m.el).onclick = m.close;
  $('[data-x=yes]', m.el).onclick = async () => {
    const status = $('#new-status', m.el).value;
    try { await api('/api/reports/' + r.id + '/status', { method: 'POST', body: { status } }); m.close(); toast(`Status updated: ${r.id} is now ${status}.`, 'ok'); done(); }
    catch (e) { toast(e.message, 'error'); }
  };
}

// ---------- the report form (submit new / edit) with photo upload ----------
function reportForm(box, o = {}) {
  const r = o.report || null;
  const state = { photo: null, remove: false };
  const sel = (id, list, cur) => `<select id="${id}"><option value="">-- Select --</option>${list.map(v => `<option${v === cur ? ' selected' : ''}>${esc(v)}</option>`).join('')}</select>`;
  const field = (label, inner, req = true, cls = '') => `<label class="${cls}">${label}${req ? ' <span class="req">*</span>' : ''}${inner}</label>`;
  const today = new Date().toISOString().slice(0, 10);
  box.innerHTML = `<div class="form-head"><div class="circle">${ICON.plus}</div><div><h2>${r ? 'Edit Facility Damage Report' : 'Report Facility Damage'}</h2><small>${r ? 'Update the details below and save your changes.' : 'Fill out the form below to submit a damage report.'}</small></div><div class="rid">${r ? esc(r.id) : 'ID is automatic'}</div></div>
  <form novalidate><div class="grid3">
    ${field('Reporter Name', `<input id="f-name" value="${esc(r ? r.reporter : (me.manage ? '' : me.name))}" placeholder="Full name" ${me.manage ? '' : 'readonly'}>`)}
    ${field('Contact Number', `<input id="f-contact" value="${esc(r ? r.contact : '')}" placeholder="09XXXXXXXXX">`)}
    ${field('Barangay', `<input id="f-barangay" value="${esc(r ? r.barangay : '')}" placeholder="Barangay">`)}
    ${field('Facility Name', `<input id="f-facility" value="${esc(r ? r.facilityName : '')}" placeholder="Name of the damaged facility">`)}
    ${field('Facility Type', sel('f-ftype', FACILITY_TYPES, r && r.facilityType))}
    ${field('Damage Type', sel('f-dtype', DAMAGE_TYPES, r && r.damageType))}
    ${field('Severity Level', sel('f-severity', SEVERITIES, r && r.severity))}
    ${field('Estimated Damage Cost (PHP)', `<input id="f-cost" value="${esc(r ? r.cost : '')}" placeholder="0.00" inputmode="decimal">`)}
    ${field('Date Reported', `<input value="${esc(r ? r.date : today)}" readonly>`, false)}
    ${field('Damage Description', `<textarea id="f-desc" rows="3" placeholder="Describe what is damaged and how it happened...">${esc(r ? r.description : '')}</textarea>`, true, 'span3')}
  </div>
  <p><b>${ICON.camera} Upload Photo (Optional)</b></p>
  <div class="photo-row"><div class="drop" id="drop"><div style="font-size:30px">${ICON.cloud}</div><b>Click to upload photo</b><span>or drag and drop image here</span><small>Supported formats: JPG, PNG, JPEG | Max size: 5MB</small><input type="file" id="f-file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" hidden></div>
  <div class="preview" id="preview"></div></div>
  <div id="f-err" class="form-error" hidden></div>
  <div class="btn-pair"><button class="btn primary" type="submit" id="f-submit">${ICON.send} ${r ? 'Save Changes' : 'Submit Report'}</button>${r ? '<span></span>' : '<button class="btn light" type="button" id="f-clear">Clear</button>'}</div></form>`;

  const err = msg => { const e = $('#f-err', box); e.textContent = msg; e.hidden = !msg; };
  function preview() {
    const p = $('#preview', box);
    const src = state.photo ? state.photo.data : (r && r.photo && !state.remove ? r.photo : '');
    p.innerHTML = src ? `<img src="${esc(src)}" alt="Photo preview"><button type="button" class="btn small gray" id="f-rm">Remove</button>` : 'No photo selected';
    const rm = $('#f-rm', box);
    if (rm) rm.onclick = () => { state.photo = null; state.remove = true; $('#f-file', box).value = ''; preview(); };
  }
  function setPhoto(file) {
    if (!file) return;
    if (!/\.(jpe?g|png)$/i.test(file.name)) { err('Photo must be a JPG, JPEG or PNG file.'); return; }
    if (file.size > 5 * 1024 * 1024) { err('Photo is too large (maximum size is 5 MB).'); return; }
    err('');
    const reader = new FileReader();
    reader.onload = () => { state.photo = { name: file.name, data: reader.result }; state.remove = false; preview(); };
    reader.readAsDataURL(file);
  }
  const drop = $('#drop', box), fileInput = $('#f-file', box);
  drop.onclick = () => fileInput.click();
  fileInput.onclick = e => e.stopPropagation();
  fileInput.onchange = () => setPhoto(fileInput.files[0]);
  ['dragenter', 'dragover'].forEach(ev => drop.addEventListener(ev, e => { e.preventDefault(); drop.classList.add('over'); }));
  ['dragleave', 'drop'].forEach(ev => drop.addEventListener(ev, e => { e.preventDefault(); drop.classList.remove('over'); }));
  drop.addEventListener('drop', e => setPhoto(e.dataTransfer.files[0]));
  preview();

  const clear = $('#f-clear', box);
  if (clear) clear.onclick = () => {
    $$('input:not([readonly]):not([type=file]), textarea', box).forEach(i => { i.value = ''; });
    $$('select', box).forEach(s => { s.selectedIndex = 0; });
    fileInput.value = ''; state.photo = null; err(''); preview();
  };

  $('form', box).addEventListener('submit', async e => {
    e.preventDefault(); err('');
    const body = {
      reporterName: $('#f-name', box).value, contact: $('#f-contact', box).value, barangay: $('#f-barangay', box).value,
      facilityName: $('#f-facility', box).value, facilityType: $('#f-ftype', box).value, damageType: $('#f-dtype', box).value,
      severity: $('#f-severity', box).value, cost: $('#f-cost', box).value, description: $('#f-desc', box).value,
      photoName: state.photo ? state.photo.name : '', photoData: state.photo ? state.photo.data : '', removePhoto: state.remove ? 'true' : ''
    };
    const btn = $('#f-submit', box); btn.disabled = true;
    try {
      const res = r ? await api('/api/reports/' + r.id, { method: 'PUT', body }) : await api('/api/reports', { method: 'POST', body });
      if (r) toast('Report ' + r.id + ' was updated successfully.', 'ok');
      else { toast(`Report ${res.id} was submitted successfully.\nPriority: ${res.priority}`, 'ok'); playAlarm(res.severity); }
      if (o.onSaved) o.onSaved(res);
    } catch (ex) { err(ex.message); toast(ex.message, 'error'); }
    finally { btn.disabled = false; }
  });
}

// ---------- dashboards ----------
const statCard = (name, glyph, cls, value, extra) => `<div class="stat ${cls}"><div class="circle">${ICON[glyph]}</div><div><div class="name">${name}</div><div class="num">${value}</div></div>${extra == null ? '' : `<div class="new">↑ ${extra} new this week</div>`}</div>`;
const DONUT_COLORS = ['#2178f0', '#f58228', '#28a05a', '#7850d2', '#00a0be', '#8c96a5'];
function donut(items) {
  const total = items.reduce((a, i) => a + i.count, 0), R = 40, C = 2 * Math.PI * R;
  let off = 0;
  const segs = items.map((it, i) => {
    const len = total ? C * it.count / total : 0;
    const s = `<circle r="${R}" cx="60" cy="60" fill="none" stroke="${DONUT_COLORS[i]}" stroke-width="20" stroke-dasharray="${len} ${C - len}" stroke-dashoffset="${-off}" transform="rotate(-90 60 60)"/>`;
    off += len; return s;
  }).join('');
  const svg = `<svg viewBox="0 0 120 120">${total ? segs : `<circle r="${R}" cx="60" cy="60" fill="none" stroke="#e1e8f2" stroke-width="20"/>`}<text x="60" y="58" text-anchor="middle" font-size="20" font-weight="800" fill="#1b2a41">${total}</text><text x="60" y="73" text-anchor="middle" font-size="8" fill="#6b7a90">Total Reports</text></svg>`;
  const legend = items.map((it, i) => `<div><i style="background:${DONUT_COLORS[i]}"></i>${esc(it.label)}<span>${it.count} (${total ? Math.round(100 * it.count / total) : 0}%)</span></div>`).join('');
  return `<div class="donut-wrap">${svg}<div class="legend">${legend}</div></div>`;
}
function bars(values) {
  const labels = ['Low', 'Moderate', 'High', 'Critical'], colors = ['#2ea05a', '#fabe28', '#fa781e', '#eb3c3c'];
  const max = Math.max(4, ...values);
  return `<div class="bars">${values.map((v, i) => `<div class="bar">${v}<div style="height:${Math.round(100 * v / max)}%;background:${colors[i]}"></div></div>`).join('')}</div><div class="bar-labels">${labels.map(l => `<span>${l}</span>`).join('')}</div>`;
}
function activity(list) {
  if (!list.length) return '<p class="muted">No recent activity yet.</p>';
  const kinds = { ADD: ['#22a362', ICON.plus], DELETE: ['#e03131', ICON.trash] };
  return `<div class="activity">${list.map(a => { const k = kinds[a.kind] || ['#1565e0', ICON.pencil]; return `<div class="row"><div class="dot" style="background:${k[0]}">${k[1]}</div><div><b>${esc(a.title)}</b><small>${esc(a.detail)}</small></div><span class="time">${esc(a.time)}</span></div>`; }).join('')}</div>`;
}

async function pageAdminDashboard(el) {
  const s = await api('/api/stats');
  setBell(s.pending);
  el.innerHTML = `<div class="stat-row six">
    ${statCard('Total Reports', 'doc', 'blue', s.total, s.totalNew)}${statCard('Critical Reports', 'exclaim', 'red', s.critical, s.criticalNew)}
    ${statCard('High Priority Reports', 'alert', 'orange', s.high, s.highNew)}${statCard('Under Review', 'search', 'blue', s.review, s.reviewNew)}
    ${statCard('In Progress', 'gear', 'green', s.progress, s.progressNew)}${statCard('Repaired', 'check', 'purple', s.repaired, s.repairedNew)}</div>
  <div class="mid-row">
    <section class="card"><h3>${ICON.doc} Reports by Facility Type</h3>${donut(s.facility)}</section>
    <section class="card"><h3>${ICON.alert} Reports by Severity</h3>${bars(s.severity)}</section>
    <section class="card"><h3>${ICON.clock} Recent Activity</h3>${activity(s.activity)}</section>
    <div class="stack"><section class="card"><h3>${ICON.alert} Quick Actions</h3><div class="quick">
      <button class="btn primary left" data-go="REPORT_FORM">${ICON.plus} Add New Report</button><button class="btn teal left" data-go="VIEWALL">${ICON.doc} View All Reports</button>
      <button class="btn purple left" data-go="USERS">${ICON.users} Manage Users</button><button class="btn orange left" id="gen-report">${ICON.chart} Generate Report</button></div></section>
      <section class="card"><h3>${ICON.gear} System Information</h3><div class="kv"><span>Total Reports</span><b>${s.total}</b></div><div class="kv"><span>Last Saved</span><b>${esc(s.lastSaved)}</b></div>
      <div class="kv"><span>Storage</span><b>${esc(s.storage)}</b></div><div class="kv"><span>System Version</span><b>${esc(s.version)}</b></div></section></div></div>
  <section class="card" id="dash-table"></section>`;
  $('#gen-report').onclick = () => { window.location.href = '/api/export'; };
  const table = reportTable($('#dash-table'), { title: 'All Damage Reports', actions: 'manage', size: 5, onChange: () => go('DASHBOARD') });
  refreshCurrent = () => go('DASHBOARD');
  lastTotal = s.total; lastCritical = s.critical; lastHigh = s.high;
  await table.load();
}

async function pageReporterDashboard(el) {
  const s = await api('/api/stats');
  el.innerHTML = `<div class="stat-row four">${statCard('My Reports', 'doc', 'blue', s.total)}${statCard('Repaired', 'check', 'green', s.repaired)}${statCard('Under Review', 'clock', 'orange', s.review)}${statCard('High Priority', 'exclaim', 'red', s.high)}</div>
  <div class="two-col"><div class="stack"><section class="card" id="dash-form"></section><section class="card"><h3>${ICON.bell} Recent Updates</h3>${activity(s.activity)}</section></div>
  <div class="stack"><section class="card" id="dash-table"></section>
    <section class="card"><h3>${ICON.gear} Quick Access</h3><div class="tiles">
      <button class="tile" style="background:#1565e0" data-go="VIEWALL"><span class="ico">${ICON.doc}</span><b>View All Reports</b><small>Check all damage reports</small></button>
      <button class="tile" style="background:#22a362" data-go="SEARCH"><span class="ico">${ICON.search}</span><b>Search Reports</b><small>Find specific reports</small></button>
      <button class="tile" style="background:#f59e0b" data-go="CONTACTS"><span class="ico">${ICON.phone}</span><b>Emergency Contacts</b><small>View helpful numbers</small></button>
      <button class="tile" style="background:#6f4cc9" data-go="PROFILE"><span class="ico">${ICON.user}</span><b>My Profile</b><small>Account and password</small></button></div></section>
    <div class="quote">${sceneSvg()}<p>“Bawat ulat ay mahalaga.<br>Tayo ang solusyon para sa<br>tatag na barangay.”</p></div></div></div>`;
  reportForm($('#dash-form'), { onSaved: () => go('DASHBOARD') });
  const table = reportTable($('#dash-table'), { title: 'My Reports', compact: true, actions: 'view', size: 3 });
  refreshCurrent = () => table.load();
  await table.load();
}

// ---------- other pages ----------
function pageContacts(el) {
  const rows = [['National Emergency Hotline', '911', 'Police, fire and medical emergencies'], ['Barangay Hall', '(000) 000-0000', 'Replace with your barangay hall number'], ['Barangay Disaster Risk Reduction (BDRRM)', '0900-000-0000', 'Flood, typhoon and earthquake response'], ['Barangay Health Center', '(000) 000-0000', 'Medical assistance'], ['Fire Station', '(000) 000-0000', 'Fire and rescue'], ['Police Station', '(000) 000-0000', 'Peace and order']];
  el.innerHTML = `<section class="card"><h3 style="font-size:18px">${ICON.phone} Emergency Contacts</h3><div class="tbl-wrap"><table><thead><tr><th>Office</th><th>Contact Number</th><th>Notes</th></tr></thead><tbody>${rows.map(r => `<tr style="cursor:default">${r.map(c => `<td>${esc(c)}</td>`).join('')}</tr>`).join('')}</tbody></table></div>
    <p class="muted"><i>Only 911 is a real number. Edit the list in web/app.js (pageContacts) to add your barangay's actual contacts.</i></p></section>`;
}
async function pageUsers(el) {
  const users = await api('/api/users');
  el.innerHTML = `<section class="card"><h3 style="font-size:18px">${ICON.users} User Accounts</h3><div class="tbl-wrap"><table><thead><tr><th>Full Name</th><th>Username</th><th>Role</th></tr></thead><tbody>${users.map(u => `<tr style="cursor:default"><td>${esc(u.name)}</td><td>${esc(u.username)}</td><td>${esc(u.role)}</td></tr>`).join('')}</tbody></table></div>
    <p><button class="btn purple" id="add-user">${ICON.plus} ADD REPORTER ACCOUNT</button> <span class="muted"><i>Accounts are kept in memory and reset when the server restarts.</i></span></p></section>`;
  $('#add-user').onclick = () => {
    const m = openModal(`<h2>Add Reporter Account</h2><label>Full name<input id="u-name"></label><label>Username<input id="u-user"></label><label>Password<input id="u-pass" type="password"></label><label>Confirm password<input id="u-conf" type="password"></label><div id="u-err" class="form-error" hidden></div><div class="actions"><button class="btn gray" data-x="no">Cancel</button><button class="btn purple" data-x="yes">Create</button></div>`);
    $('[data-x=no]', m.el).onclick = m.close;
    $('[data-x=yes]', m.el).onclick = async () => {
      const show = t => { const e = $('#u-err', m.el); e.textContent = t; e.hidden = false; };
      if ($('#u-pass', m.el).value !== $('#u-conf', m.el).value) { show('Password and confirmation do not match.'); return; }
      try { await api('/api/users', { method: 'POST', body: { name: $('#u-name', m.el).value, username: $('#u-user', m.el).value, password: $('#u-pass', m.el).value } }); m.close(); toast('Account created.', 'ok'); go('USERS'); }
      catch (e) { show(e.message); }
    };
  };
}
function pageProfile(el) {
  el.innerHTML = `<section class="card" style="max-width:640px"><h3 style="font-size:18px">${me.manage ? ICON.gear : ICON.user} ${me.manage ? 'Settings' : 'My Profile'}</h3>
    <div class="kv"><span>Name</span><b>${esc(me.name)}</b></div><div class="kv"><span>Username</span><b>${esc(me.username)}</b></div><div class="kv"><span>Role</span><b>${esc(me.role)}</b></div><hr style="border:0;border-top:1px solid var(--border);margin:14px 0">
    <label>Current password<input id="p-cur" type="password"></label><label>New password<input id="p-new" type="password"></label><label>Confirm password<input id="p-conf" type="password"></label><div id="p-err" class="form-error" hidden></div>
    <button class="btn primary" id="p-save">CHANGE PASSWORD</button><hr style="border:0;border-top:1px solid var(--border);margin:14px 0">
    <label class="check" style="color:var(--text);font-weight:700"><input type="checkbox" id="p-sound" ${soundOn ? 'checked' : ''}> Alarm sound for new reports</label><button class="btn orange" id="p-test">TEST SOUND</button></section>`;
  $('#p-sound').onchange = e => { soundOn = e.target.checked; localStorage.setItem('uymaysira_sound', soundOn ? 'on' : 'off'); };
  $('#p-test').onclick = () => playAlarm('CRITICAL', true);
  $('#p-save').onclick = async () => {
    const e = $('#p-err'); e.hidden = true;
    if ($('#p-new').value !== $('#p-conf').value) { e.textContent = 'New password and confirmation do not match.'; e.hidden = false; return; }
    try { await api('/api/password', { method: 'POST', body: { current: $('#p-cur').value, newPassword: $('#p-new').value } }); toast('Password changed (until the server restarts).', 'ok'); go('PROFILE'); }
    catch (ex) { e.textContent = ex.message; e.hidden = false; }
  };
}

// ---------- banner, sidebar, login, polling ----------
function buildSidebar() {
  const items = MENUS[me.manage ? 'admin' : 'reporter'];
  $('#nav').innerHTML = items.map(([key, label, glyph]) => (key === 'LOGOUT' ? '<div class="nav-sep"></div>' : '') + `<button class="nav-item" data-go="${key}"><span class="ico">${ICON[glyph]}</span>${esc(label)}</button>`).join('');
  $('#nav').onclick = e => { const b = e.target.closest('.nav-item'); if (b) go(b.dataset.go); };
  $('#side-hall').innerHTML = hallSvg();
}
function buildBanner() {
  $('#banner').innerHTML = `${sceneSvg()}<div class="banner-text"><h1></h1><p></p></div>
    <div class="banner-cards"><div class="mini-card" id="user-card" style="cursor:pointer"><button class="bell" id="bell" title="Notifications">${ICON.bell}<span class="badge" id="bell-badge" hidden>0</span></button>
      <div class="avatar">${ICON.user}</div><div><b>${esc(me.name)}</b><small>${esc(me.role)}</small></div><span>▾</span>
      <div class="user-menu" id="user-menu" hidden><button data-go="PROFILE">${me.manage ? 'Settings' : 'My Profile'}</button><button id="menu-logout">Logout</button></div></div>
    <div class="mini-card"><span style="font-size:22px">${ICON.clock}</span><div><b id="clock-date"></b><small id="clock-time"></small></div></div></div>`;
  $('#user-card').onclick = e => { if (!e.target.closest('#bell')) $('#user-menu').hidden = !$('#user-menu').hidden; };
  $('#menu-logout').onclick = e => { e.stopPropagation(); $('#user-menu').hidden = true; logout(); };
  $('#user-menu').addEventListener('click', e => { const b = e.target.closest('[data-go]'); if (b) { $('#user-menu').hidden = true; go(b.dataset.go); } });
  document.addEventListener('click', e => { const m = $('#user-menu'); if (m && !e.target.closest('#user-card')) m.hidden = true; });
  $('#bell').onclick = async () => {
    try {
      const s = await api('/api/stats');
      const list = s.activity.length ? s.activity.map(a => `<li><b>${esc(a.title)}</b>: ${esc(a.detail)} <span class="muted">(${esc(a.time)})</span></li>`).join('') : '<li>No recent activity yet.</li>';
      const m = openModal(`<h2>Notifications</h2>${me.manage ? `<p>${s.pending} report(s) waiting for review.</p>` : ''}<ul>${list}</ul><div class="actions"><button class="btn primary" data-x="ok">Close</button></div>`);
      $('[data-x=ok]', m.el).onclick = m.close;
    } catch (e) { toast(e.message, 'error'); }
  };
  updateClock();
}
function setBell(n) { const b = $('#bell-badge'); if (b) { b.textContent = n > 9 ? '9+' : n; b.hidden = !n || !me || !me.manage; } }
function updateClock() {
  const now = new Date();
  const d = $('#clock-date'), t = $('#clock-time');
  if (d) d.textContent = now.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  if (t) t.textContent = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
}

/** ALARM: the admin page asks the server every 5 seconds if a new report arrived. */
async function poll() {
  if (!me || !me.manage) return;
  try {
    const s = await api('/api/stats');
    setBell(s.pending);
    if (lastTotal !== null && s.total > lastTotal) {
      const worst = s.critical > lastCritical ? 'CRITICAL' : (s.high > lastHigh ? 'HIGH' : 'LOW');
      toast('New damage report received!', 'alert');
      playAlarm(worst);
      if (refreshCurrent) refreshCurrent();
    }
    lastTotal = s.total; lastCritical = s.critical; lastHigh = s.high;
  } catch (e) { console.warn('Could not check for new reports:', e.message); }
}
function startTimers() { stopTimers(); pollTimer = setInterval(poll, 5000); clockTimer = setInterval(updateClock, 20000); }
function stopTimers() { clearInterval(pollTimer); clearInterval(clockTimer); lastTotal = null; }

function showLogin() {
  stopTimers();
  $('#app').hidden = true; $('#login-view').hidden = false;
  $('#login-hall').innerHTML = hallSvg();
  $('#login-pass').value = '';
}
function startApp() {
  $('#login-view').hidden = true; $('#app').hidden = false;
  buildSidebar(); buildBanner(); go('DASHBOARD'); startTimers();
}
async function logout() {
  confirmBox('Logout', 'Do you want to log out?', 'Logout', async () => {
    try { await api('/api/logout', { method: 'POST' }); } catch (e) { console.warn(e.message); }
    me = null; showLogin();
  });
}
$('#login-show').onchange = e => { $('#login-pass').type = e.target.checked ? 'text' : 'password'; };
$('#login-form').addEventListener('submit', async e => {
  e.preventDefault();
  const err = $('#login-error'); err.hidden = true;
  unlockAudio();   // browsers allow sound only after a click
  try {
    await api('/api/login', { method: 'POST', body: { username: $('#login-user').value, password: $('#login-pass').value } });
    me = await api('/api/me');
    startApp();
  } catch (ex) { err.textContent = ex.message; err.hidden = false; $('#login-pass').value = ''; }
});
(async function boot() {
  try { me = await api('/api/me'); startApp(); } catch (e) { me = null; showLogin(); }
})();
