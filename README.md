# 📫 redisponse
A request-response structure lib for Redis pub-subs. Need the req-res of an HTTP server but already have Redis pub-subs? Use this

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
    <version>1.0.0</version>
</dependency>
```
If you're new to shading jars, check this out: [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/examples/includes-excludes.html)

## Contributing
All contributions welcome! If it's a large one, maybe open an issue first discussing what you'd like to do. Submit a PR and I'll try to look at it asap

## Usage
Internally, Redisponse uses [lettuce.io](https://github.com/lettuce-io/lettuce-core) as its redis client, not that you need it though.

Start by making a new Redisponse instance in both of your communicating programs:
```java
{
    // Feel free to use any of the constructors provided. I reccommend the RedisURI one
    Redisponse redisponse = new Redisponse("db.example.com", "6969", "foobar");
    
    // Initialize some endpoints
    redisponse.response("my-channel", req -> {
        return req + ". Oh look, some output!";
    });
    
    // Only 1 response handler per channel!
    redisponse.response("another-channel", req -> {
        String dbInfo = ...; // maybe some database calls based on req
        return dbInfo;
    });
    
    // Potentially in another class, make some requests!
    redisponse.request("my-channel", "Some input").thenAccept(res -> {
        System.out.println(res); // "Some input. Oh look, some output!"
    });
    
    // example
    redisponse.request("another-channel", User#getInfo()).thenAccept(res -> {
        System.out.println(res); // some db info for the user
    });
}
```