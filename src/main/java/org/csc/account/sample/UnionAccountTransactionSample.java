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
import org.apache.commons.lang3.StringUtils;
import org.csc.account.api.*;
import org.csc.account.api.enums.TransTypeEnum;
import org.csc.account.gens.TxTest.PTSTCommand;
import org.csc.account.gens.TxTest.PTSTModule;
import org.csc.account.gens.TxTest.ReqUnionAccountTransaction;
import org.csc.account.gens.TxTest.RespCreateUnionAccount;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Tx;

import java.math.BigInteger;

@NActorProvider
@Slf4j
@Data
public class UnionAccountTransactionSample extends SessionModules<ReqUnionAccountTransaction> {
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
		return new String[] { PTSTCommand.TUA.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqUnionAccountTransaction pb, final CompleteHandler handler) {
		RespCreateUnionAccount.Builder oRespCreateUnionAccount = RespCreateUnionAccount.newBuilder();

		// if (!isDev) {
		// oRespCreateUnionAccount.setRetCode(-1);
		// handler.onFinished(PacketHelper.toPBReturn(pack,
		// oRespCreateUnionAccount.build()));
		// return;
		// }

		Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
		Tx.TransactionBody.Builder oMultiTransactionBody = Tx.TransactionBody.newBuilder();

		try {
			Tx.TransactionInput.Builder oMultiTransactionInput4 = Tx.TransactionInput.newBuilder();
			oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getUnionAccountAddress())));
			oMultiTransactionInput4
					.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(new BigInteger(pb.getAmount()))));
			int nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(pb.getUnionAccountAddress())));
			oMultiTransactionInput4.setNonce(nonce);
			oMultiTransactionBody.setInput(oMultiTransactionInput4);
			oMultiTransactionBody.setType(TransTypeEnum.TYPE_UnionAccountTransaction.value());
			oMultiTransactionBody.setTimestamp(System.currentTimeMillis());
			if (StringUtils.isNotBlank(pb.getRelTxHash())) {
				oMultiTransactionBody.setData(ByteString.copyFrom(encApi.hexDec(pb.getRelTxHash())));
			}
			oMultiTransactionBody.setExtData(ByteString.copyFrom(encApi.hexDec(pb.getRelAddress())));
			Tx.TransactionOutput.Builder oMultiTransactionOutput1 = Tx.TransactionOutput.newBuilder();
			oMultiTransactionOutput1.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getToAddress())));
			oMultiTransactionOutput1
					.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(new BigInteger(pb.getAmount()))));
			oMultiTransactionBody.addOutputs(oMultiTransactionOutput1);


			oMultiTransactionBody.setSignatures(ByteString.copyFrom(encApi.ecSign(pb.getRelKey(), oMultiTransactionBody.build().toByteArray())));
			oMultiTransaction.setBody(oMultiTransactionBody);

			String txHash = encApi.hexEnc(transactionHelper.CreateMultiTransaction(oMultiTransaction).getKey().toByteArray());
			oRespCreateUnionAccount.setRetMsg(txHash);
			oRespCreateUnionAccount.setRetCode(1);
		} catch (Exception e) {
			e.printStackTrace();
			oRespCreateUnionAccount.setRetCode(-1);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateUnionAccount.build()));
		return;
	}
}
