package org.camuthig.credentials.core

import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import java.io.File
import javax.crypto.Cipher

/**
 * Retrieve encrypted credentials from resources or files stored on the application's classpath.
 *
 * The credentials file will default to using the "credentials.conf.enc" file stored in your application's resources
 * directory. However, this can be overridden with either the "credentials.file" or "credentials.resource" system
 * properties to use either a local file or resource.
 *
 * The master key file will default to using the "master.key" file store in your application's resources directory.
 * However this can also be overridden with either the "credentials.masterKeyFile" or "credentials.masterKeyResource"
 * system properties.
 */
open class ClassLoaderCredentialsStore(protected val loader: ClassLoader) : CredentialsStore {
    protected val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    private val fileStore: FileCredentialsStore

    init {
        fileStore = FileCredentialsStore(getConfiguration(), getKey())
    }

    override fun load(): Config {
        return fileStore.load()
    }

    override fun upsert(key: String, value: String) {
        fileStore.upsert(key, value)
    }

    override fun delete(key: String) {
        fileStore.delete(key)
    }

    override fun generate() {
        fileStore.generate()
    }

    override fun rekey() {
        fileStore.rekey()
    }

    protected fun getKey(): File {
        var specified = 0
        // override credentials.conf with credentials.file, credentials.resource, if provided
        var resource = System.getProperty("credentials.masterKeyResource")
        if (resource != null) {
            specified += 1
        }

        val file = System.getProperty("credentials.masterKeyFile")
        if (file != null) {
            specified += 1
        }

        if (specified > 1) {
            throw ConfigException.Generic(
                "You set more than one of credentials.masterKeyFile='" + file
                        + "', credentials.masterKeyResource='" + resource
                        + "'; don't know which one to use!"
            )
        }

        if (specified == 0) {
            return File(loader.getResource("master.key").toURI())
        }

        if (resource != null) {
            if (resource.startsWith("/")) {
                resource = resource.substring(1)
            }

            return File(loader.getResource(resource).toURI())
        }

        if (file != null) {
            return File(file)
        }

        throw ConfigException.BugOrBroken("A master key was not set.")
    }

    protected fun getConfiguration(): File {
        var specified = 0
        // override credentials.conf with credentials.file, credentials.resource, if provided
        var resource = System.getProperty("credentials.resource")
        if (resource != null) {
            specified += 1
        }

        val file = System.getProperty("credentials.file")
        if (file != null) {
            specified += 1
        }

        if (specified > 1) {
            throw ConfigException.Generic(
                "You set more than one of credentials.file='" + file
                        + "', credentials.resource='" + resource
                        + "'; don't know which one to use!"
            )
        }

        if (specified == 0) {
            return File(loader.getResource("credentials.conf.enc").toURI())
        }

        if (resource != null) {
            if (resource.startsWith("/")) {
                resource = resource.substring(1)
            }

            return File(loader.getResource(resource).toURI())
        }

        if (file != null) {
            return File(file)
        }

        throw ConfigException.BugOrBroken("A configuration file was not set.")
    }
}