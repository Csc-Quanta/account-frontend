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
import org.csc.account.api.enums.TransTypeEnum;
import org.csc.account.gens.Tximpl.PTXTCommand;
import org.csc.account.gens.Tximpl.ReqCreateMultiTransaction;
import org.csc.account.gens.Tximpl.RespCreateTransaction;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Tx;

@NActorProvider
@Slf4j
@Data
public class SaveMultiTransactionImpl extends SessionModules<ReqCreateMultiTransaction> {
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	ITransactionHelper transactionHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PTXTCommand.MTX.name() };
	}

	@Override
	public String getModule() {
		return "API";//return PTXTModule.TXT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateMultiTransaction pb, final CompleteHandler handler) {
		RespCreateTransaction.Builder oRespCreateTx = RespCreateTransaction.newBuilder();

		try {
			Tx.Transaction.Builder oTransaction = transactionHelper.parse(pb.getTransaction());
			if (oTransaction.getBody().getType() == TransTypeEnum.TYPE_CreateContract.value()) {
				oRespCreateTx.setContractHash(encApi
						.hexEnc(transactionHelper.getContractAddressByTransaction(oTransaction.build()).toByteArray()));
			}
			oRespCreateTx.setTxHash(encApi.hexEnc(transactionHelper.CreateMultiTransaction(oTransaction).getKey().toByteArray()));
			oRespCreateTx.setRetCode(1);
		} catch (Exception e) {
			log.error("error on create tx::" , e);

			oRespCreateTx.clear();
			oRespCreateTx.setRetCode(-1);
			oRespCreateTx.setRetMsg(e == null || e.getMessage() == null ? "" : e.getMessage());
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateTx.build()));
	}
}
