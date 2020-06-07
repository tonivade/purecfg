# PureCFG

Inspired by this [presentation](https://github.com/jdegoes/scalaworld-2015/blob/master/presentation.md)

## Objectives

- Create a pure functional library to load configuration from different sources, like properties files, json, yaml or any other source. 
- Allow to switch from sources without changing the program.
- Self documented.
- Type safe.

Now it supports properties and toml files.

## Example

```toml
[server]
  host = "localhost"
  port = 8080
```

```java
record HostAndPort(String host, Integer port) { }

var readHostAndPort = PureCFG.map2(PureCFG.readString("host"), PureCFG.readInt("port"), HostAndPort::new);
var program = PureCFG.readConfig("server", readHostAndPort);

var config = program.unsafeRun(Source.fromToml("config.toml"));
assertEquals(new HostAndPort("localhost", 8080), config);
```

## License

Distributed under MIT License
