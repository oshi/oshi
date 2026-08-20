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
TOOLCHAINS = {
    'clang': dict(
        cmd=['clang++', '-std=c++17', '-fsyntax-only', '-ferror-limit=0'],
        undeclared=r"use of undeclared identifier '([A-Za-z0-9_]+)'"),
    'gcc': dict(
        cmd=['g++', '-std=c++17', '-fsyntax-only', '-fmax-errors=0'],
        undeclared=r"error: '([A-Za-z0-9_]+)' was not declared"),
    'msvc': dict(
        cmd=['cl', '/std:c++17', '/Zs', '/nologo', '/D_CRT_SECURE_NO_WARNINGS'],
        undeclared=r"error C2065: '([A-Za-z0-9_]+)': undeclared identifier"),
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
    # anything the compiler reported that is neither a skip nor an assert means the harness itself
    # is broken on this platform, and must not be mistaken for a clean run
    other = [l for l in log.splitlines()
             if re.search(r'\berror\b', l) and 'FFMAUDIT' not in l
             and not re.search(tc['undeclared'], l)]

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
    if findings:
        print(f'\n{args.platform}: {len(findings)} signature mismatches')
        for f in findings:
            print('  ' + f)
        return 1
    print(f'\n{args.platform}: no signature mismatches')
    return 0


def extract_norm(a):
    return a.rsplit('.', 1)[-1] if a.startswith('java.lang.foreign.') else a


if __name__ == '__main__':
    sys.exit(main())
