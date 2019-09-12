package ai.turbochain.ipex.wallet.controller;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.spark.blockchain.rpcclient.BitcoinRPCClient;
import com.spark.blockchain.rpcclient.BitcoinUtil;

import ai.turbochain.ipex.wallet.config.Constant;
import ai.turbochain.ipex.wallet.service.AccountService;
import ai.turbochain.ipex.wallet.util.MessageResult;
import ai.turbochain.ipex.wallet.utils.HttpRequest;

@RestController
@RequestMapping("/rpc")
public class WalletController {
	private Logger logger = LoggerFactory.getLogger(WalletController.class);

	@Autowired
	private BitcoinRPCClient rpcClient;

	@Autowired
	private AccountService accountService;

	/**
	 * 获取链最新区块高度
	 * 
	 * @return
	 */
	@GetMapping("height")
	public MessageResult getHeight() {
		try {
			String resultStr = HttpRequest.sendGetData(Constant.ACT_BLOCKNO_LATEST, "");
			JSONObject resultObj = JSONObject.parseObject(resultStr);
			Long height = resultObj.getLong("height");
			MessageResult result = new MessageResult(0, "success");
			result.setData(height);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "查询失败,error:" + e.getMessage());
		}
	}

	/**
	 * 创建钱包
	 * 
	 * @param password
	 * @param apiCode
	 * @param priv
	 * @param label
	 * @param email
	 * @return
	 */
	@GetMapping("address/{account}")
	public MessageResult createWallet(@PathVariable String account, String password, String priv, String email) {
		logger.info("create new wallet:password={},priv={},label={},email={}", password, priv, account, email);
		try {
			String url = Constant.ACT_CREATE_WALLET + Constant.PWD_PARAM + password + Constant.APICODE_PARAM
					+ Constant.LABLE_PARAM + account;
			if (priv != null && priv.isEmpty() == false) {
				url += Constant.PRIV_PARAM + priv;
			}
			if (email != null && email.isEmpty() == false) {
				url += Constant.EMAIL_PARAM + email;
			}
			String createWalletData = HttpRequest.sendGetData(url, "");
			JSONObject jsonResult = JSONObject.parseObject(createWalletData);
			logger.info(createWalletData);
			accountService.saveOne(account, jsonResult.getString("address"), password, jsonResult.getString("guid"),
					priv, email);
			MessageResult result = new MessageResult(0, "success");
			result.setData(jsonResult.getString("address"));
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "查询失败， error: " + e.getMessage());
		}
	}

	@GetMapping({ "transfer", "withdraw" })
	public MessageResult withdraw(String address, BigDecimal amount, BigDecimal fee) {
		logger.info("withdraw:address={},amount={},fee={}", address, amount, fee);
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			return MessageResult.error(500, "额度须大于0");
		}
		try {
			String txid = BitcoinUtil.sendTransaction(rpcClient, address, amount, fee);
			MessageResult result = new MessageResult(0, "success");
			result.setData(txid);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "error:" + e.getMessage());
		}
	}

	@GetMapping("balance")
	public MessageResult balance() {
		try {
			BigDecimal balance = new BigDecimal(rpcClient.getBalance());

			MessageResult result = new MessageResult(0, "success");
			result.setData(balance);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "error:" + e.getMessage());
		}
	}

	@GetMapping("balance/{address}")
	public MessageResult balance(@PathVariable String address) {
		try {
			String account = rpcClient.getAccount(address);
			System.out.println("account=" + account + ",address=" + address);
			BigDecimal balance = new BigDecimal(rpcClient.getBalance(account));
			MessageResult result = new MessageResult(0, "success");
			result.setData(balance);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "error:" + e.getMessage());
		}
	}
}
