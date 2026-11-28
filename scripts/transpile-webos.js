"use strict";

const fs = require("fs");
const path = require("path");
const babel = require("@babel/core");
const presetEnv = require("@babel/preset-env");

const root = process.cwd();
const sourceDir = path.join(root, "webos");
const outputDir = path.join(root, "webos-build");

fs.rmSync(outputDir, { recursive: true, force: true });
fs.cpSync(sourceDir, outputDir, { recursive: true });

const htmlPath = path.join(outputDir, "index.html");
const html = fs.readFileSync(htmlPath, "utf8");
let transformedCount = 0;

const output = html.replace(/<script\\b([^>]*)>([\\s\\S]*?)<\\/script>/gi, (tag, attributes, code) => {
  if (!code.trim() || /\\bsrc\\s*=/i.test(attributes)) return tag;
  if (/\\btype\\s*=\\s*["'](?:application\\/json|importmap)["']/i.test(attributes)) return tag;

  const transformed = babel.transformSync(code, {
    babelrc: false,
    configFile: false,
    sourceType: "script",
    comments: false,
    compact: false,
    presets: [[presetEnv, {
      targets: { chrome: "53" },
      modules: false,
      bugfixes: true,
      useBuiltIns: false
    }]]
  });

  transformedCount += 1;
  return "<script" + attributes + ">\\n" + transformed.code + "\\n</script>";
});

if (transformedCount === 0) throw new Error("No inline JavaScript was transpiled");
fs.writeFileSync(htmlPath, output, "utf8");
console.log("Transpiled " + transformedCount + " inline script(s) for LG webOS Chromium 53.");
console.log("Package staging directory: " + outputDir);
