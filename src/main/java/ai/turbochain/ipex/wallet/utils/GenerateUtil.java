package ai.turbochain.ipex.wallet.utils;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.digests.RIPEMD160Digest;
 
/**
 * 生成虚拟货币地址
 * 
 * @author 
 * @date 2018年11月16日 下午2:44:17
 */
public class GenerateUtil {
 
	private static Logger logger = LoggerFactory.getLogger(GenerateUtil.class);
 
	/**
	 * 生成比特币地址(主网络版本字节0x00,测试网络0x6F)
	 * 
	 * @author 
	 * @date 2018年11月16日 下午2:51:39
	 */
	public static String bit() {
		NetworkParameters params = TestNet3Params.get();
		ECKey key = new ECKey();
		logger.warn("私钥 => {}", key.getPrivateKeyAsHex());
		logger.warn("公钥 => {}", key.getPublicKeyAsHex());
		logger.warn("地址 => {}", key.toAddress(params));
		return key.toAddress(params).toString();
	}
 
	public static void main(String[] args) throws DecoderException {
		parent("00");
	}
	
	/**
	 * 比特系列地址生成方法
	 * 
	 * @author 
	 * @date 2018年11月21日 下午4:43:45
	 */
	private static String parent(String version) {
		try {
			// 椭圆曲线加密生成私钥
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
			ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
			keyGen.initialize(ecSpec);
			// 创建密钥对
			KeyPair kp = keyGen.generateKeyPair();
			PublicKey pub = kp.getPublic();
			PrivateKey pvt = kp.getPrivate();
			// 获取私钥
			ECPrivateKey epvt = (ECPrivateKey) pvt;
			String sepvt = Utils.adjustTo64(epvt.getS().toString(16)).toUpperCase();
			logger.warn("s[" + sepvt.length() + "]: " + sepvt);
			logger.warn("私钥{}", sepvt);	
			
			// 获取公钥
			ECPublicKey epub = (ECPublicKey) pub;
			ECPoint pt = epub.getW();
			String sx = Utils.adjustTo64(pt.getAffineX().toString(16)).toUpperCase();
			String sy = Utils.adjustTo64(pt.getAffineY().toString(16)).toUpperCase();
			// 公钥
			String bcPub = "04" + sx + sy;
			logger.warn("公钥{}", bcPub);
			// sha256
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			byte[] s1 = sha.digest(bcPub.getBytes("UTF-8"));
			logger.warn("sha256后{}", Utils.byte2Hex(s1).toUpperCase());
			// ripemd160
			RIPEMD160Digest digest = new RIPEMD160Digest();
			digest.update(s1, 0, s1.length);
			byte[] ripemd160Bytes = new byte[digest.getDigestSize()];
			digest.doFinal(ripemd160Bytes, 0);
			logger.warn("ripemd160加密后{}", Utils.bytesToHexString(ripemd160Bytes));
			// 添加主网络版本字节
			byte[] networkID = new BigInteger(version, 16).toByteArray();
			byte[] extendedRipemd160Bytes = Utils.add(networkID, ripemd160Bytes);
			logger.warn("添加NetworkID后{}", Utils.bytesToHexString(extendedRipemd160Bytes));
			// 重复sha256两次
			byte[] twiceSha256Bytes = Utils.sha256(Utils.sha256(extendedRipemd160Bytes));
			logger.warn("两次sha256加密后{}", Utils.bytesToHexString(twiceSha256Bytes));
			// 获取前四个字节当做地址校验和
			byte[] checksum = new byte[4];
			System.arraycopy(twiceSha256Bytes, 0, checksum, 0, 4);
			logger.warn("checksum{}", Utils.bytesToHexString(checksum));
			byte[] binaryBitcoinAddressBytes = Utils.add(extendedRipemd160Bytes, checksum);
			logger.warn("添加checksum之后{}", Utils.bytesToHexString(binaryBitcoinAddressBytes));
			// 使用base58对地址进行编码
			String ltccoinAddress = Base58.encode(binaryBitcoinAddressBytes);
			logger.warn("地址{}", ltccoinAddress);
			return ltccoinAddress;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	/**
	 * 生成莱特币地址30
	 * 
	 * @author 
	 * @date 2018年11月20日 上午9:06:05
	 */
	public static String ltc() {
		return parent("30");
	}
 
	/**
	 * 木币地址49
	 * @author 1
	 * @date 2019年6月11日 上午11:38:34
	 */
	public static String wood() {
		return parent("49");
	}
	
	/**
	 * 生成狗狗币地址
	 * 
	 * @author 
	 * @date 2018年11月20日 下午3:35:48
	 */
	public static String doge() {
		return parent("1e");
	}
 
	/**
	 * 生成dash(达世币)地址4c
	 * 
	 * @author 
	 * @date 2018年11月21日 上午9:18:34
	 */
	public static String dash() {
		return parent("4c");
	}
 
	/**
	 * 生成比特黄金的地址26
	 * 
	 * @author 
	 * @date 2018年11月21日 下午4:31:30
	 */
	public static String btg() {
		return parent("26");
	}
 
	/**
	 * 萌奈币（Mona）地址生成32
	 * 
	 * @author 
	 * @date 2018年11月21日 下午4:45:22
	 */
	public static String mona() {
		return parent("32");
	}
 
	/**
	 * 量子链(qtum)地址生成3A
	 * 
	 * @author 
	 * @date 2018年11月21日 下午4:47:10
	 */
	public static String qtum() {
		return parent("3A");
	}
 
	/**
	 * 蜗牛币（rdd）地址生成3d
	 * 
	 * @author 
	 * @date 2018年11月21日 下午4:53:26
	 */
	public static String rdd() {
		return parent("3d");
	}
	static final String HEXES = "0123456789abcdef";
	public static String getHex(byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}
}