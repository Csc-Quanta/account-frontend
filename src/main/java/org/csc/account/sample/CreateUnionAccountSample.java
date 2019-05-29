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
import org.csc.account.bean.HashPair;
import org.csc.account.gens.TxTest.PTSTCommand;
import org.csc.account.gens.TxTest.PTSTModule;
import org.csc.account.gens.TxTest.ReqCreateUnionAccount;
import org.csc.account.gens.TxTest.RespCreateUnionAccount;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Tx;
import org.csc.evmapi.gens.Tx.UnionAccountData;

import java.math.BigInteger;

@NActorProvider
@Slf4j
@Data
public class CreateUnionAccountSample extends SessionModules<ReqCreateUnionAccount> {
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
		return new String[] { PTSTCommand.TCA.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateUnionAccount pb, final CompleteHandler handler) {
		RespCreateUnionAccount.Builder oRespCreateUnionAccount = RespCreateUnionAccount.newBuilder();
		Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
		Tx.TransactionBody.Builder oMultiTransactionBody = Tx.TransactionBody.newBuilder();

		if (!isDev) {
			oRespCreateUnionAccount.setRetCode(-1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateUnionAccount.build()));
			return;
		}
		
		try {
			Tx.TransactionInput.Builder oMultiTransactionInput4 = Tx.TransactionInput.newBuilder();
			oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getAddress())));

			oMultiTransactionInput4.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(BigInteger.ZERO)));
			int nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(pb.getAddress())));
			oMultiTransactionInput4.setNonce(nonce);
			oMultiTransactionBody.setInput(oMultiTransactionInput4);

			oMultiTransactionBody.setType(TransTypeEnum.TYPE_CreateUnionAccount.value());
			oMultiTransactionBody.setTimestamp(System.currentTimeMillis());

			UnionAccountData.Builder oUnionAccountData = UnionAccountData.newBuilder();
			oUnionAccountData.setAcceptLimit(pb.getAcceptLimit());
			oUnionAccountData
					.setAcceptMax(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(new BigInteger(pb.getAcceptMax()))));
			oUnionAccountData.setMaxTrans(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(new BigInteger(pb.getMax()))));

			for (int i = 0; i < pb.getRelAddressCount(); i++) {
				oUnionAccountData.addAddress(ByteString.copyFrom(encApi.hexDec(pb.getRelAddress(i))));
			}

			oMultiTransactionBody.setData(oUnionAccountData.build().toByteString());
			oMultiTransaction.clearHash();
			oMultiTransactionBody.clearSignatures();

			oMultiTransactionBody.setSignatures(ByteString.copyFrom(encApi.ecSign(pb.getPrivKey(), oMultiTransactionBody.build().toByteArray())));

			oMultiTransaction.setBody(oMultiTransactionBody);
			HashPair hp = transactionHelper.CreateMultiTransaction(oMultiTransaction);

			oRespCreateUnionAccount.setRetCode(1);
			oRespCreateUnionAccount.setRetMsg(encApi.hexEnc(hp.getKey().toByteArray()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateUnionAccount.build()));
		return;
	}
}
