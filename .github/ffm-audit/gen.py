import json, os, sys, re

# layout -> (bytes, is_pointer); None = void
LAYOUT = {
    'ADDRESS': (8, True),
    'JAVA_INT': (4, False),
    'JAVA_LONG': (8, False),
    'JAVA_SHORT': (2, False),
    'JAVA_BYTE': (1, False),
    'JAVA_CHAR': (2, False),
    'JAVA_BOOLEAN': (1, False),
    'JAVA_FLOAT': (4, False),
    'JAVA_DOUBLE': (8, False),
    'SIZE_T': (8, False),
    'JAVA_LONG_UNALIGNED': (8, False),
    'JAVA_INT_UNALIGNED': (4, False),
    'CG_SIZE_LAYOUT': (16, False),
}

PRE = r'''
// Generated: checks OSHI's FFM function descriptors against the platform's real prototypes.
// Compiled as C++ only so a function type can be decomposed into its parameters. Deliberately uses
// no standard library header, so it runs anywhere a C++ compiler and the platform SDK exist.
%s

typedef decltype(sizeof(0)) audit_size_t;

struct no_arg {};                       // stands in for a parameter the real function does not have

template <class T> struct is_ptr { static const bool value = false; };
template <class T> struct is_ptr<T *> { static const bool value = true; };
template <class T> struct is_voidt { static const bool value = false; };
template <> struct is_voidt<void> { static const bool value = true; };
template <class T> struct rm_ptr { typedef T type; };
template <class T> struct rm_ptr<T *> { typedef T type; };
// The C++ spelling of some SDK typedefs is a reference where the C spelling is a pointer -- REFIID
// and REFCLSID are the common case. A reference is passed as a pointer, so compare it as one.
template <class T> struct deref { typedef T type; };
template <class T> struct deref<T &> { typedef T *type; };

template <class T> struct sz { static const audit_size_t value = sizeof(T); };
template <> struct sz<void> { static const audit_size_t value = 0; };

// nth type of a parameter pack; out of range falls back to no_arg rather than failing to compile,
// so an arity mismatch reports once instead of cascading
template <unsigned N, class... A> struct nth { typedef no_arg type; };
template <class H, class... T> struct nth<0, H, T...> { typedef H type; };
template <unsigned N, class H, class... T> struct nth<N, H, T...> { typedef typename nth<N - 1, T...>::type type; };

template <class F> struct sig;
template <class R, class... A> struct sig<R(A...)> {
  static const unsigned arity = sizeof...(A);
  typedef R ret;
  template <unsigned I> struct arg { typedef typename nth<I, A...>::type type; };
};
// variadic C functions: only the fixed parameters are checked
template <class R, class... A> struct sig<R(A..., ...)> {
  static const unsigned arity = sizeof...(A);
  typedef R ret;
  template <unsigned I> struct arg { typedef typename nth<I, A...>::type type; };
};
// since C++17 noexcept is part of the function type, and glibc declares much of libc noexcept,
// so the same two forms have to be matched again with it
template <class R, class... A> struct sig<R(A...) noexcept> {
  static const unsigned arity = sizeof...(A);
  typedef R ret;
  template <unsigned I> struct arg { typedef typename nth<I, A...>::type type; };
};
template <class R, class... A> struct sig<R(A..., ...) noexcept> {
  static const unsigned arity = sizeof...(A);
  typedef R ret;
  template <unsigned I> struct arg { typedef typename nth<I, A...>::type type; };
};
'''

def emit(bindings, includes, skip):
    out = [PRE % '\n'.join('#include <%s>' % h for h in includes)]
    checked = 0
    for b in bindings:
        s = b['symbol']
        if s in skip:
            continue
        ret, args = b['ret'], b['args']
        if ret is not None and ret not in LAYOUT:
            continue
        if any(a not in LAYOUT for a in args):
            continue
        checked += 1
        t = 'S_%s' % s
        out.append('typedef sig<rm_ptr<decltype(&%s)>::type> %s;' % (s, t))
        out.append('static_assert(%s::arity == %d, "FFMAUDIT %s: arity");' % (t, len(args), s))
        if ret is None:
            out.append('static_assert(is_voidt<%s::ret>::value, "FFMAUDIT %s: returns void");' % (t, s))
        else:
            rb, rp = LAYOUT[ret]
            out.append('static_assert(sz<deref<%s::ret>::type>::value == %d, "FFMAUDIT %s: return size");' % (t, rb, s))
            out.append('static_assert(is_ptr<deref<%s::ret>::type>::value == %s, "FFMAUDIT %s: return is%s a pointer");'
                       % (t, 'true' if rp else 'false', s, '' if rp else ' not'))
        for i, a in enumerate(args):
            ab, ap = LAYOUT[a]
            out.append('static_assert(sz<deref<%s::arg<%d>::type>::type>::value == %d, "FFMAUDIT %s: arg%d is %d bytes");'
                       % (t, i, ab, s, i, ab))
            out.append('static_assert(is_ptr<deref<%s::arg<%d>::type>::type>::value == %s, "FFMAUDIT %s: arg%d is%s a pointer");'
                       % (t, i, 'true' if ap else 'false', s, i, '' if ap else ' not'))
        out.append('')
    return '\n'.join(out), checked

if __name__ == '__main__':
    bindings = json.load(open(sys.argv[1]))
    includes = sys.argv[2].split(',')
    skip = set(open(sys.argv[3]).read().split()) if len(sys.argv) > 3 else set()
    code, n = emit(bindings, includes, skip)
    open(sys.argv[4], 'w').write(code)
    print(f'emitted checks for {n} of {len(bindings)} bindings ({len(skip)} skipped)', file=sys.stderr)


def _int(expr, consts):
    """A count or padding amount: an integer literal, a named int constant, or * and + over those."""
    expr = expr.strip()
    if not re.fullmatch(r'[\w\s*+]+', expr):
        return None
    try:
        return int(eval(expr, {'__builtins__': {}}, dict(consts)))
    except Exception:
        return None


def _size_of(expr, sizes, consts=None):
    """Byte size of one struct element, or None if it cannot be resolved yet."""
    consts = consts or {}
    expr = expr.strip()
    m = re.match(r'(?:MemoryLayout\s*\.\s*)?paddingLayout\(\s*(.+?)\s*\)\s*(?:\.withName\("[^"]*"\))?\s*$', expr, re.S)
    if m:
        return _int(m.group(1), consts)
    m = re.match(r'(?:MemoryLayout\s*\.\s*)?sequenceLayout\(\s*(.+?)\s*,\s*(.+?)\s*\)\s*(?:\.withName\("[^"]*"\))?\s*$',
                 expr, re.S)
    if m:
        cnt = _int(m.group(1), consts)
        inner = _size_of(m.group(2), sizes, consts)
        return None if (cnt is None or inner is None) else cnt * inner
    # an inline anonymous struct/union used as one element
    m = re.match(r'(?:MemoryLayout\s*\.\s*)?(struct|union)Layout\s*\((.*)\)\s*(?:\.withName\("[^"]*"\))?\s*$',
                 expr, re.S)
    if m:
        import extract as _x
        total = 0
        for part in _x.split_top(m.group(2)):
            n = _size_of(part, sizes, consts)
            if n is None:
                return None
            total = max(total, n) if m.group(1) == 'union' else total + n
        return total
    head = re.match(r'([A-Za-z_][\w.]*)', expr)
    if not head:
        return None
    # the leading dotted path may be qualified (ValueLayout.JAVA_INT) and is usually followed by
    # .withName(...); take the first component that names a layout rather than the whole chain
    for tok in head.group(1).split('.'):
        if tok in LAYOUT:
            return LAYOUT[tok][0]
        if tok in sizes:
            return sizes[tok]
    return None


def struct_sizes(structs):
    """Resolve every layout's total size and its named fields' offsets, packed as written.

    Two files may declare layouts with the same simple name -- ADDRINFO_LAYOUT and UTMPX_LAYOUT each
    exist for several platforms, and both VariantFFM and GuidFFM call theirs LAYOUT. Sizes are
    therefore resolved per file, and a nested reference is looked up in its own file first, exactly
    as Java would resolve it. A name that is ambiguous across files and absent from the referring
    one is left unresolved rather than silently matched to the wrong struct.
    """
    per_file, sizes, offsets = {}, {}, {}
    unresolved = set((s['file'], s['name']) for s in structs)
    by_key = {(s['file'], s['name']): s for s in structs}
    global_names = {}
    for s in structs:
        global_names.setdefault(s['name'], set()).add(s['file'])
    for _ in range(8):
        progressed = False
        for key in sorted(unresolved):
            s = by_key[key]
            scope = dict(per_file.get(s['file'], {}))
            # names unique across the whole run are also visible, as a static import would be
            for nm, files in global_names.items():
                if nm not in scope and len(files) == 1:
                    other = (next(iter(files)), nm)
                    if other in sizes:
                        scope[nm] = sizes[other]
            off, cur, ok = {}, 0, True
            for e in s['elems']:
                n = _size_of(e['expr'], scope, s.get('consts'))
                if n is None:
                    ok = False
                    break
                if e['name']:
                    off[e['name']] = 0 if s.get('union') else cur
                cur = max(cur, n) if s.get('union') else cur + n
            if ok:
                sizes[key], offsets[key] = cur, off
                per_file.setdefault(s['file'], {})[s['name']] = cur
                unresolved.discard(key)
                progressed = True
        if not progressed:
            break
    return sizes, offsets, unresolved


STRUCT_PRE = r'''
// A mismatch instantiates cmp<actual, expected>, so the compiler prints both numbers in its note
// rather than only the value OSHI claimed.
template <unsigned long Actual, unsigned long Expected> struct cmp;
template <unsigned long N> struct cmp<N, N> { typedef int ok; };

// Generated: checks OSHI's FFM struct layouts against the platform's real struct definitions.
// FFM structLayout is packed exactly as written -- padding is explicit, never inferred -- so each
// named field's offset is the running sum of the element sizes before it. A mismatch here is the
// defect a size-only test cannot see: a compensating paddingLayout can leave the total correct
// while every interior offset is wrong.
'''


def ctype_for(st, mapping):
    """The mapped C type, looked up by simple name or by File.NAME when a name is not unique."""
    qualified = '%s.%s' % (os.path.basename(st['file']).replace('.java', ''), st['name'])
    return mapping.get(qualified) or mapping.get(st['name'])


def emit_structs(structs, mapping, sizes, offsets, skip):
    """One static_assert per struct size and per named field offset, for mapped layouts only."""
    out = [STRUCT_PRE]
    checked = fields = 0
    for s in sorted(structs, key=lambda x: (x['name'], x['file'])):
        name, key = s['name'], (s['file'], s['name'])
        ctype = ctype_for(s, mapping)
        if not ctype or name in skip or key not in sizes:
            continue
        checked += 1
        out.append('typedef cmp<sizeof(%s), %dUL>::ok %s_size; // FFMAUDIT %s: size'
                   % (ctype, sizes[key], name, name))
        for field, off in sorted(offsets[key].items(), key=lambda kv: kv[1]):
            if (name, field) in skip:
                continue
            fields += 1
            out.append('typedef cmp<__builtin_offsetof(%s, %s), %dUL>::ok %s_%s_off; '
                       '// FFMAUDIT %s: %s offset' % (ctype, field, off, name, field, name, field))
        out.append('')
    return '\n'.join(out), checked, fields


DUMP_PRE = """
// Generated: prints the platform's real size and field offsets for each mapped layout, so a
// mismatch can be corrected against the header rather than guessed at.
%s
extern "C" int printf(const char *, ...);
int main() {
"""


def emit_dump(structs, mapping, offsets, skip):
    """A program that prints sizeof and offsetof for every mapped layout, as the header sees them."""
    out = []
    for st in sorted(structs, key=lambda x: (x['name'], x['file'])):
        name, key = st['name'], (st['file'], st['name'])
        ctype = ctype_for(st, mapping)
        if not ctype or name in skip:
            continue
        out.append('  printf("%-24s size %6lu\\n", "' + name + '", (unsigned long)sizeof(' + ctype + '));')
        for field, _ in sorted(offsets.get(key, {}).items(), key=lambda kv: kv[1]):
            if (name, field) in skip:
                continue
            out.append('  printf("  %-22s %6lu\\n", "' + field + '", '
                       '(unsigned long)__builtin_offsetof(' + ctype + ', ' + field + '));')
    out.append('  return 0;')
    out.append('}')
    return '\n'.join(out)
