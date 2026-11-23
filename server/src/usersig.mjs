import { createHmac } from "node:crypto";
import { deflateSync } from "node:zlib";

export const USER_SIG_ALGORITHM = "TLS-SHA256 (HMAC-SHA256 -> zlib -> Base64)";

/**
 * 生成腾讯云 IM UserSig。
 *
 * 签名内容：T{SDKAppID}U{userId}S{secretKey}E{expireAt}
 * 步骤：HMAC-SHA256 -> zlib(deflate) -> Base64
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

    const expireAt = now + expireSeconds;
    const content = `T${sdkAppId}U${userId}S${secretKey}E${expireAt}`;
    const digest = createHmac("sha256", secretKey).update(content).digest();
    const userSig = deflateSync(digest).toString("base64");

    return { userSig, expireAt };
}
