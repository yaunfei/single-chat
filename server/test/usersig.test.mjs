import assert from "node:assert/strict";
import { createHmac } from "node:crypto";
import { inflateSync } from "node:zlib";
import test from "node:test";
import { USER_SIG_ALGORITHM, generateUserSig } from "../src/usersig.mjs";

const params = {
    sdkAppId: 1400000000,
    secretKey: "unit-test-im-secret",
    userId: "u_1234567890",
    expireSeconds: 3600,
    now: 1_700_000_000,
};

test("generates a signature that matches the documented algorithm", () => {
    const { userSig, expireAt } = generateUserSig(params);

    assert.equal(expireAt, params.now + params.expireSeconds);
    assert.equal(USER_SIG_ALGORITHM, "TLS-SHA256 (HMAC-SHA256 -> zlib -> Base64)");

    const content = `T${params.sdkAppId}U${params.userId}S${params.secretKey}E${expireAt}`;
    const expectedDigest = createHmac("sha256", params.secretKey).update(content).digest();
    const decoded = inflateSync(Buffer.from(userSig, "base64"));

    assert.deepEqual(decoded, expectedDigest);
});

test("is deterministic for the same inputs and timestamp", () => {
    assert.equal(generateUserSig(params).userSig, generateUserSig(params).userSig);
});

test("validates required inputs", () => {
    assert.throws(() => generateUserSig({ ...params, sdkAppId: 0 }), /sdkAppId/);
    assert.throws(() => generateUserSig({ ...params, secretKey: "" }), /secretKey/);
    assert.throws(() => generateUserSig({ ...params, userId: "" }), /userId/);
    assert.throws(() => generateUserSig({ ...params, expireSeconds: 0 }), /expireSeconds/);
});
