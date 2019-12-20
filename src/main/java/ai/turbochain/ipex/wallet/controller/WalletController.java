package ai.turbochain.ipex.wallet.controller;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.bitcoinj.core.Coin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;

import ai.turbochain.ipex.wallet.config.Constant;
import ai.turbochain.ipex.wallet.entity.Account;
import ai.turbochain.ipex.wallet.entity.BalanceData;
import ai.turbochain.ipex.wallet.entity.BtcBean;
import ai.turbochain.ipex.wallet.service.AccountService;
import ai.turbochain.ipex.wallet.service.UTXOTransactionService;
import ai.turbochain.ipex.wallet.util.MessageResult;
import ai.turbochain.ipex.wallet.utils.BTCAccountGenerator;
import ai.turbochain.ipex.wallet.utils.HttpRequest;

@RestController
@RequestMapping("/rpc")
public class WalletController {
	private Logger logger = LoggerFactory.getLogger(WalletController.class);

	@Autowired
	private AccountService accountService;
	@Autowired
	private UTXOTransactionService utxoTransactionService;
	@Autowired
	private BTCAccountGenerator btcAccountGenerator;

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
	public MessageResult createWallet(@PathVariable String account,
			@RequestParam(required = false, defaultValue = "") String password) {
		logger.info("create new wallet:account={},password={}", account, password);
		try {
			BtcBean btcBean = btcAccountGenerator.createBtcAccount();
			accountService.saveOne(account, btcBean.getFile(), btcBean.getBtcAddress(), "");
			MessageResult result = new MessageResult(0, "success");
			result.setData(btcBean.getBtcAddress());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "查询失败， error: " + e.getMessage());
		}
	}

	@GetMapping({ "transfer", "withdraw" })
	public MessageResult withdraw(String username, String address, BigDecimal amount) {
		logger.info("withdraw:uid={},receiveAddr={},amount={}", username, address, amount);
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			return MessageResult.error(500, "提现额度须大于0");
		}
		Account account = accountService.findByName(username);
		if (account == null) {
			return MessageResult.error(500, "请传入正确的用户名" + username);
		}
		BigInteger cong = amount.multiply(new BigDecimal("10").pow(8)).toBigInteger();
		try {
			String txid = utxoTransactionService.getBtcUtxo(account.getWalletFile(), account.getAddress(), address,
					Coin.valueOf(cong.longValue()));
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
			BigDecimal balance = accountService.findBalanceSum();
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
			List<BalanceData> balanceList = utxoTransactionService.getBalance(address);
			BigDecimal balances = new BigDecimal("0");
			for (BalanceData balance : balanceList) {
				balances = balances.add(new BigDecimal(balance.getFinalBalance()));
			}
			MessageResult result = new MessageResult(0, "success");
			result.setData(balances);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "error:" + e.getMessage());
		}
	}
}
