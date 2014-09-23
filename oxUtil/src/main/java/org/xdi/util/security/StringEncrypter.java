package org.xdi.util.security;

import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.xdi.util.Util;

/**
 * Encryption algorithms
 * 
 * @author ssudala
 */
public class StringEncrypter {

	private static final Logger log = Logger.getLogger(StringEncrypter.class);
	private final ReentrantLock lock = new ReentrantLock();
	
	private static String encodeSalt;

	// lazy init via static holder
    private static class Holder {
        static final StringEncrypter instance = createInstance();

        private static StringEncrypter createInstance() {
            try {
                return new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME);
            } catch (EncryptionException e) {
                log.error("Failed to create default StringEncrypter instance", e);
                return null;
            }
        }
    }

	public static StringEncrypter defaultInstance() throws EncryptionException {
		StringEncrypter.encodeSalt = encodeSalt;
		return Holder.instance;
	}

	/**
	 * Exception thrown from encryption failures
	 * 
	 * @author ssudala
	 */
	public static class EncryptionException extends Exception {
		/**
		 * Serial UID
		 */
		private static final long serialVersionUID = -7220454928814292801L;

		/**
		 * Default constructor
		 * 
		 * @param t
		 *            Wrapped exception
		 */
		public EncryptionException(final Throwable t) {
			super(t);
		}
	}

	/**
	 * Default encryption key
	 */
	public static final String DEFAULT_ENCRYPTION_KEY = "This is a fairly long phrase used to encrypt";

	/**
	 * DES encryption scheme
	 */
	public static final String DES_ENCRYPTION_SCHEME = "DES";

	/**
	 * Desede encryption scheme
	 */
	public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";

	/**
	 * Unicode format
	 */
	private static final String UNICODE_FORMAT = "UTF8";

	/**
	 * Convert a byte stream to a string
	 * 
	 * @param bytes
	 *            Byte stream
	 * @return String representation
	 */
	private static String bytes2String(final byte[] bytes) {
		final StringBuffer stringBuffer = new StringBuffer();
		for (final byte element : bytes) {
			stringBuffer.append((char) element);
		}
		return stringBuffer.toString();
	}

	/**
	 * Cipher being used
	 */
	private Cipher cipher;

	/**
	 * Key factory being used
	 */
	private SecretKeyFactory keyFactory;


	private Base64 base64 = new Base64();

	/**
	 * Constructor specifying encryption scheme, using default key
	 * 
	 * @param encryptionScheme
	 *            Encryption scheme to use
	 * @throws EncryptionException
	 */
	public StringEncrypter(final String encryptionScheme) throws EncryptionException {
		this(encryptionScheme, StringEncrypter.DEFAULT_ENCRYPTION_KEY);
	}

	/**
	 * Constructor specifying scheme and key
	 * 
	 * @param encryptionScheme
	 *            Encryption scheme to use
	 * @param encryptionKey
	 *            Encryption key to use
	 * @throws EncryptionException
	 */
	public StringEncrypter(final String encryptionScheme, final String encryptionKey) throws EncryptionException {

		if (encryptionKey == null) {
			throw new IllegalArgumentException("encryption key was null");
		}
		if (encryptionKey.trim().length() < 24) {
			throw new IllegalArgumentException("encryption key was less than 24 characters");
		}

		try {
			keyFactory = SecretKeyFactory.getInstance(encryptionScheme);
			cipher = Cipher.getInstance(encryptionScheme);

		} catch (final NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		} catch (final NoSuchPaddingException e) {
			throw new EncryptionException(e);
		}

	}

	/**
	 * Decrypt a string encrypted with this encrypter
	 * 
	 * @param encryptedString
	 *            Encrypted string
	 * @return Decrypted string
	 * @throws EncryptionException
	 */
	public String decrypt(final String encryptedString, String encryptionKey) throws EncryptionException {
		if ((encryptedString == null) || (encryptedString.trim().length() <= 0)) {
			throw new IllegalArgumentException("encrypted string was null or empty");
		}

		lock.lock();
		try {
			final byte[] keyAsBytes = encryptionKey.getBytes(StringEncrypter.UNICODE_FORMAT);
			String encryptionScheme = StringEncrypter.DESEDE_ENCRYPTION_SCHEME;
			KeySpec keySpec;
			if (encryptionScheme.equalsIgnoreCase(StringEncrypter.DESEDE_ENCRYPTION_SCHEME)) {
				keySpec = new DESedeKeySpec(keyAsBytes);
			} else if (encryptionScheme.equalsIgnoreCase(StringEncrypter.DES_ENCRYPTION_SCHEME)) {
				keySpec = new DESKeySpec(keyAsBytes);
			} else {
				throw new IllegalArgumentException("Encryption scheme not supported: " + encryptionScheme);
			}

			final SecretKey key = keyFactory.generateSecret(keySpec);
			cipher.init(Cipher.DECRYPT_MODE, key);

			final byte[] cleartext = base64.decode(encryptedString.getBytes(Util.UTF8));
			final byte[] ciphertext = cipher.doFinal(cleartext);

			return StringEncrypter.bytes2String(ciphertext);
		} catch (final Exception e) {
			throw new EncryptionException(e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Encrypt a string
	 * 
	 * @param unencryptedString
	 *            String to encrypt
	 * @return Encrypted string (using scheme and key specified at construction)
	 * @throws EncryptionException
	 */
	public String encrypt(final String unencryptedString, String encryptionKey) throws EncryptionException {
		if ((unencryptedString == null) || (unencryptedString.trim().length() == 0)) {
			throw new IllegalArgumentException("unencrypted string was null or empty");
		}

		lock.lock();
		try {
			final byte[] keyAsBytes = encryptionKey.getBytes(StringEncrypter.UNICODE_FORMAT);
			String encryptionScheme = StringEncrypter.DESEDE_ENCRYPTION_SCHEME;
			KeySpec keySpec;
			if (encryptionScheme.equalsIgnoreCase(StringEncrypter.DESEDE_ENCRYPTION_SCHEME)) {
				keySpec = new DESedeKeySpec(keyAsBytes);
			} else if (encryptionScheme.equalsIgnoreCase(StringEncrypter.DES_ENCRYPTION_SCHEME)) {
				keySpec = new DESKeySpec(keyAsBytes);
			} else {
				throw new IllegalArgumentException("Encryption scheme not supported: " + encryptionScheme);
			}
			
			
			final SecretKey key = keyFactory.generateSecret(keySpec);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			final byte[] cleartext = unencryptedString
					.getBytes(StringEncrypter.UNICODE_FORMAT);
			final byte[] ciphertext = cipher.doFinal(cleartext);

			return new String(base64.encode(ciphertext), Util.UTF8);
		} catch (final Exception e) {
			throw new EncryptionException(e);
		} finally {
			lock.unlock();
		}
	}

	/*
	 * private String decrypt2(final String password){ final String
	 * encryptionKey = "123456789012345678901234567890"; final String
	 * encryptionScheme = StringEncrypter.DESEDE_ENCRYPTION_SCHEME;
	 * 
	 * try { final StringEncrypter encrypter = new StringEncrypter(
	 * encryptionScheme, encryptionKey ); return encrypter.decrypt(password); }
	 * catch (final EncryptionException e) { e.printStackTrace(); } return
	 * "invalidpass"; }
	 */
}
