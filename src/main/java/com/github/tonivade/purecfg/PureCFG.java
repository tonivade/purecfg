/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.data.NonEmptyString;
import com.github.tonivade.purefun.free.FreeAp;
import com.github.tonivade.purefun.typeclasses.Applicative;

import static java.util.Objects.requireNonNull;

@HigherKind
public final class PureCFG<T> {

  private final FreeAp<DSL.µ, T> value;

  private PureCFG(DSL<T> value) {
    this(FreeAp.lift(value.kind1()));
  }

  private PureCFG(FreeAp<DSL.µ, T> value) {
    this.value = requireNonNull(value);
  }

  public <R> PureCFG<R> map(Function1<T, R> mapper) {
    return new PureCFG<>(value.map(mapper));
  }

  public <R> PureCFG<R> ap(PureCFG<Function1<T, R>> apply) {
    return new PureCFG<>(value.ap(apply.value));
  }

  public static <A, B, C> PureCFG<C> map2(PureCFG<A> fa, PureCFG<B> fb, Function2<A, B, C> apply) {
    return fb.ap(fa.map(apply.curried()));
  }

  public static <A, B, C, D> PureCFG<D> map3(
      PureCFG<A> fa, PureCFG<B> fb, PureCFG<C> fc, Function3<A, B, C, D> apply) {
    return fc.ap(map2(fa, fb, (a, b) -> apply.curried().apply(a).apply(b)));
  }

  public static <A, B, C, D, E> PureCFG<E> map4(
      PureCFG<A> fa, PureCFG<B> fb, PureCFG<C> fc, PureCFG<D> fd, Function4<A, B, C, D, E> apply) {
    return fd.ap(map3(fa, fb, fc, (a, b, c) -> apply.curried().apply(a).apply(b).apply(c)));
  }

  public static <A, B, C, D, E, F> PureCFG<F> map5(
      PureCFG<A> fa, PureCFG<B> fb, PureCFG<C> fc, PureCFG<D> fd, PureCFG<E> fe, Function5<A, B, C, D, E, F> apply) {
    return fe.ap(map4(fa, fb, fc, fd, (a, b, c, d) -> apply.curried().apply(a).apply(b).apply(c).apply(d)));
  }

  public static <T> PureCFG<T> pure(T value) {
    return new PureCFG<>(FreeAp.pure(value));
  }

  public static PureCFG<Integer> readInt(NonEmptyString key) {
    return new PureCFG<>(new DSL.ReadInt(key));
  }

  public static PureCFG<String> readString(NonEmptyString key) {
    return new PureCFG<>(new DSL.ReadString(key));
  }

  public static Applicative<PureCFG.µ> applicative() {
    return PureCFGApplicative.instance();
  }
}

@Instance
interface PureCFGApplicative extends Applicative<PureCFG.µ> {

  @Override
  default <T> Higher1<PureCFG.µ, T> pure(T value) {
    return PureCFG.pure(value).kind1();
  }

  @Override
  default <T, R> Higher1<PureCFG.µ, R> ap(
      Higher1<PureCFG.µ, T> value, Higher1<PureCFG.µ, Function1<T, R>> apply) {
    return value.fix1(PureCFG::narrowK).ap(apply.fix1(PureCFG::narrowK)).kind1();
  }
}

