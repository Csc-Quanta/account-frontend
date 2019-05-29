package org.csc.account.sample;

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
import org.csc.account.gens.TxTest.ReqCryptoTokenLoadTest;
import org.csc.account.gens.TxTest.RespCryptoTokenLoadTest;
import org.csc.bcapi.EncAPI;

@NActorProvider
@Slf4j
@Data
public class CryptoTransactionLoadTestSample extends SessionModules<ReqCryptoTokenLoadTest> {
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
	@ActorRequire(name = "CryptoTransactionLoadTest_Store", scope = "global")
	CryptoTransactionLoadTestStore cryptoTransactionLoadTestStore;
	
	@Override
	public String[] getCmds() {
		return new String[] { PTSTCommand.LCO.name() };
	}
	boolean isDev = props().get("org.brewchain.man.dev", "true").equals("true");

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCryptoTokenLoadTest pb, final CompleteHandler handler) {
		RespCryptoTokenLoadTest.Builder oRespCryptoTokenLoadTest = RespCryptoTokenLoadTest.newBuilder();
		if (!isDev) {
			oRespCryptoTokenLoadTest.setSuccessTransfer(-1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespCryptoTokenLoadTest.build()));
			return;
		}
		try {
			cryptoTransactionLoadTestStore.setNeedAccount(pb.getFromCount());
			oRespCryptoTokenLoadTest.setCreate(cryptoTransactionLoadTestStore.getSendNewTx().get());
			oRespCryptoTokenLoadTest.setTransfer(cryptoTransactionLoadTestStore.getSendTransferTx().get());
			oRespCryptoTokenLoadTest.setSuccessTransfer(cryptoTransactionLoadTestStore.getExecuteTransferTx().get());
			
			oRespCryptoTokenLoadTest.setTxHash(cryptoTransactionLoadTestStore.getTwo());
		} catch (Exception e) {
			e.printStackTrace();
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCryptoTokenLoadTest.build()));
		return;
	}
}