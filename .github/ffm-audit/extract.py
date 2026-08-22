import re, sys, json, glob, os

def balanced(s, i):
    """s[i] == '('; return (inner, index_after_close)"""
    d = 0
    for j in range(i, len(s)):
        if s[j] == '(':
            d += 1
        elif s[j] == ')':
            d -= 1
            if d == 0:
                return s[i+1:j], j+1
    raise ValueError('unbalanced')

def split_top(s):
    out, d, cur = [], 0, ''
    for ch in s:
        if ch == ',' and d == 0:
            out.append(cur.strip()); cur = ''
            continue
        if ch in '([': d += 1
        elif ch in ')]': d -= 1
        cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out

def extract(path):
    src = open(path).read()
    # strip comments so they can't contain matches
    src = re.sub(r'/\*.*?\*/', ' ', src, flags=re.S)
    src = re.sub(r'//[^\n]*', ' ', src)
    flat = re.sub(r'\s+', ' ', src)
    found = []

    # idiom 1: LINKER.downcallHandle(LIB.findOrThrow("name"), FunctionDescriptor.of[Void](...))
    for m in re.finditer(r'findOrThrow\(\s*"([A-Za-z0-9_$]+)"\s*\)', flat):
        name = m.group(1)
        rest = flat[m.end():m.end()+600]
        fd = re.search(r'FunctionDescriptor\.(of|ofVoid)\s*\(', rest)
        if not fd:
            continue
        inner, _ = balanced(rest, fd.end()-1)
        parts = split_top(inner)
        if fd.group(1) == 'ofVoid':
            ret, args = None, parts
        else:
            ret, args = (parts[0] if parts else None), parts[1:]
        found.append({'symbol': name, 'ret': ret, 'args': args, 'file': path})

    # idiom 2: downcall(LIB, "name", RET_or_null, args...)
    for m in re.finditer(r'(?<![A-Za-z])downcall\s*\(', flat):
        inner, _ = balanced(flat, m.end()-1)
        parts = split_top(inner)
        if len(parts) < 2:
            continue
        sym = re.match(r'"([A-Za-z0-9_$]+)"', parts[1])
        if not sym:
            continue
        ret = parts[2] if len(parts) > 2 else None
        if ret == 'null':
            ret = None
        found.append({'symbol': sym.group(1), 'ret': ret, 'args': parts[3:], 'file': path})
    return found

if __name__ == '__main__':
    all_found = []
    for pat in sys.argv[1:]:
        for f in glob.glob(pat, recursive=True):
            all_found += extract(f)
    # drop Linker.Option arguments, which follow the layouts
    def norm(a):
        return a.rsplit('.', 1)[-1] if a.startswith('java.lang.foreign.') else a
    for e in all_found:
        e['args'] = [norm(a) for a in e['args'] if 'Linker.Option' not in a and 'captureCallState' not in a]
        if e['ret']:
            e['ret'] = norm(e['ret'])
    json.dump(all_found, sys.stdout, indent=1)


def extract_structs(path):
    """Every MemoryLayout.structLayout in one file, as an ordered element list.

    FFM struct layouts are packed exactly as written -- padding is explicit, never inferred -- so
    each named field's offset is the running sum of the sizes before it. That is what makes the
    offsets checkable arithmetically against the real header.
    """
    src = open(path).read()
    src = re.sub(r'/\*.*?\*/', ' ', src, flags=re.S)
    src = re.sub(r'//[^\n]*', ' ', src)
    flat = re.sub(r'\s+', ' ', src)
    # int constants declared in the same file, for sequenceLayout counts and padding arithmetic
    consts = {}
    for cm in re.finditer(r'(?:static\s+final\s+)?int\s+([A-Z][A-Z0-9_]*)\s*=\s*([0-9][0-9xXa-fA-F ]*)\s*;', flat):
        try:
            consts[cm.group(1)] = int(cm.group(2).strip(), 0)
        except ValueError:
            pass
    out = []
    for m in re.finditer(r'(\w+)\s*=\s*(?:MemoryLayout\.)?(?:struct|union)Layout\s*\(', flat):
        inner, _ = balanced(flat, m.end() - 1)
        elems = []
        for part in split_top(inner):
            part = part.strip()
            if not part:
                continue
            nm = re.search(r'\.withName\("([^"]+)"\)\s*$', part)
            elems.append({'name': nm.group(1) if nm else None, 'expr': part})
        out.append({'name': m.group(1), 'elems': elems, 'file': path, 'consts': consts,
                    'union': 'unionLayout' in flat[m.start():m.end()]})
    return out
