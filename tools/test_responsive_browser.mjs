/**
 * Zero-NPM browser geometry regression, using the runner's own Chrome.
 * Renders actual plugin CSS and a public table produced by the real PHP
 * builder. All work is CI-only; no browser scripts ship to WordPress.
 *
 * Usage: node tools/test_responsive_browser.mjs PUBLIC.html ADMIN.html OUTPUTDIR
 */
import { spawn } from "node:child_process";
import { mkdtemp, mkdir, readFile, writeFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { setTimeout as sleep } from "node:timers/promises";

const [, , publicArg, adminArg, outputArg] = process.argv;
if (!publicArg || !adminArg || !outputArg) {
    throw new Error("Expected PUBLIC.html ADMIN.html OUTPUTDIR");
}
const fixtures = [
    { label: "public", path: resolve(publicArg) },
    { label: "admin", path: resolve(adminArg) },
];
const output = resolve(outputArg);
await mkdir(output, { recursive: true });
const chrome = process.env.CHROME_BIN || "google-chrome";
const profile = await mkdtemp(join(tmpdir(), "sidrena-browser-"));
const browser = spawn(chrome, [
    "--headless=new", "--no-sandbox", "--disable-gpu", "--hide-scrollbars",
    "--disable-dev-shm-usage", "--no-first-run", "--no-default-browser-check",
    "--allow-file-access-from-files",
    "--remote-debugging-port=0", "--user-data-dir=" + profile,
    "about:blank",
], { stdio: "ignore" });

const widths = [240, 280, 320, 360, 393, 411, 480, 600, 720, 782, 1024, 1280, 1920];
let socket;
try {
    let port;
    for (let attempt = 0; attempt < 300; attempt++) {
        if (browser.exitCode !== null) throw new Error("Chrome exited: " + browser.exitCode);
        try {
            port = Number((await readFile(join(profile, "DevToolsActivePort"), "utf8"))
                .split("\n")[0]);
            if (port > 0) break;
        } catch {
            // Browser has not bound its local development port yet.
        }
        await sleep(100);
    }
    if (!port) throw new Error("Chrome remote debugging port never became ready");

    let target;
    for (let attempt = 0; attempt < 30; attempt++) {
        const pages = await fetch("http://127.0.0.1:" + port + "/json/list")
            .then(r => r.json());
        target = pages.find(x => x.type === "page");
        if (target) break;
        await sleep(100);
    }
    if (!target) throw new Error("Chrome has no page target");
    socket = new WebSocket(target.webSocketDebuggerUrl);
    await new Promise((done, fail) => {
        socket.addEventListener("open", done, { once: true });
        socket.addEventListener("error", fail, { once: true });
    });
    let seq = 0;
    const pending = new Map();
    socket.addEventListener("message", event => {
        let payload;
        try { payload = JSON.parse(event.data); } catch { return; }
        if (!payload.id) return;
        const entry = pending.get(payload.id);
        if (!entry) return;
        pending.delete(payload.id);
        if (payload.error) entry.reject(new Error(JSON.stringify(payload.error)));
        else entry.resolve(payload.result ?? {});
    });
    function cdp(method, params = {}) {
        return new Promise((resolve, reject) => {
            const id = ++seq;
            pending.set(id, { resolve, reject });
            socket.send(JSON.stringify({ id, method, params }));
        });
    }
    await cdp("Page.enable");
    await cdp("Runtime.enable");

    const failures = [];
    const results = [];
    for (const fixture of fixtures) {
        for (const width of widths) {
            const height = width >= 1024 ? 900 : 900;
            await cdp("Emulation.setDeviceMetricsOverride", {
                width, height, deviceScaleFactor: 1, mobile: width <= 782,
            });
            await cdp("Page.navigate", { url: pathToFileURL(fixture.path).href });
            let ready = false;
            for (let count = 0; count < 70; count++) {
                const check = await cdp("Runtime.evaluate", {
                    expression: "document.readyState === 'complete' && document.styleSheets.length >= 1",
                    returnByValue: true,
                });
                if (check.result?.value) { ready = true; break; }
                await sleep(80);
            }
            if (!ready) throw new Error("Failed to load " + fixture.path);

            const expression = `(() => {
                const width = document.documentElement.clientWidth;
                const pageWidth = document.documentElement.scrollWidth;
                const type = ${JSON.stringify(fixture.label)};
                const selector = type === "public"
                    ? "main,.cjenik-hero,.sidrena-cjenik,.cjenik-toolbar,.cjenik-search input,.cjenik-table-wrap,.cjenik-table tbody tr"
                    : ".brendigo-admin-shell,.brendigo-admin-main,.brendigo-admin-topbar,.sidrena-cijena-panels,.sidrena-cijena-card,.sidrena-cijena-catalog-wrap,.brendigo-woo-catalog-tools,.sidrena-android-bridge__outlets";
                const offenders = [];
                for (const element of document.querySelectorAll(selector)) {
                    const rect = element.getBoundingClientRect();
                    if (rect.width <= 0 || rect.height <= 0) continue;
                    if (rect.left < -2 || rect.right > width + 2) {
                        offenders.push({
                            element: element.tagName.toLowerCase() + "." + element.className,
                            left: Math.round(rect.left), right: Math.round(rect.right)
                        });
                    }
                }
                const wrap = document.querySelector(type === "public"
                    ? ".cjenik-table-wrap" : ".sidrena-cijena-catalog-wrap");
                const table = document.querySelector(type === "public"
                    ? ".cjenik-table tbody tr" : "#sidrena-cijena-catalog");
                return {
                    width, pageWidth, overflow: pageWidth - width,
                    offenders,
                    tableDisplay: table ? getComputedStyle(table).display : null,
                    wrapperScrollWidth: wrap ? wrap.scrollWidth : 0,
                    wrapperClientWidth: wrap ? wrap.clientWidth : 0,
                    toolDisplay: type === "admin" ?
                        getComputedStyle(document.querySelector(".brendigo-woo-catalog-tools")).display : null,
                    cardHeadFlexWrap: type === "admin" ?
                        getComputedStyle(document.querySelector(".sidrena-cijena-card__head")).flexWrap : null,
                    cardTitleOverflowWrap: type === "admin" ?
                        getComputedStyle(document.querySelector(".sidrena-cijena-card__head h2")).overflowWrap : null,
                    primaryButtonColor: type === "admin" ?
                        getComputedStyle(document.querySelector(".brendigo-woo-catalog-footer .button-primary")).color : null,
                    navDisplay: type === "admin" ?
                        getComputedStyle(document.querySelector(".brendigo-admin-nav")).display : null,
                    navItemMinHeight: type === "admin" ?
                        parseFloat(getComputedStyle(document.querySelector(".brendigo-admin-nav__item")).minHeight) : null,
                    saveButtonMinHeight: type === "admin" ?
                        parseFloat(getComputedStyle(document.querySelector(".brendigo-woo-catalog-footer .button")).minHeight) : null,
                    catalogRowInputMinHeight: type === "admin" ?
                        parseFloat(getComputedStyle(document.querySelector(".sidrena-cijena-catalog tbody input")).minHeight) : null,
                    catalogRowButtonMinHeight: type === "admin" ?
                        parseFloat(getComputedStyle(document.querySelector(".sidrena-cijena-catalog tbody button")).minHeight) : null,
                    navItemCount: type === "admin"
                        ? document.querySelectorAll(".brendigo-admin-nav__item").length : null,
                    licenseInputOutside: type === "admin"
                        ? [...document.querySelectorAll(".bazna-license-form input[type=text]")].some(el => {
                            const r = el.getBoundingClientRect();
                            return r.left < -2 || r.right > width + 2;
                        }) : false,
                    licenseButtonMinHeight: type === "admin"
                        ? parseFloat(getComputedStyle(document.querySelector(".bazna-license-form .button")).minHeight) : null,
                    licenseFieldCount: type === "admin"
                        ? document.querySelectorAll(".bazna-license-form input[type=text]").length : null,
                    navItemOutside: type === "admin" && width <= 480 ?
                        [...document.querySelectorAll(".brendigo-admin-nav__item")].some(el => {
                            const r = el.getBoundingClientRect();
                            return r.left < -2 || r.right > width + 2;
                        }) : false,
                    priceCellWhiteSpace: type === "public" ? (() => {
                        const cell = document.querySelector(".cjenik-table tbody tr td:nth-child(n + 3)");
                        return cell ? getComputedStyle(cell).whiteSpace : null;
                    })() : null
                };
            })()`;
            const evaluation = await cdp("Runtime.evaluate", {
                expression, returnByValue: true, awaitPromise: true,
            });
            const result = evaluation.result?.value;
            if (!result) throw new Error("Cannot read browser geometry: " + fixture.label);
            const errors = [];
            if (result.width !== width) errors.push("viewport expected " + width + " got " + result.width);
            if (result.overflow > 2) errors.push("page horizontal overflow " + result.overflow + "px");
            if (result.offenders.length) errors.push("clipped panels " + JSON.stringify(result.offenders.slice(0, 5)));
            if (fixture.label === "public") {
                const expect = width <= 720 ? "block" : "table-row";
                if (result.tableDisplay !== expect) {
                    errors.push("table mode expected " + expect + " got " + result.tableDisplay);
                }
                if (width <= 720 && result.priceCellWhiteSpace !== null &&
                    result.priceCellWhiteSpace !== "normal") {
                    errors.push("mobile catalog prices/URLs cannot wrap: " + result.priceCellWhiteSpace);
                }
            } else {
                // Run all admin assertions independently: a contrast failure
                // must not accidentally skip the mobile layout checks.
                // Eight BAZNA administrator destinations must stay present,
                // including the separate licensing screen.
                if (result.navItemCount !== 8) {
                    errors.push("admin fixture does not expose eight tabs");
                }
                if (result.licenseFieldCount !== 2 || result.licenseInputOutside ||
                    result.licenseButtonMinHeight < 44) {
                    errors.push("license form is clipped, missing, or has undersized actions");
                }
                if (result.primaryButtonColor !== "rgb(7, 24, 45)") {
                    errors.push("orange primary action text must be dark navy, got " + result.primaryButtonColor);
                }
                if (width <= 782) {
                    if (result.toolDisplay !== "grid") {
                        errors.push("admin form does not stack on small screens");
                    }
                    if (result.cardHeadFlexWrap !== "wrap" || result.cardTitleOverflowWrap !== "anywhere") {
                        errors.push("admin catalog heading must wrap safely on small screens");
                    }
                    if (result.navItemMinHeight < 44 || result.saveButtonMinHeight < 44) {
                        errors.push("mobile nav/save tap targets must be at least 44px high");
                    }
                    if (result.catalogRowInputMinHeight < 44 || result.catalogRowButtonMinHeight < 44) {
                        errors.push("editable catalog input/action must be at least 44px high");
                    }
                    if (width <= 480 && (result.navDisplay !== "grid" || result.navItemOutside)) {
                        errors.push("phone sidebar nav must be a visible two-column grid");
                    }
                }
            }

            const shot = await cdp("Page.captureScreenshot", {
                format: "png", captureBeyondViewport: false, fromSurface: true,
            });
            const file = fixture.label + "-" + width + ".png";
            await writeFile(join(output, file), Buffer.from(shot.data, "base64"));
            results.push({ view: fixture.label, requestedWidth: width, ...result, pass: errors.length === 0 });
            if (errors.length) failures.push(fixture.label + "/" + width + ": " + errors.join("; "));
            console.log((errors.length ? "FAIL " : "PASS ") + fixture.label + " " +
                width + "px — page overflow " + result.overflow + "px");
        }
    }
    await writeFile(join(output, "responsive-results.json"), JSON.stringify(results, null, 2));
    if (failures.length) throw new Error(failures.join("\n"));
    console.log("PASS: public template and eight-tab admin fixture × 13 Chrome viewports = 26 geometry screenshots");
} finally {
    if (socket && socket.readyState === WebSocket.OPEN) socket.close();
    browser.kill("SIGTERM");
    await rm(profile, { recursive: true, force: true, maxRetries: 4, retryDelay: 200 });
}
