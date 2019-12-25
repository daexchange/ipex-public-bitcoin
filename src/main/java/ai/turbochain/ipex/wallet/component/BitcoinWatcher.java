package ai.turbochain.ipex.wallet.component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import ai.turbochain.ipex.wallet.config.Constant;
import ai.turbochain.ipex.wallet.entity.BalanceData;
import ai.turbochain.ipex.wallet.entity.Deposit;
import ai.turbochain.ipex.wallet.service.AccountService;
import ai.turbochain.ipex.wallet.service.UTXOTransactionService;
import ai.turbochain.ipex.wallet.utils.HttpRequest;

@Component
public class BitcoinWatcher extends Watcher {
	@Autowired
	private AccountService accountService;
	@Autowired
	private UTXOTransactionService utxoTransactionService;
	// 比特币单位转换聪
	private BigDecimal bitcoin = new BigDecimal("100000000");
	//private BlockExplorer blockExplorer = new BlockExplorer();

	@Override
	public List<Deposit> replayBlock(Long startBlockNumber, Long endBlockNumber) {
		List<Deposit> deposits = new ArrayList<Deposit>();
		try {
			for (Long blockHeight = startBlockNumber; blockHeight <= endBlockNumber; blockHeight++) {
				System.out.println("区块"+blockHeight);
				String blockHeightData = HttpRequest
						.sendGetData(Constant.ACT_BLOCKNO_HEIGHT + blockHeight + Constant.FORMAT_PARAM, "");
				JSONObject jsonObject = JSONObject.parseObject(blockHeightData);
				JSONArray blocksArray = jsonObject.getJSONArray("blocks");
				String blockHash = blocksArray.getJSONObject(0).getString("hash");
				Long height = blocksArray.getJSONObject(0).getLong("height");
				JSONArray txList = blocksArray.getJSONObject(0).getJSONArray("tx");
				for (int i = 0; i < txList.size(); i++) {
					JSONObject txObj = txList.getJSONObject(i);
					String txHash = txObj.getString("hash");
					JSONArray inputsArray = txObj.getJSONArray("inputs");
					Boolean flag = false;
					for (int j = 0; j < inputsArray.size(); j++) {
						JSONObject input = inputsArray.getJSONObject(j);
						JSONObject prevout = input.getJSONObject("prev_out");
						if (prevout == null) {
							continue;
						}
						String address = prevout.getString("addr");
						
						if (StringUtils.isNotBlank(address) && accountService.isAddressExist(address.toLowerCase())) {
							flag = true;
							//BigDecimal balance = new BigDecimal(blockExplorer.getAddress(address).getFinalBalance());
							List<BalanceData> balanceList = utxoTransactionService.getBalance(address);
							BigDecimal balances = new BigDecimal("0");
							for (BalanceData balance : balanceList) {
								balances = balances.add(new BigDecimal(balance.getFinalBalance()));
							}
							accountService.updateBTCBalance(address, balances);
							break;
						}
					}
					if (flag == true) {
						continue;
					}
					JSONArray outArray = txObj.getJSONArray("out");
					for (int j = 0; j < outArray.size(); j++) {
						JSONObject out = outArray.getJSONObject(j);
						String address = out.getString("addr");
						if (StringUtils.isNotBlank(address) && accountService.isAddressExist(address.toLowerCase())) {
							BigDecimal amount = out.getBigDecimal("value").divide(bitcoin).setScale(8,
									BigDecimal.ROUND_DOWN);
							Deposit deposit = new Deposit();
							deposit.setTxid(txHash);
							deposit.setBlockHeight(height);
							deposit.setBlockHash(blockHash);
							deposit.setAddress(address);
							deposit.setAmount(amount);
							deposit.setTime(txObj.getDate("time"));
							deposits.add(deposit);
							try {
								List<BalanceData> balanceList = utxoTransactionService.getBalance(address);
								BigDecimal balances = new BigDecimal("0");
								for (BalanceData balance : balanceList) {
									balances = balances.add(new BigDecimal(balance.getFinalBalance()));
								}
								accountService.updateBTCBalance(address, balances);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return deposits;
	}

	@Override
	public Long getNetworkBlockHeight() {
		try {
			// 获取最新块的块高
			String result = HttpRequest.sendGetData(Constant.ACT_BLOCKNO_LATEST, "");
			JSONObject resultObj = JSONObject.parseObject(result);
			return resultObj.getLong("height");
		} catch (Exception e) {
			e.printStackTrace();
			return 0L;
		}
	}
}
