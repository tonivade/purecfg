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
import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.free.FreeAp;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Monoid;

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

  public T unsafeRun(Source source) {
    return value.foldMap(
        new Interpreter<>(new IdVisitor(Key.empty(), source)),
        IdInstances.applicative()).fix1(Id::narrowK).get();
  }

  public Option<T> safeRun(Source source) {
    return value.foldMap(
        new Interpreter<>(new OptionVisitor(Key.empty(), source)),
        OptionInstances.applicative()).fix1(Option::narrowK);
  }

  public Validation<Validation.Result<String>, T> validatedRun(Source source) {
    return value.foldMap(
        new Interpreter<>(new ValidationVisitor(Key.empty(), source)),
        ValidationInstances.applicative(Validation.Result::concat)).fix1(Validation::narrowK);
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

  public static <T> PureCFG<Iterable<T>> readIterable(String key, PureCFG<T> item) {
    return new PureCFG<>(new DSL.ReadIterable<>(key, item));
  }

  public static <T> PureCFG<T> readConfig(String key, PureCFG<T> cfg) {
    return new PureCFG<>(new DSL.ReadConfig<>(key, cfg));
  }

  public static Applicative<PureCFG.µ> applicative() {
    return PureCFGApplicative.instance();
  }

  private static final class Interpreter<F extends Kind> implements FunctionK<DSL.µ, F> {

    private final DSL.Visitor<F> visitor;

    private Interpreter(DSL.Visitor<F> visitor) {
      this.visitor = requireNonNull(visitor);
    }

    @Override
    public <T> Higher1<F, T> apply(Higher1<DSL.µ, T> from) {
      return from.fix1(DSL::narrowK).accept(visitor);
    }
  }

  private static abstract class AbstractVisitor<F extends Kind> implements DSL.Visitor<F> {

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

    protected String getProperty(DSL<?> value) {
      return source.getProperty(extend(value));
    }

    protected <T> Sequence<Higher1<F, T>> readAll(DSL.ReadIterable<T> value) {
      String key = extend(value);
      Iterable<DSL.ReadConfig<T>> properties = source.getProperties(key, value);
      return ImmutableArray.from(properties).map(this::<T>visit);
    }
  }

  private static final class IdVisitor extends AbstractVisitor<Id.µ> {

    private IdVisitor(Key baseKey, Source source) {
      super(baseKey, source);
    }

    @Override
    public Id<String> visit(DSL.ReadString value) {
      return Id.of(getProperty(value));
    }

    @Override
    public Id<Integer> visit(DSL.ReadInt value) {
      return Id.of(Integer.parseInt(getProperty(value)));
    }

    @Override
    public Id<Boolean> visit(DSL.ReadBoolean value) {
      return Id.of(Boolean.parseBoolean(getProperty(value)));
    }

    @Override
    public <T> Id<Iterable<T>> visit(DSL.ReadIterable<T> value) {
      return SequenceInstances.traverse()
          .sequence(IdInstances.applicative(), readAll(value))
          .fix1(Id::narrowK).map(s -> s.fix1(Sequence::narrowK));
    }

    @Override
    public <A> Id<A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), IdInstances.applicative()).fix1(Id::narrowK);
    }

    private <A> Interpreter<Id.µ> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new IdVisitor(Key.with(value.key()), getSource()));
    }
  }

  private static final class OptionVisitor extends AbstractVisitor<Option.µ> {

    private OptionVisitor(Key baseKey, Source source) {
      super(baseKey, source);
    }

    @Override
    public Option<String> visit(DSL.ReadString value) {
      return read(value);
    }

    @Override
    public Option<Integer> visit(DSL.ReadInt value) {
      return read(value).map(Integer::parseInt);
    }

    @Override
    public Option<Boolean> visit(DSL.ReadBoolean value) {
      return read(value).map(Boolean::parseBoolean);
    }

    @Override
    public <T> Option<Iterable<T>> visit(DSL.ReadIterable<T> value) {
      return SequenceInstances.traverse()
          .sequence(OptionInstances.applicative(), readAll(value))
          .fix1(Option::narrowK).map(s -> s.fix1(Sequence::narrowK));
    }

    @Override
    public <A> Option<A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), OptionInstances.applicative()).fix1(Option::narrowK);
    }

    private Option<String> read(DSL<?> dsl) {
      return Option.of(getProperty(dsl));
    }

    private <A> Interpreter<Option.µ> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new OptionVisitor(Key.with(value.key()), getSource()));
    }
  }

  private static final class ValidationVisitor
      extends AbstractVisitor<Higher1<Validation.µ, Validation.Result<String>>> {

    private ValidationVisitor(Key baseKey, Source source) {
      super(baseKey, source);
    }

    @Override
    public Validation<Validation.Result<String>, String> visit(DSL.ReadString value) {
      return read(value).fold(() -> invalid(value), this::valid);
    }

    @Override
    public Validation<Validation.Result<String>, Integer> visit(DSL.ReadInt value) {
      return read(value).map(Integer::parseInt).fold(() -> invalid(value), this::valid);
    }

    @Override
    public Validation<Validation.Result<String>, Boolean> visit(DSL.ReadBoolean value) {
      return read(value).map(Boolean::parseBoolean).fold(() -> invalid(value), this::valid);
    }

    @Override
    public <T> Validation<Validation.Result<String>, Iterable<T>> visit(DSL.ReadIterable<T> value) {
      return SequenceInstances.traverse()
          .sequence(ValidationInstances.applicative(Validation.Result::concat), readAll(value))
          .fix1(Validation::narrowK).map(s -> s.fix1(Sequence::narrowK));
    }

    @Override
    public <A> Validation<Validation.Result<String>, A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(nestedInterpreter(value), ValidationInstances.applicative(Validation.Result::concat))
          .fix1(Validation::narrowK);
    }

    private Option<String> read(DSL<?> dsl) {
      return Option.of(getProperty(dsl));
    }

    private <A> Interpreter<Higher1<Validation.µ, Validation.Result<String>>> nestedInterpreter(DSL.ReadConfig<A> value) {
      return new Interpreter<>(new ValidationVisitor(Key.with(value.key()), getSource()));
    }

    private <T> Validation<Validation.Result<String>, T> invalid(DSL<T> value) {
      return Validation.invalid(Validation.Result.of("key not found: " + extend(value)));
    }

    private <T> Validation<Validation.Result<String>, T> valid(T value) {
      return Validation.valid(value);
    }
  }

  private static final class ConstVisitor implements DSL.Visitor<Higher1<Const.µ, String>> {

    private final Key baseKey;

    private ConstVisitor(Key baseKey) {
      this.baseKey = requireNonNull(baseKey);
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
      return visit(new DSL.ReadConfig<>(extend(value) + ".[]", value.next())).fix2(Const::narrowK).retag();
    }

    @Override
    public <A> Const<String, A> visit(DSL.ReadConfig<A> value) {
      return value.next().foldMap(
          nestedInterpreter(value), ConstInstances.applicative(Monoid.string())).fix1(Const::narrowK);
    }

    private <T> Const<String, T> typeOf(DSL.AbstractRead<T> value, String type) {
      return Const.of("- " + extend(value) + ": " + type + "\n");
    }

    private <A> Interpreter<Higher1<Const.µ, String>> nestedInterpreter(DSL.ReadConfig<A> value) {
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

