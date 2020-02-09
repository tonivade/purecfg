/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.data.NonEmptyString;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Id;

import static java.util.Objects.requireNonNull;

@HigherKind
public interface DSL<T> {

  String key();

  <F extends Kind> Higher1<F, T> accept(Visitor<F> visitor);

  interface Visitor<F extends Kind> {
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

    private final PureCFG<T> item;

    protected ReadIterable(String key, PureCFG<T> item) {
      super(key);
      this.item = requireNonNull(item);
    }

    public PureCFG<T> next() {
      return item;
    }

    @Override
    public <F extends Kind> Higher1<F, Iterable<T>> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }

  final class ReadConfig<T> extends AbstractRead<T> {

    private final PureCFG<T> config;

    protected ReadConfig(String key, PureCFG<T> config) {
      super(key);
      this.config = requireNonNull(config);
    }

    public PureCFG<T> next() {
      return config;
    }

    @Override
    public <F extends Kind> Higher1<F, T> accept(Visitor<F> visitor) {
      return visitor.visit(this);
    }
  }
}
