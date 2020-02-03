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
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.data.NonEmptyString;
import com.github.tonivade.purefun.free.FreeAp;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;

import java.util.Properties;

import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;

@HigherKind
public final class PureCFG<T> {

  private final FreeAp<DSL.µ, T> value;

  private PureCFG(DSL<T> value) {
    this(FreeAp.lift(value.kind1()));
  }

  protected FreeAp<DSL.µ, T> value() {
    return value;
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

  protected <G extends Kind> Higher1<G, T> foldMap(FunctionK<DSL.µ, G> functionK, Applicative<G> applicative) {
    return value.foldMap(functionK, applicative);
  }

  public T fromProperties(Properties properties) {
    return value.foldMap(new PropertiesInterpreter(properties), IdInstances.applicative()).fix1(Id::narrowK).get();
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

  public static PureCFG<Integer> readInt(String key) {
    return new PureCFG<>(new DSL.ReadInt<>(NonEmptyString.of(key), identity()));
  }

  public static PureCFG<String> readString(String key) {
    return new PureCFG<>(new DSL.ReadString<>(NonEmptyString.of(key), identity()));
  }

  public static <T> PureCFG<T> readConfig(String key, PureCFG<T> cfg) {
    return new PureCFG<>(new DSL.ReadConfig<>(NonEmptyString.of(key), cfg));
  }

  public static Applicative<PureCFG.µ> applicative() {
    return PureCFGApplicative.instance();
  }

  private static final class PropertiesInterpreter implements FunctionK<DSL.µ, Id.µ> {

    private final Key baseKey;
    private final Properties properties;

    private PropertiesInterpreter(Properties properties) {
      this(Key.empty(), properties);
    }

    private PropertiesInterpreter(String baseKey, Properties properties) {
      this(Key.with(baseKey), properties);
    }

    private PropertiesInterpreter(Key baseKey, Properties properties) {
      this.baseKey = requireNonNull(baseKey);
      this.properties = requireNonNull(properties);
    }

    @Override
    public <T> Higher1<Id.µ, T> apply(Higher1<DSL.µ, T> from) {
      DSL<T> dsl = from.fix1(DSL::narrowK);
      String key = baseKey.extend(dsl.key());
      if (dsl instanceof DSL.ReadInt) {
        DSL.ReadInt<T> readInt = (DSL.ReadInt<T>) dsl;
        return Id.of(Integer.parseInt(properties.getProperty(key))).map(readInt.value());
      }
      if (dsl instanceof DSL.ReadString) {
        DSL.ReadString<T> readString = (DSL.ReadString<T>) dsl;
        return Id.of(properties.getProperty(key)).map(readString.value());
      }
      if (dsl instanceof DSL.ReadConfig) {
        DSL.ReadConfig<T> readConfig = (DSL.ReadConfig<T>) dsl;
        return readConfig.next().foldMap(
            new PropertiesInterpreter(key, properties), IdInstances.applicative()).fix1(Id::narrowK);
      }
      throw new IllegalStateException();
    }
  }

  @FunctionalInterface
  private interface Key {

    String extend(String key);

    static Key with(String baseKey) {
      requireNonNull(baseKey);
      return key -> baseKey + "." + key;
    }

    static Key empty() {
      return key -> key;
    }
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

