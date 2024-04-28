// IN2011 Computer Networks
// Coursework 2023/2024
//
// Construct the hashID for a string

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashID {

	public static String computeHashID(String line) {
		if (!line.endsWith("\n")) {
			throw new IllegalArgumentException("Input to HashID must end with a newline character.");
		}

		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = md.digest(line.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 hashing not supported in this environment.", e);
		}
	}


	private static String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}
}
