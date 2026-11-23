const DEV_SECRETS = new Set(["dev-only-change-me", "change-me-in-production"]);

function toInt(value, fallback) {
    const parsed = Number.parseInt(value ?? "", 10);
    return Number.isFinite(parsed) ? parsed : fallback;
}

function toBool(value, fallback) {
    if (value == null) return fallback;
    return ["1", "true", "yes", "on"].includes(String(value).toLowerCase());
}

export function loadConfig(env = process.env) {
    const isProduction = env.NODE_ENV === "production";
    const jwtSecret = env.JWT_SECRET || "dev-only-change-me";
    const imSecretKey = env.IM_SECRET_KEY || "dev-only-change-me";

    if (isProduction && (DEV_SECRETS.has(jwtSecret) || DEV_SECRETS.has(imSecretKey))) {
        throw new Error("生产环境必须设置 JWT_SECRET 与 IM_SECRET_KEY，不能使用占位值");
    }

    return {
        isProduction,
        port: toInt(env.PORT, 8787),
        jwtSecret,
        jwtTtlSeconds: toInt(env.JWT_TTL_SECONDS, 7 * 24 * 60 * 60),
        imSdkAppId: toInt(env.IM_SDK_APP_ID, 1400000000),
        imSecretKey,
        userSigExpireSeconds: toInt(env.IM_USERSIG_EXPIRE_SECONDS, 7 * 24 * 60 * 60),
        showSmsCode: toBool(env.DEV_SHOW_SMS_CODE, !isProduction),
        corsOrigin: env.CORS_ORIGIN || "*",
    };
}
