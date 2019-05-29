package org.csc.account.action.transaction;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.csc.account.api.ITransactionHelper;
import org.csc.account.gens.Tximpl.MultiTransactionImpl;
import org.csc.account.gens.Tximpl.PTXTCommand;
import org.csc.account.gens.Tximpl.ReqSyncTx;
import org.csc.account.gens.Tximpl.RespSyncTx;
import org.csc.evmapi.gens.Tx;

import java.math.BigInteger;

@NActorProvider
@Slf4j
@Data
public class SyncTransactionImpl extends SessionModules<ReqSyncTx> {
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	ITransactionHelper transactionHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PTXTCommand.AYC.name() };
	}

	@Override
	public String getModule() {
		return "API";//return PTXTModule.TXT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqSyncTx pb, final CompleteHandler handler) {
		RespSyncTx.Builder oRespSyncTx = RespSyncTx.newBuilder();
		oRespSyncTx.setRetCode(1);
		for (MultiTransactionImpl oTransaction : pb.getTxsList()) {
			try {
				Tx.Transaction.Builder oMultiTransaction = transactionHelper.parse(oTransaction);
				transactionHelper.syncTransaction(oMultiTransaction,new BigInteger("0"));
			} catch (Exception e) {
				oRespSyncTx.addErrList(oTransaction.getTxHash());
				oRespSyncTx.setRetCode(-1);
			}
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespSyncTx.build()));
	}
}
