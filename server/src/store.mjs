export class SmsCodeStore {
    constructor({
        now = () => Date.now(),
        ttlMs = 5 * 60 * 1000,
        cooldownMs = 60 * 1000,
        maxAttempts = 5,
        codeFactory = () => String(Math.floor(100000 + Math.random() * 900000)),
    } = {}) {
        this.now = now;
        this.ttlMs = ttlMs;
        this.cooldownMs = cooldownMs;
        this.maxAttempts = maxAttempts;
        this.codeFactory = codeFactory;
        this.entries = new Map();
    }

    request(phone) {
        const current = this.now();
        const existing = this.entries.get(phone);
        if (existing && current - existing.sentAt < this.cooldownMs) {
            return {
                ok: false,
                reason: "SMS_COOLDOWN",
                retryAfterSeconds: Math.ceil((this.cooldownMs - (current - existing.sentAt)) / 1000),
            };
        }

        const code = this.codeFactory();
        this.entries.set(phone, { code, sentAt: current, attempts: 0 });
        return {
            ok: true,
            code,
            cooldownSeconds: Math.ceil(this.cooldownMs / 1000),
        };
    }

    verify(phone, code) {
        const entry = this.entries.get(phone);
        if (!entry) return { ok: false, reason: "CODE_EXPIRED" };

        if (this.now() - entry.sentAt > this.ttlMs) {
            this.entries.delete(phone);
            return { ok: false, reason: "CODE_EXPIRED" };
        }

        if (entry.attempts >= this.maxAttempts) {
            this.entries.delete(phone);
            return { ok: false, reason: "TOO_MANY_ATTEMPTS" };
        }

        entry.attempts += 1;
        if (entry.code !== String(code)) {
            return {
                ok: false,
                reason: "INVALID_CODE",
                remainingAttempts: this.maxAttempts - entry.attempts,
            };
        }

        this.entries.delete(phone);
        return { ok: true };
    }
}
