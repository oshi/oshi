import json, sys, re

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
