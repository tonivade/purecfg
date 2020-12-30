/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.data.SequenceOf.toSequence;
import static com.github.tonivade.purefun.type.ConstOf.toConst;
import static com.github.tonivade.purefun.type.IdOf.toId;
import static com.github.tonivade.purefun.type.OptionOf.toOption;
import static com.github.tonivade.purefun.type.ValidationOf.toValidation;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.data.SequenceOf;
import com.github.tonivade.purefun.data.Sequence_;
import com.github.tonivade.purefun.free.FreeAp;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Const_;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Option_;
import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.type.Validation.Result;
import com.github.tonivade.purefun.type.Validation_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Instance;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.Semigroup;

@HigherKind
public final class PureCFG<T> implements PureCFGOf<T> {

  private final FreeAp<DSL_, T> value;

  protected PureCFG(DSL<T> value) {
    this(FreeAp.lift(value));
  }

  protected PureCFG(FreeAp<DSL_, T> value) {
    this.value = checkNonNull(value);
  }

  public <R> PureCFG<R> map(Function1<? super T, ? extends R> mapper) {
    return new PureCFG<>(value.map(mapper));
  }

  public <R> PureCFG<R> ap(PureCFG<Function1<? super T, ? extends R>> apply) {
    return new PureCFG<>(value.ap(apply.value));
  }

  protected <G extends Witness> Kind<G, T> foldMap(FunctionK<DSL_, G> functionK, Applicative<G> applicative) {
    return value.foldMap(functionK, applicative);
  }

  public T unsafeRun(Source source) {
    return value.foldMap(
        new Interpreter<>(new IdVisitor(Key.empty(), source)),
        Instance.applicative(Id_.class)).fix(toId()).get();
  }

  public Option<T> safeRun(Source source) {
    return value.foldMap(
        new Interpreter<>(new OptionVisitor(Key.empty(), source)),
        Instance.applicative(Option_.class)).fix(toOption());
  }

  public Validation<Validation.Result<String>, T> validatedRun(Source source) {
    var instance = new Instance<Kind<Validation_, Validation.Result<String>>>() {};
    Semigroup<Result<String>> semigroup = Validation.Result::concat;
    return value.foldMap(
        new Interpreter<>(new ValidationVisitor(Key.empty(), source)),
        instance.applicative(semigroup)).fix(toValidation());
  }

  public String describe() {
    var instance = new Instance<Kind<Const_, String>>() {};
    return value.analyze(
        new Interpreter<>(new ConstVisitor(Key.empty())),
        instance.applicative(Monoid.string()));
  }

  public static <A, B, C> PureCFG<C> mapN(PureCFG<? extends A> fa, PureCFG<? extends B> fb, 
      Function2<? super A, ? super B, ? extends C> apply) {
    return fb.ap(fa.map(apply.curried()));
  }

  public static <A, B, C, D> PureCFG<D> mapN(
      PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc, 
      Function3<? super A, ? super B, ? super C, ? extends D> apply) {
    return fc.ap(mapN(fa, fb, (a, b) -> apply.curried().apply(a).apply(b)));
  }

  public static <A, B, C, D, E> PureCFG<E> mapN(
      PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc, PureCFG<? extends D> fd, 
      Function4<? super A, ? super B, ? super C, ? super D, ? extends E> apply) {
    return fd.ap(mapN(fa, fb, fc, (a, b, c) -> apply.curried().apply(a).apply(b).apply(c)));
  }

  public static <A, B, C, D, E, F> PureCFG<F> mapN(
      PureCFG<? extends A> fa, PureCFG<? extends B> fb, PureCFG<? extends C> fc, PureCFG<? extends D> fd, PureCFG<? extends E> fe, 
      Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends F> apply) {
    return fe.ap(mapN(fa, fb, fc, fd, (a, b, c, d) -> apply.curried().apply(a).apply(b).apply(c).apply(d)));
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
    return new PureCFG<>(new DSL.ReadIterable<>(key, item));
  }

  public static <T> PureCFG<T> readConfig(String key, PureCFG<? extends T> cfg) {
    return new PureCFG<>(new DSL.ReadConfig<>(key, cfg));
  }

  public static Applicative<PureCFG_> applicative() {
    return PureCFGApplicative.INSTANCE;
  }

  private static final class Interpreter<F extends Witness> implements FunctionK<DSL_, F> {

    private final DSL.Visitor<F> visitor;

    private Interpreter(DSL.Visitor<F> visitor) {
      this.visitor = checkNonNull(visitor);
    }

    @Override
    public <T> Kind<F, T> apply(Kind<DSL_, ? extends T> from) {
      return from.fix(DSLOf::<T>narrowK).accept(visitor);
    }
  }

  private abstract static class AbstractVisitor<F extends Witness> implements DSL.Visitor<F> {

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
      return Instance.traverse(Sequence_.class)
          .sequence(Instance.applicative(Id_.class), readAll(value))
          .fix(toId()).map(s -> s.fix(SequenceOf::narrowK));
    }

    @Override
    public <T> Id<Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      return Instance.traverse(Sequence_.class)
          .sequence(Instance.applicative(Id_.class), readAll(value))
          .fix(toId()).map(s -> s.fix(toSequence()));
    }

    @Override
    public <A> Id<A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), Instance.applicative(Id_.class)).fix(toId());
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
      return Instance.traverse(Sequence_.class)
          .sequence(Instance.applicative(Option_.class), readAll(value))
          .fix(toOption()).map(s -> s.fix(toSequence()));
    }

    @Override
    public <T> Option<Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      return Instance.traverse(Sequence_.class)
          .sequence(Instance.applicative(Option_.class), readAll(value))
          .fix(toOption()).map(s -> s.fix(toSequence()));
    }

    @Override
    public <A> Option<A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), Instance.applicative(Option_.class)).fix(toOption());
    }

    private <A> Interpreter<Option_> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new OptionVisitor(Key.with(value.key()), getSource()));
    }
  }

  private static final class ValidationVisitor
      extends AbstractVisitor<Kind<Validation_, Validation.Result<String>>> {

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
      var instance = new Instance<Kind<Validation_, Validation.Result<String>>>() {};
      Semigroup<Result<String>> semigroup = Validation.Result::concat;
      return Instance.traverse(Sequence_.class)
          .sequence(instance.applicative(semigroup), readAll(value))
          .fix(toValidation()).map(s -> s.fix(toSequence()));
    }

    @Override
    public <T> Validation<Validation.Result<String>, Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      var instance = new Instance<Kind<Validation_, Validation.Result<String>>>() {};
      Semigroup<Result<String>> semigroup = Validation.Result::concat;
      return Instance.traverse(Sequence_.class)
          .sequence(instance.applicative(semigroup), readAll(value))
          .fix(toValidation()).map(s -> s.fix(toSequence()));
    }

    @Override
    public <A> Validation<Validation.Result<String>, A> visit(DSL.ReadConfig<A> value) {
      var instance = new Instance<Kind<Validation_, Validation.Result<String>>>() {};
      Semigroup<Result<String>> semigroup = Validation.Result::concat;
      return value.next().foldMap(nestedInterpreter(value), instance.applicative(semigroup))
          .fix(toValidation());
    }

    private <A> Interpreter<Kind<Validation_, Validation.Result<String>>> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new ValidationVisitor(Key.with(value.key()), getSource()));
    }

    private <T> Validation<Validation.Result<String>, T> invalid(DSL<T> value) {
      return Validation.invalid(Validation.Result.of("key not found: " + extend(value)));
    }

    private <T> Validation<Validation.Result<String>, T> valid(T value) {
      return Validation.valid(value);
    }
  }

  private static final class ConstVisitor implements DSL.Visitor<Kind<Const_, String>> {

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
      return visit(new DSL.ReadConfig<>(extend(value) + ".[]", value.next())).fix(toConst()).retag();
    }

    @Override
    public <T> Const<String, Iterable<T>> visit(DSL.ReadPrimitiveIterable<T> value) {
      return typeOf(value, value.type().getSimpleName() + "[]");
    }

    @Override
    public <A> Const<String, A> visit(DSL.ReadConfig<A> value) {
      var instance = new Instance<Kind<Const_, String>>() {};
      return value.next().foldMap(
          nestedInterpreter(value), instance.applicative(Monoid.string())).fix(toConst());
    }

    private <T> Const<String, T> typeOf(DSL<T> value, String type) {
      return Const.of("- " + extend(value) + ": " + type + "\n");
    }

    private <A> Interpreter<Kind<Const_, String>> nestedInterpreter(DSL.ReadConfig<A> value) {
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
}

interface PureCFGApplicative extends Applicative<PureCFG_> {

  PureCFGApplicative INSTANCE = new PureCFGApplicative() { };

  @Override
  default <T> PureCFG<T> pure(T value) {
    return PureCFG.pure(value);
  }

  @Override
  default <T, R> PureCFG<R> ap(Kind<PureCFG_, ? extends T> value, 
      Kind<PureCFG_, ? extends Function1<? super T, ? extends R>> apply) {
    return value.fix(PureCFGOf::<T>narrowK).ap(apply.fix(PureCFGOf::narrowK));
  }
}

