import { createHmac, timingSafeEqual } from "node:crypto";

const DEFAULT_ISSUER = "single-chat-bff";

function base64url(value) {
    return Buffer.from(value).toString("base64url");
}

function sign(unsigned, secret) {
    return createHmac("sha256", secret).update(unsigned).digest("base64url");
}

export function signJwt(payload, { secret, ttlSeconds, issuer = DEFAULT_ISSUER, now = Date.now() }) {
    if (!secret) throw new TypeError("secret 不能为空");
    const issuedAt = Math.floor(now / 1000);
    const header = base64url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
    const claims = base64url(
        JSON.stringify({
            iss: issuer,
            iat: issuedAt,
            exp: issuedAt + ttlSeconds,
            ...payload,
        }),
    );
    const unsigned = `${header}.${claims}`;
    return `${unsigned}.${sign(unsigned, secret)}`;
}

export function verifyJwt(token, { secret, issuer = DEFAULT_ISSUER, now = Date.now() }) {
    if (typeof token !== "string") return null;
    const parts = token.split(".");
    if (parts.length !== 3) return null;

    const [headerPart, claimsPart, signaturePart] = parts;
    const unsigned = `${headerPart}.${claimsPart}`;
    const expected = Buffer.from(sign(unsigned, secret));
    const actual = Buffer.from(signaturePart);
    if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) return null;

    let header;
    let claims;
    try {
        header = JSON.parse(Buffer.from(headerPart, "base64url").toString("utf8"));
        claims = JSON.parse(Buffer.from(claimsPart, "base64url").toString("utf8"));
    } catch {
        return null;
    }

    if (header?.alg !== "HS256") return null;
    const nowSeconds = Math.floor(now / 1000);
    if (typeof claims?.exp !== "number" || claims.exp <= nowSeconds) return null;
    if (typeof claims.nbf === "number" && claims.nbf > nowSeconds) return null;
    if (claims.iss !== issuer) return null;

    return claims;
}
