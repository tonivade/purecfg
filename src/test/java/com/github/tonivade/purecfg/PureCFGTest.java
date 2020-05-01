/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple3;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Validation;
import com.moandjiezana.toml.Toml;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Properties;

import static com.github.tonivade.purecfg.PureCFG.map2;
import static com.github.tonivade.purecfg.PureCFG.map3;
import static com.github.tonivade.purecfg.PureCFG.readBoolean;
import static com.github.tonivade.purecfg.PureCFG.readInt;
import static com.github.tonivade.purecfg.PureCFG.readIterable;
import static com.github.tonivade.purecfg.PureCFG.readString;
import static com.github.tonivade.purecfg.Source.from;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PureCFGTest {

  private final ImmutableList<User> expectedUsers = listOf(
      new User("a", "a"),
      new User("b", "b"),
      new User("c", "c"));
  private final Config expectedConfig = new Config("localhost", 8080, true);

  @Test
  void run() {
    PureCFG<Config> cfg = readConfig();

    Properties properties = new Properties();
    properties.put("server.host", "localhost");
    properties.put("server.port", "8080");
    properties.put("server.active", "true");
    Source source = Source.from(properties);

    assertAll(
        () -> assertEquals(expectedConfig, cfg.unsafeRun(source)),
        () -> assertEquals(expectedConfig, cfg.safeRun(source).get()),
        () -> assertEquals(expectedConfig, cfg.validatedRun(source).get())
    );
  }

  @Test
  void runToml() {
    PureCFG<Config> cfg = readConfig();
    Toml toml = new Toml().read("[server]\n  host = \"localhost\"\n  port = 8080\n  active = true");
    Source source = Source.from(toml);

    assertAll(
        () -> assertEquals(expectedConfig, cfg.unsafeRun(source)),
        () -> assertEquals(expectedConfig, cfg.safeRun(source).get()),
        () -> assertEquals(expectedConfig, cfg.validatedRun(source).get())
    );
  }

  @Test
  void iterable() {
    PureCFG<Iterable<String>> iterable = readIterable("list", String.class);

    Properties properties = new Properties();
    properties.put("list.0", "a");
    properties.put("list.1", "b");
    properties.put("list.2", "c");

    Option<Iterable<String>> option = iterable.safeRun(Source.from(properties));

    assertAll(
        () -> assertEquals(listOf("a", "b", "c"), option.get()),
        () -> assertEquals("- list: String[]\n", iterable.describe())
    );
  }

  @Test
  void iterableToml() {
    PureCFG<Iterable<String>> iterable = readIterable("list", String.class);

    Toml toml = new Toml().read("list = [ \"a\", \"b\", \"c\" ]");

    Option<Iterable<String>> option = iterable.safeRun(Source.from(toml));

    assertAll(
        () -> assertEquals(listOf("a", "b", "c"), option.get()),
        () -> assertEquals("- list: String[]\n", iterable.describe())
    );
  }

  @Test
  void iterableOf() {
    Properties properties = new Properties();
    properties.put("user.0.name", "a");
    properties.put("user.0.pass", "a");
    properties.put("user.1.name", "b");
    properties.put("user.1.pass", "b");
    properties.put("user.2.name", "c");
    properties.put("user.2.pass", "c");

    Option<Iterable<User>> option = readUsers().safeRun(from(properties));

    assertEquals(Option.some(expectedUsers), option);
  }

  @Test
  void iterableOfToml() {
    String source =
        "[[user]]\n  name = \"a\"\npass = \"a\"\n" +
        "[[user]]\n  name = \"b\"\npass = \"b\"\n" +
        "[[user]]\n  name = \"c\"\npass = \"c\"\n";

    Option<Iterable<User>> option = readUsers().safeRun(from(new Toml().read(source)));

    assertEquals(Option.some(expectedUsers), option);
  }

  @Test
  void analyzeListOf() {
    PureCFG<Iterable<Tuple3<String, Integer, Boolean>>> iterable =
        readIterable("list", map3(readString("a"), readInt("b"), readBoolean("c"), Tuple::of));

    assertEquals("- list.[].a: String\n- list.[].b: Integer\n- list.[].c: Boolean\n", iterable.describe());
  }

  @Test
  void errorToml() {
    PureCFG<Config> cfg = readConfig();

    Toml toml = new Toml();
    Source source = Source.from(toml);

    assertAll(
        () -> assertThrows(NullPointerException.class, () -> cfg.unsafeRun(source)),
        () -> assertEquals(Option.none(), cfg.safeRun(source)),
        () -> assertEquals(
            Validation.invalid(
                Validation.Result.of(
                    "key not found: server.active",
                    "key not found: server.port",
                    "key not found: server.host")),
            cfg.validatedRun(source))
    );
  }

  @Test
  void error() {
    PureCFG<Config> cfg = readConfig();

    Properties properties = new Properties();
    Source source = Source.from(properties);

    assertAll(
        () -> assertThrows(NullPointerException.class, () -> cfg.unsafeRun(source)),
        () -> assertEquals(Option.none(), cfg.safeRun(source)),
        () -> assertEquals(
            Validation.invalid(
                Validation.Result.of(
                    "key not found: server.active",
                    "key not found: server.port",
                    "key not found: server.host")),
            cfg.validatedRun(source))
    );
  }

  @Test
  void analyze() {
    PureCFG<Config> program = readConfig();

    String result = program.describe();

    assertEquals("- server.host: String\n- server.port: Integer\n- server.active: Boolean\n", result);
  }

  private PureCFG<Config> readConfig() {
    PureCFG<String> host = readString("host");
    PureCFG<Integer> port = readInt("port");
    PureCFG<Boolean> active = readBoolean("active");

    PureCFG<Config> hostAndPort = map3(host, port, active, Config::new);

    return PureCFG.readConfig("server", hostAndPort);
  }

  private PureCFG<Iterable<User>> readUsers() {
    PureCFG<User> userProgram = map2(readString("name"), readString("pass"), User::new);
    return readIterable("user", userProgram);
  }
}

final class Config {

  final String host;
  final int port;
  final boolean active;

  Config(String host, int port, boolean active) {
    this.host = host;
    this.port = port;
    this.active = active;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Config config = (Config) o;
    return port == config.port &&
        active == config.active &&
        Objects.equals(host, config.host);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port, active);
  }

  @Override
  public String toString() {
    return "Config{" +
        "host='" + host + '\'' +
        ", port=" + port +
        ", active=" + active +
        '}';
  }
}

final class User {

  final String name;
  final String pass;

  User(String name, String pass) {
    this.name = name;
    this.pass = pass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(name, user.name) && Objects.equals(pass, user.pass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, pass);
  }

  @Override
  public String toString() {
    return "User{" +
        "name='" + name + '\'' +
        ", pass='" + pass + '\'' +
        '}';
  }
}
