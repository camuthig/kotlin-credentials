# Credentials

This project creates the core logic to store confidential credentials colocated with the project the support in an
encrypted formated to avoid exposing these values.

The projects works with two files:

* An encrypted configuration file, compatible with the Typesafe Config format. This is where your credentials will live,
and this file should be included in your version control.
* A master key that allows encrypting and decrypting the configuration file. This should **not** be included in your
version control.
