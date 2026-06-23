// مسار الملف: src/main/java/com/pharmacy/utils/security/PasswordUtil.java

package com.pharmacy.utils.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * كلاس الأمان للتعامل مع تشفير كلمات المرور.
 * تم استخدام PBKDF2 (Password-Based Key Derivation Function 2) 
 * لإبطاء عملية التشفير عمداً، مما يجعل هجمات القوة الغاشمة (Brute-Force) غير مجدية حسابياً.
 */
public class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    /**
     * تشفير كلمة المرور بدمجها مع ملح (Salt) عشوائي.
     *
     * @param plain كلمة المرور النصية
     * @return السلسلة المشفرة بصيغة (Base64(Salt):Base64(Hash))
     */
    public static String hashPassword(String plain) {
        if (plain == null || plain.trim().isEmpty()) {
            throw new IllegalArgumentException("لا يمكن تشفير كلمة مرور فارغة.");
        }

        try {
            // 1. توليد الملح (Salt) العشوائي
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // 2. تطبيق خوارزمية التشفير
            byte[] hash = generateHash(plain.toCharArray(), salt);

            // 3. دمج الملح والهاش باستخدام Base64 لتخزينها في قاعدة البيانات
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);

            return saltBase64 + ":" + hashBase64;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("خطأ فادح في النظام: فشل تهيئة خوارزمية التشفير.", e);
        }
    }

    /**
     * التحقق من تطابق كلمة المرور المدخلة مع الكلمة المشفرة المخزنة.
     *
     * @param plain الكلمة المدخلة
     * @param storedPassword الكلمة المخزنة بصيغة (Base64(Salt):Base64(Hash))
     * @return true إذا تطابق الهاش، وإلا false
     */
    public static boolean verifyPassword(String plain, String storedPassword) {
        if (plain == null || storedPassword == null || !storedPassword.contains(":")) {
            return false;
        }

        try {
            // 1. استخراج الملح والهاش من السلسلة المخزنة
            String[] parts = storedPassword.split(":");
            if (parts.length != 2) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHash = Base64.getDecoder().decode(parts[1]);

            // 2. تشفير الكلمة المدخلة باستخدام نفس الملح
            byte[] testHash = generateHash(plain.toCharArray(), salt);

            // 3. مقارنة النتيجتين (يجب استخدام MessageDigest.isEqual لمنع هجمات التوقيت Timing Attacks)
            return MessageDigest.isEqual(storedHash, testHash);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            System.err.println("[خطأ أمني] فشل التحقق من كلمة المرور. قد تكون بنية الهاش تالفة: " + e.getMessage());
            return false;
        }
    }

    /**
     * دالة مساعدة لتوليد الهاش باستخدام PBKDF2.
     */
    private static byte[] generateHash(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] hash = factory.generateSecret(spec).getEncoded();
        spec.clearPassword(); // تنظيف الذاكرة لأسباب أمنية
        return hash;
    }
}