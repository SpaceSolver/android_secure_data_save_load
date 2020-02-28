// TODO Base64を辞める
// TODO SharedPreferenceにデータを保存する
// TODO SharedPreferenceを消したりするボタンを作る


package com.example.securedatastorage;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "KeyStoreProviderSample";
    private TextView logView;
    private KeyStore mKeyStore;
    private String providerName;
    private String encryptAlgorithm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logView = (TextView)findViewById(R.id.LogText);

        encryptAlgorithm = getString(R.string.EncryptAlgorith);
        providerName = getString(R.string.KeyProviderName);
        prepareKeyStore(getString(R.string.SecureKeyAlias), providerName);
    }

    private void log(String s){
        logView.append(s + "\n");
    }

    private void prepareKeyStore(String alias, String providerName) {
        try {
            mKeyStore = KeyStore.getInstance(providerName);
            mKeyStore.load(null);
            createNewKey(mKeyStore, alias, providerName);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void createNewKey(KeyStore keyStore, String alias, String providerName) {
        try {
            // Create new key if not having been registered
            if (!keyStore.containsAlias(alias)) {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_RSA, providerName);
                keyPairGenerator.initialize(
                        new KeyGenParameterSpec.Builder(
                                alias,
                                KeyProperties.PURPOSE_DECRYPT)
                                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                                .build());
                keyPairGenerator.generateKeyPair();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private String encryptString(KeyStore keyStore, String alias, String plainText) {
        String encryptedText = null;
        try {
            PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();

            Cipher cipher = Cipher.getInstance(encryptAlgorithm);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    outputStream, cipher);
            cipherOutputStream.write(plainText.getBytes("UTF-8"));
            cipherOutputStream.close();

            byte [] bytes = outputStream.toByteArray();
            encryptedText = Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return encryptedText;
    }

    private String decryptString(KeyStore keyStore, String alias, String encryptedText) {
        String plainText = null;
        try {
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, null);

            Cipher cipher = Cipher.getInstance(encryptAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(Base64.decode(encryptedText, Base64.DEFAULT)), cipher);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int b;
            while ((b = cipherInputStream.read()) != -1) {
                outputStream.write(b);
            }
            outputStream.close();
            plainText = outputStream.toString("UTF-8");
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return plainText;
    }


    public void onStoreButtonClicked(View view) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        log("> store");
        log("< store");
    }

    public void onLoadButtonClicked(View view) {
        log("> load");
        log("< load");
    }

    public void onClearButtonClicked(View view) {
        logView.setText(null);
    }
}
