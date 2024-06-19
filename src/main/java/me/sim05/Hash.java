package me.sim05;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class Hash {
    private final String password;
    private byte[] salt;
    private byte[] hashedPassword;

    public Hash(String passwordToHash) {
        password = passwordToHash;
    }

    public void generateSalt () {
        SecureRandom random = new SecureRandom();
        salt = new byte[16];
        random.nextBytes(salt);
    }

    public void hashPassword() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt);

        hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
    }

    public String getHashedPassword() {
        return Base64.getEncoder().encodeToString(hashedPassword);
    }
    public String getSalt() {
        return Base64.getEncoder().encodeToString(salt);
    }

    public static boolean verify(String password, String salt, String hashedPassword) throws NoSuchAlgorithmException {
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        byte[] hashedPasswordBytes = Base64.getDecoder().decode(hashedPassword);

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(saltBytes);

        byte[] hashedPasswordToCheck = md.digest(password.getBytes(StandardCharsets.UTF_8));

        return MessageDigest.isEqual(hashedPasswordBytes, hashedPasswordToCheck);
    }
    public static String[] separateSaltAndHash(String saltAndHash) {
        return saltAndHash.split(":");
    }
}
