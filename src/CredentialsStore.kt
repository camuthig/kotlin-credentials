package org.camuthig.credentials.core

import com.typesafe.config.Config

/**
 * An interface for interacting with encrypted credentials files.
 */
interface CredentialsStore {
    /**
     * Load an encrypted credentials file into a Config object.
     */
    fun load(): Config

    /**
     * Add or update a new credential to the encrypted file.
     */
    fun upsert(key: String, value: String)

    /**
     * Remove an existing credential from the encrypted file.
     */
    fun delete(key: String)

    /**
     * Generate a black credentials file and a new master key.
     */
    fun generate()

    /**
     * Decrypt the credentials file and re-encrypt it using a newly generated key.
     */
    fun rekey()
}