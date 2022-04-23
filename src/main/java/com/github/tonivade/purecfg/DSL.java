/*
 * Copyright (c) 2020-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import static com.github.tonivade.purefun.Precondition.checkNonEmpty;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;

@HigherKind
public sealed interface DSL<T> extends DSLOf<T> {

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

  record Pure<T>(String key, T value) implements DSL<T> {

    public Pure {
      checkNonEmpty(key);
      checkNonNull(value);
    }

    public T get() {
      return value;
    }

    @Override
    public <F extends Witness> Kind<F, T> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  record ReadInt(String key) implements DSL<Integer> {

    public ReadInt {
      checkNonEmpty(key);
    }

    @Override
    public <F extends Witness> Kind<F, Integer> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  record ReadString(String key) implements DSL<String> {

    public ReadString {
      checkNonEmpty(key);
    }

    @Override
    public <F extends Witness> Kind<F, String> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  record ReadBoolean(String key) implements DSL<Boolean> {

    public ReadBoolean {
      checkNonEmpty(key);
    }

    @Override
    public <F extends Witness> Kind<F, Boolean> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  record ReadPrimitiveIterable<T>(String key, Class<T> type) implements DSL<Iterable<T>> {

    public ReadPrimitiveIterable {
      checkNonEmpty(key);
      checkNonNull(type);
    }

    @Override
    public <F extends Witness> Kind<F, Iterable<T>> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  record ReadIterable<T>(String key, PureCFG<T> next) implements DSL<Iterable<T>> {

    public ReadIterable {
      checkNonEmpty(key);
      checkNonNull(next);
    }

    @Override
    public <F extends Witness> Kind<F, Iterable<T>> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  record ReadConfig<T>(String key, PureCFG<T> next) implements DSL<T> {

    public ReadConfig {
      checkNonNull(key);
      checkNonNull(next);
    }

    @Override
    public <F extends Witness> Kind<F, T> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }
}
