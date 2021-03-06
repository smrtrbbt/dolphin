package general;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class Helper {
	public static final String addressRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]):\\d{4}\\d?$";
	public static final String nameRegex = "^(?!.*[-_]{2,})(?=^[^-_].*[^-_]$)[\\w\\s-]{3,24}$";
	
	
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();
	
	public static String generateToken() {
	    byte[] randomBytes = new byte[48];
	    secureRandom.nextBytes(randomBytes);
	    return base64Encoder.encodeToString(randomBytes);	
	}
	
	public static String generateId(String fullAddress) {
		String id = String.format("%s%d", fullAddress, System.currentTimeMillis());
		String hash = Sha256(id);
		return hash;
	}

	public static byte[] encodeMessage(Serializable msg, String key) throws IOException {
		byte[] ret;
		
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = new ObjectOutputStream(bos)){
			out.writeObject(msg);
			out.flush();
			
			ret = bos.toByteArray();
		}
		
		return ret;
	}
	
	public static Object decodeMessage(byte[] buffer, String key) throws IOException, ClassNotFoundException {
		Object msg;
		
		try (ByteArrayInputStream bit = new ByteArrayInputStream(buffer);
				ObjectInput in = new ObjectInputStream(bit)){
			msg = in.readObject();
		}
		
		return msg;
	}
	
	private static String Sha256(String val) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			byte[] encodedhash = digest.digest(val.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(encodedhash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
    }
	
	private static String bytesToHex(byte[] hash) {
	    StringBuffer hexString = new StringBuffer();
	    for (int i = 0; i < hash.length; i++) {
	    String hex = Integer.toHexString(0xff & hash[i]);
	    if(hex.length() == 1) hexString.append('0');
	        hexString.append(hex);
	    }
	    return hexString.toString();
	}
}