package org.csc.account.action.transaction;

import com.google.protobuf.ByteString;
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
import org.csc.account.gens.Tximpl.ReqGetTxByHash;
import org.csc.account.gens.Tximpl.RespGetTxByHash;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Tx;

@NActorProvider
@Slf4j
@Data
public class GetTxByTxHashImpl extends SessionModules<ReqGetTxByHash> {
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	ITransactionHelper transactionHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PTXTCommand.GTX.name() };
	}

	@Override
	public String getModule() {
		return "API";//return PTXTModule.TXT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetTxByHash pb, final CompleteHandler handler) {
		RespGetTxByHash.Builder oRespGetTxByHash = RespGetTxByHash.newBuilder();

		try {
			Tx.Transaction oTransaction = transactionHelper.GetTransaction(ByteString.copyFrom(encApi.hexDec(pb.getHash())));
			
			if (oTransaction !=null) {
				MultiTransactionImpl.Builder oMultiTransactionImpl = transactionHelper.parseToImpl(oTransaction);
				oRespGetTxByHash.setTransaction(oMultiTransactionImpl);
				oRespGetTxByHash.setRetCode(1);
			} else {
				oRespGetTxByHash.setRetCode(-2);
			}
			
		} catch (Exception e) {
			oRespGetTxByHash.setRetCode(-1);
//			e.printStackTrace();
			log.error("GetTxByTxHashImpl error",e);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetTxByHash.build()));
	}
}
