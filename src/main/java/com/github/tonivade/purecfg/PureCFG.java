/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import static java.util.Objects.requireNonNull;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.data.SequenceOf;
import com.github.tonivade.purefun.free.FreeAp;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.ConstOf;
import com.github.tonivade.purefun.type.Const_;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.IdOf;
import com.github.tonivade.purefun.type.Id_;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionOf;
import com.github.tonivade.purefun.type.Option_;
import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.type.Validation_;
import com.github.tonivade.purefun.type.ValidationOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Monoid;

@HigherKind
public final class PureCFG<T> implements PureCFGOf<T> {

  private final FreeAp<DSL_, T> value;

  protected PureCFG(DSL<T> value) {
    this(FreeAp.lift(value));
  }

  protected PureCFG(FreeAp<DSL_, T> value) {
    this.value = requireNonNull(value);
  }

  public <R> PureCFG<R> map(Function1<T, R> mapper) {
    return new PureCFG<>(value.map(mapper));
  }

  public <R> PureCFG<R> ap(PureCFG<Function1<T, R>> apply) {
    return new PureCFG<>(value.ap(apply.value));
  }

  protected <G extends Kind> Higher1<G, T> foldMap(FunctionK<DSL_, G> functionK, Applicative<G> applicative) {
    return value.foldMap(functionK, applicative);
  }

  public T unsafeRun(Source source) {
    return value.foldMap(
        new Interpreter<>(new IdVisitor(Key.empty(), source)),
        IdInstances.applicative()).fix1(IdOf::narrowK).get();
  }

  public Option<T> safeRun(Source source) {
    return value.foldMap(
        new Interpreter<>(new OptionVisitor(Key.empty(), source)),
        OptionInstances.applicative()).fix1(OptionOf::narrowK);
  }

  public Validation<Validation.Result<String>, T> validatedRun(Source source) {
    return value.foldMap(
        new Interpreter<>(new ValidationVisitor(Key.empty(), source)),
        ValidationInstances.applicative(Validation.Result::concat)).fix1(ValidationOf::narrowK);
  }

  public String describe() {
    return value.analyze(
        new Interpreter<>(new ConstVisitor(Key.empty())),
        ConstInstances.applicative(Monoid.string()));
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

  public static <T> PureCFG<Iterable<T>> readIterable(String key, Class<T> type) {
    return new PureCFG<>(new DSL.ReadPrimitiveIterable<>(key, type));
  }

  public static <T> PureCFG<Iterable<T>> readIterable(String key, PureCFG<T> item) {
    return new PureCFG<>(new DSL.ReadIterable<>(key, item));
  }

  public static <T> PureCFG<T> readConfig(String key, PureCFG<T> cfg) {
    return new PureCFG<>(new DSL.ReadConfig<>(key, cfg));
  }

  public static Applicative<PureCFG_> applicative() {
    return PureCFGApplicative.INSTANCE;
  }

  private static final class Interpreter<F extends Kind> implements FunctionK<DSL_, F> {

    private final DSL.Visitor<F> visitor;

    private Interpreter(DSL.Visitor<F> visitor) {
      this.visitor = requireNonNull(visitor);
    }

    @Override
    public <T> Higher1<F, T> apply(Higher1<DSL_, T> from) {
      return from.fix1(DSLOf::narrowK).accept(visitor);
    }
  }

  private abstract static class AbstractVisitor<F extends Kind> implements DSL.Visitor<F> {

    private final Key baseKey;
    private final Source source;

    private AbstractVisitor(Key baseKey, Source source) {
      this.baseKey = requireNonNull(baseKey);
      this.source = requireNonNull(source);
    }

    protected Source getSource() {
      return source;
    }

    protected String extend(DSL<?> value) {
      return baseKey.extend(value);
    }

    protected Option<String> getString(DSL<?> value) {
      return source.getString(extend(value));
    }

    protected Option<Integer> getInteger(DSL<?> value) {
      return source.getInteger(extend(value));
    }

    protected Option<Boolean> getBoolean(DSL<?> value) {
      return source.getBoolean(extend(value));
    }

    protected <T> Sequence<Higher1<F, T>> readAll(DSL.ReadPrimitiveIterable<T> value) {
      Iterable<DSL<T>> properties = source.getIterable(extend(value), value.type());
      return ImmutableArray.from(properties).map(dsl -> dsl.accept(this));
    }

    protected <T> Sequence<Higher1<F, T>> readAll(DSL.ReadIterable<T> value) {
      Iterable<DSL<T>> properties = source.getIterable(extend(value), value.next());
      return ImmutableArray.from(properties).map(dsl -> dsl.accept(this));
    }
  }

  private static final class IdVisitor extends AbstractVisitor<Id_> {

    private IdVisitor(Key baseKey, Source source) {
      super(baseKey, source);
    }

    @Override
    public <T> Id<T> visit(DSL.Pure<T> value) {
      return Id.of(value.get());
    }

    @Override
    public Id<String> visit(DSL.ReadString value) {
      return Id.of(getString(value).getOrElseThrow(NullPointerException::new));
    }

    @Override
    public Id<Integer> visit(DSL.ReadInt value) {
      return Id.of(getInteger(value).getOrElseThrow(NullPointerException::new));
    }

    @Override
    public Id<Boolean> visit(DSL.ReadBoolean value) {
      return Id.of(getBoolean(value).getOrElseThrow(NullPointerException::new));
    }

    @Override
    public <T> Id<Iterable<T>> visit(DSL.ReadIterable<T> value) {
      return SequenceInstances.traverse()
          .sequence(IdInstances.applicative(), readAll(value))
          .fix1(IdOf::narrowK).map(s -> s.fix1(SequenceOf::narrowK));
    }

    @Override
    public <T> Id<Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      return SequenceInstances.traverse()
          .sequence(IdInstances.applicative(), readAll(value))
          .fix1(IdOf::narrowK).map(s -> s.fix1(SequenceOf::narrowK));
    }

    @Override
    public <A> Id<A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), IdInstances.applicative()).fix1(IdOf::narrowK);
    }

    private <A> Interpreter<Id_> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new IdVisitor(Key.with(value.key()), getSource()));
    }
  }

  private static final class OptionVisitor extends AbstractVisitor<Option_> {

    private OptionVisitor(Key baseKey, Source source) {
      super(baseKey, source);
    }

    @Override
    public <T> Option<T> visit(DSL.Pure<T> value) {
      return Option.of(value.get());
    }

    @Override
    public Option<String> visit(DSL.ReadString value) {
      return getString(value);
    }

    @Override
    public Option<Integer> visit(DSL.ReadInt value) {
      return getInteger(value);
    }

    @Override
    public Option<Boolean> visit(DSL.ReadBoolean value) {
      return getBoolean(value);
    }

    @Override
    public <T> Option<Iterable<T>> visit(DSL.ReadIterable<T> value) {
      return SequenceInstances.traverse()
          .sequence(OptionInstances.applicative(), readAll(value))
          .fix1(OptionOf::narrowK).map(s -> s.fix1(SequenceOf::narrowK));
    }

    @Override
    public <T> Option<Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      return SequenceInstances.traverse()
          .sequence(OptionInstances.applicative(), readAll(value))
          .fix1(OptionOf::narrowK).map(s -> s.fix1(SequenceOf::narrowK));
    }

    @Override
    public <A> Option<A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), OptionInstances.applicative()).fix1(OptionOf::narrowK);
    }

    private <A> Interpreter<Option_> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new OptionVisitor(Key.with(value.key()), getSource()));
    }
  }

  private static final class ValidationVisitor
      extends AbstractVisitor<Higher1<Validation_, Validation.Result<String>>> {

    private ValidationVisitor(Key baseKey, Source source) {
      super(baseKey, source);
    }

    @Override
    public <T> Validation<Validation.Result<String>, T> visit(DSL.Pure<T> value) {
      return Validation.valid(value.get());
    }

    @Override
    public Validation<Validation.Result<String>, String> visit(DSL.ReadString value) {
      return getString(value).fold(() -> invalid(value), this::valid);
    }

    @Override
    public Validation<Validation.Result<String>, Integer> visit(DSL.ReadInt value) {
      return getInteger(value).fold(() -> invalid(value), this::valid);
    }

    @Override
    public Validation<Validation.Result<String>, Boolean> visit(DSL.ReadBoolean value) {
      return getBoolean(value).fold(() -> invalid(value), this::valid);
    }

    @Override
    public <T> Validation<Validation.Result<String>, Iterable<T>> visit(DSL.ReadIterable<T> value) {
      return SequenceInstances.traverse()
          .sequence(ValidationInstances.applicative(Validation.Result::concat), readAll(value))
          .fix1(ValidationOf::narrowK).map(s -> s.fix1(SequenceOf::narrowK));
    }

    @Override
    public <T> Validation<Validation.Result<String>, Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      return SequenceInstances.traverse()
          .sequence(ValidationInstances.applicative(Validation.Result::concat), readAll(value))
          .fix1(ValidationOf::narrowK).map(s -> s.fix1(SequenceOf::narrowK));
    }

    @Override
    public <A> Validation<Validation.Result<String>, A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), ValidationInstances.applicative(Validation.Result::concat))
          .fix1(ValidationOf::narrowK);
    }

    private <A> Interpreter<Higher1<Validation_, Validation.Result<String>>> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new ValidationVisitor(Key.with(value.key()), getSource()));
    }

    private <T> Validation<Validation.Result<String>, T> invalid(DSL<T> value) {
      return Validation.invalid(Validation.Result.of("key not found: " + extend(value)));
    }

    private <T> Validation<Validation.Result<String>, T> valid(T value) {
      return Validation.valid(value);
    }
  }

  private static final class ConstVisitor implements DSL.Visitor<Higher1<Const_, String>> {

    private final Key baseKey;

    private ConstVisitor(Key baseKey) {
      this.baseKey = requireNonNull(baseKey);
    }

    @Override
    public <T> Const<String, T> visit(DSL.Pure<T> value) {
      return typeOf(value, String.valueOf(value.get()));
    }

    @Override
    public Const<String, String> visit(DSL.ReadString value) {
      return typeOf(value, "String");
    }

    @Override
    public Const<String, Integer> visit(DSL.ReadInt value) {
      return typeOf(value, "Integer");
    }

    @Override
    public Const<String, Boolean> visit(DSL.ReadBoolean value) {
      return typeOf(value, "Boolean");
    }

    @Override
    public <T> Const<String, Iterable<T>> visit(DSL.ReadIterable<T> value) {
      return visit(new DSL.ReadConfig<>(extend(value) + ".[]", value.next())).fix2(ConstOf::narrowK).retag();
    }

    @Override
    public <T> Const<String, Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      return typeOf(value, value.type().getSimpleName() + "[]");
    }

    @Override
    public <A> Const<String, A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(
          nestedInterpreter(value), ConstInstances.applicative(Monoid.string())).fix1(ConstOf::narrowK);
    }

    private <T> Const<String, T> typeOf(DSL<T> value, String type) {
      return Const.of("- " + extend(value) + ": " + type + "\n");
    }

    private <A> Interpreter<Higher1<Const_, String>> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new ConstVisitor(Key.with(extend(value))));
    }

    private String extend(DSL<?> value) {
      return baseKey.extend(value);
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

interface PureCFGApplicative extends Applicative<PureCFG_> {

  PureCFGApplicative INSTANCE = new PureCFGApplicative() { };

  @Override
  default <T> PureCFG<T> pure(T value) {
    return PureCFG.pure(value);
  }

  @Override
  default <T, R> PureCFG<R> ap(Higher1<PureCFG_, T> value, Higher1<PureCFG_, Function1<T, R>> apply) {
    return value.fix1(PureCFGOf::narrowK).ap(apply.fix1(PureCFGOf::narrowK));
  }
}

