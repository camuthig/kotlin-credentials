# Credentials

![Build](https://img.shields.io/travis/camuthig/kotlin-credentials.svg?style=flat-square)

This project creates the core logic to store confidential credentials colocated with the project the support in an
encrypted formatted to avoid exposing these values.

The projects works with two files:

* An encrypted configuration file, compatible with the Typesafe Config format. This is where your credentials will live,
and this file should be included in your version control.
* A master key that allows encrypting and decrypting the configuration file. This should **not** be included in your
version control.

## Setup

Add the dependency to your project dependencies. In Gradle this would be:

```kotlin
dependencies {
    implementation("org.camuthig.credentials:core:0.1.0-SNAPSHOT")
}
```

## Usage

### ClassLoaderCredentialsStore
The simplest implementation of the `CredentialsStore` interface is this `ClassLoaderCredentialsStore`, which works
similarly to the default Typesafe Config loader, based on the `CloaseLoader` passed in.

To create a new credentials files in your `resources` directory called `application.conf.enc` and add a credentials to
it you would be able to do something along the lines of:

```kotlin
import org.camuthig.credentials.core.*

val store = ClassLoaderCredentialsStore(ClassLoader.getSystemClassLoader())

store.generate()

store.upsert("myTopSecretKey", "ThisWouldBeYourSecretKeyValue")

// This value can be later retrieved using
store.get("myTopSecretKey")
```

The store will default to using the `application.conf.enc` and `master.key` files in your resources directory. However,
this can be overridden using the following system properties

* `credentials.file` - A file path at which the configuration file can be found
* `credentials.resource` - A resource path at which the configuration cna be found
* `credentials.masterKeyFile` - A file path at which the configuration file can be found
* `credentials.masterKeyResource` - a resource path at which the configuration file can be found

### FileCredentialsStore

If, for some reason, using the `ClassLoader` isn't the `FileCredentialsStore` can instead be used directly. This class
is what powers the `ClassLoaderCredentialsStore`. It accepts `File` objects for the configuration file and key, and
can be used in the same way as the `ClassLoaderCredentialsStore`.
