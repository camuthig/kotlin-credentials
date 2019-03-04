package org.camuthig.credentials.core.typesafe

import com.typesafe.config.*
import org.camuthig.credentials.core.CredentialsStore

/**
 * A loading strategy to pull the configuration from an encrypted file on the system.
 */
open class CredentialsLoadingStrategy(private val store: CredentialsStore): ConfigLoadingStrategy {
    override fun parseApplicationConfig(parseOptions: ConfigParseOptions): Config {
        return store.load()
    }
}
