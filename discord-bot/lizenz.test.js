// Test der Lizenz-Prüfung: node lizenz.test.js  (discord.js muss nicht installiert sein)
const Module = require('module');
const orig = Module._load;
const chain = () => new Proxy({}, { get: (t, k) => k === 'toJSON' ? () => ({}) : () => chain() }); // Builder-Attrappe
Module._load = (req, ...a) => req === 'discord.js'
    ? { SlashCommandBuilder: class { constructor() { return chain(); } }, PermissionFlagsBits: {} }
    : orig(req, ...a);
const assert = require('assert');
const { check } = require('./lizenz');

const A = 'a'.repeat(32), B = 'b'.repeat(32);
const db = { K1: { uuid: null, bis: null }, K2: { uuid: null, bis: 1000 } };

assert.deepStrictEqual(check(db, 'NOPE', A), { ok: false, grund: 'Key ungültig' });
assert.deepStrictEqual(check(db, 'K1', A), { ok: true, changed: true }); // erstes Benutzen bindet
assert.strictEqual(db.K1.uuid, A);
assert.deepStrictEqual(check(db, 'K1', A), { ok: true });                // gleicher Account ok
assert.strictEqual(check(db, 'K1', B).ok, false);                        // anderer Account nicht
assert.strictEqual(check(db, 'K2', A, 999).ok, true);                    // noch gültig
assert.strictEqual(check(db, 'K2', A, 1001).grund, 'Lizenz abgelaufen');
console.log('Lizenz-Tests OK');
