package org.camuthig.credentials.core

import java.io.File
import java.lang.Exception
import kotlin.test.*

class CredentialsStoreTest {
    private fun buildStore(): CredentialsStore {
        return ClassLoaderCredentialsStore(ClassLoader.getSystemClassLoader())
    }

    @Test
    fun `it should add a value to the credentials`() {
        val file = "updatedResource.conf.enc"
        val credentials = "uYjsLAfBEDzvZpVNfEC1H8801R+N3bDpFsyY4XRWkVE="

        try {
            System.setProperty("credentials.file", file)
            File(file).writeText(credentials)

            val store = buildStore()
            store.upsert("two", "blah")

            val updated = store.load()
            assertEquals("blah", updated.getString("two"))
        } finally {
            System.clearProperty("credentials.file")
            File(file).delete()
        }
    }

    @Test
    fun `it should update a value in the credentials`() {
        val file = "updatedResource.conf.enc"
        val credentials = "uYjsLAfBEDzvZpVNfEC1H8801R+N3bDpFsyY4XRWkVE="

        try {
            System.setProperty("credentials.file", file)
            File(file).writeText(credentials)

            val store = buildStore()
            store.upsert("credentials.one", "blah")

            val updated = store.load()
            assertEquals("blah", updated.getString("credentials.one"))
        } finally {
            System.clearProperty("credentials.file")
            File(file).delete()
        }
    }

    @Test
    fun `it should remove a value from the credentials`() {
        val file = "updatedResource.conf.enc"
        val credentials = "uYjsLAfBEDzvZpVNfEC1H8801R+N3bDpFsyY4XRWkVE="

        try {
            System.setProperty("credentials.file", file)
            File(file).writeText(credentials)

            val store = buildStore()
            store.delete("credentials.one")

            val updated = store.load()
            assertFalse(updated.hasPath("credentials.one"))
            assert(updated.hasPath("credentials"))
        } finally {
            System.clearProperty("credentials.file")
            File(file).delete()
        }
    }

    @Test
    fun `it should find credentials using a default resource and key`() {
        // TODO
        val store = buildStore()
        val config = store.load()

        assertEquals("one", config.getString("credentials.one"))
    }

    @Test
    fun `it should find credentials using a configured resource and key`() {
        try {
            System.setProperty("credentials.resource", "configured.conf.enc")
            System.setProperty("credendials.masterKeyResource", "configured.key")

            val store = buildStore()
            val config = store.load()
            assertEquals("two", config.getString("credentials.two"))
        } finally {
            System.clearProperty("credentials.resource")
            System.clearProperty("credentials.masterKeyResource")
        }
    }

    @Test
    fun `it should find credentials using a configured file and key`() {
        try {
            System.setProperty("credentials.file", "test/resources/file.conf.enc")
            System.setProperty("credendials.masterKeyFile", "test/resources/file.key")

            val store = buildStore()
            val config = store.load()
            assertEquals("three", config.getString("credentials.three"))
        } finally {
            System.clearProperty("credentials.file")
            System.clearProperty("credentials.masterKeyFile")
        }
    }

    @Test
    fun `it should generate a key and credentials file`() {
        val credentialsFile = "test/resources/generated.conf.enc"
        val keyFile = "test/resources/generated.key"
        try {
            System.setProperty("credentials.file", credentialsFile)
            System.setProperty("credentials.masterKeyFile", keyFile)

            val store = buildStore()

            store.generate()

            assertTrue(File(credentialsFile).exists())
            assertTrue(File(keyFile).exists())
            assertNotNull(store.load())

        } finally {
            System.clearProperty("credentials.file")
            System.clearProperty("credentials.masterKeyFile")
            File(credentialsFile).delete()
            File(keyFile).delete()
        }
    }

    @Test(expected = Exception::class)
    fun `it should fail to generate with an existing credentials file`() {
        val credentialsFile = "test/resources/configured.conf.enc"
        val keyFile = "test/resources/generated.key"
        try {
            System.setProperty("credentials.file", credentialsFile)
            System.setProperty("credentials.masterKeyFile", keyFile)

            val store = buildStore()

            store.generate()
        } finally {
            System.clearProperty("credentials.file")
            System.clearProperty("credentials.masterKeyFile")
        }
    }

    @Test(expected = Exception::class)
    fun `it should fail to generate with an existing key`() {
        val credentialsFile = "test/resources/generated.conf.enc"
        val keyFile = "test/resources/configured.key"
        try {
            System.setProperty("credentials.file", credentialsFile)
            System.setProperty("credentials.masterKeyFile", keyFile)

            val store = buildStore()

            store.generate()
        } finally {
            System.clearProperty("credentials.file")
            System.clearProperty("credentials.masterKeyFile")
        }
    }

    @Test
    fun `it should rekey an existing credentials file`() {
        val credentialsFile = "test/resources/generated.conf.enc"
        val keyFile = "test/resources/generated.key"
        try {
            System.setProperty("credentials.file", credentialsFile)
            System.setProperty("credentials.masterKeyFile", keyFile)

            val store = buildStore()

            store.generate()
            val originalKey = File(keyFile).readBytes()

            store.upsert("test", "1,2,3")

            assertTrue(File(credentialsFile).exists())
            assertTrue(File(keyFile).exists())
            assertEquals("1,2,3", store.load().getString("test"))

            store.rekey()

            val newKey = File(keyFile).readBytes()

            assertFalse(originalKey.contentEquals(newKey))
            assertEquals("1,2,3", store.load().getString("test"))
        } finally {
            System.clearProperty("credentials.file")
            System.clearProperty("credentials.masterKeyFile")
            File(credentialsFile).delete()
            File(keyFile).delete()
        }
    }
}