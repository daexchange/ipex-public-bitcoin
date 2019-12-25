package ai.turbochain.ipex.wallet.utils.timer;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ai.turbochain.ipex.wallet.entity.Account;
import ai.turbochain.ipex.wallet.entity.BalanceData;
import ai.turbochain.ipex.wallet.service.AccountService;
import ai.turbochain.ipex.wallet.service.UTXOTransactionService;

@Component
public class CoinCollectJob {
	private Logger logger = LoggerFactory.getLogger(CoinCollectJob.class);
	@Autowired
	private AccountService accountService;
	@Autowired
	private UTXOTransactionService utxoTransactionService;

	/**
	 * 同步比特币地址余额
	 */
	@Scheduled(cron = "0 0 */2 * * ?")
	public void updateBTCAccount() {
		logger.info("更新比特币地址余额调度器开始执行");
		List<Account> accountLists = accountService.findAll();
		for (Account account : accountLists) {
			try {
				List<BalanceData> balanceList = utxoTransactionService.getBalance(account.getAddress());
				BigDecimal balances = new BigDecimal("0");
				for (BalanceData balance : balanceList) {
					balances = balances.add(new BigDecimal(balance.getFinalBalance()));
				}
				accountService.updateBTCBalance(account.getAddress(), balances);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
