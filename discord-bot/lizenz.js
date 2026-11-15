// Lizenz-Modul für den ChunkBug-Addon (discord.js v14, keine zusätzlichen Pakete).
// Einbinden in deinen Bot:   require('./lizenz')(client);   (vor client.login)
//
// Discord (nur Admins):  /lizenz erstellen [tage] [notiz]  |  /lizenz liste  |  /lizenz loeschen <key>  |  /lizenz reset <key>
// Addon fragt an:        GET http://<server>:<port>/lizenz?key=...&uuid=...   ->  {"ok":true} oder {"ok":false,"grund":"..."}
// Ein Key wird beim ersten Benutzen an den Minecraft-Account (UUID) gebunden.

const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { SlashCommandBuilder, PermissionFlagsBits } = require('discord.js');

const FILE = path.join(__dirname, 'lizenzen.json');
const PORT = Number(process.env.LIZENZ_PORT || 3000);

function load() {
    try { return JSON.parse(fs.readFileSync(FILE, 'utf8')); } catch { return {}; }
}
function save(db) { // erst Temp-Datei, dann umbenennen -> Datei geht bei Absturz nicht kaputt
    fs.writeFileSync(FILE + '.tmp', JSON.stringify(db, null, 2));
    fs.renameSync(FILE + '.tmp', FILE);
}
function newKey() { // 96 Bit Zufall, nicht erratbar
    return 'CB-' + crypto.randomBytes(12).toString('hex').toUpperCase().match(/.{4}/g).join('-');
}

/** Prüft einen Key. Bindet ihn beim ersten Mal an die UUID. Gibt {ok, grund, changed} zurück. */
function check(db, key, uuid, now = Date.now()) {
    const l = db[key];
    if (!l) return { ok: false, grund: 'Key ungültig' };
    if (l.bis && now > l.bis) return { ok: false, grund: 'Lizenz abgelaufen' };
    if (!l.uuid) { l.uuid = uuid; return { ok: true, changed: true }; }
    if (l.uuid !== uuid) return { ok: false, grund: 'Key gehört zu einem anderen Minecraft-Account' };
    return { ok: true };
}

const command = new SlashCommandBuilder()
    .setName('lizenz').setDescription('ChunkBug-Addon Lizenzen')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
    .addSubcommand(s => s.setName('erstellen').setDescription('Neuen Lizenz-Key erstellen')
        .addIntegerOption(o => o.setName('tage').setDescription('Gültig für X Tage (leer = für immer)').setMinValue(1))
        .addStringOption(o => o.setName('notiz').setDescription('Für wen ist der Key?')))
    .addSubcommand(s => s.setName('liste').setDescription('Alle Keys anzeigen'))
    .addSubcommand(s => s.setName('loeschen').setDescription('Key löschen (sperrt den Nutzer)')
        .addStringOption(o => o.setName('key').setDescription('Lizenz-Key').setRequired(true)))
    .addSubcommand(s => s.setName('reset').setDescription('Key vom Minecraft-Account lösen (Accountwechsel)')
        .addStringOption(o => o.setName('key').setDescription('Lizenz-Key').setRequired(true)));

async function onCommand(i) {
    const db = load();
    const sub = i.options.getSubcommand();
    if (sub === 'erstellen') {
        const tage = i.options.getInteger('tage');
        const key = newKey();
        db[key] = { notiz: i.options.getString('notiz') || '', bis: tage ? Date.now() + tage * 86400000 : null, uuid: null, von: i.user.id };
        save(db);
        return i.reply({ content: `Neuer Key: \`${key}\`` + (tage ? ` (gültig ${tage} Tage)` : ' (unbegrenzt)'), ephemeral: true });
    }
    if (sub === 'liste') {
        const lines = Object.entries(db).map(([k, l]) =>
            `\`${k}\` ${l.notiz || '-'} | ${l.uuid ? 'gebunden' : 'frei'} | ${l.bis ? 'bis ' + new Date(l.bis).toLocaleDateString('de-DE') : 'unbegrenzt'}`);
        const text = lines.join('\n') || 'Keine Keys.';
        return i.reply({ content: text.length > 1900 ? text.slice(0, 1900) + '\n…' : text, ephemeral: true });
    }
    const key = i.options.getString('key').trim().toUpperCase();
    if (!db[key]) return i.reply({ content: 'Key nicht gefunden.', ephemeral: true });
    if (sub === 'loeschen') delete db[key]; else db[key].uuid = null;
    save(db);
    return i.reply({ content: sub === 'loeschen' ? `Key \`${key}\` gelöscht.` : `Key \`${key}\` ist wieder frei.`, ephemeral: true });
}

function startServer() {
    http.createServer((req, res) => {
        const url = new URL(req.url, 'http://x');
        const reply = (o) => { res.writeHead(200, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(o)); };
        if (url.pathname !== '/lizenz') { res.writeHead(404); return res.end(); }
        const key = (url.searchParams.get('key') || '').trim().toUpperCase();
        const uuid = (url.searchParams.get('uuid') || '').replace(/-/g, '').toLowerCase();
        if (!/^[0-9a-f]{32}$/.test(uuid)) return reply({ ok: false, grund: 'Ungültige Anfrage' });
        if (!key) return reply({ ok: false, grund: 'Kein Key eingetragen' });
        const db = load();
        const r = check(db, key, uuid);
        if (r.changed) save(db);
        reply({ ok: r.ok, grund: r.grund });
    }).listen(PORT, () => console.log(`[Lizenz] Server läuft auf Port ${PORT}`));
}

module.exports = function lizenz(client) {
    client.once('ready', () => client.application.commands.create(command.toJSON())
        .then(() => console.log('[Lizenz] /lizenz registriert'))
        .catch(e => console.error('[Lizenz] Befehl konnte nicht registriert werden:', e)));
    client.on('interactionCreate', (i) => {
        if (i.isChatInputCommand() && i.commandName === 'lizenz') onCommand(i).catch(e => console.error('[Lizenz]', e));
    });
    startServer();
};
module.exports.check = check;
