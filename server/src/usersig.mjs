import { createHmac } from "node:crypto";
import { deflateSync, inflateSync } from "node:zlib";

export const USER_SIG_ALGORITHM = "TLSSigAPIv2 (HMAC-SHA256 -> JSON deflate -> Base64URL)";

/**
 * 生成腾讯云 IM UserSig，与官方 TLSSigAPIv2 (tls-sig-api-v2) 逐字节一致。
 *
 * 签名内容：`TLS.identifier:<userId>\nTLS.sdkappid:<sdkAppId>\nTLS.time:<now>\nTLS.expire:<expireSeconds>\n`
 * 结构：JSON{TLS.ver,TLS.identifier,TLS.sdkappid,TLS.time,TLS.expire,TLS.sig} -> zlib deflate
 *      -> Base64 -> `+`/`/`/`=` 转义为 `*`/`-`/`_`
 *
 * 密钥只在服务端使用，绝不能下发到客户端。
 */
export function generateUserSig({
    sdkAppId,
    secretKey,
    userId,
    expireSeconds = 7 * 24 * 60 * 60,
    now = Math.floor(Date.now() / 1000),
}) {
    if (!Number.isInteger(sdkAppId) || sdkAppId <= 0) {
        throw new TypeError("sdkAppId 必须是正整数");
    }
    if (!secretKey) throw new TypeError("secretKey 不能为空");
    if (!userId) throw new TypeError("userId 不能为空");
    if (!Number.isInteger(expireSeconds) || expireSeconds <= 0) {
        throw new TypeError("expireSeconds 必须是正整数");
    }

    const identifier = String(userId);
    const sdkappid = Number(sdkAppId);
    const currTime = now;
    const expire = expireSeconds;

    const contentToBeSigned =
        `TLS.identifier:${identifier}\n` +
        `TLS.sdkappid:${sdkappid}\n` +
        `TLS.time:${currTime}\n` +
        `TLS.expire:${expire}\n`;
    const sig = createHmac("sha256", secretKey).update(contentToBeSigned).digest("base64");

    const sigDoc = {
        "TLS.ver": "2.0",
        "TLS.identifier": identifier,
        "TLS.sdkappid": sdkappid,
        "TLS.time": currTime,
        "TLS.expire": expire,
        "TLS.sig": sig,
    };
    const compressed = deflateSync(Buffer.from(JSON.stringify(sigDoc))).toString("base64");
    const userSig = compressed
        .replace(/\+/g, "*")
        .replace(/\//g, "-")
        .replace(/=/g, "_");

    return { userSig, expireAt: currTime + expire };
}

/** 解开 UserSig（仅测试/调试用），返回其中的 JSON 文档。 */
export function decodeUserSig(userSig) {
    const b64 = userSig
        .replace(/\*/g, "+")
        .replace(/-/g, "/")
        .replace(/_/g, "=");
    const padded = b64 + "=".repeat((4 - (b64.length % 4)) % 4);
    return JSON.parse(inflateSync(Buffer.from(padded, "base64")).toString("utf8"));
}
