import { createServer } from "node:http";
import { existsSync } from "node:fs";
import { loadEnvFile } from "node:process";
import { loadConfig } from "./config.mjs";
import { createApp } from "./app.mjs";

const envFile = new URL("../.env", import.meta.url);
if (existsSync(envFile)) {
    try {
        loadEnvFile(envFile);
    } catch (error) {
        console.warn(`[single-chat-bff] 无法读取 .env: ${error.message}`);
    }
}

const config = loadConfig();
const server = createServer(createApp({ config }));

server.listen(config.port, () => {
    console.log(`[single-chat-bff] listening on http://localhost:${config.port}`);
    if (!config.isProduction) {
        console.log("[single-chat-bff] dev mode: SMS 验证码会随响应返回，仅用于本地联调");
    }
});

function shutdown(signal) {
    console.log(`[single-chat-bff] ${signal} received, shutting down`);
    server.close(() => process.exit(0));
    setTimeout(() => process.exit(1), 5_000).unref();
}

process.on("SIGINT", () => shutdown("SIGINT"));
process.on("SIGTERM", () => shutdown("SIGTERM"));
