import assert from "node:assert/strict";
import test from "node:test";
import { SmsCodeStore } from "../src/store.mjs";

function createStore(overrides = {}) {
    let clock = 1_000_000;
    const store = new SmsCodeStore({
        now: () => clock,
        ttlMs: 1_000,
        cooldownMs: 500,
        maxAttempts: 3,
        codeFactory: () => "123456",
        ...overrides,
    });
    return {
        store,
        advance(ms) {
            clock += ms;
        },
    };
}

test("issues a code and enforces resend cooldown", () => {
    const { store, advance } = createStore();

    const first = store.request("13800138000");
    assert.equal(first.ok, true);
    assert.equal(first.code, "123456");

    const blocked = store.request("13800138000");
    assert.equal(blocked.ok, false);
    assert.equal(blocked.reason, "SMS_COOLDOWN");

    advance(501);
    assert.equal(store.request("13800138000").ok, true);
});

test("accepts the correct code exactly once", () => {
    const { store } = createStore();
    store.request("13800138000");

    assert.deepEqual(store.verify("13800138000", "123456"), { ok: true });
    assert.equal(store.verify("13800138000", "123456").reason, "CODE_EXPIRED");
});

test("expires a code after its ttl", () => {
    const { store, advance } = createStore();
    store.request("13800138000");
    advance(1_001);

    assert.equal(store.verify("13800138000", "123456").reason, "CODE_EXPIRED");
});

test("limits failed attempts", () => {
    const { store } = createStore();
    store.request("13800138000");

    assert.equal(store.verify("13800138000", "000000").reason, "INVALID_CODE");
    assert.equal(store.verify("13800138000", "000000").reason, "INVALID_CODE");
    assert.equal(store.verify("13800138000", "000000").reason, "INVALID_CODE");
    assert.equal(store.verify("13800138000", "123456").reason, "TOO_MANY_ATTEMPTS");
});
