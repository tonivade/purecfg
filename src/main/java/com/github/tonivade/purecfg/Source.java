/*
 * Copyright (c) 2020-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.data.ImmutableArray.toImmutableArray;
import static java.lang.Boolean.TRUE;
import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

public interface Source {

  Option<String> getString(String key);
  Option<Integer> getInteger(String key);
  Option<Boolean> getBoolean(String key);

  <T> Iterable<DSL<T>> getIterable(String key, Class<T> type);
  <T> Iterable<DSL<T>> getIterable(String key, PureCFG<T> next);

  /**
   * <p>Reads configuration from properties files:</p>
   *
   * <pre>
   *   server.host=localhost
   *   server.port=8080
   *   server.active=true
   * </pre>
   *
   * <p>Also it supports lists</p>
   *
   * <pre>
   *   list.0=a
   *   list.1=b
   *   list.2=c
   * </pre>
   *
   * <p>An also lists of complex elements:</p>
   *
   * <pre>
   *   list.0.id=1
   *   list.0.name=a
   *   list.1.id=2
   *   list.1.name=b
   *   list.2.id=3
   *   list.2.name=c
   * </pre>
   *
   * @param file file name
   * @return the created source for the given file
   */
  static Source fromProperties(String file) {
    return from(PropertiesSource.read(file));
  }

  /**
   * <p>Reads configuration from toml files:</p>
   *
   * <pre>
   *   [server]
   *     host = "localhost"
   *     port = 8080
   *     active = true
   * </pre>
   *
   * <p>Also it supports lists</p>
   *
   * <pre>
   *   list = [ "a", "b", "c" ]
   * </pre>
   *
   * <p>An also lists of complex elements:</p>
   *
   * <pre>
   *   [[list]]
   *    id = 1
   *    name = "a"
   *   [[list]]
   *    id = 2
   *    name = "b"
   *   [[list]]
   *    id = 3
   *    name = "c"
   * </pre>
   *
   * @param file file name
   * @return the created source for the given file
   */
  static Source fromToml(String file) {
    return from(TomlSource.read(file));
  }

  /**
   * Reads arguments from command line. With this format:
   *
   * <ul>
   *   <li>{@code -param value}: for params with value.</li>
   *   <li>{@code --param} for boolean params.</li>
   * </ul>
   *
   * Example:
   *
   * <pre>
   *   --host localhost --port 8080 --active
   * </pre>
   *
   * Will be parsed as:
   *
   * <pre>
   *   host=localhost
   *   port=8080
   *   active=true
   * </pre>
   *
   * @param args command line arguments
   * @return the created source for the given arguments
   */
  static Source fromArgs(String... args) {
    return from(SourceModule.parseArgs(args));
  }

  static Source from(Properties properties) {
    return new PropertiesSource(properties);
  }

  static Source from(TomlParseResult toml) {
    return new TomlSource(toml);
  }

  final class PropertiesSource implements Source {

    private final Properties properties;

    public PropertiesSource(Properties properties) {
      this.properties = checkNonNull(properties);
    }

    public static Properties read(String file) {
      try {
        Properties properties = new Properties();
        properties.load(checkNonNull(PropertiesSource.class.getClassLoader().getResourceAsStream(file)));
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
      String regex = "(" + key.replaceAll("\\.", ".") + "\\.\\d+).*";
      return properties.keySet().stream()
          .map(Object::toString)
          .flatMap(k -> getKey(k, regex))
          .distinct()
          .sorted();
    }

    @SuppressWarnings("unchecked")
    private <T> DSL<T> readKey(String key, Class<T> type) {
      return switch (type.getSimpleName()) {
        case "String" -> (DSL<T>) new DSL.ReadString(key);
        case "Integer" -> (DSL<T>) new DSL.ReadInt(key);
        case "Boolean" -> (DSL<T>) new DSL.ReadBoolean(key);
        default -> throw new UnsupportedOperationException("this class is not supported: " + type.getName());
      };
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

    private final TomlParseResult toml;

    public TomlSource(TomlParseResult toml) {
      this.toml = checkNonNull(toml);
    }

    public static TomlParseResult read(String file) {
      try {
        return Toml.parse(checkNonNull(TomlSource.class.getClassLoader().getResourceAsStream(file)));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public Option<String> getString(String key) {
      return Try.of(() -> TomlSource.<String>readValue(toml, key)).toOption();
    }

    @Override
    public Option<Integer> getInteger(String key) {
      return Try.of(() -> TomlSource.<Long>readValue(toml, key).intValue()).toOption();
    }

    @Override
    public Option<Boolean> getBoolean(String key) {
      return Try.of(() -> TomlSource.<Boolean>readValue(toml, key)).toOption();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterable<DSL<T>> getIterable(String key, Class<T> type) {
      TomlArray array = toml.getArrayOrEmpty(key);
      if (array.isEmpty()) {
        return ImmutableArray.empty();
      }
      List<DSL<T>> result = new ArrayList<>();
      for (int i = 0; i < array.size(); i++) {
        var item = (T) array.get(i);
        result.add(new DSL.Pure<>(key, item));
      }
      return ImmutableArray.from(result);
    }

    @Override
    public <T> Iterable<DSL<T>> getIterable(String key, PureCFG<T> next) {
      TomlArray array = toml.getArrayOrEmpty(key);
      if (array.isEmpty()) {
        return ImmutableArray.empty();
      }
      List<DSL<T>> result = new ArrayList<>();
      for (int i = 0; i < array.size(); i++) {
        result.add(new DSL.ReadConfig<>(key + "." + i, next));
      }
      return ImmutableArray.from(result);
    }

    @SuppressWarnings("unchecked")
    private static <T> T readValue(TomlParseResult toml, String key) {
      Object current = toml;
      for (String item : key.split("\\.")) {
        if (current instanceof TomlTable table) {
          current = table.get(item);
        } else if (current instanceof TomlArray array) {
          current = array.get(Integer.parseInt(item));
        }
      }
      return (T) current;
    }
  }
}

interface SourceModule {
  static Properties parseArgs(String[] args) {
    Properties properties = new Properties();
    for (int i = 0; i < args.length; i++) {
      String current = args[i];
      if (current.charAt(0) == '-') {
        if (current.length() < 2) {
          throw new IllegalArgumentException("Not a valid argument: " + current);
        }
        if (current.charAt(1) == '-') {
          if (current.length() < 3) {
            throw new IllegalArgumentException("Not a valid argument: " + current);
          }
          // --opt
          properties.setProperty(current.substring(2), TRUE.toString());
        } else {
          if (args.length == i + 1) {
            throw new IllegalArgumentException("Expected arg after: " + current);
          }
          // -opt
          properties.setProperty(current.substring(1), args[++i]);
        }
      } else {
        throw new IllegalArgumentException("invalid param: " + current);
      }
    }
    return properties;
  }
}
