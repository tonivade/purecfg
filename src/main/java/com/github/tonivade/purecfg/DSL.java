/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.data.NonEmptyString;

import static java.util.Objects.requireNonNull;

@HigherKind
public interface DSL<T> {

  String key();

  <F extends Kind> Higher1<F, T> accept(Visitor<F> visitor);

  interface Visitor<F extends Kind> {
    <T> Higher1<F, T> visit(None<T> value);
    <T> Higher1<F, T> visit(Pure<T> value);
    Higher1<F, String> visit(ReadString value);
    Higher1<F, Integer> visit(ReadInt value);
    Higher1<F, Boolean> visit(ReadBoolean value);
    <T> Higher1<F, Iterable<T>> visit(ReadIterable<T> value);
    <T> Higher1<F, T> visit(ReadConfig<T> value);
  }

  abstract class AbstractRead<T> implements DSL<T> {

    private final NonEmptyString key;

    private AbstractRead(String key) {
      this.key = NonEmptyString.of(key);
    }

    @Override
    public String key() {
      return key.get();
    }
  }

  final class None<T> implements DSL<T> {

    private Class<T> type;

    protected None(Class<T> type) {
      this.type = requireNonNull(type);
    }

    @Override
    public String key() {
      return "it";
    }

    public Class<T> type() {
      return type;
    }

    @Override
    public <F extends Kind> Higher1<F, T> accept(Visitor<F> visitor) {
      return visitor.visit(this);
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
    public <F extends Kind> Higher1<F, T> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadInt extends AbstractRead<Integer> {

    protected ReadInt(String key) {
      super(key);
    }

    @Override
    public <F extends Kind> Higher1<F, Integer> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadString extends AbstractRead<String> {

    protected ReadString(String key) {
      super(key);
    }

    @Override
    public <F extends Kind> Higher1<F, String> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadBoolean extends AbstractRead<Boolean> {

    protected ReadBoolean(String key) {
      super(key);
    }

    @Override
    public <F extends Kind> Higher1<F, Boolean> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadIterable<T> extends AbstractRead<Iterable<T>> {

    private final PureCFG<T> next;

    protected ReadIterable(String key, PureCFG<T> next) {
      super(key);
      this.next = requireNonNull(next);
    }

    public PureCFG<T> next() {
      return next;
    }

    @Override
    public <F extends Kind> Higher1<F, Iterable<T>> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadConfig<T> extends AbstractRead<T> {

    private final PureCFG<T> next;

    protected ReadConfig(String key, PureCFG<T> next) {
      super(key);
      this.next = requireNonNull(next);
    }

    public PureCFG<T> next() {
      return next;
    }

    @Override
    public <F extends Kind> Higher1<F, T> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }
}
