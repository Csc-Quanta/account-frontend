package org.csc.account.sample;

import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.csc.account.api.IAccountHelper;
import org.csc.account.api.IBlockHelper;
import org.csc.account.api.IBlockStore;
import org.csc.account.api.IChainHelper;
import org.csc.account.api.ITransactionHelper;
import org.csc.account.api.enums.TransTypeEnum;
import org.csc.account.gens.TxTest.PTSTCommand;
import org.csc.account.gens.TxTest.PTSTModule;
import org.csc.account.gens.TxTest.ReqSideChainReg;
import org.csc.account.gens.TxTest.ReqSideChainSync;
import org.csc.account.gens.TxTest.RespSideChainSync;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Tx;
import org.csc.evmapi.gens.Tx.SideChainData;
import org.csc.evmapi.gens.Tx.SideChainTxData;

import com.google.protobuf.ByteString;

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
public class SideChainSyncSampleImpl extends SessionModules<ReqSideChainSync> {

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
	@ActorRequire(name = "BlockStore_Helper", scope = "global")
	IBlockStore blockStore;
	boolean isDev = props().get("org.brewchain.man.dev", "true").equals("true");

	@Override
	public String[] getCmds() {
		return new String[] { PTSTCommand.SST.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqSideChainSync pb, final CompleteHandler handler) {
		RespSideChainSync.Builder oRespSideChainSync = RespSideChainSync.newBuilder();
		Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
		Tx.TransactionBody.Builder oMultiTransactionBody = Tx.TransactionBody.newBuilder();

		try {
			Tx.TransactionInput.Builder oMultiTransactionInput4 = Tx.TransactionInput.newBuilder();
			oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getChainAddress())));
			oMultiTransactionInput4.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(BigInteger.ZERO)));
			int nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(pb.getChainAddress())));
			oMultiTransactionInput4.setNonce(nonce);
			oMultiTransactionBody.setInput(oMultiTransactionInput4);
			oMultiTransactionBody.setType(TransTypeEnum.TYPE_sideChainSync.value());
			oMultiTransactionBody.setTimestamp(System.currentTimeMillis());

			SideChainData.Builder oSideChainData = SideChainData.newBuilder();
			oSideChainData.setHash(ByteString.copyFrom(encApi.hexDec(pb.getBlockHash())));
			oSideChainData.setNumber(pb.getBlockNumber());
			for (int i = 0; i < pb.getTxHashCount(); i++) {
				SideChainTxData.Builder oSideChainTxData = SideChainTxData.newBuilder();
				oSideChainTxData.setI(i);
				oSideChainTxData.setTxHash(ByteString.copyFrom(encApi.hexDec(pb.getTxHash(i))));
				oSideChainTxData.setStatus(ByteString.copyFromUtf8(pb.getStatus(i)));
				oSideChainTxData.setResult(ByteString.copyFromUtf8(pb.getResult(i)));
				oSideChainData.addSideChainTx(oSideChainTxData);
			}
			oMultiTransactionBody.setData(oSideChainData.build().toByteString());
			
			oMultiTransactionBody.setExtData(ByteString.copyFrom(encApi.hexDec(pb.getAddress())));
		

			oMultiTransactionBody.setSignatures(
					ByteString.copyFrom(encApi.ecSign(pb.getPrivKey(), oMultiTransactionBody.build().toByteArray())));
			oMultiTransaction.setBody(oMultiTransactionBody);

			String txHash = encApi.hexEnc(transactionHelper.CreateMultiTransaction(oMultiTransaction).getKey().toByteArray());
			
			oRespSideChainSync.setRetCode(1);
			oRespSideChainSync.setTxHash(txHash);
		} catch (Exception e) {
			oRespSideChainSync.clear();
			oRespSideChainSync.setRetCode(-1);
			oRespSideChainSync.setRetMsg(e.getMessage());
			log.error("===>Side Chain Sync Error. ", e);
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespSideChainSync.build()));
	}
}
