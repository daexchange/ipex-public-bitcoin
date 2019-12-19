package ai.turbochain.ipex.wallet.utils;

import java.util.List;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.UTXO;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.ScriptBuilder;

import com.google.common.collect.Lists;
import com.osp.blockchain.btc.model.BlockchainUnspentOutput;
import com.osp.blockchain.btc.model.Utxos;

import ai.turbochain.ipex.wallet.entity.BtcUtxo;

public class CastObjectUtil {

	public static List<BtcUtxo> UtxosCastBtcUtxo(Utxos utxos) {
		List<BtcUtxo> btcUtxoList = Lists.newArrayList();
		for (BlockchainUnspentOutput blockchainUnspentOutput : utxos.getUnspent_outputs()) {
			BtcUtxo btcUtxo = new BtcUtxo();
			btcUtxo.setN(blockchainUnspentOutput.getTx_output_n());
			btcUtxo.setConfirmations(blockchainUnspentOutput.getConfirmations());
			btcUtxo.setScript(blockchainUnspentOutput.getScript());
			if (null != blockchainUnspentOutput.getXpub()) {
				btcUtxo.setTransactionHash(blockchainUnspentOutput.getTx_hash_big_endian());
			} else {
				btcUtxo.setTransactionHash(blockchainUnspentOutput.getTx_hash());
			}
			btcUtxo.setTransactionIndex(blockchainUnspentOutput.getTx_index());
			btcUtxo.setValue(blockchainUnspentOutput.getValue());
			if (null != blockchainUnspentOutput.getXpub() && null != blockchainUnspentOutput.getXpub().getPath()) {
				String pathStr = blockchainUnspentOutput.getXpub().getPath();
				String path = pathStr.substring(1, pathStr.length());
				btcUtxo.setPath(path);
			}
			btcUtxoList.add(btcUtxo);
		}
		return btcUtxoList;
	}

	public static List<UTXO> UtxosCastBtcUTXO(Utxos utxos, String btcAddress) {
		List<UTXO> btcUtxoList = Lists.newArrayList();
		for (BlockchainUnspentOutput blockchainUnspentOutput : utxos.getUnspent_outputs()) {
			UTXO btcUtxo = new UTXO(Sha256Hash.wrap(blockchainUnspentOutput.getTx_hash()),
					Long.valueOf(blockchainUnspentOutput.getTx_output_n()),
					Coin.valueOf(Long.valueOf(blockchainUnspentOutput.getValue())), 0, false,
					ScriptBuilder.createOutputScript(LegacyAddress.fromBase58(MainNetParams.get(), btcAddress)));
			btcUtxoList.add(btcUtxo);
		}
		return btcUtxoList;
	}

}
