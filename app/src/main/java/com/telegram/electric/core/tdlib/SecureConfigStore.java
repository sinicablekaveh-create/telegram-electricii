package com.telegram.electric.core.tdlib;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Encrypts Telegram application credentials and the TDLib database key at rest.
 */
final class SecureConfigStore {
    private static final String KEY_ALIAS = "telegram-electric-config-v1";
    private static final String PREFS_NAME = "telegram_config";
    private static final String VALUE_KEY = "encrypted_config";
    private static final int IV_LENGTH = 12;

    private final SharedPreferences preferences;

    SecureConfigStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    synchronized Config load() throws Exception {
        String encoded = preferences.getString(VALUE_KEY, null);
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }

        byte[] packed = Base64.decode(encoded, Base64.NO_WRAP);
        if (packed.length <= IV_LENGTH) {
            throw new IllegalStateException("Encrypted Telegram settings are incomplete.");
        }

        byte[] iv = new byte[IV_LENGTH];
        byte[] ciphertext = new byte[packed.length - IV_LENGTH];
        System.arraycopy(packed, 0, iv, 0, IV_LENGTH);
        System.arraycopy(packed, IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
        JSONObject data = new JSONObject(new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8));
        int apiId = data.getInt("api_id");
        String apiHash = data.getString("api_hash");
        String databaseKey = data.getString("database_key");
        if (apiId <= 0 || apiHash.isEmpty() || databaseKey.isEmpty()) {
            throw new IllegalStateException("Stored Telegram settings are incomplete.");
        }
        return new Config(apiId, apiHash, databaseKey);
    }

    synchronized Config save(int apiId, String apiHash) throws Exception {
        if (apiId <= 0 || apiHash == null || apiHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a valid Telegram API ID and API hash.");
        }

        Config current = load();
        String databaseKey = current == null ? createDatabaseKey() : current.databaseKey;
        Config config = new Config(apiId, apiHash.trim(), databaseKey);

        JSONObject data = new JSONObject();
        data.put("api_id", config.apiId);
        data.put("api_hash", config.apiHash);
        data.put("database_key", config.databaseKey);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] encrypted = cipher.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        ByteBuffer packed = ByteBuffer.allocate(iv.length + encrypted.length);
        packed.put(iv);
        packed.put(encrypted);
        String encoded = Base64.encodeToString(packed.array(), Base64.NO_WRAP);
        if (!preferences.edit().putString(VALUE_KEY, encoded).commit()) {
            throw new IllegalStateException("Could not save encrypted Telegram settings.");
        }
        return config;
    }

    private String createDatabaseKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
        );
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build();
        generator.init(spec);
        return generator.generateKey();
    }

    static final class Config {
        final int apiId;
        final String apiHash;
        final String databaseKey;

        Config(int apiId, String apiHash, String databaseKey) {
            this.apiId = apiId;
            this.apiHash = apiHash;
            this.databaseKey = databaseKey;
        }
    }
}
