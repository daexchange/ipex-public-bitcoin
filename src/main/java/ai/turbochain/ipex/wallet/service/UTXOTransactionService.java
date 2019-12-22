package ai.turbochain.ipex.wallet.service;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.UTXO;
import org.bitcoinj.core.UTXOProvider;
import org.bitcoinj.core.UTXOProviderException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.osp.blockchain.btc.client.BtcClient;
import com.osp.blockchain.btc.client.BtcInterface;
import com.osp.blockchain.btc.model.BlockchainBalance;
import com.osp.blockchain.btc.model.BlockchainUnspentOutput;
import com.osp.blockchain.btc.model.Utxos;
import com.osp.blockchain.btc.model.UtxosAddress;
import com.osp.blockchain.btc.util.BtcException;
import com.osp.blockchain.btc.util.ConversionUtils;
import com.osp.blockchain.http.HttpUtil;

import ai.turbochain.ipex.wallet.entity.Account;
import ai.turbochain.ipex.wallet.entity.BalanceData;
import ai.turbochain.ipex.wallet.util.MessageResult;
import info.blockchain.api.pushtx.PushTx;

@Service
public class UTXOTransactionService {

	private static Logger logger = LoggerFactory.getLogger(UTXOTransactionService.class);

	BtcInterface btcInterface = BtcClient.getInterface();

	private NetworkParameters params;

	@Autowired
	private AccountService accountService;

	public UTXOTransactionService() {
		params = MainNetParams.get();
	}

	/**
	 * 用UTXO进行交易,completeTx()后，将tx转成hex的String，调用接口广播
	 */
	public String startTxWithUTXO(final List<UTXO> utxoList, String filePath, String sendAddr, String receiveAddr,
			Coin value) {
		try {
			Wallet wallet = Wallet.loadFromFile(new File(filePath));
			// 设置UTXOProvider
			wallet.setUTXOProvider(new UTXOProvider() {
				@Override
				public List<UTXO> getOpenTransactionOutputs(List<ECKey> keys) throws UTXOProviderException {
					return utxoList;
				}

				@Override
				public int getChainHeadHeight() throws UTXOProviderException {
					return Integer.MAX_VALUE;
				}

				@Override
				public NetworkParameters getParams() {
					return params;
				}
			});

			Transaction tx = new Transaction(params);
			// 收款方的output，转账金额和收款地址
			tx.addOutput(value, ScriptBuilder.createOutputScript(LegacyAddress.fromBase58(params, receiveAddr)));
			SendRequest sendRequest = SendRequest.forTx(tx);
			// 找零地址设为付款地址
			sendRequest.changeAddress = LegacyAddress.fromBase58(params, sendAddr);
			// 设置手续费
			sendRequest.feePerKb = Coin.valueOf(2000);
			// 签名等操作，完成tx
			wallet.completeTx(sendRequest);
			// 对外广播的hex
			String hexString = new String(Hex.encode(tx.unsafeBitcoinSerialize()));
			logger.info("hexString:" + hexString);
			logger.info("coins sent. transaction hash: " + tx.getHashAsString());
			logger.info("tx: " + tx.toString());
			// 广播交易
			PushTx.pushTx(hexString);
			return tx.getHashAsString();
		} catch (InsufficientMoneyException e) {
			logger.error("Not enough coins in your wallet. Missing " + e.missing.getValue()
					+ " satoshis are missing (including fees)");
			e.printStackTrace();
			return "";
		} catch (Wallet.DustySendRequested dustySendRequested) {
			dustySendRequested.printStackTrace();
			return "";
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public MessageResult transferFromWallet(String receiveAddr, Coin value, BigDecimal minAmount, BigDecimal fee) {
		logger.info("transferFromWallet 方法");
		BigDecimal amount = new BigDecimal(value.getValue());
		List<Account> accounts = accountService.findByBalance(minAmount);
		if (accounts == null || accounts.size() == 0) {
			MessageResult messageResult = new MessageResult(500, "没有满足条件的转账账户(大于0.001)!");
			logger.info(messageResult.toString());
			return messageResult;
		}
		String txnHashString = "";
		BigDecimal transferredAmount = BigDecimal.ZERO;
		for (Account account : accounts) {
			BigDecimal realAmount = account.getBalance().subtract(fee);
			if (realAmount.compareTo(amount.subtract(transferredAmount)) > 0) {
				realAmount = amount.subtract(transferredAmount);
			}
			try {
				String txid = this.getBtcUtxo(account.getWalletFile(), account.getAddress(), receiveAddr,
						Coin.valueOf(realAmount.longValue()));
				if (StringUtils.isNotEmpty(txid)) {
					logger.info("transfer address={},amount={},txid={}", account.getAddress(), realAmount, txid);
					transferredAmount = transferredAmount.add(realAmount);
					txnHashString = txnHashString + txid + ",";
					try {
						List<BalanceData> balanceList = this.getBalance(account.getAddress());
						BigDecimal balances = new BigDecimal("0");
						for (BalanceData balance : balanceList) {
							balances = balances.add(new BigDecimal(balance.getFinalBalance()));
						}
						accountService.updateBTCBalance(account.getAddress(), balances);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (transferredAmount.compareTo(amount) >= 0) {
				break;
			}
		}
		MessageResult result = new MessageResult(0, "success");
		result.setData(txnHashString.substring(0, txnHashString.length() - 1));
		return result;
	}

	public String getBtcUtxo(String filePath, String sendAddr, String receiveAddr, Coin value) throws Exception {
		Utxos utxos = null;
		List<UTXO> btcUtxoList = Lists.newArrayList();
		if (StringUtils.isNotBlank(sendAddr)) {
			try {
				UtxosAddress utxosAddress = btcInterface.getUtxosByAddress(sendAddr);
				utxos = ConversionUtils.ConverUtxoAddress(utxosAddress);
			} catch (Exception e) {
				throw new Exception(sendAddr + "不存在UTXO" + e.getMessage());
			}
		}
		for (BlockchainUnspentOutput blockchainUnspentOutput : utxos.getUnspent_outputs()) {
			UTXO btcUtxo = new UTXO(Sha256Hash.wrap(blockchainUnspentOutput.getTx_hash_big_endian()),
					Long.valueOf(blockchainUnspentOutput.getTx_output_n()),
					Coin.valueOf(Long.valueOf(blockchainUnspentOutput.getValue())), 0, false,
					ScriptBuilder.createOutputScript(LegacyAddress.fromBase58(MainNetParams.get(), sendAddr)));
			btcUtxoList.add(btcUtxo);
		}
		// 开始转账
		return startTxWithUTXO(btcUtxoList, filePath, sendAddr, receiveAddr, value);
	}

	/**
	 * 获得手续费
	 * 
	 * @return
	 */
	public JSONObject getRecommendBtcFees() throws Exception {
		String str = HttpUtil.sendGet("https://bitcoinfees.earn.com/api/v1/fees/recommended");
		if (StringUtils.isNotBlank(str)) {
			JSONObject jsonObject = JSONObject.parseObject(str);
			return jsonObject;
		} else {
			return null;
		}
	}

	public List<BalanceData> getBalance(String address) throws BtcException {
		List<BalanceData> balanceList = Lists.newArrayList();
		Map<String, BlockchainBalance> balanceMap = btcInterface.getBalance(address);
		for (String key : balanceMap.keySet()) {
			BalanceData balanceData = new BalanceData();
			balanceData.setAddress(key);
			balanceData.setFinalBalance(balanceMap.get(key).getFinal_balance());
			balanceData.setTxCount(balanceMap.get(key).getN_tx());
			balanceData.setTotalReceived(balanceMap.get(key).getTotal_received());
			balanceList.add(balanceData);
		}
		return balanceList;
	}

}
