# redisponse
📫 A request-response structure lib for Redis pub-subs. Need the req-res of an HTTP server but already have Redis pub-subs? Use this

The idea of redisponse was to provide a two-way-esque communication for pubsubs. They are traditionally fire-and-forget but I wanted to get a req-res
structure without having to set up a whole HTTP server

### Why Java 16?
Do you need directions to the bingo hall too? Update your java, it's starting to smell :)

## Getting started
Clone, install, and shade it into your program.
```
git clone
cd redisponse
mvn clean package install
```
```xml
<dependency>
    <groupId>sh.sagan</groupId>
    <artifactId>redisponse</artifactId>
    <!-- Check for the most recent version! -->
    <version>1.0-SNAPSHOT</version>
</dependency>
```
If you're new to shading jars, check this out: [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/examples/includes-excludes.html)

## Contributing
All contributions welcome! If it's a large one, maybe open an issue first discussing what you'd like to do. Submit a PR and I'll try to look at it asap
