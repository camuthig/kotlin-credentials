package org.camuthig.credentials.core

import com.typesafe.config.*
import java.io.*
import java.security.SecureRandom
import java.util.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Store encrypted credentials and the master key within a specified file on the system.
 */
open class FileCredentialsStore(protected val credentialsFile: File, protected val masterKeyFile: File) : CredentialsStore {
    protected val cipher:Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

    override fun load(): Config {
        if (!credentialsFile.exists()) {
            throw CredentialsFileMissing("Unable to find the credentials file ${credentialsFile.path}")
        }

        if (!masterKeyFile.exists()) {
            throw KeyFileMissing("Unable to find the key file ${masterKeyFile.path}")
        }

        return ConfigFactory.parseString(decryptCredentials(readFile(), loadKey()), ConfigParseOptions.defaults().setAllowMissing(false))
    }

    override fun upsert(key: String, value: String) {
        val credentials = load()

        store(credentials.withValue(key, ConfigValueFactory.fromAnyRef(value)))
    }

    override fun delete(key: String) {
        val credentials = load()

        store(credentials.withoutPath(key))
    }

    override fun generate() {
        if (credentialsFile.exists()) {
            throw Exception("The credentials file ${credentialsFile.path} already exists")
        }

        if (masterKeyFile.exists()) {
            throw Exception("The key file ${masterKeyFile.path} already exists")
        }

        credentialsFile.createNewFile()
        masterKeyFile.createNewFile()

        storeKey(generateKey())
    }

    override fun rekey() {
        val credentials = load()

        storeKey(generateKey())

        store(credentials)
    }

    protected fun store(credentials: Config) {
        writeFile(credentialsFile, encryptCredentials(StringBuffer(credentials.root().render(ConfigRenderOptions.defaults().setJson(false).setOriginComments(false))), loadKey()))
    }

    protected fun generateKey(): Pair<SecretKey, ByteArray> {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val secretKey = keyGen.generateKey()

        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        return Pair(SecretKeySpec(secretKey.encoded, "AES"), salt)
    }

    protected fun loadKey(): Pair<SecretKey, ByteArray> {
        val key = masterKeyFile.readBytes()
        val salt = key.takeLast(16)
        val password = key.dropLast(16)

        return Pair(SecretKeySpec(password.toByteArray(), "AES"), salt.toByteArray())
    }

    protected fun storeKey(key: Pair<SecretKey, ByteArray>)  = masterKeyFile.writeBytes(key.first.encoded + key.second)

    protected fun decryptCredentials(credentials: ByteArray, key: Pair<SecretKey, ByteArray>): String {
        var content = ""

        ByteArrayInputStream(credentials).use { inputFile ->
            cipher.init(Cipher.DECRYPT_MODE, key.first, IvParameterSpec(key.second,0, cipher.blockSize))

            CipherInputStream(inputFile, cipher).use { cipherIn ->
                InputStreamReader(cipherIn).use { inputReader ->
                    BufferedReader(inputReader).use { reader ->

                        val sb = StringBuilder()
                        reader.forEachLine { line ->
                            sb.appendln(line)
                        }

                        content = sb.toString()
                    }
                }
            }

            return content
        }
    }

    protected fun encryptCredentials(credentials: StringBuffer, key: Pair<SecretKey, ByteArray>): ByteArray {
        val encrypted = ByteArrayOutputStream()
        cipher.init(Cipher.ENCRYPT_MODE, key.first, IvParameterSpec(key.second,0, cipher.blockSize))

        CipherOutputStream(encrypted, cipher).bufferedWriter().use { cipherOut ->
            credentials.forEach { c ->
                cipherOut.append(c)
            }
        }

        return encrypted.toByteArray()
    }

    protected fun writeFile(file: File, credentials: ByteArray) {
        // TODO This could be streamed with some alterations
        file.writeBytes(Base64.getEncoder().encode(credentials))
    }

    protected fun readFile(): ByteArray {
        val encoded = StringBuilder()
        credentialsFile.forEachLine {
            encoded.appendln(it)
        }

        // TODO This could be streamed with some alterations
        return Base64.getDecoder().decode(encoded.toString().trimEnd())
    }
}