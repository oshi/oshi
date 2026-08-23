#!/usr/bin/env python3
"""Check oshi-core-ffm's FunctionDescriptors against the platform's real prototypes.

Extracts every native binding from the FFM sources, emits one static_assert per return value and
per parameter, and compiles that against the platform SDK headers. A failed assert means OSHI's
descriptor disagrees with the C declaration -- the 4-vs-8-byte class of defect that otherwise
surfaces as an intermittent crash rather than a test failure.

Symbols the SDK does not declare (private or dlopen-only APIs) are reported separately and are not
failures; there is nothing to compare them against.
"""
import argparse, glob, json, os, re, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))

# compiler, syntax-only flags, and how each one words the two diagnostics we harvest
# Each compiler words the diagnostics we harvest differently, including the order of the two names
# in a missing-member message and, for gcc, the quote characters around them.
Q = r"[\u2018']"      # opening quote: gcc uses a curly one unless the locale forbids it
QE = r"[\u2019']"     # closing quote
TOOLCHAINS = {
    'clang': dict(
        cmd=['clang++', '-std=c++17', '-fsyntax-only', '-ferror-limit=0'],
        undeclared=r"use of undeclared identifier '([A-Za-z0-9_]+)'",
        no_member=r"no member named '(?P<member>\w+)' in '(?:struct )?(?P<type>\w+)'",
        unknown_type=r"(?:incomplete type|unknown type name) '(?:struct )?(\w+)'"),
    'gcc': dict(
        cmd=['g++', '-std=c++17', '-fsyntax-only', '-fmax-errors=0'],
        undeclared=r"error: " + Q + r"([A-Za-z0-9_]+)" + QE + r" (?:was|has) not (?:been )?declared",
        # gcc names the type first: 'struct statvfs' has no member named '_f_spare'
        no_member=Q + r"(?:struct |union )?(?P<type>\w+)" + QE + r" has no member named " + Q
                  + r"(?P<member>\w+)" + QE,
        # covers both "invalid use of incomplete type 'struct x'" and the sizeof wording
        unknown_type=r"incomplete type " + Q + r"(?:struct |union )?(\w+)" + QE),
    'msvc': dict(
        cmd=['cl', '/std:c++17', '/Zs', '/nologo', '/D_CRT_SECURE_NO_WARNINGS'],
        undeclared=r"error C2065: '([A-Za-z0-9_]+)': undeclared identifier",
        # C2039: 'ut_pad': is not a member of 'utmpx'
        no_member=r"error C2039: '(?P<member>\w+)': is not a member of '(?:struct )?(?P<type>\w+)'",
        unknown_type=r"error C2027: use of undefined type '(?:struct )?(\w+)'"),
}


def run(argv, src):
    p = subprocess.run(argv + [src], capture_output=True, text=True)
    return p.stdout + p.stderr


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--platform', required=True, help='mac | linux | windows | <unix flavor>')
    ap.add_argument('--toolchain', required=True, choices=sorted(TOOLCHAINS))
    ap.add_argument('--sources', required=True, help='glob for the FFM binding sources')
    ap.add_argument('--workdir', default='.')
    ap.add_argument('--cflags', default='', help='extra compiler flags, space separated')
    args = ap.parse_args()

    PRE_INCLUDES = '// Generated: OSHI FFM struct layouts vs the platform headers.\n%s\n'
    tc = TOOLCHAINS[args.toolchain]
    cmd = tc['cmd'] + [f for f in args.cflags.split(' ') if f]
    work = args.workdir
    os.makedirs(work, exist_ok=True)

    sys.path.insert(0, HERE)
    import extract, gen

    bindings = []
    for f in glob.glob(args.sources, recursive=True):
        bindings += extract.extract(f)
    for e in bindings:
        e['args'] = [extract_norm(a) for a in e['args']
                     if 'Linker.Option' not in a and 'captureCallState' not in a]
        if e['ret']:
            e['ret'] = extract_norm(e['ret'])
    print(f'{args.platform}: {len(bindings)} native bindings extracted', flush=True)

    includes = [l.strip() for l in open(os.path.join(HERE, 'includes', args.platform + '.txt'))
                if l.strip() and not l.startswith('#')]
    exc_file = os.path.join(HERE, 'exceptions', args.platform + '.txt')
    exceptions = set()
    if os.path.exists(exc_file):
        for line in open(exc_file):
            line = line.strip()
            if line and not line.startswith('#'):
                exceptions.add(line.split('#')[0].split()[0])
    src = os.path.join(work, 'check.cpp')
    skip, log = set(exceptions), ''
    for _ in range(8):
        code, checked = gen.emit(bindings, includes, skip)
        open(src, 'w').write(code)
        log = run(cmd, src)
        symbols = {e['symbol'] for e in bindings}
        undeclared = set(re.findall(tc['undeclared'], log)) & symbols
        if not undeclared:
            break
        skip |= undeclared
    open(os.path.join(work, 'compile.log'), 'w').write(log)

    findings = sorted({m for l in log.splitlines() if 'static_assert(' not in l
                       for m in re.findall(r"FFMAUDIT ([A-Za-z0-9_]+: [^\"']+)", l)})

    # --- struct layout pass -------------------------------------------------
    structs = []
    for f in glob.glob(args.sources, recursive=True):
        structs += extract.extract_structs(f)
    mapping, map_file = {}, os.path.join(HERE, 'structs', args.platform + '.txt')
    if os.path.exists(map_file):
        for line in open(map_file):
            line = line.split('#')[0].strip()
            if '=' in line:
                k, v = line.split('=', 1)
                mapping[k.strip()] = v.strip()
    sizes, offsets, unresolved = gen.struct_sizes(structs)
    ssrc = os.path.join(work, 'structs.cpp')
    sskip, slog = set(), ''
    for _ in range(8):
        scode, schecked, sfields = gen.emit_structs(structs, mapping, sizes, offsets, sskip)
        open(ssrc, 'w').write(PRE_INCLUDES % '\n'.join('#include <%s>' % h for h in includes) + scode)
        slog = run(cmd, ssrc)
        # a type or member the SDK does not declare: drop that layout and retry
        # a field the SDK does not declare drops just that field; an unknown type drops the layout
        drop = set()
        ctype_owner = {mapping[n].split()[-1]: n for n in mapping}
        for m in re.finditer(tc['no_member'], slog):
            owner, fld = m.group('type'), m.group('member')
            if owner in ctype_owner:
                drop.add((ctype_owner[owner], fld))
        for typ in re.findall(tc['unknown_type'], slog) + re.findall(tc['undeclared'], slog):
            if typ in ctype_owner:
                drop.add(ctype_owner[typ])
        drop -= sskip
        if not drop:
            break
        sskip |= drop
    open(os.path.join(work, 'structs.log'), 'w').write(slog)
    # Pair each failure with both numbers: cmp<what the header says, what OSHI's layout says>. The
    # three compilers word the "undefined template" diagnostic differently and MSVC does not echo the
    # offending source line at all, but all of them name the file and line -- and the generated source
    # says which check is on that line.
    labels = {}
    for n, l in enumerate(open(ssrc).read().splitlines(), 1):
        lab = re.search(r'// FFMAUDIT ([A-Za-z0-9_]+: .+?)\s*$', l)
        if lab:
            labels[n] = lab.group(1)
    sfindings, failed_lines = set(), set()
    for l in slog.splitlines():
        n = re.search(r'structs\.cpp[:(](\d+)', l)
        if not n or int(n.group(1)) not in labels:
            continue
        failed_lines.add(int(n.group(1)))
        c = re.search(r'cmp<\s*(\d+)\s*,\s*(\d+)\s*>', l)
        if c:
            sfindings.add('%s -- header says %s, layout says %s'
                          % (labels[int(n.group(1))], c.group(1), c.group(2)))
    sfindings = sorted(sfindings)
    unmapped = sorted({n for (f, n) in sizes if not gen.ctype_for({'file': f, 'name': n}, mapping)})
    print(f'{args.platform}: {len(structs)} struct layouts, {schecked} checked against the SDK '
          f'({sfields} field offsets), {len(unmapped)} unmapped, {len(unresolved)} unresolvable', flush=True)
    if unmapped:
        print('  unmapped (add to structs/%s.txt to check): %s' % (args.platform, ', '.join(unmapped)))
    if unresolved:
        print('  unresolvable layout arithmetic: ' + ', '.join(sorted(n for (_f, n) in unresolved)))
    if sskip:
        layouts = sorted(x for x in sskip if isinstance(x, str))
        fields = sorted('%s.%s' % x for x in sskip if isinstance(x, tuple))
        if layouts:
            print('  no SDK declaration: ' + ', '.join(layouts))
        if fields:
            print('  fields absent from the SDK struct: ' + ', '.join(fields))
    # Anything either pass reported that is neither a recognized finding nor a documented skip means
    # the harness itself is broken on this platform, and must not be mistaken for a clean run. Both
    # passes need this: a structs.cpp that fails to compile for an unexpected reason produces no
    # cmp<> findings at all, which would otherwise read as a clean struct audit.
    def unexpected(text, recognized, ok_lines=()):
        out = []
        for l in text.splitlines():
            if not re.search(r'\berror\b', l) or re.search(tc['undeclared'], l):
                continue
            if re.match(r'\d+ errors? generated\.$', l.strip()):
                continue      # clang's tally of the diagnostics above, not a diagnostic itself
            if any(re.search(p, l) for p in recognized):
                continue
            n = re.search(r'\.cpp[:(](\d+)', l)
            if n and int(n.group(1)) in ok_lines:
                continue      # a follow-on diagnostic on a line already counted as a finding
            out.append(l)
        return out

    # signature pass: a failed static_assert carries the FFMAUDIT message on the diagnostic line
    other = unexpected(log, [r'FFMAUDIT'])
    # struct pass: a mismatch is an undefined cmp<header value, layout value>; the rest are the skips
    other += unexpected(slog, [r'cmp<\s*\d+\s*,\s*\d+\s*>', tc['no_member'], tc['unknown_type']],
                        failed_lines)

    undeclared_only = skip - exceptions
    print(f'{args.platform}: checked {checked} bindings, {len(undeclared_only)} with no SDK '
          f'declaration, {len(exceptions & skip)} documented exceptions', flush=True)
    if undeclared_only:
        print('  no SDK declaration: ' + ', '.join(sorted(undeclared_only)))
    if exceptions & skip:
        print('  documented exceptions: ' + ', '.join(sorted(exceptions & skip)))
    if other:
        print(f'\n{args.platform}: HARNESS ERROR -- {len(other)} unexpected compiler errors')
        for l in other[:20]:
            print('  ' + l.strip())
        return 2
    if findings or sfindings:
        if findings:
            print(f'\n{args.platform}: {len(findings)} signature mismatches')
            for f in findings:
                print('  ' + f)
        if sfindings:
            print(f'\n{args.platform}: {len(sfindings)} struct layout mismatches')
            for f in sfindings:
                print('  ' + f)
        return 1
    print(f'\n{args.platform}: no signature or struct layout mismatches')
    return 0


def extract_norm(a):
    return a.rsplit('.', 1)[-1] if a.startswith('java.lang.foreign.') else a


if __name__ == '__main__':
    sys.exit(main())
