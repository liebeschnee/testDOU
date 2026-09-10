# -*- coding: utf-8 -*-
"""一键包名迁移：com.example.emmc -> test.dou（app + uitest 源文件/工程配置）"""
import os
import shutil

ROOT = r"e:\11_tool\trae\TestU"

# (模块, 旧子目录, 新子目录)
MOVE_MAP = [
    # com.example.emmc.testdrive -> test.dou.testdrive
    (os.path.join("app", "src", "main", "java", "com", "example", "emmc", "testdrive"),
     os.path.join("app", "src", "main", "java", "test", "dou", "testdrive")),
    # com.example.emmc.testrunner -> test.dou.testrunner
    (os.path.join("uitest", "src", "main", "java", "com", "example", "emmc", "testrunner"),
     os.path.join("uitest", "src", "main", "java", "test", "dou", "testrunner")),
]

# 需要做文本替换的工程文件（相对路径）
TEXT_FILES = [
    "app/build.gradle",
    "uitest/build.gradle",
    "app/src/main/AndroidManifest.xml",
    "uitest/src/main/AndroidManifest.xml",
    "gen_dou_cases.py",
]


def collect_java(basedir):
    out = []
    for mod in ("app", "uitest"):
        d = os.path.join(basedir, mod, "src", "main", "java")
        for dirpath, _, files in os.walk(d):
            for f in files:
                if f.endswith(".java"):
                    out.append(os.path.join(dirpath, f))
    return out


def replace_all(path):
    with open(path, "r", encoding="utf-8") as f:
        data = f.read()
    new = data.replace("com.example.emmc", "test.dou")
    if new != data:
        with open(path, "w", encoding="utf-8") as f:
            f.write(new)
        print("替换:", os.path.relpath(path, ROOT))


def main():
    # 1. 源文件文本替换
    for p in collect_java(ROOT):
        replace_all(p)
    # 2. 工程配置文件文本替换
    for rel in TEXT_FILES:
        p = os.path.join(ROOT, rel)
        if os.path.exists(p):
            replace_all(p)

    # 3. 移动目录 com/example/emmc/* -> test/dou/*
    for old_rel, new_rel in MOVE_MAP:
        old = os.path.join(ROOT, old_rel)
        new = os.path.join(ROOT, new_rel)
        if os.path.exists(old) and not os.path.exists(new):
            os.makedirs(os.path.dirname(new), exist_ok=True)
            shutil.move(old, new)
            print("移动:", old_rel, "->", new_rel)

    # 4. 清理空目录 com/example/emmc/...
    for mod in ("app", "uitest"):
        base = os.path.join(ROOT, mod, "src", "main", "java", "com")
        if os.path.exists(base):
            shutil.rmtree(base, ignore_errors=True)
            print("清理:", base)
    print("完成")


if __name__ == "__main__":
    main()