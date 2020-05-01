/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import com.moandjiezana.toml.Toml;

import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.github.tonivade.purefun.data.ImmutableArray.toImmutableArray;
import static java.util.Objects.requireNonNull;

public interface Source {

  String getProperty(String key);

  <T> Iterable<DSL.ReadConfig<T>> getProperties(String key, DSL.ReadIterable<T> value);

  static Source fromProperties(Properties properties) {
    return new PropertiesSource(properties);
  }

  static Source fromToml(Toml toml) {
    return new TomlSource(toml);
  }

  final class PropertiesSource implements Source {

    private final Properties properties;

    public PropertiesSource(Properties properties) {
      this.properties = requireNonNull(properties);
    }

    @Override
    public String getProperty(String key) {
      return properties.getProperty(key);
    }

    @Override
    public <T> Iterable<DSL.ReadConfig<T>> getProperties(String key, DSL.ReadIterable<T> value) {
      String regex = "(" + key.replaceAll("\\.", "\\.") + "\\.\\d+)\\..*";
      Stream<DSL.ReadConfig<T>> stream = properties.keySet().stream()
          .map(Object::toString)
          .distinct()
          .sorted()
          .flatMap(k -> getKey(k, regex))
          .map(k -> new DSL.ReadConfig<>(k, value.next()));
      return stream.collect(toImmutableArray());
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

    @Override
    public String getProperty(String key) {
      return toml.getString(key);
    }

    @Override
    public <T> Iterable<DSL.ReadConfig<T>> getProperties(String key, DSL.ReadIterable<T> value) {
      List<Object> list = toml.getList(key);
      return null;
    }
  }
}
