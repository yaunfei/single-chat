import assert from "node:assert/strict";
import { createHmac } from "node:crypto";
import { createRequire } from "node:module";
import { inflateSync } from "node:zlib";
import test from "node:test";
import { USER_SIG_ALGORITHM, generateUserSig } from "../src/usersig.mjs";

const require = createRequire(import.meta.url);
// 官方参考实现：tls-sig-api-v2@1.0.2（MIT），用于逐字节交叉验证。
const { Api: OfficialApi } = require("../vendor/TLSSigAPIv2.cjs");

const params = {
    sdkAppId: 1400000000,
    secretKey: "unit-test-im-secret",
    userId: "u_1234567890",
    expireSeconds: 3600,
    now: 1_700_000_000,
};

function unpackUserSig(userSig) {
    const b64 = userSig.replace(/\*/g, "+").replace(/-/g, "/").replace(/_/g, "=");
    const padded = b64 + "=".repeat((4 - (b64.length % 4)) % 4);
    return JSON.parse(inflateSync(Buffer.from(padded, "base64")).toString("utf8"));
}

function hmacSig({ identifier, sdkappid, time, expire, secretKey }) {
    const content =
        `TLS.identifier:${identifier}\n` +
        `TLS.sdkappid:${sdkappid}\n` +
        `TLS.time:${time}\n` +
        `TLS.expire:${expire}\n`;
    return createHmac("sha256", secretKey).update(content).digest("base64");
}

test("generates a signature that matches the official TLSSigAPIv2 algorithm", () => {
    const { userSig, expireAt } = generateUserSig(params);

    assert.equal(expireAt, params.now + params.expireSeconds);
    assert.equal(USER_SIG_ALGORITHM, "TLSSigAPIv2 (HMAC-SHA256 -> JSON deflate -> Base64URL)");

    // 输出必须是 base64url 变体字符集（无 + / =）
    assert.match(userSig, /^[A-Za-z0-9*_-]+$/);

    const doc = unpackUserSig(userSig);
    assert.equal(doc["TLS.ver"], "2.0");
    assert.equal(doc["TLS.identifier"], params.userId);
    assert.equal(doc["TLS.sdkappid"], params.sdkAppId);
    assert.equal(doc["TLS.time"], params.now);
    assert.equal(doc["TLS.expire"], params.expireSeconds);
    assert.equal(
        doc["TLS.sig"],
        hmacSig({
            identifier: params.userId,
            sdkappid: params.sdkAppId,
            time: params.now,
            expire: params.expireSeconds,
            secretKey: params.secretKey,
        }),
    );
});

test("official reference sig passes the same verification", () => {
    const official = new OfficialApi(params.sdkAppId, params.secretKey);
    const officialSig = official.genUserSig(params.userId, params.expireSeconds);
    const doc = unpackUserSig(officialSig);
    assert.equal(
        doc["TLS.sig"],
        hmacSig({
            identifier: doc["TLS.identifier"],
            sdkappid: doc["TLS.sdkappid"],
            time: doc["TLS.time"],
            expire: doc["TLS.expire"],
            secretKey: params.secretKey,
        }),
    );
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
