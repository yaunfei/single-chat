import assert from "node:assert/strict";
import { createServer } from "node:http";
import { after, before, test } from "node:test";
import { createApp } from "../src/app.mjs";
import { SmsCodeStore } from "../src/store.mjs";

const config = {
    isProduction: false,
    jwtSecret: "test-jwt-secret",
    jwtTtlSeconds: 3600,
    imSdkAppId: 1400000000,
    imSecretKey: "test-im-secret",
    userSigExpireSeconds: 3600,
    showSmsCode: true,
    corsOrigin: "*",
};

let server;
let baseUrl;

before(async () => {
    const smsStore = new SmsCodeStore({ codeFactory: () => "123456" });
    server = createServer(createApp({ config, smsStore }));
    await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
    const address = server.address();
    baseUrl = `http://127.0.0.1:${address.port}`;
});

after(async () => {
    server.closeAllConnections();
    await new Promise((resolve) => server.close(resolve));
});

function post(path, body, token) {
    return fetch(`${baseUrl}${path}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(body),
    });
}

test("health check responds with request id", async () => {
    const response = await fetch(`${baseUrl}/healthz`);
    assert.equal(response.status, 200);
    assert.ok(response.headers.get("x-request-id"));

    const body = await response.json();
    assert.equal(body.status, "ok");
    assert.equal(typeof body.uptimeSeconds, "number");
});

test("runs the full sms -> login -> me -> usersig -> push flow", async () => {
    const phone = "13800138001";

    const smsResponse = await post("/api/v1/auth/sms", { phone });
    assert.equal(smsResponse.status, 200);
    const sms = await smsResponse.json();
    assert.equal(sms.sent, true);
    assert.equal(sms.devCode, "123456");
    assert.equal(sms.cooldownSeconds, 60);

    const cooldownResponse = await post("/api/v1/auth/sms", { phone });
    assert.equal(cooldownResponse.status, 429);
    const cooldown = await cooldownResponse.json();
    assert.equal(cooldown.error.code, "SMS_COOLDOWN");

    const loginResponse = await post("/api/v1/auth/login", {
        phone,
        code: sms.devCode,
    });
    assert.equal(loginResponse.status, 200);
    const login = await loginResponse.json();
    assert.ok(login.token);
    assert.equal(login.user.phone, phone);
    assert.match(login.user.id, /^u_[0-9a-f]{10}$/);

    const authHeaders = { Authorization: `Bearer ${login.token}` };

    const meResponse = await fetch(`${baseUrl}/api/v1/me`, { headers: authHeaders });
    assert.equal(meResponse.status, 200);
    const me = await meResponse.json();
    assert.equal(me.user.id, login.user.id);

    const userSigResponse = await fetch(`${baseUrl}/api/v1/im/usersig`, { headers: authHeaders });
    assert.equal(userSigResponse.status, 200);
    const userSig = await userSigResponse.json();
    assert.equal(userSig.userId, login.user.id);
    assert.equal(userSig.sdkAppId, config.imSdkAppId);
    assert.ok(userSig.userSig.length > 40);
    assert.ok(userSig.expireAt > Math.floor(Date.now() / 1000));

    const pushResponse = await post(
        "/api/v1/push/token",
        { provider: "huawei", token: "push-token", deviceId: "device-1" },
        login.token,
    );
    assert.equal(pushResponse.status, 204);
});

test("rejects a wrong verification code", async () => {
    const phone = "13800138002";
    const smsResponse = await post("/api/v1/auth/sms", { phone });
    assert.equal(smsResponse.status, 200);

    const loginResponse = await post("/api/v1/auth/login", { phone, code: "000000" });
    assert.equal(loginResponse.status, 401);
    const body = await loginResponse.json();
    assert.equal(body.error.code, "INVALID_CODE");
});

test("requires authentication for protected endpoints", async () => {
    for (const path of ["/api/v1/me", "/api/v1/im/usersig"]) {
        const response = await fetch(`${baseUrl}${path}`);
        assert.equal(response.status, 401);
        const body = await response.json();
        assert.equal(body.error.code, "UNAUTHORIZED");
    }
});

test("returns structured errors for unknown routes and wrong methods", async () => {
    const missing = await fetch(`${baseUrl}/api/v1/nope`);
    assert.equal(missing.status, 404);
    assert.equal((await missing.json()).error.code, "NOT_FOUND");

    const wrongMethod = await fetch(`${baseUrl}/healthz`, { method: "POST" });
    assert.equal(wrongMethod.status, 405);
    assert.equal(wrongMethod.headers.get("allow"), "GET");
    assert.equal((await wrongMethod.json()).error.code, "METHOD_NOT_ALLOWED");
});

test("answers cors preflight requests", async () => {
    const response = await fetch(`${baseUrl}/api/v1/auth/sms`, { method: "OPTIONS" });
    assert.equal(response.status, 204);
    assert.equal(response.headers.get("access-control-allow-origin"), "*");
});

test("rejects malformed json bodies", async () => {
    const response = await fetch(`${baseUrl}/api/v1/auth/sms`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: "{",
    });
    assert.equal(response.status, 400);
    assert.equal((await response.json()).error.code, "BAD_JSON");
});
