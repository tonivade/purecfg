/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.data.NonEmptyString;

import static java.util.Objects.requireNonNull;

@HigherKind
public interface DSL<T> {

  String getKey();

  abstract class AbstractRead<T> implements DSL<T> {

    private final String key;

    private AbstractRead(NonEmptyString key) {
      this.key = requireNonNull(key).get();
    }

    @Override
    public String getKey() {
      return key;
    }
  }

  final class ReadInt extends AbstractRead<Integer> {

    protected ReadInt(NonEmptyString key) {
      super(key);
    }
  }

  final class ReadString extends AbstractRead<String> {

    protected ReadString(NonEmptyString key) {
      super(key);
    }
  }
}
