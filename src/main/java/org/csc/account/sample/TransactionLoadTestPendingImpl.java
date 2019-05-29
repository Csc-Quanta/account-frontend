/*
package org.csc.account.sample;

import org.csc.account.api.IAccountHelper;
import org.csc.account.api.IBlockHelper;
import org.csc.account.api.IChainHelper;
import org.csc.account.api.ITransactionHelper;
import org.csc.account.gens.TxTest.PTSTModule;
import org.csc.account.gens.TxTest.ReqCommonTest;
import org.csc.account.gens.TxTest.RespCreateTransactionTest;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Tx;
import org.csc.evmapi.gens.Tx.MultiTransaction;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class TransactionLoadTestPendingImpl extends SessionModules<ReqCommonTest> {
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
	@ActorRequire(name = "TransactionLoadTest_Store", scope = "global")
	TransactionLoadTestStore transactionLoadTestStore;
	boolean isDev = props().get("org.brewchain.man.dev", "true").equals("true");
	
	@Override
	public String[] getCmds() {
		return new String[] { "PEN" };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCommonTest pb, final CompleteHandler handler) {
		RespCreateTransactionTest.Builder oRespCreateTransactionTest = RespCreateTransactionTest.newBuilder();
		
		if (!isDev) {
			oRespCreateTransactionTest.setRetcode(-1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateTransactionTest.build()));
			return;
		}
		
		String txHash = "";
		try {
			Tx.Transaction.Builder tx = transactionLoadTestStore.getOne();
			if (tx != null) {
//				txHash = transactionHelper.CreateMultiTransaction(tx).getHexKey();
				oRespCreateTransactionTest.setRetmsg("success");
				oRespCreateTransactionTest.setTxhash(tx.getTxHash());
				oRespCreateTransactionTest
						.setFrom(encApi.hexEnc(tx.getTxBody().getInputs(0).getAddress().toByteArray()));
				oRespCreateTransactionTest
						.setTo(encApi.hexEnc(tx.getTxBody().getOutputs(0).getAddress().toByteArray()));

				if (txHash.length() != 64) {
					log.error("wrong txHash::" + txHash);
				}
			} else {
				log.error("cannot find test case::");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("test fail::", e);
			oRespCreateTransactionTest.clear();
			oRespCreateTransactionTest.setRetmsg("error");
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateTransactionTest.build()));
		return;
	}
}
*/
