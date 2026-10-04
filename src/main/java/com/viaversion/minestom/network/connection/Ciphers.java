package com.viaversion.minestom.network.connection;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minestom.server.extras.mojangAuth.MojangCrypt;

record Ciphers(Cipher encrypt, Cipher decrypt) {

    static Ciphers of(final SecretKey key) {
        return new Ciphers(MojangCrypt.getCipher(Cipher.ENCRYPT_MODE, key), MojangCrypt.getCipher(Cipher.DECRYPT_MODE, key));
    }
}
