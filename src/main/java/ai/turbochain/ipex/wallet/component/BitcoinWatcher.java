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
import ai.turbochain.ipex.wallet.entity.Deposit;
import ai.turbochain.ipex.wallet.service.AccountService;
import ai.turbochain.ipex.wallet.utils.HttpRequest;

@Component
public class BitcoinWatcher extends Watcher {
	@Autowired
	private AccountService accountService;
	// 比特币单位转换聪
	private BigDecimal bitcoin = new BigDecimal("100000000");

	@Override
	public List<Deposit> replayBlock(Long startBlockNumber, Long endBlockNumber) {
		List<Deposit> deposits = new ArrayList<Deposit>();
		try {
			for (Long blockHeight = startBlockNumber; blockHeight <= endBlockNumber; blockHeight++) {
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
					JSONArray outArray = txObj.getJSONArray("out");
					for (int j = 0; j < outArray.size(); j++) {
						JSONObject out = outArray.getJSONObject(j);
						String address = out.getString("addr");
						if (StringUtils.isNotBlank(address) && accountService.isAddressExist(address)) {
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
