package org.csc.account.sample;

import org.csc.account.gens.TxTest.PTSTCommand;
import org.csc.account.gens.TxTest.PTSTModule;
import org.csc.account.gens.TxTest.ReqCommonTest;
import org.csc.account.gens.TxTest.RespCommonTest;

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
public class TransactionLoadTestPerResultImpl extends SessionModules<ReqCommonTest> {
	@ActorRequire(name = "TransactionLoadTest_Store", scope = "global")
	TransactionLoadTestStore transactionLoadTestStore;
	boolean isDev = props().get("org.brewchain.man.dev", "true").equals("true");

	@Override
	public String[] getCmds() {
		return new String[] { PTSTCommand.LTR.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCommonTest pb, final CompleteHandler handler) {
		RespCommonTest.Builder oRespCommonTest = RespCommonTest.newBuilder();
		if (!isDev) {
			oRespCommonTest.setRetcode(-1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespCommonTest.build()));
			return;
		}
		oRespCommonTest.setRetmsg("total::" + transactionLoadTestStore.remain());
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCommonTest.build()));
		return;
	}
}
