package org.csc.account.sample;

import com.google.protobuf.ByteString;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.csc.account.api.IAccountHelper;
import org.csc.account.api.IBlockHelper;
import org.csc.account.api.IChainHelper;
import org.csc.account.api.ITransactionHelper;
import org.csc.account.gens.TxTest.PTSTCommand;
import org.csc.account.gens.TxTest.PTSTModule;
import org.csc.account.gens.TxTest.ReqThreadTransaction;
import org.csc.account.gens.TxTest.RespCommonTest;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.bcapi.KeyPairs;
import org.csc.evmapi.gens.Tx;

import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;

@NActorProvider
@Slf4j
@Data
public class ThreadTransactionSampleImpl extends SessionModules<ReqThreadTransaction> {
	@ActorRequire(name = "Block_Helper", scope = "global")
	IBlockHelper blockHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "BlockChain_Helper", scope = "global")
	IChainHelper blockChainHelper;
	@ActorRequire(name = "Account_Helper", scope = "global")
	IAccountHelper accountHelper;
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	ITransactionHelper transactionHelper;
	boolean isDev = props().get("org.brewchain.man.dev", "true").equals("true");
	@Override
	public String[] getCmds() {
		return new String[] { PTSTCommand.MTT.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqThreadTransaction pb, final CompleteHandler handler) {
		RespCommonTest.Builder oRespCommonTest = RespCommonTest.newBuilder();
		if (!isDev) {
			oRespCommonTest.setRetcode(-1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespCommonTest.build()));
			return;
		}
		
		for (int i = 0; i < pb.getThreads(); i++) {
			ThreadTransaction oThreadTransaction = new ThreadTransaction(transactionHelper, encApi, pb.getDuration(),
					pb.getAddress(i), pb.getPrivKey(i), "");
			oThreadTransaction.start();
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCommonTest.build()));
		return;

	}

	public class ThreadTransaction extends Thread {
		private final ITransactionHelper th;
		private final EncAPI encApi;
		private final int duration;
		private final String address;
		private final String privKey;
		private final String pubKey;

		public ThreadTransaction(ITransactionHelper transactionHelper, EncAPI enc, int duration, String address,
				String privKey, String pubKey) {
			this.th = transactionHelper;
			this.encApi = enc;
			this.duration = duration;
			this.address = address;
			this.privKey = privKey;
			this.pubKey = pubKey;
		}

		@Override
		public void run() {
			final Timer timer = new Timer();
			TimerTask oTimerTask = new TimerTask() {
				@Override
				public void run() {
					try {
						KeyPairs oTo = encApi.genKeys();

						Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
						Tx.TransactionBody.Builder oMultiTransactionBody = Tx.TransactionBody.newBuilder();

						Tx.TransactionInput.Builder oMultiTransactionInput4 = Tx.TransactionInput.newBuilder();
						oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(address)));
						oMultiTransactionInput4
								.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(new BigInteger("2"))));
						int nonce = 0;
						nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(address)));
						oMultiTransactionInput4.setNonce(nonce);
						oMultiTransactionBody.setInput(oMultiTransactionInput4);

						Tx.TransactionOutput.Builder oMultiTransactionOutput1 = Tx.TransactionOutput.newBuilder();
						oMultiTransactionOutput1.setAddress(ByteString.copyFrom(encApi.hexDec(oTo.getAddress())));
						oMultiTransactionOutput1
								.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(new BigInteger("2"))));
						oMultiTransactionBody.addOutputs(oMultiTransactionOutput1);

						oMultiTransaction.clearHash();
						oMultiTransactionBody.clearSignatures();
						oMultiTransactionBody.setTimestamp(System.currentTimeMillis());
						// 签名

						oMultiTransactionBody.setSignatures(ByteString.copyFrom(encApi.ecSign(privKey, oMultiTransactionBody.build().toByteArray())));

						oMultiTransaction.setBody(oMultiTransactionBody);
						ByteString txHash = th.CreateMultiTransaction(oMultiTransaction).getKey();
//						log.debug("Thread Transaction Test ==> txHash::" + txHash + " form::" + address + " nonce::"
//								+ nonce + " to::" + oTo.getAddress());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			timer.schedule(oTimerTask, 0, this.duration);
			oTimerTask = null;
		}
	}
}
