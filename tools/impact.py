#!/usr/bin/env python3
"""impact.py - o'zgarish ta'siri tahlilchisi (prototype, Java).

Eski (merge-base) va yangi (head) kodni solishtiradi, o'zgargan metodlarni
topadi, o'zgarish turini aniqlaydi va metod ishlatilgan joylarni ko'rsatadi.
LLM kerak emas, kod hech qayerga yuborilmaydi.
"""
import argparse, os, re, subprocess, sys, urllib.parse, urllib.request
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
            ptypes.append(norm(p.child_by_field_name("type"), src))
        elif p.type == "spread_parameter":
            ptypes.append(norm(p.named_children[0], src) + "...")
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
        "ptypes": ptypes, "ret": norm(rt, src) if rt is not None else "",
        "annotations": annotations, "modifiers": set(modifiers), "throws": set(throws),
        "body": norm(body, src), "body_node": body, "src": src,
        "start": n.start_point[0] + 1, "end": n.end_point[0] + 1,
    }


def strip_parens(t):
    t = t.strip()
    return t[1:-1].strip() if t.startswith("(") and t.endswith(")") else t


def body_facts(info):
    src, conds, lits, throws = info["src"], Counter(), Counter(), Counter()
    if info["body_node"] is None:
        return conds, lits, throws
    for d in descendants(info["body_node"]):
        if d.type in ("if_statement", "while_statement"):
            conds[strip_parens(norm(d.child_by_field_name("condition"), src))] += 1
        elif d.type == "ternary_expression":
            conds[strip_parens(norm(d.child_by_field_name("condition"), src))] += 1
        elif d.type in LITERALS:
            lits[txt(d, src)] += 1
        elif d.type == "throw_statement":
            throws[norm(d, src).replace("throw ", "", 1).rstrip(" ;")] += 1
    return conds, lits, throws


# ---------- classification (Uzbek templates) ----------
def sig_findings(o, n):
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


def pair_diff(old, new):
    """Counter diff -> (removed list, added list)."""
    return sorted((old - new).elements()), sorted((new - old).elements())


def body_findings(o, n):
    if o["body"] == n["body"]:
        return []
    f = []
    oc, ol, ot = body_facts(o)
    nc, nl, nt = body_facts(n)
    rc, ac = pair_diff(oc, nc)
    if rc and len(rc) == len(ac):
        for a, b in zip(rc, ac):
            f.append(("M", f"Shart o'zgardi: `{a}` -> `{b}`."))
    else:
        f += [("M", f"Shart olib tashlandi: `{c}`.") for c in rc]
        f += [("M", f"Yangi shart qo'shildi: `{c}`.") for c in ac]
    rl, al = pair_diff(ol, nl)
    if rl and len(rl) == len(al):
        for a, b in zip(rl, al):
            f.append(("M", f"Qiymat (konstanta) o'zgardi: `{a}` -> `{b}`."))
    else:
        f += [("L", f"Qiymat olib tashlandi: `{c}`.") for c in rl]
        f += [("L", f"Yangi qiymat qo'shildi: `{c}`.") for c in al]
    rt, at = pair_diff(ot, nt)
    f += [("M", f"Yangi istisno tashlanadi: `{c}`.") for c in at]
    f += [("L", f"Istisno olib tashlandi: `{c}`.") for c in rt]
    if not f:
        f.append(("L", "Ichki mantiq o'zgardi (imzo bir xil). Natija o'zgarishi mumkin, kodni ko'rib chiqing."))
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
            if re.search(rf"\b(void|public|private|protected|static)\b.*\b{re.escape(name)}\s*\(", s) and s.endswith("{"):
                continue  # declaration of another overload/override
            if (path, enc) in seen:
                continue
            seen.add((path, enc))
            src = show(self.repo, self.head, path) or b""
            confident = path == change["path"] or cls.encode() in src
            out.append({"path": path, "line": lineno, "key": enc, "confident": confident,
                        "test": "/test/" in path or path.endswith(("Test.java", "Tests.java"))})
        return out


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


def render(repo, head, changes, new_methods):
    finder = Callers(repo, head)
    for c in changes:
        c["findings"] = list(dict.fromkeys(c["findings"]))
        c["callers"] = finder.find(c)
        if len(c["callers"]) > 10:
            c["findings"].append(("M", f"Keng ta'sir doirasi: {len(c['callers'])} ta joyda ishlatiladi."))
        c["risk"] = max((RISK[r][2] for r, _ in c["findings"]), default=1)
    changes.sort(key=lambda c: (-c["risk"], -len(c["callers"])))
    if not changes:
        return "## ✅ O'zgarish ta'siri\n\nMavjud metodlarda ta'sir qiladigan o'zgarish topilmadi.\n"
    out = [f"## ⚠️ O'zgarish ta'siri\n\nO'zgargan metodlar: **{len(changes)}**"
           + (f", yangi qo'shilgan metodlar: {new_methods}" if new_methods else "") + "\n"]
    for c in changes:
        icon, label, _ = next(v for v in RISK.values() if v[2] == c["risk"])
        sig = f"{c['cls']}.{c['name']}({', '.join(c['ptypes'])})"
        out.append(f"### {icon} `{sig}`  ·  xavf: **{label}**\n`{c['path']}:{c['start']}`\n")
        out += [f"- {RISK[r][0]} {m}" for r, m in sorted(c["findings"], key=lambda x: -RISK[x[0]][2])]
        prod = [x for x in c["callers"] if not x["test"]]
        tests = [x for x in c["callers"] if x["test"]]
        if prod:
            out.append(f"\n**Ishlatilgan joylar ({len(prod)}):**")
            for x in prod[:15]:
                note = "" if x["confident"] else " _(ishonch past: faqat nom bo'yicha topildi)_"
                out.append(f"- `{x['key']}` - `{x['path']}:{x['line']}`{note}")
            if len(prod) > 15:
                out.append(f"- ... va yana {len(prod) - 15} ta")
        else:
            out.append("\nBoshqa joyda ishlatilishi topilmadi.")
        out.append("\n🧪 Testlar: " + (", ".join(sorted({f"`{t['key'].split('.')[0]}`" for t in tests}))
                                       if tests else "bu metodni chaqiradigan test topilmadi") + "\n")
    out.append("_Bu hisobot statik tahlilga asoslangan: \"ehtimol ta'sirlanadi\", \"aniq buziladi\" emas._")
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
    ap.add_argument("--post-gitlab", action="store_true", help="MR ga komment yozish")
    a = ap.parse_args()
    base = git(a.repo, "merge-base", a.base, a.head).strip()
    changes, new_methods = analyze(a.repo, base, a.head)
    report = render(a.repo, a.head, changes, new_methods)
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
