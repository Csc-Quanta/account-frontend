package org.csc.account.sample;

import java.math.BigInteger;

import org.csc.account.api.IAccountHelper;
import org.csc.account.api.IBlockHelper;
import org.csc.account.api.IBlockStore;
import org.csc.account.api.IChainHelper;
import org.csc.account.api.ITransactionHelper;
import org.csc.account.api.enums.TransTypeEnum;
import org.csc.account.bean.HashPair;
import org.csc.account.gens.TxTest.PTSTCommand;
import org.csc.account.gens.TxTest.PTSTModule;
import org.csc.account.gens.TxTest.ReqSideChainReg;
import org.csc.account.gens.TxTest.ReqUnionAccountTransaction;
import org.csc.account.gens.TxTest.RespSideChainReg;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Tx;
import org.csc.evmapi.gens.Tx.UnionAccountData;

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
public class SideChainRegSampleImpl extends SessionModules<ReqSideChainReg> {

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
		return new String[] { PTSTCommand.SCR.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, final ReqSideChainReg pb, final CompleteHandler handler) {
		RespSideChainReg.Builder oRespSideChainReg = RespSideChainReg.newBuilder();
		Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
		Tx.TransactionBody.Builder oMultiTransactionBody = Tx.TransactionBody.newBuilder();

		try {
			Tx.TransactionInput.Builder oMultiTransactionInput4 = Tx.TransactionInput.newBuilder();
			oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getAddress())));

			oMultiTransactionInput4.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(BigInteger.ZERO)));
			int nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(pb.getAddress())));
			oMultiTransactionInput4.setNonce(nonce);
			oMultiTransactionBody.setInput(oMultiTransactionInput4);

			oMultiTransactionBody.setType(TransTypeEnum.TYPE_sideChainReg.value());
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

			oRespSideChainReg.setRetCode(1);
			oRespSideChainReg.setTxHash(encApi.hexEnc(hp.getKey().toByteArray()));
		} catch (Exception e) {
			oRespSideChainReg.clear();
			oRespSideChainReg.setRetCode(-1);
			oRespSideChainReg.setRetMsg(e.getMessage());
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespSideChainReg.build()));
		return;
	}

}
