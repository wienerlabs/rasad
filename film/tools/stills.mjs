import { bundle } from "@remotion/bundler";
import { openBrowser, renderStill, selectComposition } from "@remotion/renderer";
import path from "node:path";

const frames = process.argv.slice(2).map(Number);
const chromiumOptions = { gl: "angle" };
const serveUrl = await bundle({ entryPoint: path.resolve("src/index.ts") });
const browser = await openBrowser("chrome", { chromiumOptions });
const composition = await selectComposition({ serveUrl, id: "RasadSilent", puppeteerInstance: browser, chromiumOptions });
for (const frame of frames) {
  const started = Date.now();
  await renderStill({ composition, serveUrl, frame, output: path.resolve(`out/stills/f${String(frame).padStart(4, "0")}.jpg`), imageFormat: "jpeg", jpegQuality: 88, puppeteerInstance: browser, chromiumOptions });
  console.log(`frame ${frame} ${Date.now() - started}ms`);
}
await browser.close({ silent: true });
