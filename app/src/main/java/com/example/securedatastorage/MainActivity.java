// TODO Base64はSharedPreference保存を意識した機構らしいので必要
// TODO SharedPreferenceにデータを保存する
// TODO SharedPreferenceを消したりするボタンを作る


package com.example.securedatastorage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PrefKey = "User";
    private TextView encryptTextView;
    private TextView encryptedResultView;
    private TextView decryptResultView;
    private TextView logView;
    private KeyStore mKeyStore;
    private String providerName;
    private String encryptAlgorithm;
    private String aliasName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        encryptTextView = (TextView)findViewById(R.id.encryptText);
        encryptedResultView = (TextView)findViewById(R.id.encryptedText);
        decryptResultView = (TextView)findViewById(R.id.decryptedOutputView);
        logView = (TextView)findViewById(R.id.LogText);

        encryptAlgorithm = getString(R.string.EncryptAlgorith);
        providerName = getString(R.string.KeyProviderName);
        aliasName = getString(R.string.SecureKeyAlias);
        prepareKeyStore(aliasName, providerName);

        loadSavedData();
    }

    private void loadSavedData(){
        SharedPreferences pref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        String saved = pref.getString(PrefKey, "");
        if( saved != "" ) {
            String result = null;
            try {
                result = decryptString(mKeyStore, aliasName, saved);
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            encryptedResultView.setText("saved key is [" + saved + "] decrypt to [" + result + "]");
        }
    }

    private void log(String s){
        logView.append(s + "\n");
        Log.d(TAG, s);
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
        log("start encrypt w/ [" + plainText + "]");
        String encryptedText = null;
        try {
            PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();

            // work arround:
            //   need OAEPParameterSpec for Cipher#init
            //   ref: https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html
            //   ref: https://stackoverflow.com/questions/36015194/android-keystoreexception-unknown-error
            //   ref: https://teratail.com/questions/122006
            OAEPParameterSpec spec = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
            Cipher cipher = Cipher.getInstance(encryptAlgorithm);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, spec);

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

        log("encrypted to [" + encryptedText + "]");
        return encryptedText;
    }

    private String decryptString(KeyStore keyStore, String alias, String encryptedText) throws NoSuchPaddingException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, InvalidKeyException, IOException {
        log("> decryptString [" + encryptedText + "]");
        String plainText = null;
        try {
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, null);

            Cipher cipher = Cipher.getInstance(encryptAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(Base64.decode(encryptedText, Base64.DEFAULT)), cipher);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int b;
            while (true){
                b = cipherInputStream.read();
                if( b == -1 ){
                    break;
                }
                outputStream.write(b);
            }
            outputStream.close();
            plainText = outputStream.toString("UTF-8");
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            throw e;
        }
        log("< decrypted to [" + plainText +"]");
        return plainText;
    }


    public void onEncryptButtonClicked(View view) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        log("> encrypt");
        String input = encryptTextView.getText().toString();
        String result = encryptString(mKeyStore, aliasName, input);
        encryptedResultView.setText(result);
        log("< encrypt");
    }

    public void onDecryptButtonClicked(View view) throws NoSuchPaddingException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, InvalidKeyException, IOException {
        log("> decrypt");
        String input = encryptedResultView.getText().toString();
        String result = decryptString(mKeyStore, aliasName, input);
        decryptResultView.setText(result);
        log("< [" + input + "] is decrypted [" + result + "]");
    }

    public void onClearButtonClicked(View view) {
        logView.setText(null);
        encryptTextView.setText(getString(R.string.encrypt_string_default));
        encryptedResultView.setText(getString(R.string.encrypted_string_default));
        decryptResultView.setText(null);
    }

    // SharedPreferences need apply() method to make data stable.
    public void onSaveButtonClicked(View view) {
        String body = encryptedResultView.getText().toString();
        SharedPreferences pref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        pref.edit().putString(PrefKey, body).apply();
    }

    public void onLoadButtonClicked(View view) {
        SharedPreferences pref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        String saved = pref.getString(PrefKey, "");
        log("saved key is [" + saved + "]");
        encryptedResultView.setText("saved key is [" + saved + "]");
    }

    public void onPurgeButtonClicked(View view) {
        SharedPreferences pref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        pref.edit().remove(PrefKey).apply();
    }
}
