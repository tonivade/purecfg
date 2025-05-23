/*
 * Copyright (c) 2020-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Applicable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Function3;
import com.github.tonivade.purefun.core.Function4;
import com.github.tonivade.purefun.core.Function5;
import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.data.SequenceOf;
import com.github.tonivade.purefun.free.FreeAp;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.ConstOf;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.IdOf;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionOf;
import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.type.Validation.Result;
import com.github.tonivade.purefun.type.ValidationOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Instance;
import com.github.tonivade.purefun.typeclasses.Instances;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.Semigroup;

@HigherKind
public final class PureCFG<T> implements PureCFGOf<T>, Applicable<PureCFG<?>, T> {

  private final FreeAp<DSL<?>, T> value;

  private PureCFG(DSL<T> value) {
    this(FreeAp.lift(value));
  }

  private PureCFG(FreeAp<DSL<?>, T> value) {
    this.value = checkNonNull(value);
  }

  @Override
  public <R> PureCFG<R> map(Function1<? super T, ? extends R> mapper) {
    return new PureCFG<>(value.map(mapper));
  }

  @Override
  public <R> PureCFG<R> ap(Kind<PureCFG<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return new PureCFG<>(value.ap(apply.fix(PureCFGOf::toPureCFG).value));
  }

  private <G extends Kind<G, ?>> Kind<G, T> foldMap(FunctionK<DSL<?>, G> functionK, Applicative<G> applicative) {
    return value.foldMap(functionK, applicative);
  }

  public T unsafeRun(Source source) {
    return value.foldMap(
        new Interpreter<>(new IdVisitor(Key.empty(), source)),
        Instances.applicative()).fix(IdOf::toId).value();
  }

  public Option<T> safeRun(Source source) {
    return value.foldMap(
        new Interpreter<>(new OptionVisitor(Key.empty(), source)),
        Instances.applicative()).fix(OptionOf::toOption);
  }

  public Validation<Validation.Result<String>, T> validatedRun(Source source) {
    var instance = new Instance<Validation<Validation.Result<String>, ?>>() {};
    Semigroup<Result<String>> semigroup = Validation.Result::concat;
    return value.foldMap(
        new Interpreter<>(new ValidationVisitor(Key.empty(), source)),
        instance.applicative(semigroup)).fix(ValidationOf::toValidation);
  }

  public String describe() {
    var instance = new Instance<Const<String, ?>>() {};
    return value.analyze(
        new Interpreter<>(new ConstVisitor(Key.empty())),
        instance.applicative(Monoid.string()));
  }

  public static <A, B, C> PureCFG<C> mapN(PureCFG<? extends A> fa, PureCFG<? extends B> fb,
      Function2<? super A, ? super B, ? extends C> apply) {
    return fb.ap(fa.map(apply.curried()));
  }

  public static <A, B> Map2<A, B> mapN(PureCFG<? extends A> fa, PureCFG<? extends B> fb) {
    return new Map2<>(fa, fb);
  }

  public static <A, B, C, D> PureCFG<D> mapN(
      PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc,
      Function3<? super A, ? super B, ? super C, ? extends D> apply) {
    return fc.ap(mapN(fa, fb, (a, b) -> apply.curried().apply(a).apply(b)));
  }

  public static <A, B, C> Map3<A, B, C> mapN(
      PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc) {
    return new Map3<>(fa, fb, fc);
  }

  public static <A, B, C, D, E> PureCFG<E> mapN(
      PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc, PureCFG<? extends D> fd,
      Function4<? super A, ? super B, ? super C, ? super D, ? extends E> apply) {
    return fd.ap(mapN(fa, fb, fc, (a, b, c) -> apply.curried().apply(a).apply(b).apply(c)));
  }

  public static <A, B, C, D> Map4<A, B, C, D> mapN(
      PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc, PureCFG<? extends D> fd) {
    return new Map4<>(fa, fb, fc, fd);
  }

  public static <A, B, C, D, E, F> PureCFG<F> mapN(
      PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc, PureCFG<? extends D> fd, PureCFG<? extends E> fe,
      Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends F> apply) {
    return fe.ap(mapN(fa, fb, fc, fd, (a, b, c, d) -> apply.curried().apply(a).apply(b).apply(c).apply(d)));
  }

  public static <A, B, C, D, E> Map5<A, B, C, D, E> mapN(
      PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc, PureCFG<? extends D> fd, PureCFG<? extends E> fe) {
    return new Map5<>(fa, fb, fc, fd, fe);
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

  public static <T> PureCFG<Iterable<T>> readIterable(String key, PureCFG<? extends T> item) {
    return new PureCFG<>(new DSL.ReadIterable<>(key, PureCFGOf.toPureCFG(item)));
  }

  public static <T> PureCFG<T> readConfig(String key, PureCFG<? extends T> cfg) {
    return new PureCFG<>(new DSL.ReadConfig<>(key, PureCFGOf.toPureCFG(cfg)));
  }

  public static Applicative<PureCFG<?>> applicative() {
    return PureCFGApplicative.INSTANCE;
  }

  private static final class Interpreter<F extends Kind<F, ?>> implements FunctionK<DSL<?>, F> {

    private final DSL.Visitor<F> visitor;

    private Interpreter(DSL.Visitor<F> visitor) {
      this.visitor = checkNonNull(visitor);
    }

    @Override
    public <T> Kind<F, T> apply(Kind<DSL<?>, ? extends T> from) {
      return from.fix(DSLOf::<T>toDSL).accept(visitor);
    }
  }

  private abstract static class AbstractVisitor<F extends Kind<F, ?>> implements DSL.Visitor<F> {

    private final Key baseKey;
    private final Source source;

    private AbstractVisitor(Key baseKey, Source source) {
      this.baseKey = checkNonNull(baseKey);
      this.source = checkNonNull(source);
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

    protected <T> Sequence<Kind<F, T>> readAll(DSL.ReadPrimitiveIterable<T> value) {
      Iterable<DSL<T>> properties = source.getIterable(extend(value), value.type());
      return ImmutableArray.from(properties).map(dsl -> dsl.accept(this));
    }

    protected <T> Sequence<Kind<F, T>> readAll(DSL.ReadIterable<T> value) {
      Iterable<DSL<T>> properties = source.getIterable(extend(value), value.next());
      return ImmutableArray.from(properties).map(dsl -> dsl.accept(this));
    }
  }

  private static final class IdVisitor extends AbstractVisitor<Id<?>> {

    private IdVisitor(Key baseKey, Source source) {
      super(baseKey, source);
    }

    @Override
    public <T> Id<T> visit(DSL.Pure<T> value) {
      return Id.of(value.get());
    }

    @Override
    public Id<String> visit(DSL.ReadString value) {
      return Id.of(getString(value).getOrElseThrow());
    }

    @Override
    public Id<Integer> visit(DSL.ReadInt value) {
      return Id.of(getInteger(value).getOrElseThrow());
    }

    @Override
    public Id<Boolean> visit(DSL.ReadBoolean value) {
      return Id.of(getBoolean(value).getOrElseThrow());
    }

    @Override
    public <T> Id<Iterable<T>> visit(DSL.ReadIterable<T> value) {
      return Instances.<Sequence<?>>traverse()
          .sequence(Instances.applicative(), readAll(value))
          .fix(IdOf::toId).map(s -> s.fix(SequenceOf::toSequence));
    }

    @Override
    public <T> Id<Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      return Instances.<Sequence<?>>traverse()
          .sequence(Instances.applicative(), readAll(value))
          .fix(IdOf::toId).map(s -> s.fix(SequenceOf::toSequence));
    }

    @Override
    public <A> Id<A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), Instances.applicative()).fix(IdOf::toId);
    }

    private <A> Interpreter<Id<?>> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new IdVisitor(Key.with(value.key()), getSource()));
    }
  }

  private static final class OptionVisitor extends AbstractVisitor<Option<?>> {

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
      return Instances.<Sequence<?>>traverse()
          .sequence(Instances.applicative(), readAll(value))
          .fix(OptionOf::toOption).map(s -> s.fix(SequenceOf::toSequence));
    }

    @Override
    public <T> Option<Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      return Instances.<Sequence<?>>traverse()
          .sequence(Instances.applicative(), readAll(value))
          .fix(OptionOf::toOption).map(s -> s.fix(SequenceOf::toSequence));
    }

    @Override
    public <A> Option<A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), Instances.applicative()).fix(OptionOf::toOption);
    }

    private <A> Interpreter<Option<?>> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new OptionVisitor(Key.with(value.key()), getSource()));
    }
  }

  private static final class ValidationVisitor
      extends AbstractVisitor<Validation<Validation.Result<String>, ?>> {

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
      var instance = new Instance<Validation<Validation.Result<String>, ?>>() {};
      Semigroup<Result<String>> semigroup = Validation.Result::concat;
      return Instances.<Sequence<?>>traverse()
          .sequence(instance.applicative(semigroup), readAll(value))
          .fix(ValidationOf::toValidation).map(s -> s.fix(SequenceOf::toSequence));
    }

    @Override
    public <T> Validation<Validation.Result<String>, Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      var instance = new Instance<Validation<Validation.Result<String>, ?>>() {};
      Semigroup<Result<String>> semigroup = Validation.Result::concat;
      return Instances.<Sequence<?>>traverse()
          .sequence(instance.applicative(semigroup), readAll(value))
          .fix(ValidationOf::toValidation).map(s -> s.fix(SequenceOf::toSequence));
    }

    @Override
    public <A> Validation<Validation.Result<String>, A> visit(DSL.ReadConfig<A> value) {
      var instance = new Instance<Validation<Validation.Result<String>, ?>>() {};
      Semigroup<Result<String>> semigroup = Validation.Result::concat;
      return value.next().foldMap(nestedInterpreter(value), instance.applicative(semigroup))
          .fix(ValidationOf::toValidation);
    }

    private <A> Interpreter<Validation<Validation.Result<String>, ?>> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new ValidationVisitor(Key.with(value.key()), getSource()));
    }

    private <T> Validation<Validation.Result<String>, T> invalid(DSL<T> value) {
      return Validation.invalid(Validation.Result.of("key not found: " + extend(value)));
    }

    private <T> Validation<Validation.Result<String>, T> valid(T value) {
      return Validation.valid(value);
    }
  }

  private static final class ConstVisitor implements DSL.Visitor<Const<String, ?>> {

    private final Key baseKey;

    private ConstVisitor(Key baseKey) {
      this.baseKey = checkNonNull(baseKey);
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
      return visit(new DSL.ReadConfig<>(extend(value) + ".[]", value.next())).fix(ConstOf::toConst).retag();
    }

    @Override
    public <T> Const<String, Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      return typeOf(value, value.type().getSimpleName() + "[]");
    }

    @Override
    public <A> Const<String, A> visit(DSL.ReadConfig<A> value) {
      var instance = new Instance<Const<String, ?>>() {};
      return value.next().foldMap(
          nestedInterpreter(value), instance.applicative(Monoid.string())).fix(ConstOf::toConst);
    }

    private <T> Const<String, T> typeOf(DSL<T> value, String type) {
      return Const.of("- " + extend(value) + ": " + type + "\n");
    }

    private <A> Interpreter<Const<String, ?>> nestedInterpreter(DSL.ReadConfig<A> value) {
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
      checkNonNull(baseKey);
      return dsl -> baseKey + "." + dsl.key();
    }

    static Key empty() {
      return DSL::key;
    }
  }

  public static record Map2<A, B>(PureCFG<? extends A> fa, PureCFG<? extends B> fb) {

    public <C> PureCFG<C> apply(Function2<? super A, ? super B, ? extends C> apply) {
      return mapN(fa, fb, apply);
    }
  }

  public static record Map3<A, B, C>(PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc) {

    public <D> PureCFG<D> apply(Function3<? super A, ? super B, ? super C, ? extends D> apply) {
      return mapN(fa, fb, fc, apply);
    }
  }

  public static record Map4<A, B, C, D>(PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc, PureCFG<? extends D> fd) {

    public <E> PureCFG<E> apply(Function4<? super A, ? super B, ? super C, ? super D, ? extends E> apply) {
      return mapN(fa, fb, fc, fd, apply);
    }
  }

  public static record Map5<A, B, C, D, E>(PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc, PureCFG<? extends D> fd, PureCFG<? extends E> fe) {

    public <F> PureCFG<F> apply(Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends F> apply) {
      return mapN(fa, fb, fc, fd, fe, apply);
    }
  }
}

interface PureCFGApplicative extends Applicative<PureCFG<?>> {

  PureCFGApplicative INSTANCE = new PureCFGApplicative() { };

  @Override
  default <T> PureCFG<T> pure(T value) {
    return PureCFG.pure(value);
  }

  @Override
  default <T, R> PureCFG<R> ap(Kind<PureCFG<?>, ? extends T> value,
      Kind<PureCFG<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return value.fix(PureCFGOf::<T>toPureCFG).ap(apply.fix(PureCFGOf::toPureCFG));
  }
}

