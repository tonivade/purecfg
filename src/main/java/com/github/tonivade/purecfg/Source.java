/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.moandjiezana.toml.Toml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.github.tonivade.purefun.data.ImmutableArray.toImmutableArray;
import static java.util.Objects.requireNonNull;

public interface Source {

  Option<String> getString(String key);
  Option<Integer> getInteger(String key);
  Option<Boolean> getBoolean(String key);

  <T> Iterable<DSL<T>> getIterable(String key, Class<T> type);
  <T> Iterable<DSL<T>> getIterable(String key, PureCFG<T> next);

  static Source fromProperties(String file) {
    return from(PropertiesSource.read(file));
  }

  static Source fromToml(String file) {
    return from(TomlSource.read(file));
  }

  static Source from(Properties properties) {
    return new PropertiesSource(properties);
  }

  static Source from(Toml toml) {
    return new TomlSource(toml);
  }

  final class PropertiesSource implements Source {

    private final Properties properties;

    public PropertiesSource(Properties properties) {
      this.properties = requireNonNull(properties);
    }

    public static Properties read(String file) {
      try {
        Properties properties = new Properties();
        properties.load(requireNonNull(PropertiesSource.class.getClassLoader().getResourceAsStream(file)));
        return properties;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public Option<String> getString(String key) {
      return readString(key);
    }

    @Override
    public Option<Integer> getInteger(String key) {
      return readString(key).map(Integer::parseInt);
    }

    @Override
    public Option<Boolean> getBoolean(String key) {
      return readString(key).map(Boolean::parseBoolean);
    }

    @Override
    public <T> Iterable<DSL<T>> getIterable(String key, Class<T> type) {
      return iterableKeys(key).map(k -> readKey(k, type)).collect(toImmutableArray());
    }

    @Override
    public <T> Iterable<DSL<T>> getIterable(String key, PureCFG<T> next) {
      return iterableKeys(key).map(k -> new DSL.ReadConfig<>(k, next)).collect(toImmutableArray());
    }

    private Stream<String> iterableKeys(String key) {
      String regex = "(" + key.replaceAll("\\.", "\\.") + "\\.\\d+).*";
      return properties.keySet().stream()
          .map(Object::toString)
          .distinct()
          .sorted()
          .flatMap(k -> getKey(k, regex));
    }

    @SuppressWarnings("unchecked")
    private <T> DSL<T> readKey(String key, Class<T> type) {
      switch (type.getSimpleName()) {
        case "String":
          return (DSL<T>) new DSL.ReadString(key);
        case "Integer":
          return (DSL<T>) new DSL.ReadInt(key);
        case "Boolean":
          return (DSL<T>) new DSL.ReadBoolean(key);
      }
      throw new UnsupportedOperationException("this class is not supported: " + type.getName());
    }

    private Option<String> readString(String key) {
      return Option.of(properties.getProperty(key));
    }

    private Stream<String> getKey(String key, String regex) {
      Matcher matcher = Pattern.compile(regex).matcher(key);

      if (matcher.find()) {
        return Stream.of(matcher.group(1));
      }

      return Stream.empty();
    }
  }

  final class TomlSource implements Source {

    private final Toml toml;

    public TomlSource(Toml toml) {
      this.toml = requireNonNull(toml);
    }

    public static Toml read(String file) {
      return new Toml().read(requireNonNull(TomlSource.class.getClassLoader().getResourceAsStream(file)));
    }

    @Override
    public Option<String> getString(String key) {
      return Try.of(() -> toml.getString(key)).toOption();
    }

    @Override
    public Option<Integer> getInteger(String key) {
      return Try.of(() -> toml.getLong(key).intValue()).toOption();
    }

    @Override
    public Option<Boolean> getBoolean(String key) {
      return Try.of(() -> toml.getBoolean(key)).toOption();
    }

    @Override
    public <T> Iterable<DSL<T>> getIterable(String key, Class<T> type) {
      List<Object> list = toml.getList(key);
      if (list.get(0) instanceof Toml) {
        return ImmutableArray.empty();
      }
      return list.stream().map(it -> new DSL.Pure<>(key, (T) it)).collect(toImmutableArray());
    }

    @Override
    public <T> Iterable<DSL<T>> getIterable(String key, PureCFG<T> next) {
      throw new UnsupportedOperationException();
    }
  }
}
