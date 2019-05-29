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
import org.csc.account.api.*;
import org.csc.account.api.enums.TransTypeEnum;
import org.csc.account.gens.TxTest.PTSTCommand;
import org.csc.account.gens.TxTest.PTSTModule;
import org.csc.account.gens.TxTest.ReqVoteTransaction;
import org.csc.account.gens.TxTest.RespVoteTransaction;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Tx;
import org.csc.evmapi.gens.Tx.SanctionData;

import java.math.BigInteger;

@NActorProvider
@Slf4j
@Data
public class VoteTransactionSample extends SessionModules<ReqVoteTransaction> {
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
	// @ActorRequire(name = "BlockStore_UnStable", scope = "global")
	// BlockUnStableStore unStableStore;
	@ActorRequire(name = "BlockStore_Helper", scope = "global")
	IBlockStore blockStore;
	boolean isDev = props().get("org.brewchain.man.dev", "true").equals("true");

	@Override
	public String[] getCmds() {
		return new String[] { PTSTCommand.VTS.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqVoteTransaction pb, final CompleteHandler handler) {
		RespVoteTransaction.Builder oRespVoteTransaction = RespVoteTransaction.newBuilder();

		if (!isDev) {
			oRespVoteTransaction.setRetCode(-1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespVoteTransaction.build()));
			return;
		}

		Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
		Tx.TransactionBody.Builder oMultiTransactionBody = Tx.TransactionBody.newBuilder();

		try {
			Tx.TransactionInput.Builder oMultiTransactionInput4 = Tx.TransactionInput.newBuilder();
			oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getAddress())));

			oMultiTransactionInput4
					.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(new BigInteger(pb.getAmount()))));
			int nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(pb.getAddress())));
			oMultiTransactionInput4.setNonce(nonce);
			oMultiTransactionBody.setInput(oMultiTransactionInput4);
			oMultiTransactionBody.setType(TransTypeEnum.TYPE_Sanction.value());
			oMultiTransactionBody.setTimestamp(System.currentTimeMillis());

			SanctionData.Builder oSanctionData = SanctionData.newBuilder();
			oSanctionData.setContent(ByteString.copyFromUtf8(pb.getVoteContent()));
			oSanctionData.setEndBlockNumber(pb.getEndHeight());
			oSanctionData.setResult(ByteString.copyFromUtf8(pb.getResult()));
			oMultiTransactionBody.setData(oSanctionData.build().toByteString());

			Tx.TransactionOutput.Builder oMultiTransactionOutput1 = Tx.TransactionOutput.newBuilder();
			oMultiTransactionOutput1.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getVoteAddress())));
			oMultiTransactionOutput1
					.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(new BigInteger(pb.getAmount()))));
			oMultiTransactionBody.addOutputs(oMultiTransactionOutput1);

			oMultiTransactionBody.setSignatures(ByteString.copyFrom(encApi.ecSign(pb.getPrivKey(), oMultiTransactionBody.build().toByteArray())));
			oMultiTransaction.setBody(oMultiTransactionBody);

			ByteString txHash = transactionHelper.CreateMultiTransaction(oMultiTransaction).getKey();
			oRespVoteTransaction.setRetMsg(encApi.hexEnc(txHash.toByteArray()));
			oRespVoteTransaction.setRetCode(1);
		} catch (Exception e) {
			e.printStackTrace();
			oRespVoteTransaction.setRetCode(-1);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespVoteTransaction.build()));
		return;
	}
}
