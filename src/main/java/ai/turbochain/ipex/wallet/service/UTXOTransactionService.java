package ai.turbochain.ipex.wallet.service;

import java.io.File;
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

import ai.turbochain.ipex.wallet.entity.BalanceData;
import info.blockchain.api.pushtx.PushTx;

@Service
public class UTXOTransactionService {

	private static Logger logger = LoggerFactory.getLogger(UTXOTransactionService.class);

	BtcInterface btcInterface = BtcClient.getInterface();

	private NetworkParameters params;

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
			System.out.println(wallet);
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
