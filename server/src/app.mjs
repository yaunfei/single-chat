import { createHash, randomUUID } from "node:crypto";
import { SmsCodeStore } from "./store.mjs";
import { signJwt, verifyJwt } from "./jwt.mjs";
import { generateUserSig } from "./usersig.mjs";

const PHONE_PATTERN = /^1[3-9]\d{9}$/;
const MAX_BODY_BYTES = 64 * 1024;

const METHOD_ALLOW_LIST = new Map([
    ["/healthz", "GET"],
    ["/api/v1/auth/sms", "POST"],
    ["/api/v1/auth/login", "POST"],
    ["/api/v1/me", "GET"],
    ["/api/v1/im/usersig", "GET"],
    ["/api/v1/push/token", "POST"],
]);

function readJson(req) {
    return new Promise((resolve, reject) => {
        const chunks = [];
        let size = 0;
        let tooLarge = false;

        req.on("data", (chunk) => {
            if (tooLarge) return;
            size += chunk.length;
            if (size > MAX_BODY_BYTES) {
                tooLarge = true;
                chunks.length = 0;
                return;
            }
            chunks.push(chunk);
        });

        req.on("end", () => {
            if (tooLarge) {
                const error = new Error("payload too large");
                error.code = "PAYLOAD_TOO_LARGE";
                reject(error);
                return;
            }
            if (chunks.length === 0) {
                resolve({});
                return;
            }
            try {
                resolve(JSON.parse(Buffer.concat(chunks).toString("utf8")));
            } catch {
                const error = new Error("bad json");
                error.code = "BAD_JSON";
                reject(error);
            }
        });

        req.on("error", reject);
    });
}

function sendJson(res, status, payload) {
    if (res.writableEnded) return;
    const body = JSON.stringify(payload);
    res.writeHead(status, {
        "Content-Type": "application/json; charset=utf-8",
        "Content-Length": Buffer.byteLength(body),
    });
    res.end(body);
}

function sendError(res, status, code, message, requestId, extra = {}) {
    sendJson(res, status, {
        error: {
            code,
            message,
            requestId,
            ...extra,
        },
    });
}

export function createApp({
    config,
    smsStore = new SmsCodeStore(),
    now = () => Date.now(),
}) {
    const usersById = new Map();
    const usersByPhone = new Map();
    const pushTokens = new Map();

    function getOrCreateUser(phone) {
        const existing = usersByPhone.get(phone);
        if (existing) return existing;

        const id = "u_" + createHash("sha256").update(phone).digest("hex").slice(0, 10);
        const user = { id, phone, displayName: "用户" + phone.slice(-4) };
        usersByPhone.set(phone, user);
        usersById.set(id, user);
        return user;
    }

    function authenticate(req) {
        const header = req.headers.authorization ?? "";
        if (!header.startsWith("Bearer ")) return null;
        const token = header.slice(7).trim();
        if (!token) return null;

        const claims = verifyJwt(token, { secret: config.jwtSecret, now: now() });
        if (!claims || typeof claims.sub !== "string") return null;
        return usersById.get(claims.sub) ?? null;
    }

    const routes = new Map();

    routes.set("GET /healthz", async (_req, res) => {
        sendJson(res, 200, {
            status: "ok",
            uptimeSeconds: Math.floor(process.uptime()),
        });
    });

    routes.set("POST /api/v1/auth/sms", async (req, res, requestId) => {
        const body = await readJson(req);
        const phone = typeof body.phone === "string" ? body.phone.trim() : "";
        if (!PHONE_PATTERN.test(phone)) {
            return sendError(res, 400, "INVALID_PHONE", "手机号格式不正确", requestId);
        }

        const result = smsStore.request(phone);
        if (!result.ok) {
            return sendError(res, 429, "SMS_COOLDOWN", "发送过于频繁，请稍后再试", requestId, {
                retryAfterSeconds: result.retryAfterSeconds,
            });
        }

        sendJson(res, 200, {
            sent: true,
            cooldownSeconds: result.cooldownSeconds,
            ...(config.showSmsCode ? { devCode: result.code } : {}),
        });
    });

    routes.set("POST /api/v1/auth/login", async (req, res, requestId) => {
        const body = await readJson(req);
        const phone = typeof body.phone === "string" ? body.phone.trim() : "";
        const code = typeof body.code === "string" ? body.code.trim() : "";

        if (!PHONE_PATTERN.test(phone)) {
            return sendError(res, 400, "INVALID_PHONE", "手机号格式不正确", requestId);
        }

        const result = smsStore.verify(phone, code);
        if (!result.ok) {
            const status = result.reason === "TOO_MANY_ATTEMPTS" ? 429 : 401;
            const messages = {
                CODE_EXPIRED: "验证码已过期，请重新获取",
                INVALID_CODE: "验证码不正确",
                TOO_MANY_ATTEMPTS: "尝试次数过多，请重新获取验证码",
            };
            return sendError(res, status, result.reason, messages[result.reason] ?? "验证码校验失败", requestId);
        }

        const user = getOrCreateUser(phone);
        const token = signJwt(
            { sub: user.id, phone: user.phone, name: user.displayName },
            { secret: config.jwtSecret, ttlSeconds: config.jwtTtlSeconds, now: now() },
        );

        sendJson(res, 200, {
            token,
            expiresIn: config.jwtTtlSeconds,
            user,
        });
    });

    routes.set("GET /api/v1/me", async (req, res, requestId) => {
        const user = authenticate(req);
        if (!user) {
            return sendError(res, 401, "UNAUTHORIZED", "缺少或无效的登录凭证", requestId);
        }
        sendJson(res, 200, { user });
    });

    routes.set("GET /api/v1/im/usersig", async (req, res, requestId) => {
        const user = authenticate(req);
        if (!user) {
            return sendError(res, 401, "UNAUTHORIZED", "缺少或无效的登录凭证", requestId);
        }

        const signature = generateUserSig({
            sdkAppId: config.imSdkAppId,
            secretKey: config.imSecretKey,
            userId: user.id,
            expireSeconds: config.userSigExpireSeconds,
            now: Math.floor(now() / 1000),
        });

        sendJson(res, 200, {
            userId: user.id,
            sdkAppId: config.imSdkAppId,
            ...signature,
        });
    });

    routes.set("POST /api/v1/push/token", async (req, res, requestId) => {
        const user = authenticate(req);
        if (!user) {
            return sendError(res, 401, "UNAUTHORIZED", "缺少或无效的登录凭证", requestId);
        }

        const body = await readJson(req);
        const provider = typeof body.provider === "string" ? body.provider.trim().toLowerCase() : "";
        const token = typeof body.token === "string" ? body.token.trim() : "";
        if (!provider || !token) {
            return sendError(res, 400, "INVALID_PUSH_TOKEN", "provider 与 token 不能为空", requestId);
        }

        const deviceId =
            typeof body.deviceId === "string" && body.deviceId.trim() ? body.deviceId.trim() : "default";
        pushTokens.set(`${user.id}:${deviceId}`, {
            provider,
            token,
            deviceId,
            updatedAt: now(),
        });

        res.writeHead(204);
        res.end();
    });

    return async function handler(req, res) {
        const requestId = randomUUID().slice(0, 8);
        res.setHeader("X-Request-Id", requestId);
        res.setHeader("Access-Control-Allow-Origin", config.corsOrigin);
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.setHeader("Vary", "Origin");

        if (req.method === "OPTIONS") {
            res.writeHead(204);
            res.end();
            return;
        }

        const url = new URL(req.url ?? "/", "http://localhost");
        const path = url.pathname;
        const route = routes.get(`${req.method} ${path}`);

        try {
            if (route) {
                await route(req, res, requestId);
                return;
            }

            const allowedMethod = METHOD_ALLOW_LIST.get(path);
            if (allowedMethod) {
                res.setHeader("Allow", allowedMethod);
                return sendError(res, 405, "METHOD_NOT_ALLOWED", "请求方法不支持", requestId);
            }

            return sendError(res, 404, "NOT_FOUND", "接口不存在", requestId);
        } catch (error) {
            if (error?.code === "BAD_JSON" || error?.code === "PAYLOAD_TOO_LARGE") {
                return sendError(res, 400, "BAD_JSON", "请求体不是合法 JSON 或过大", requestId);
            }
            console.error(`[${requestId}]`, error);
            return sendError(res, 500, "INTERNAL_ERROR", "服务内部错误", requestId);
        }
    };
}
