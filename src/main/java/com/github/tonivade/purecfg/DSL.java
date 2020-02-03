/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.data.NonEmptyString;

import static java.util.Objects.requireNonNull;

@HigherKind
public interface DSL<T> {

  String key();

  abstract class AbstractRead<T, R> implements DSL<R> {

    private final String key;
    private final Function1<T, R> value;

    private AbstractRead(NonEmptyString key, Function1<T, R> value) {
      this.key = requireNonNull(key).get();
      this.value = requireNonNull(value);
    }

    @Override
    public String key() {
      return key;
    }

    public Function1<T, R> value() {
      return value;
    }
  }

  final class ReadInt<T> extends AbstractRead<Integer, T> {

    protected ReadInt(NonEmptyString key, Function1<Integer, T> value) {
      super(key, value);
    }
  }

  final class ReadString<T> extends AbstractRead<String, T> {

    protected ReadString(NonEmptyString key, Function1<String, T> value) {
      super(key, value);
    }
  }
}
