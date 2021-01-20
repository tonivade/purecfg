/*
 * Copyright (c) 2020-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import static java.util.Objects.requireNonNull;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.data.NonEmptyString;

@HigherKind(sealed = true)
public interface DSL<T> extends DSLOf<T> {

  String key();

  <F extends Witness> Kind<F, T> accept(Visitor<F> visitor);

  interface Visitor<F extends Witness> {

    <T> Kind<F, T> visit(Pure<T> value);

    Kind<F, String> visit(ReadString value);

    Kind<F, Integer> visit(ReadInt value);

    Kind<F, Boolean> visit(ReadBoolean value);

    <T> Kind<F, Iterable<T>> visit(ReadPrimitiveIterable<T> value);

    <T> Kind<F, Iterable<T>> visit(ReadIterable<T> value);

    <T> Kind<F, T> visit(ReadConfig<T> value);
  }

  abstract class AbstractRead<T> implements SealedDSL<T> {

    private final NonEmptyString key;

    private AbstractRead(String key) {
      this.key = NonEmptyString.of(key);
    }

    @Override
    public String key() {
      return key.get();
    }
  }

  final class Pure<T> extends AbstractRead<T> {

    private final T value;

    protected Pure(String key, T value) {
      super(key);
      this.value = requireNonNull(value);
    }

    public T get() {
      return value;
    }

    @Override
    public <F extends Witness> Kind<F, T> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadInt extends AbstractRead<Integer> {

    protected ReadInt(String key) {
      super(key);
    }

    @Override
    public <F extends Witness> Kind<F, Integer> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadString extends AbstractRead<String> {

    protected ReadString(String key) {
      super(key);
    }

    @Override
    public <F extends Witness> Kind<F, String> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadBoolean extends AbstractRead<Boolean> {

    protected ReadBoolean(String key) {
      super(key);
    }

    @Override
    public <F extends Witness> Kind<F, Boolean> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadPrimitiveIterable<T> extends AbstractRead<Iterable<T>> {

    private final Class<T> type;

    public ReadPrimitiveIterable(String key, Class<T> type) {
      super(key);
      this.type = type;
    }

    public Class<T> type() {
      return type;
    }

    @Override
    public <F extends Witness> Kind<F, Iterable<T>> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadIterable<T> extends AbstractRead<Iterable<T>> {

    private final PureCFG<? extends T> next;

    protected ReadIterable(String key, PureCFG<? extends T> next) {
      super(key);
      this.next = requireNonNull(next);
    }

    public PureCFG<T> next() {
      return PureCFGOf.narrowK(next);
    }

    @Override
    public <F extends Witness> Kind<F, Iterable<T>> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadConfig<T> extends AbstractRead<T> {

    private final PureCFG<? extends T> next;

    protected ReadConfig(String key, PureCFG<? extends T> next) {
      super(key);
      this.next = requireNonNull(next);
    }

    public PureCFG<T> next() {
      return PureCFGOf.narrowK(next);
    }

    @Override
    public <F extends Witness> Kind<F, T> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }
}
