<p align="center">
  <img src="https://pac4j.github.io/pac4j/img/logo-spring-webflux.png" width="300" />
</p>

The `spring-webflux-pac4j` project is an **easy and powerful security library for Spring Webflux / Spring Boot web applications and web services**. It supports authentication and authorization, but also logout and advanced features like session fixation and CSRF protection.
It's based on the **[pac4j security engine](https://github.com/pac4j/pac4j)**. It's available under the Apache 2 license.

| spring-webflux-pac4j | JDK | pac4j | Spring |
|----------------------|-----|-------|--------|
| version >= 3         | 17  | v6    | v6     |
| version >= 2         | 17  | v5    | v6     |
| version >= 1         | 11  | v5    | v5     |

[**Main concepts and components:**](https://www.pac4j.org/docs/main-concepts-and-components.html)

1) A [**client**](https://www.pac4j.org/docs/clients.html) represents an authentication mechanism. It performs the login process and returns a user profile. An indirect client is for web application authentication while a direct client is for web services authentication:

&#9656; OAuth - SAML - CAS - OpenID Connect - HTTP - Google App Engine - LDAP - SQL - JWT - MongoDB - CouchDB - Kerberos - IP address - Kerberos (SPNEGO) - REST API

2) An [**authorizer**](https://www.pac4j.org/docs/authorizers.html) is meant to check authorizations on the authenticated user profile(s) or on the current web context:

&#9656; Roles / permissions - Anonymous / remember-me / (fully) authenticated - Profile type, attribute -  CORS - CSRF - Security headers - IP address, HTTP method

3) A [**matcher**](https://www.pac4j.org/docs/matchers.html) defines whether the `SecurityFilter` must be applied and can be used for additional web processing

4) The `SecurityFilter` protects an url by checking that the user is authenticated and that the authorizations are valid, according to the clients and authorizers configuration. If the user is not authenticated, it performs authentication for direct clients or starts the login process for indirect clients

5) The `CallbackController` finishes the login process for an indirect client

6) The `LogoutController` logs out the user from the application and triggers the logout at the identity provider level.


## Usage

### 1) [Add the required dependencies](https://github.com/pac4j/spring-webflux-pac4j/wiki/Dependencies)

### 2) Define:

### - the [security configuration](https://github.com/pac4j/spring-webflux-pac4j/wiki/Security-configuration)
### - the [callback configuration](https://github.com/pac4j/spring-webflux-pac4j/wiki/Callback-configuration), only for web applications
### - the [logout configuration](https://github.com/pac4j/spring-webflux-pac4j/wiki/Logout-configuration)

### 3) [Apply security](https://github.com/pac4j/spring-webflux-pac4j/wiki/Apply-security)

### 4) [Get the authenticated user profiles](https://github.com/pac4j/spring-webflux-pac4j/wiki/Get-the-authenticated-user-profiles)


## Demos

Spring Webflux boot demo: [spring-webflux-pac4j-boot-demo](https://github.com/pac4j/spring-webflux-pac4j-boot-demo).

Spring Security reactive boot demo: [spring-security-reactive-pac4j-boot-demo](https://github.com/pac4j/spring-security-reactive-pac4j-boot-demo).


## Versions

The latest released version is the [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.pac4j/spring-webflux-pac4j/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/org.pac4j/spring-webflux-pac4j), available in the [Maven central repository](https://repo.maven.apache.org/maven2).
The [next version](https://github.com/pac4j/spring-webflux-pac4j/wiki/Next-version) is under development.

See the [release notes](https://github.com/pac4j/spring-webflux-pac4j/wiki/Release-Notes). Learn more by browsing the [pac4j documentation](https://www.javadoc.io/doc/org.pac4j/pac4j-core/5.7.0/index.html) and the [spring-webflux-pac4j Javadoc](http://www.javadoc.io/doc/org.pac4j/spring-webflux-pac4j/2.0.0).

See the [migration guide](https://github.com/pac4j/spring-webflux-pac4j/wiki/Migration-guide) as well.


## Need help?

You can use the [mailing lists](https://www.pac4j.org/mailing-lists.html) or the [commercial support](https://www.pac4j.org/commercial-support.html).
