#!/usr/bin/env bash
# 把 porting-lib bundle jar (META-INF/jars/*.jar) 拆开并合并为单个 jar，
# 否则 Loom 的 modCompileOnly 不会自动解包 META-INF/jars，编译时会找不到
# CustomRenderBoundingBoxBlockEntity 等接口。
#
# 用法: ./scripts/merge_porting_lib.sh <bundle.jar> [输出 jar 名]
# 例:   ./scripts/merge_porting_lib.sh libs/porting_lib-2.3.15+1.20.1.jar
#
# 前置: bash + unzip + jar 命令（Windows 需 Git Bash 或 WSL）。
set -euo pipefail

BUNDLE="${1:?需要 bundle jar 路径}"
OUT="${2:-libs/porting_lib_merged-$(basename "$BUNDLE" | sed 's/^porting_lib-//')}"

if [ ! -f "$BUNDLE" ]; then
  echo "找不到 bundle jar: $BUNDLE" >&2
  exit 1
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

unzip -q "$BUNDLE" -d "$TMP/bundle"
mkdir -p "$TMP/flat"
for jar in "$TMP/bundle"/META-INF/jars/*.jar; do
  unzip -qo "$jar" -d "$TMP/flat"
done
# 取回顶层 fabric.mod.json / 资源（sub jar 之外的内容）
unzip -qo "$BUNDLE" 'fabric.mod.json' 'LICENSE' 'assets/*' 'pack.mcmeta' '*.accesswidener' '*.mixins.json' '*refmap*.json' -d "$TMP/flat" 2>/dev/null || true

mkdir -p "$(dirname "$OUT")"
rm -f "$OUT"
( cd "$TMP/flat" && jar cf "$OUT" . )
echo "已生成合并 jar: $OUT"
