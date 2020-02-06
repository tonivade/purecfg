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
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.free.FreeAp;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;

import java.util.Properties;

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

  protected <G extends Kind> Higher1<G, T> foldMap(FunctionK<DSL.µ, G> functionK, Applicative<G> applicative) {
    return value.foldMap(functionK, applicative);
  }

  public T unsafeRun(Properties properties) {
    return value.foldMap(
        new PropertiesInterpreter<>(new IdVisitor(Key.empty(), properties)),
        IdInstances.applicative()).fix1(Id::narrowK).get();
  }

  public Option<T> safeRun(Properties properties) {
    return value.foldMap(
        new PropertiesInterpreter<>(new OptionVisitor(Key.empty(), properties)),
        OptionInstances.applicative()).fix1(Option::narrowK);
  }

  public Validation<Validation.Result<String>, T> validatedRun(Properties properties) {
    return value.foldMap(
        new PropertiesInterpreter<>(new ValidationVisitor(Key.empty(), properties)),
        ValidationInstances.applicative(Validation.Result::concat)).fix1(Validation::narrowK);
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
    return new PureCFG<>(new DSL.ReadInt(key));
  }

  public static PureCFG<String> readString(String key) {
    return new PureCFG<>(new DSL.ReadString(key));
  }

  public static PureCFG<Boolean> readBoolean(String key) {
    return new PureCFG<>(new DSL.ReadBoolean(key));
  }

  public static <T> PureCFG<T> readConfig(String key, PureCFG<T> cfg) {
    return new PureCFG<>(new DSL.ReadConfig<>(key, cfg));
  }

  public static Applicative<PureCFG.µ> applicative() {
    return PureCFGApplicative.instance();
  }

  private static final class PropertiesInterpreter<F extends Kind> implements FunctionK<DSL.µ, F> {

    private final DSL.Visitor<F> visitor;

    private PropertiesInterpreter(DSL.Visitor<F> visitor) {
      this.visitor = requireNonNull(visitor);
    }

    @Override
    public <T> Higher1<F, T> apply(Higher1<DSL.µ, T> from) {
      return from.fix1(DSL::narrowK).accept(visitor);
    }
  }

  private static final class IdVisitor implements DSL.Visitor<Id.µ> {

    private final Key baseKey;
    private final Properties properties;

    private IdVisitor(Key baseKey, Properties properties) {
      this.baseKey = requireNonNull(baseKey);
      this.properties = requireNonNull(properties);
    }

    @Override
    public Higher1<Id.µ, String> visit(DSL.ReadString value) {
      return Id.of(properties.getProperty(baseKey.extend(value))).kind1();
    }

    @Override
    public Higher1<Id.µ, Integer> visit(DSL.ReadInt value) {
      return Id.of(Integer.parseInt(properties.getProperty(baseKey.extend(value)))).kind1();
    }

    @Override
    public Higher1<Id.µ, Boolean> visit(DSL.ReadBoolean value) {
      return Id.of(Boolean.parseBoolean(properties.getProperty(baseKey.extend(value)))).kind1();
    }

    @Override
    public <A> Higher1<Id.µ, A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), IdInstances.applicative()).fix1(Id::narrowK);
    }

    private <A> PropertiesInterpreter<Id.µ> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new PropertiesInterpreter<>(new IdVisitor(Key.with(value.key()), properties));
    }
  }

  private static final class OptionVisitor implements DSL.Visitor<Option.µ> {

    private final Key baseKey;
    private final Properties properties;

    private OptionVisitor(Key baseKey, Properties properties) {
      this.baseKey = requireNonNull(baseKey);
      this.properties = requireNonNull(properties);
    }

    @Override
    public Higher1<Option.µ, String> visit(DSL.ReadString value) {
      return Option.of(properties.getProperty(baseKey.extend(value))).kind1();
    }

    @Override
    public Higher1<Option.µ, Integer> visit(DSL.ReadInt value) {
      return Option.of(properties.getProperty(baseKey.extend(value))).map(Integer::parseInt).kind1();
    }

    @Override
    public Higher1<Option.µ, Boolean> visit(DSL.ReadBoolean value) {
      return Option.of(properties.getProperty(baseKey.extend(value))).map(Boolean::parseBoolean).kind1();
    }

    @Override
    public <A> Higher1<Option.µ, A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), OptionInstances.applicative()).fix1(Option::narrowK);
    }

    private <A> PropertiesInterpreter<Option.µ> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new PropertiesInterpreter<>(new OptionVisitor(Key.with(value.key()), properties));
    }
  }

  private static final class ValidationVisitor implements DSL.Visitor<Higher1<Validation.µ, Validation.Result<String>>> {

    private final Key baseKey;
    private final Properties properties;

    private ValidationVisitor(Key baseKey, Properties properties) {
      this.baseKey = requireNonNull(baseKey);
      this.properties = requireNonNull(properties);
    }

    @Override
    public Higher2<Validation.µ, Validation.Result<String>, String> visit(DSL.ReadString value) {
      return Option.of(properties.getProperty(baseKey.extend(value)))
          .fold(() -> invalid(value), this::valid).kind2();
    }

    @Override
    public Higher2<Validation.µ, Validation.Result<String>, Integer> visit(DSL.ReadInt value) {
      return Option.of(properties.getProperty(baseKey.extend(value))).map(Integer::parseInt)
        .fold(() -> invalid(value), this::valid).kind2();
    }

    @Override
    public Higher2<Validation.µ, Validation.Result<String>, Boolean> visit(DSL.ReadBoolean value) {
      return Option.of(properties.getProperty(baseKey.extend(value))).map(Boolean::parseBoolean)
          .fold(() -> invalid(value), this::valid).kind2();
    }

    @Override
    public <A> Higher2<Validation.µ, Validation.Result<String>, A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), ValidationInstances.applicative(Validation.Result::concat))
          .fix1(Validation::narrowK);
    }

    private <A> PropertiesInterpreter<Higher1<Validation.µ, Validation.Result<String>>> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new PropertiesInterpreter<>(new ValidationVisitor(Key.with(value.key()), properties));
    }

    private <T> Validation<Validation.Result<String>, T> invalid(DSL<T> value) {
      return Validation.invalid(Validation.Result.of("key not found: " + baseKey.extend(value)));
    }

    private <T> Validation<Validation.Result<String>, T> valid(T value) {
      return Validation.valid(value);
    }
  }

  @FunctionalInterface
  private interface Key {

    String extend(DSL<?> dsl);

    static Key with(String baseKey) {
      requireNonNull(baseKey);
      return dsl -> baseKey + "." + dsl.key();
    }

    static Key empty() {
      return DSL::key;
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

