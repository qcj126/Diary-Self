package diary.utils.crypto;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 加解密工具（传输层密码保护，使用 OAEP SHA-256）。
 */
public final class RsaCryptoUtil {

    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    private RsaCryptoUtil() {
    }

    /**
     * 使用公钥加密明文，返回 Base64 密文。
     */
    public static String encryptToBase64(String plainText, String publicKeyBase64) throws GeneralSecurityException {
        if (plainText == null) {
            throw new IllegalArgumentException("plainText 不能为空");
        }
        PublicKey publicKey = decodePublicKey(publicKeyBase64);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, new javax.crypto.spec.OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, javax.crypto.spec.PSource.PSpecified.DEFAULT));
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 使用私钥解密 Base64 密文，得到明文。
     */
    public static String decryptFromBase64(String cipherTextBase64, String privateKeyBase64)
            throws GeneralSecurityException {
        if (cipherTextBase64 == null || cipherTextBase64.isBlank()) {
            throw new IllegalArgumentException("cipherTextBase64 不能为空");
        }
        PrivateKey privateKey = decodePrivateKey(privateKeyBase64);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey, new javax.crypto.spec.OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, javax.crypto.spec.PSource.PSpecified.DEFAULT));
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherTextBase64));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static PublicKey decodePublicKey(String publicKeyBase64) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public static PrivateKey decodePrivateKey(String privateKeyBase64) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }
}
