#!/usr/bin/env python3
"""impact.py - o'zgarish ta'siri tahlilchisi (prototype, Java).

Eski (merge-base) va yangi (head) kodni solishtiradi, o'zgargan metodlarni
topadi, o'zgarish turini aniqlaydi va metod ishlatilgan joylarni ko'rsatadi.
LLM kerak emas, kod hech qayerga yuborilmaydi.
"""
import argparse, difflib, os, re, shutil, subprocess, sys, tempfile, urllib.parse, urllib.request
from collections import Counter, defaultdict

import tree_sitter_java as tsjava
from tree_sitter import Language, Parser

PARSER = Parser(Language(tsjava.language()))
TYPE_NODES = {"class_declaration", "interface_declaration", "enum_declaration", "record_declaration"}
COMMENTS = {"line_comment", "block_comment"}
LITERALS = {"decimal_integer_literal", "hex_integer_literal", "decimal_floating_point_literal",
            "string_literal", "character_literal", "true", "false", "null_literal"}
HTTP_ANN = ("@GetMapping", "@PostMapping", "@PutMapping", "@DeleteMapping", "@PatchMapping", "@RequestMapping")
RISK = {"H": ("🔴", "YUQORI", 3), "M": ("🟠", "O'RTA", 2), "L": ("🟡", "PAST", 1)}


# ---------- git ----------
def git(repo, *args):
    r = subprocess.run(["git", "-C", repo, *args], capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    if r.returncode:
        raise RuntimeError(r.stderr.strip())
    return r.stdout


def show(repo, rev, path):
    r = subprocess.run(["git", "-C", repo, "show", f"{rev}:{path}"], capture_output=True)
    return r.stdout if r.returncode == 0 else None


# ---------- parsing ----------
def txt(n, src):
    return src[n.start_byte:n.end_byte].decode("utf-8", "replace")


def tokens(node, src):
    out = []

    def w(n):
        if n.type in COMMENTS:
            return
        if n.child_count == 0:
            out.append(txt(n, src))
        else:
            for c in n.children:
                w(c)
    if node is not None:
        w(node)
    return out


def norm(node, src):
    return " ".join(tokens(node, src))


def descendants(node):
    stack = [node]
    while stack:
        n = stack.pop()
        yield n
        stack.extend(n.children)


def methods(src):
    """{key: info} for every method/constructor in a Java source."""
    res = {}
    tree = PARSER.parse(src)

    def walk(n, stack):
        if n.type in TYPE_NODES:
            stack = stack + [txt(n.child_by_field_name("name"), src)]
        if n.type in ("method_declaration", "constructor_declaration") and stack:
            info = describe(n, src, stack)
            res.setdefault(info["key"], info)
        for c in n.children:
            walk(c, stack)
    walk(tree.root_node, [])
    return res


def describe(n, src, stack):
    name = txt(n.child_by_field_name("name"), src)
    ptypes = []
    for p in n.child_by_field_name("parameters").named_children:
        if p.type == "formal_parameter":
            ptypes.append(re.sub(r"\s+", "", txt(p.child_by_field_name("type"), src)))
        elif p.type == "spread_parameter":
            ptypes.append(re.sub(r"\s+", "", txt(p.named_children[0], src)) + "...")
    rt = n.child_by_field_name("type")
    annotations, modifiers, throws = [], [], []
    for c in n.children:
        if c.type == "modifiers":
            for m in c.children:
                (annotations if m.type.endswith("annotation") else modifiers).append(norm(m, src))
        elif c.type == "throws":
            throws = [norm(t, src) for t in c.named_children]
    body = n.child_by_field_name("body")
    cls = ".".join(stack)
    return {
        "key": f"{cls}.{name}({','.join(ptypes)})", "cls": cls, "simple_cls": stack[-1], "name": name,
        "ptypes": ptypes, "ret": re.sub(r"\s+", "", txt(rt, src)) if rt is not None else "",
        "annotations": annotations, "modifiers": set(modifiers), "throws": set(throws),
        "body": norm(body, src), "body_node": body, "src": src,
        "start": n.start_point[0] + 1, "end": n.end_point[0] + 1,
    }


def collapse(t, limit=200):
    t = re.sub(r"\s+", " ", t).strip()
    return t if len(t) <= limit else t[:limit - 1] + "…"


SIMPLE = {"expression_statement", "local_variable_declaration", "return_statement", "throw_statement",
          "assert_statement", "break_statement", "continue_statement", "yield_statement"}


def statements(info):
    """Method body -> ordered list of statements (+ if/while/for/catch headers) with line numbers."""
    src, items = info["src"], []

    def item(kind, node, nrm, raw, line):
        items.append({"kind": kind, "node": node, "norm": nrm, "raw": collapse(raw), "line": line})

    def walk(n):
        for c in n.children:
            t, line = c.type, c.start_point[0] + 1
            if t in SIMPLE:
                item(t, c, norm(c, src), txt(c, src), line)
                continue
            if t in ("if_statement", "while_statement"):
                cond = c.child_by_field_name("condition")
                kw = "if" if t == "if_statement" else "while"
                item(kw, cond, kw + " " + norm(cond, src), kw + " " + txt(cond, src), line)
            elif t in ("for_statement", "enhanced_for_statement", "catch_clause"):
                body = c.child_by_field_name("body")
                toks = []
                for ch in c.children:
                    if body is not None and ch.id == body.id:
                        break
                    toks += tokens(ch, src)
                raw = src[c.start_byte:(body.start_byte if body is not None else c.end_byte)].decode("utf-8", "replace")
                item(t, None, " ".join(toks), raw, line)
            walk(c)
    if info["body_node"] is not None:
        walk(info["body_node"])
    return items


DB_CALLS = {"save", "saveAll", "saveAndFlush", "persist", "merge", "update", "delete", "deleteById",
            "deleteAll", "deleteAllById", "flush", "remove"}
SEND_PREFIX = ("send", "publish", "notify", "emit", "post", "dispatch")
SETTER = re.compile(r"^set([A-Z]\w*)$")


def lc(x):
    return x[:1].lower() + x[1:]


def calls_of(it, src):
    res = defaultdict(list)
    if it is None or it["node"] is None:
        return res
    for d in descendants(it["node"]):
        if d.type == "method_invocation":
            nm = txt(d.child_by_field_name("name"), src)
        elif d.type == "object_creation_expression":
            nm = "new " + collapse(txt(d.child_by_field_name("type"), src))
        else:
            continue
        a = d.child_by_field_name("arguments")
        res[nm].append(tuple(collapse(txt(x, src), 80) for x in a.named_children) if a else ())
    for v in res.values():
        v.sort()
    return res


def fmt(args):
    return "(" + ", ".join(args) + ")"


def call_changed(name, a, b):
    m = SETTER.match(name)
    if m:
        f = m.group(1)
        return "M", f"`{lc(f)}` maydoniga beriladigan qiymat o'zgardi: `{fmt(a)[1:-1]}` -> `{fmt(b)[1:-1]}`", f
    return "M", f"`{name}()` chaqiruvida argument o'zgardi: `{fmt(a)}` -> `{fmt(b)}`", None


def call_added(name, args):
    m = SETTER.match(name)
    if m:
        f = m.group(1)
        return "M", f"endi `{lc(f)}` maydoni o'rnatiladi: `{fmt(args)[1:-1]}`", f
    if name in DB_CALLS:
        return "M", f"endi `{name}()` chaqiriladi: ma'lumotlar bazasiga yozish/o'chirish bo'lishi mumkin", None
    if name.startswith(SEND_PREFIX):
        return "M", f"endi `{name}()` chaqiriladi: xabar/event yuborilishi mumkin", None
    return "L", f"yangi chaqiruv: `{name}{fmt(args)}`", None


def call_removed(name, args):
    m = SETTER.match(name)
    if m:
        f = m.group(1)
        return "M", f"endi `{lc(f)}` maydoni o'rnatilmaydi (avval: `{fmt(args)[1:-1]}`)", f
    if name in DB_CALLS:
        return "H", f"`{name}()` chaqiruvi olib tashlandi: o'zgarish bazaga yozilmay qolishi mumkin", None
    return "M", f"`{name}()` chaqiruvi olib tashlandi", None


KIND_TEXT = {
    "return_statement": ("M", "qaytariladigan ifoda o'zgardi", "yangi return qo'shildi", "return olib tashlandi"),
    "if": ("M", "shart o'zgardi", "yangi shart qo'shildi", "shart olib tashlandi"),
    "while": ("M", "tsikl sharti o'zgardi", "yangi tsikl qo'shildi", "tsikl olib tashlandi"),
    "throw_statement": ("M", "istisno o'zgardi", "yangi istisno tashlanadi", "istisno tashlash olib tashlandi"),
    "catch_clause": ("M", "catch bloki o'zgardi", "yangi catch qo'shildi", "catch olib tashlandi"),
    "local_variable_declaration": ("L", "o'zgaruvchi ifodasi o'zgardi", "yangi o'zgaruvchi", "o'zgaruvchi olib tashlandi"),
}


def explain(o, n, so, sn):
    parts, fields, risk = [], [], "L"
    oc, nc = calls_of(o, so), calls_of(n, sn)
    for name in sorted(set(oc) | set(nc)):
        a, b = oc.get(name, []), nc.get(name, [])
        if a == b:
            continue
        found = []
        if a and b and len(a) == len(b):
            found = [call_changed(name, x, y) for x, y in zip(a, b) if x != y]
        elif a and b:
            found = [("M", f"`{name}()` chaqiruvlari soni o'zgardi: {len(a)} -> {len(b)}", None)]
        elif b:
            found = [call_added(name, y) for y in b]
        else:
            found = [call_removed(name, x) for x in a]
        for r, msg, f in found:
            parts.append(msg)
            risk = r if RISK[r][2] > RISK[risk][2] else risk
            if f:
                fields.append(f)
    if not parts:
        kind = (n or o)["kind"]
        r, both, added, removed = KIND_TEXT.get(kind, ("L", "ifoda o'zgardi", "yangi ifoda qo'shildi", "ifoda olib tashlandi"))
        if kind == "expression_statement" and "assignment_expression" in {d.type for d in descendants((n or o)["node"])}:
            r, both = "M", "qiymat berish ifodasi o'zgardi"
        parts.append(both if (o and n) else added if n else removed)
        risk = r
    return risk, parts, tuple(dict.fromkeys(fields))


def body_findings(o, n):
    if o["body"] == n["body"]:
        return []
    so, sn = o["src"], n["src"]
    os_, ns_ = statements(o), statements(n)
    sm = difflib.SequenceMatcher(None, [x["norm"] for x in os_], [x["norm"] for x in ns_], autojunk=False)
    out = []
    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == "equal":
            continue
        olds, news = os_[i1:i2], ns_[j1:j2]
        for k in range(max(len(olds), len(news))):
            a = olds[k] if k < len(olds) else None
            b = news[k] if k < len(news) else None
            risk, parts, fields = explain(a, b, so, sn)
            line = (b or a)["line"]
            where = f"Qator {line}" if b else f"Eski kodning {line}-qatori (o'chirilgan)"
            diff = []
            if a:
                diff.append(f"- {a['raw']}")
            if b:
                diff.append(f"+ {b['raw']}")
            msg = f"{where}: {'; '.join(parts)}\n```diff\n" + "\n".join(diff) + "\n```"
            out.append((risk, msg, fields))
    if not out:
        out.append(("L", "Ichki tuzilma o'zgardi (imzo bir xil). Kodni ko'rib chiqing.", ()))
    return out


# ---------- classification (Uzbek templates) ----------
def sig_findings(o, n):
    return [(r, m, ()) for r, m in _sig(o, n)]


def _sig(o, n):
    f = []
    if o["ptypes"] != n["ptypes"]:
        f.append(("H", f"Parametrlar o'zgardi: `({', '.join(o['ptypes'])})` -> `({', '.join(n['ptypes'])})`. "
                       "Eski usulda chaqirgan joylar kompilyatsiya bo'lmaydi."))
    if o["ret"] != n["ret"]:
        f.append(("H", f"Qaytish turi o'zgardi: `{o['ret']}` -> `{n['ret']}`. Natijani ishlatgan joylar buzilishi mumkin."))
    for t in sorted(n["throws"] - o["throws"]):
        f.append(("M", f"Endi `{t}` istisnosi tashlanishi mumkin. Chaqiruvchilar uni ushlamasligi mumkin."))
    for t in sorted(o["throws"] - n["throws"]):
        f.append(("L", f"`{t}` istisnosi endi e'lon qilinmagan (throws)."))
    for a in sorted(set(n["annotations"]) - set(o["annotations"])):
        f.append(("H" if a.startswith(HTTP_ANN) else "M", f"Annotatsiya qo'shildi/o'zgardi: `{a}`."
                  + (" Endpoint o'zgargan, frontend chaqiruvlarini tekshiring." if a.startswith(HTTP_ANN) else "")))
    for a in sorted(set(o["annotations"]) - set(n["annotations"])):
        f.append(("H" if a.startswith(HTTP_ANN) else "M", f"Annotatsiya olib tashlandi/o'zgardi: `{a}`."
                  + (" Endpoint o'zgargan, frontend chaqiruvlarini tekshiring." if a.startswith(HTTP_ANN) else "")))
    if "public" in o["modifiers"] and "public" not in n["modifiers"]:
        f.append(("H", "Metod endi `public` emas: boshqa paketlardan chaqirib bo'lmaydi."))
    if ("static" in o["modifiers"]) != ("static" in n["modifiers"]):
        f.append(("H", "`static` modifikatori o'zgardi."))
    return f


# ---------- callers ----------
class Callers:
    def __init__(self, repo, head):
        self.repo, self.head, self.idx = repo, head, {}

    def enclosing(self, path, line):
        if path not in self.idx:
            src = show(self.repo, self.head, path)
            self.idx[path] = list(methods(src).values()) if src else []
        best = None
        for m in self.idx[path]:
            if m["start"] <= line <= m["end"] and (best is None or m["end"] - m["start"] < best["end"] - best["start"]):
                best = m
        return best["key"] if best else "(sinf darajasi)"

    def find(self, change):
        name, cls = change["name"], change["simple_cls"]
        pat = rf"new\s+{re.escape(cls)}\s*\(" if name == cls else rf"\b{re.escape(name)}\s*\("
        r = subprocess.run(["git", "-C", self.repo, "grep", "-n", "-I", "-E", pat, self.head, "--", "*.java"],
                           capture_output=True, text=True, encoding="utf-8", errors="replace")
        seen, out = set(), []
        for ln in r.stdout.splitlines():
            parts = ln.split(":", 3)
            if len(parts) < 4:
                continue
            _, path, lineno, text = parts
            s = text.strip()
            if s.startswith(("//", "*", "/*")):
                continue
            lineno = int(lineno)
            enc = self.enclosing(path, lineno)
            if enc == change["key"] or (path == change["path"] and enc == change.get("new_key")):
                continue
            if re.search(rf"\b(public|private|protected|static)\b[\w<>\[\],\.\s?]*\b{re.escape(name)}\s*\(", s):
                continue  # declaration of another overload/override
            if (path, enc) in seen:
                continue
            seen.add((path, enc))
            src = show(self.repo, self.head, path) or b""
            confident = path == change["path"] or cls.encode() in src
            out.append({"path": path, "line": lineno, "key": enc, "confident": confident, "text": s,
                        "test": "/test/" in path or path.endswith(("Test.java", "Tests.java"))})
        return out

    def readers(self, field, exclude_key, limit=6):
        """Where is the value of this field read (getFoo/isFoo/hasFoo calls)?"""
        pat = rf"\b(get|is|has){re.escape(field)}\s*\("
        r = subprocess.run(["git", "-C", self.repo, "grep", "-n", "-I", "-E", pat, self.head, "--", "*.java"],
                           capture_output=True, text=True, encoding="utf-8", errors="replace")
        seen, out = set(), []
        for ln in r.stdout.splitlines():
            parts = ln.split(":", 3)
            if len(parts) < 4:
                continue
            _, path, lineno, text = parts
            s = text.strip()
            if s.startswith(("//", "*", "/*")) or "/test/" in path or path.endswith(("Test.java", "Tests.java")):
                continue
            if re.search(rf"\b(public|private|protected)\b[\w<>\[\],\.\s?]*\b(get|is|has){re.escape(field)}\s*\(", s):
                continue  # getter declaration itself
            enc = self.enclosing(path, int(lineno))
            if enc == exclude_key or (path, enc) in seen:
                continue
            seen.add((path, enc))
            out.append((enc, path, int(lineno)))
        return out[:limit], len(out)


def uses_result(call):
    """Does the call site use the returned value? (heuristic)"""
    text, name = call["text"], call["name"]
    prefix = text.split(name + "(")[0]
    standalone = (text.rstrip().rstrip("}").rstrip().endswith(";") and "=" not in prefix and not re.search(r"\breturn\b", prefix)
                  and prefix.count("(") == prefix.count(")")
                  and re.fullmatch(r"[\w\.\(\)\[\]\s,\"{]*", prefix) is not None)
    return not standalone


# ---------- compile check ----------
ERR_RES = [re.compile(r"\[ERROR\]\s+(?P<f>\S.*?\.java):\[(?P<l>\d+),\d+\]\s+(?P<m>.+)"),
           re.compile(r"(?P<f>[^\s:]+\.java):(?P<l>\d+):\s+error:\s+(?P<m>.+)")]


def run_compile(cmd, cwd):
    r = subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    out, errs = r.stdout + "\n" + r.stderr, []
    for line in out.splitlines():
        for rx in ERR_RES:
            m = rx.search(line)
            if m:
                f = m.group("f").replace(cwd + os.sep, "")
                errs.append((f, int(m.group("l")), m.group("m").strip()))
                break
    return r.returncode, list(dict.fromkeys(errs)), out


def compile_section(repo, base, head, cmd):
    tmp, dirs = tempfile.mkdtemp(prefix="impact-"), {}
    try:
        for tag, rev in (("old", base), ("new", head)):
            d = os.path.join(tmp, tag)
            git(repo, "worktree", "add", "--detach", d, rev)
            dirs[tag] = d
        res = {tag: run_compile(cmd, d) for tag, d in dirs.items()}
    finally:
        for d in dirs.values():
            subprocess.run(["git", "-C", repo, "worktree", "remove", "--force", d], capture_output=True)
        shutil.rmtree(tmp, ignore_errors=True)
    (orc, oerr, _), (nrc, nerr, nout) = res["old"], res["new"]
    old_keys = {(f, m) for f, _, m in oerr}
    fresh = [e for e in nerr if (e[0], e[2]) not in old_keys]
    out = ["### 🔧 Kompilyatsiya tekshiruvi\n"]
    if orc != 0:
        out.append("⚠️ Eski kod (asosiy branch) ham kompilyatsiya bo'lmadi, faqat yangi xatolar ko'rsatiladi.\n")
    if fresh:
        out.append(f"🔴 **Aniq buziladi:** o'zgarishdan keyin {len(fresh)} ta yangi kompilyatsiya xatosi paydo bo'ldi:\n")
        out += [f"- `{f}:{l}`: {m}" for f, l, m in fresh[:20]]
        if len(fresh) > 20:
            out.append(f"- ... va yana {len(fresh) - 20} ta")
    elif nrc != 0 and not nerr:
        out.append("🔴 Yangi kod kompilyatsiya bo'lmadi, lekin xatolarni tahlil qilib bo'lmadi. Oxirgi satrlar:\n```")
        out += nout.strip().splitlines()[-12:] + ["```"]
    elif nrc != 0:
        out.append("🟠 Yangi kodda kompilyatsiya xatolari bor, lekin ular eski kodda ham bor edi (yangi xato yo'q).")
    else:
        out.append("✅ Yangi kod xatosiz kompilyatsiya bo'ldi: o'zgarish tuzilma jihatdan hech narsani buzmaydi.")
    return "\n".join(out) + "\n"


# ---------- main analysis ----------
def analyze(repo, base, head):
    changes, new_methods = [], 0
    for line in git(repo, "diff", "--no-renames", "--name-status", base, head).splitlines():
        status, path = line.split("\t", 1)
        if not path.endswith(".java"):
            continue
        old, new = show(repo, base, path), show(repo, head, path)
        om = methods(old) if old else {}
        nm = methods(new) if new else {}
        for k in om.keys() & nm.keys():
            f = sig_findings(om[k], nm[k]) + body_findings(om[k], nm[k])
            if f:
                changes.append(dict(nm[k], path=path, findings=f))
        groups = defaultdict(lambda: ([], []))
        for k in om.keys() - nm.keys():
            groups[(om[k]["cls"], om[k]["name"])][0].append(k)
        for k in nm.keys() - om.keys():
            groups[(nm[k]["cls"], nm[k]["name"])][1].append(k)
        for (_, _), (rem, add) in groups.items():
            if len(rem) == 1 and len(add) == 1:
                o, n = om[rem[0]], nm[add[0]]
                f = sig_findings(o, n) + body_findings(o, n)
                if f:
                    changes.append(dict(n, path=path, findings=f, old_key=o["key"]))
            else:
                for k in rem:
                    changes.append(dict(om[k], path=path, findings=[
                        ("H", "Metod o'chirildi yoki nomi/imzosi o'zgartirildi. Ishlatilgan joylarni tekshiring.")]))
                new_methods += len(add)
    return changes, new_methods


def render(repo, head, changes, new_methods, compile_md=""):
    finder = Callers(repo, head)
    for c in changes:
        c["findings"] = list(dict.fromkeys(c["findings"]))
        c["callers"] = finder.find(c)
        if len(c["callers"]) > 10:
            c["findings"].append(("M", f"Keng ta'sir doirasi: {len(c['callers'])} ta joyda ishlatiladi.", ()))
        c["risk"] = max((RISK[f[0]][2] for f in c["findings"]), default=1)
    changes.sort(key=lambda c: (-c["risk"], -len(c["callers"])))
    if not changes:
        return "## ✅ O'zgarish ta'siri\n\nMavjud metodlarda ta'sir qiladigan o'zgarish topilmadi.\n\n" + compile_md
    out = [f"## ⚠️ O'zgarish ta'siri\n\nO'zgargan metodlar: **{len(changes)}**"
           + (f", yangi qo'shilgan metodlar: {new_methods}" if new_methods else "") + "\n"]
    if compile_md:
        out.append(compile_md)
    for c in changes:
        icon, label, _ = next(v for v in RISK.values() if v[2] == c["risk"])
        sig = f"{c['cls']}.{c['name']}({', '.join(c['ptypes'])})"
        out.append(f"### {icon} `{sig}`  ·  xavf: **{label}**\n`{c['path']}:{c['start']}`\n")
        for risk, msg, fields in sorted(c["findings"], key=lambda x: -RISK[x[0]][2]):
            lines = msg.split("\n")
            out.append(f"- {RISK[risk][0]} {lines[0]}")
            out += ["  " + ln for ln in lines[1:]]
            for fld in fields:
                rd, total = finder.readers(fld, c["key"])
                if rd:
                    shown = ", ".join(f"`{k}` ({p.split('/')[-1]}:{ln})" for k, p, ln in rd)
                    more = f" va yana {total - len(rd)} ta" if total > len(rd) else ""
                    out.append(f"  🔎 `{lc(fld)}` qiymati o'qiladigan joylar ({total}): {shown}{more}. "
                               "Shu joylardagi mantiq yangi qiymatga qarab o'zgarishi mumkin.")
        prod = [x for x in c["callers"] if not x["test"]]
        tests = [x for x in c["callers"] if x["test"]]
        if prod:
            out.append(f"\n**Bu metod ishlatilgan joylar ({len(prod)}):**")
            returns = c["ret"] not in ("", "void")
            for x in prod[:15]:
                note = "" if x["confident"] else " _(ishonch past: faqat nom bo'yicha topildi)_"
                if returns:
                    note += " - natijani ishlatadi" if uses_result(dict(x, name=c["name"])) else " - natijani ishlatmaydi"
                out.append(f"- `{x['key']}` - `{x['path']}:{x['line']}`{note}")
            if len(prod) > 15:
                out.append(f"- ... va yana {len(prod) - 15} ta")
        else:
            out.append("\nBoshqa joyda ishlatilishi topilmadi.")
        out.append("\n🧪 Testlar: " + (", ".join(sorted({f"`{t['key'].split('.')[0]}`" for t in tests}))
                                       if tests else "bu metodni chaqiradigan test topilmadi") + "\n")
    out.append("_Bu hisobot statik tahlilga asoslangan: \"ehtimol ta'sirlanadi\" degani, \"aniq buziladi\" emas "
               "(faqat kompilyatsiya xatolari aniq)._")
    return "\n".join(out)


def post_gitlab(body):
    url = f"{os.environ['CI_API_V4_URL']}/projects/{os.environ['CI_PROJECT_ID']}" \
          f"/merge_requests/{os.environ['CI_MERGE_REQUEST_IID']}/notes"
    data = urllib.parse.urlencode({"body": body}).encode()
    req = urllib.request.Request(url, data=data, headers={"PRIVATE-TOKEN": os.environ["GITLAB_TOKEN"]})
    urllib.request.urlopen(req, timeout=30).read()


def main():
    ap = argparse.ArgumentParser(description="O'zgarish ta'siri tahlilchisi")
    ap.add_argument("--repo", default=".")
    ap.add_argument("--base", default="origin/main", help="solishtiriladigan asosiy branch")
    ap.add_argument("--head", default="HEAD")
    ap.add_argument("--out", help="hisobotni faylga yozish")
    ap.add_argument("--compile-cmd", help="eski va yangi kodni kompilyatsiya qilish buyrug'i, masalan: \"mvn -q -B compile\"")
    ap.add_argument("--post-gitlab", action="store_true", help="MR ga komment yozish")
    a = ap.parse_args()
    base = git(a.repo, "merge-base", a.base, a.head).strip()
    changes, new_methods = analyze(a.repo, base, a.head)
    compile_md = compile_section(a.repo, base, a.head, a.compile_cmd) if a.compile_cmd and changes else ""
    report = render(a.repo, a.head, changes, new_methods, compile_md)
    print(report)
    if a.out:
        open(a.out, "w", encoding="utf-8").write(report)
    if a.post_gitlab:
        try:
            post_gitlab(report)
        except Exception as e:  # advisory only: never fail the pipeline
            print(f"[impact] GitLab ga yozib bo'lmadi: {e}", file=sys.stderr)


if __name__ == "__main__":
    main()
