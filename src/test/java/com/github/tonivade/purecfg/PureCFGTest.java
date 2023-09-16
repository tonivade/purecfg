/*
 * Copyright (c) 2020-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import static com.github.tonivade.purecfg.PureCFG.mapN;
import static com.github.tonivade.purecfg.PureCFG.readBoolean;
import static com.github.tonivade.purecfg.PureCFG.readInt;
import static com.github.tonivade.purecfg.PureCFG.readIterable;
import static com.github.tonivade.purecfg.PureCFG.readString;
import static com.github.tonivade.purecfg.Source.from;
import static com.github.tonivade.purefun.Validator.equalsTo;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.tomlj.Toml;

import com.github.tonivade.purecheck.TestSuite;
import com.github.tonivade.purecheck.spec.IOTestSpec;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple3;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Validation;

class PureCFGTest extends IOTestSpec<String> {

  private final ImmutableList<User> expectedUsers = listOf(
      new User("a", "a"),
      new User("b", "b"),
      new User("c", "c"));

  private final Config expectedConfig = new Config("localhost", 8080, true);

  @Test
  void run() {
    Properties properties = new Properties();
    properties.put("server.host", "localhost");
    properties.put("server.port", "8080");
    properties.put("server.active", "true");

    test(readConfig(), Source.from(properties)).run().assertion();
  }

  @Test
  void runToml() {
    var toml = Toml.parse(
        """
        [server]
          host = "localhost"
          port = 8080
          active = true
        """);

    test(readConfig(), Source.from(toml)).run().assertion();
  }

  @Test
  void runArgs() {
    String[] args = { "-host", "localhost", "-port", "8080", "--active" };

    test(readHostAndPort(), Source.fromArgs(args)).run().assertion();
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
        () -> assertEquals(listOf("a", "b", "c"), option.getOrElseThrow()),
        () -> assertEquals("- list: String[]\n", iterable.describe())
    );
  }

  @Test
  void iterableToml() {
    PureCFG<Iterable<String>> iterable = readIterable("list", String.class);

    var toml = Toml.parse("list = [ \"a\", \"b\", \"c\" ]");

    Option<Iterable<String>> option = iterable.safeRun(Source.from(toml));

    assertAll(
        () -> assertEquals(listOf("a", "b", "c"), option.getOrElseThrow()),
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
        """
        [[user]]
           name = "a"
           pass = "a"
        [[user]]
           name = "b"
           pass = "b"
        [[user]]
           name = "c"
           pass = "c"
        """;

    Option<Iterable<User>> option = readUsers().safeRun(from(Toml.parse(source)));

    assertEquals(Option.some(expectedUsers), option);
  }

  @Test
  void analyzeListOf() {
    PureCFG<Iterable<Tuple3<String, Integer, Boolean>>> iterable =
        readIterable("list", mapN(readString("a"), readInt("b"), readBoolean("c"), Tuple::of));

    assertEquals("- list.[].a: String\n- list.[].b: Integer\n- list.[].c: Boolean\n", iterable.describe());
  }

  @Test
  void errorToml() {
    PureCFG<Config> cfg = readConfig();

    var toml = Toml.parse("");
    Source source = Source.from(toml);

    assertAll(
        () -> assertThrows(NoSuchElementException.class, () -> cfg.unsafeRun(source)),
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
        () -> assertThrows(NoSuchElementException.class, () -> cfg.unsafeRun(source)),
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

  private TestSuite<IO_, String> test(PureCFG<Config> program, Source source) {
    return suite("PureCFG",

        it.should("read config from " + source)
          .given(source)
          .when(program::unsafeRun)
          .then(equalsTo(expectedConfig)),

        it.should("read config form " + source)
          .given(source)
          .when(program::safeRun)
          .then(equalsTo(expectedConfig).compose(Option::getOrElseThrow)),

        it.should("read config form " + source)
          .given(source)
          .when(program::validatedRun)
          .then(equalsTo(expectedConfig).compose(Validation::get))

        );
  }

  private PureCFG<Config> readConfig() {
    return PureCFG.readConfig("server", readHostAndPort());
  }

  private PureCFG<Config> readHostAndPort() {
    PureCFG<String> host = readString("host");
    PureCFG<Integer> port = readInt("port");
    PureCFG<Boolean> active = readBoolean("active");

    return mapN(host, port, active, Config::new);
  }

  private PureCFG<Iterable<User>> readUsers() {
    PureCFG<User> userProgram = mapN(readString("name"), readString("pass"), User::new);
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
    Config other = (Config) o;
    return this.port == other.port &&
        this.active == other.active &&
        Objects.equals(this.host, other.host);
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
    User other = (User) o;
    return Objects.equals(this.name, other.name) && Objects.equals(this.pass, other.pass);
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
