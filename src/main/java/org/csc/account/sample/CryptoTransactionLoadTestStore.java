package org.csc.account.sample;

import com.google.protobuf.ByteString;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.csc.account.api.IAccountHelper;
import org.csc.account.api.ITransactionHelper;
import org.csc.account.api.enums.TransTypeEnum;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.bcapi.KeyPairs;
import org.csc.evmapi.gens.Act.Account;
import org.csc.evmapi.gens.Tx;
import org.csc.evmapi.gens.Tx.CryptoTokenData;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "CryptoTransactionLoadTest_Store")
@Slf4j
@Data
public class CryptoTransactionLoadTestStore implements ActorService {
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "Account_Helper", scope = "global")
	IAccountHelper accountHelper;
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	ITransactionHelper transactionHelper;

	ConcurrentHashMap<String, txstatus> acctRelTx = new ConcurrentHashMap<>();
	ConcurrentHashMap<String, KeyPairs> allAccounts = new ConcurrentHashMap<>();
	AtomicInteger sendNewTx = new AtomicInteger(0);
	AtomicInteger sendTransferTx = new AtomicInteger(0);
	AtomicInteger executeTransferTx = new AtomicInteger(0);

	int needAccount = 1000;
	int cryptoTokenEachAccount = 100;

	List<KeyPairs> accounts = new ArrayList<>();
	private AtomicInteger used_idx = new AtomicInteger(0);

	private class txstatus {
		private String txHash;

		public txstatus(String txHash) {
			this.txHash = txHash;
		}
	}

	public synchronized String getTwo() {
		return encApi.hexEnc(getOne().toByteArray());
	}

	public synchronized ByteString getOne() {
		try {
			if (accounts.size() < needAccount) {
				KeyPairs oKeyPairsA = encApi.genKeys();

				accounts.add(oKeyPairsA);
				allAccounts.put(oKeyPairsA.getAddress(), oKeyPairsA);
				Tx.Transaction.Builder tx = makeCreateCryptoToken(oKeyPairsA, ByteString.copyFromUtf8("SYB" + accounts.size()),
						cryptoTokenEachAccount);
				return transactionHelper.CreateMultiTransaction(tx).getKey();
			} else {
				if (used_idx.intValue() >= (accounts.size() - 1)) {
					used_idx.set(0);
				}
				KeyPairs oKeyPairsA = accounts.get(used_idx.getAndIncrement());
				txstatus ts = acctRelTx.get(oKeyPairsA.getAddress());
				if (ts == null) {
					KeyPairs oKeyPairsB = encApi.genKeys();
					// get last token
					Account.Builder oAccount = accountHelper
							.getAccount(ByteString.copyFrom(encApi.hexDec(oKeyPairsA.getAddress())));
					if (oAccount.getValue().getCryptosCount() > 0) {
						String tokenHash = encApi
								.hexEnc(oAccount.getValue().getCryptos(0).getTokens(0).getHash().toByteArray());
						ByteString tokenSymbol = oAccount.getValue().getCryptos(0).getSymbol();
						// make new transaction
						Tx.Transaction.Builder tx = makeCryptoTokenTransfer(oKeyPairsA, oKeyPairsB, tokenSymbol,
								tokenHash);
						ByteString txHash = transactionHelper.CreateMultiTransaction(tx).getKey();
						txstatus tStatus = new txstatus(encApi.hexEnc(txHash.toByteArray()));
						acctRelTx.put(oKeyPairsA.getAddress(), tStatus);
						allAccounts.put(oKeyPairsB.getAddress(), oKeyPairsB);

						return txHash;
					}
				} else {
					Tx.Transaction tx = transactionHelper.GetTransaction(ByteString.copyFrom(encApi.hexDec(ts.txHash)));
					if (!tx.getStatus().isEmpty()) {
						// finish
						// if (tx.getStatus().equals("D")) {
						KeyPairs txFrom = allAccounts
								.get(encApi.hexEnc(tx.getBody().getInput().getAddress().toByteArray()));
						KeyPairs txTo = allAccounts
								.get(encApi.hexEnc(tx.getBody().getOutputs(0).getAddress().toByteArray()));
						String tokenHash = encApi.hexEnc(tx.getBody().getInput().getCryptoToken(0).toByteArray());
						ByteString tokenSymbol = tx.getBody().getInput().getSymbol();

						Tx.Transaction.Builder newTx = makeCryptoTokenTransfer(txTo, txFrom, tokenSymbol, tokenHash);
						ByteString txHash = transactionHelper.CreateMultiTransaction(newTx).getKey();
						txstatus tStatus = new txstatus(encApi.hexEnc(txHash.toByteArray()));
						acctRelTx.put(oKeyPairsA.getAddress(), tStatus);

						return txHash;
						// } else {
						// Account.Builder oAccount = accountHelper
						// .GetAccount(ByteString.copyFrom(encApi.hexDec(oKeyPairsA.getAddress())));
						// KeyPairs oKeyPairsB = encApi.genKeys();
						//
						// String tokenHash =
						// encApi.hexEnc(oAccount.getValue().getCryptos(0)
						// .getTokens(oAccount.getValue().getCryptos(0).getTokensCount()
						// - 1).getHash()
						// .toByteArray());
						// String tokenSymbol =
						// oAccount.getValue().getCryptos(0).getSymbol();
						// // make new transaction
						// MultiTransaction.Builder newTx =
						// makeCryptoTokenTransfer(oKeyPairsA, oKeyPairsB,
						// tokenSymbol, tokenHash);
						// String txHash =
						// transactionHelper.CreateMultiTransaction(newTx).getKey();
						// txstatus tStatus = new txstatus(txHash,
						// System.currentTimeMillis());
						// acctRelTx.put(oKeyPairsA.getAddress(), tStatus);
						// allAccounts.put(oKeyPairsB.getAddress(), oKeyPairsB);
						//
						// return txHash;
						// }
					} else {
						// unfinish, do nothing
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	private Tx.Transaction.Builder makeCryptoTokenTransfer(KeyPairs oKeyPairsA, KeyPairs oKeyPairsB, ByteString tokenSymbol,
			String tokenHash) {
		Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
		Tx.TransactionBody.Builder oMultiTransactionBody = Tx.TransactionBody.newBuilder();

//		if (!isDev) {
//			oRespCreateTransactionTest.setRetcode(-1);
//			handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateTransactionTest.build()));
//			return;
//		}

		try {
			Tx.TransactionInput.Builder oMultiTransactionInput4 = Tx.TransactionInput.newBuilder();
			oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyPairsA.getAddress())));
			oMultiTransactionInput4.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(BigInteger.ZERO)));

			int nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(oKeyPairsA.getAddress())));
			oMultiTransactionInput4.setNonce(nonce);
			oMultiTransactionInput4.addCryptoToken(ByteString.copyFrom(encApi.hexDec(tokenHash)));
			oMultiTransactionInput4.setSymbol(tokenSymbol);
			oMultiTransactionBody.setInput(oMultiTransactionInput4);

			Tx.TransactionOutput.Builder oMultiTransactionOutput1 = Tx.TransactionOutput.newBuilder();
			oMultiTransactionOutput1.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyPairsB.getAddress())));
			oMultiTransactionOutput1.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(BigInteger.ZERO)));
			oMultiTransactionOutput1.addCryptoToken(ByteString.copyFrom(encApi.hexDec(tokenHash)));
			//oMultiTransactionOutput1.setSymbol(tokenSymbol);
			oMultiTransactionBody.addOutputs(oMultiTransactionOutput1);

			oMultiTransactionBody.setType(TransTypeEnum.TYPE_CryptoTokenTransaction.value());
			oMultiTransaction.clearHash();
			oMultiTransactionBody.clearSignatures();
			oMultiTransactionBody.setTimestamp(System.currentTimeMillis());
			// 签名
			oMultiTransactionBody.setSignatures(ByteString.copyFrom(encApi.ecSign(oKeyPairsA.getPrikey(), oMultiTransactionBody.build().toByteArray())));

			oMultiTransaction.setBody(oMultiTransactionBody);
			return oMultiTransaction;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Tx.Transaction.Builder makeCreateCryptoToken(KeyPairs from, ByteString symbol, int total) {
		Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
		Tx.TransactionBody.Builder oMultiTransactionBody = Tx.TransactionBody.newBuilder();

		try {
			Tx.TransactionInput.Builder oMultiTransactionInput4 = Tx.TransactionInput.newBuilder();
			oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(from.getAddress())));
			oMultiTransactionInput4.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(BigInteger.ZERO)));
			int nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(from.getAddress())));
			oMultiTransactionInput4.setNonce(nonce);

			oMultiTransactionBody.setInput(oMultiTransactionInput4);

			CryptoTokenData.Builder oCryptoTokenData = CryptoTokenData.newBuilder();
			oCryptoTokenData.setSymbol(symbol);
			oCryptoTokenData.setTotal(total);
			for (int i = 0; i < total; i++) {
				oCryptoTokenData.addName(symbol.toStringUtf8() + i);
				oCryptoTokenData.addCode(symbol.toStringUtf8() + i);
			}

			oMultiTransactionBody.setType(TransTypeEnum.TYPE_CreateCryptoToken.value());
			oMultiTransactionBody.setData(ByteString.copyFrom(oCryptoTokenData.build().toByteArray()));
			oMultiTransaction.clearHash();
			oMultiTransactionBody.clearSignatures();
			oMultiTransactionBody.setTimestamp(System.currentTimeMillis());
			// 签名

			oMultiTransactionBody.setSignatures(ByteString.copyFrom(encApi.ecSign(from.getPrikey(), oMultiTransactionBody.build().toByteArray())));

			oMultiTransaction.setBody(oMultiTransactionBody);

			return oMultiTransaction;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
