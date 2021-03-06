package pond.common.f;

import pond.common.S;

public class Either<A, B> {
  public final Option<A> _a;
  public final Option<B> _b;

  protected Either(Option<A> a, Option<B> b) {
    _a = a;
    _b = b;
  }

  public static <A, B> Either<A, B> _a(A a) {
    return new Either<A, B>(S.<A>some(a), S.<B>none());
  }

  public static <A, B> Either<A, B> _b(B b) {
    return new Either<A, B>(S.<A>none(), S.some(b));
  }

  public static <A, B, C> _3<A, B, C> _3a(A a) {
    return new _3<A, B, C>(S.some(a), S.<B>none(), S.<C>none());
  }

  public static <A, B, C> _3<A, B, C> _3b(B b) {
    return new _3<A, B, C>(S.<A>none(), S.some(b), S.<C>none());
  }

  public static <A, B, C> _3<A, B, C> _3c(C c) {
    return new _3<A, B, C>(S.<A>none(), S.<B>none(), S.<C>some(c));
  }

  public static <A, B, C, D> _4<A, B, C, D> _4a(A a) {
    return new _4<A, B, C, D>(S.some(a), S.<B>none(), S.<C>none(), S.<D>none());
  }

  public static <A, B, C, D> _4<A, B, C, D> _4b(B b) {
    return new _4<A, B, C, D>(S.<A>none(), S.some(b), S.<C>none(), S.<D>none());
  }

  public static <A, B, C, D> _4<A, B, C, D> _4c(C c) {
    return new _4<A, B, C, D>(S.<A>none(), S.<B>none(), S.<C>some(c), S.<D>none());
  }

  public static <A, B, C, D> _4<A, B, C, D> _4d(D d) {
    return new _4<A, B, C, D>(S.<A>none(), S.<B>none(), S.<C>none(), S.<D>some(d));
  }

  public static <A, B, C, D, E> _5<A, B, C, D, E> _5a(A a) {
    return new _5<A, B, C, D, E>(S.some(a), S.<B>none(), S.<C>none(), S.<D>none(), S.<E>none());
  }

  public static <A, B, C, D, E> _5<A, B, C, D, E> _5b(B b) {
    return new _5<A, B, C, D, E>(S.<A>none(), S.some(b), S.<C>none(), S.<D>none(), S.<E>none());
  }

  public static <A, B, C, D, E> _5<A, B, C, D, E> _5c(C c) {
    return new _5<A, B, C, D, E>(S.<A>none(), S.<B>none(), S.<C>some(c), S.<D>none(), S.<E>none());
  }

  public static <A, B, C, D, E> _5<A, B, C, D, E> _5d(D d) {
    return new _5<A, B, C, D, E>(S.<A>none(), S.<B>none(), S.<C>none(), S.<D>some(d), S.<E>none());
  }

  public static <A, B, C, D, E> _5<A, B, C, D, E> _5e(E e) {
    return new _5<A, B, C, D, E>(S.<A>none(), S.<B>none(), S.<C>none(), S.<D>none(), S.<E>some(e));
  }

  public static class _3<A, B, C> {
    public final Option<A> _a;
    public final Option<B> _b;
    public final Option<C> _c;

    protected _3(Option<A> a, Option<B> b, Option<C> c) {
      _a = a;
      _b = b;
      _c = c;
    }
  }

  public static class _4<A, B, C, D> {
    public final Option<A> _a;
    public final Option<B> _b;
    public final Option<C> _c;
    public final Option<D> _d;

    protected _4(Option<A> a, Option<B> b, Option<C> c, Option<D> d) {
      _a = a;
      _b = b;
      _c = c;
      _d = d;
    }
  }

  public static class _5<A, B, C, D, E> {
    public final Option<A> _a;
    public final Option<B> _b;
    public final Option<C> _c;
    public final Option<D> _d;
    public final Option<E> _e;

    protected _5(Option<A> a, Option<B> b, Option<C> c, Option<D> d,
                 Option<E> e) {
      _a = a;
      _b = b;
      _c = c;
      _d = d;
      _e = e;
    }

  }
}
