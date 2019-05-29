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
import org.csc.account.api.enums.TransTypeEnum;
import org.csc.account.gens.TxTest.PTSTCommand;
import org.csc.account.gens.TxTest.PTSTModule;
import org.csc.account.gens.TxTest.ReqCreateContract;
import org.csc.account.gens.TxTest.RespContract;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Tx;

import java.math.BigInteger;

@NActorProvider
@Slf4j
@Data
public class TransactionCreateContract extends SessionModules<ReqCreateContract> {
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
		return new String[] { PTSTCommand.TCC.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateContract pb, final CompleteHandler handler) {
		RespContract.Builder oRespContract = RespContract.newBuilder();

		if (!isDev) {
			oRespContract.setRetcode(-1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespContract.build()));
			return;
		}

		try {
			Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
			Tx.TransactionBody.Builder oMultiTransactionBody = Tx.TransactionBody.newBuilder();

			Tx.TransactionInput.Builder oMultiTransactionInput4 = Tx.TransactionInput.newBuilder();
			oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getAddress())));
			oMultiTransactionInput4.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(BigInteger.ZERO)));
			int nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(pb.getAddress())));
			oMultiTransactionInput4.setNonce(nonce);
			oMultiTransactionBody.setInput(oMultiTransactionInput4);
			oMultiTransactionBody.setType(TransTypeEnum.TYPE_CreateContract.value());
			oMultiTransactionBody.setData(ByteString.copyFrom(encApi.hexDec(pb.getData())));
			oMultiTransactionBody.setExtData(ByteString.copyFromUtf8("aabbccdd:12331 \n xxxx"));
			oMultiTransaction.clearHash();
			oMultiTransactionBody.clearSignatures();
			oMultiTransactionBody.setTimestamp(System.currentTimeMillis());
			// 签名
			oMultiTransactionBody.setSignatures(ByteString.copyFrom(encApi.ecSign(pb.getPrivKey(), oMultiTransactionBody.build().toByteArray())));
			oMultiTransaction.setBody(oMultiTransactionBody);

			oRespContract.setContractHash(encApi.hexEnc(
					transactionHelper.getContractAddressByTransaction(oMultiTransaction.build()).toByteArray()));

			String txHash = encApi.hexEnc(transactionHelper.CreateMultiTransaction(oMultiTransaction).getKey().toByteArray());
			oRespContract.setTxHash(txHash);
		} catch (Exception e) {
			e.printStackTrace();
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespContract.build()));
	}
}
