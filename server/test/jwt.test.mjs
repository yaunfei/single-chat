import assert from "node:assert/strict";
import test from "node:test";
import { signJwt, verifyJwt } from "../src/jwt.mjs";

const baseOptions = {
    secret: "unit-test-secret",
    ttlSeconds: 60,
    now: 1_700_000_000_000,
};

test("signs and verifies a token", () => {
    const token = signJwt({ sub: "u_1", phone: "13800138000" }, baseOptions);
    const claims = verifyJwt(token, baseOptions);

    assert.ok(claims);
    assert.equal(claims.sub, "u_1");
    assert.equal(claims.phone, "13800138000");
    assert.equal(claims.exp, Math.floor(baseOptions.now / 1000) + 60);
});

test("rejects a tampered payload", () => {
    const token = signJwt({ sub: "u_1" }, baseOptions);
    const [header, , signature] = token.split(".");
    const tamperedClaims = Buffer.from(
        JSON.stringify({ sub: "u_2", iss: "single-chat-bff", exp: 9_999_999_999 }),
    ).toString("base64url");

    assert.equal(verifyJwt(`${header}.${tamperedClaims}.${signature}`, baseOptions), null);
});

test("rejects an expired token", () => {
    const token = signJwt({ sub: "u_1" }, baseOptions);
    assert.equal(verifyJwt(token, { ...baseOptions, now: baseOptions.now + 61_000 }), null);
});

test("rejects a token signed with another secret", () => {
    const token = signJwt({ sub: "u_1" }, baseOptions);
    assert.equal(verifyJwt(token, { ...baseOptions, secret: "another-secret" }), null);
});

test("rejects malformed tokens", () => {
    assert.equal(verifyJwt("not-a-jwt", baseOptions), null);
    assert.equal(verifyJwt("a.b", baseOptions), null);
    assert.equal(verifyJwt(null, baseOptions), null);
});
