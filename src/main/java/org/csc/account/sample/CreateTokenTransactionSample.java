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
import org.csc.account.gens.TxTest.*;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Tx;

import java.math.BigInteger;

@NActorProvider
@Slf4j
@Data
public class CreateTokenTransactionSample extends SessionModules<ReqCreateTransactionTest> {
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
	@Override
	public String[] getCmds() {
		return new String[] { PTSTCommand.TCT.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}
	boolean isDev = props().get("org.brewchain.man.dev", "true").equals("true");
	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateTransactionTest pb, final CompleteHandler handler) {
		RespCreateTransactionTest.Builder oRespCreateTransactionTest = RespCreateTransactionTest.newBuilder();

		if (!isDev) {
			oRespCreateTransactionTest.setRetcode(-1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateTransactionTest.build()));
			return;
		}
		
		Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
		Tx.TransactionBody.Builder oMultiTransactionBody = Tx.TransactionBody.newBuilder();

		try {

			for (ReqTransactionAccount input : pb.getInputList()) {
				Tx.TransactionInput.Builder oMultiTransactionInput4 = Tx.TransactionInput.newBuilder();
				oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(input.getAddress())));
				oMultiTransactionInput4.setAmount(ByteString
						.copyFrom(ByteUtil.bigIntegerToBytes(new BigInteger(input.getAmount()))));
				int nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(input.getAddress())));
				oMultiTransactionInput4.setNonce(nonce);
				oMultiTransactionInput4.setToken(ByteString.copyFromUtf8(input.getErc20Symbol()));

				oMultiTransactionBody.setInput(oMultiTransactionInput4);

				oRespCreateTransactionTest
						.addTrace("add input address::" + input.getAddress() + " nonce::" + nonce + " balance::"
								+ accountHelper.getBalance(ByteString.copyFrom(encApi.hexDec(input.getAddress()))));
			}
 
			oMultiTransactionBody.setType(TransTypeEnum.TYPE_CreateToken.value());
//			oMultiTransactionBody.setData(ByteString.copyFrom(encApi.hexDec(pb.getData())));
			oMultiTransaction.clearHash();
			oMultiTransactionBody.clearSignatures();
			oMultiTransactionBody.setTimestamp(System.currentTimeMillis());
			// 签名
			for (ReqTransactionSignature input : pb.getSignatureList()) {
				oMultiTransactionBody.setSignatures(ByteString.copyFrom(encApi.ecSign(input.getPrivKey(), oMultiTransactionBody.build().toByteArray())));
			}
			oMultiTransaction.setBody(oMultiTransactionBody);

//			JsonFormat jsonFormat = new JsonFormat();
//			String jsonView = jsonFormat.printToString(oMultiTransaction.build());
//			System.out.print(jsonView);
			ByteString txHash = transactionHelper.CreateMultiTransaction(oMultiTransaction).getKey();
			oRespCreateTransactionTest.setTxhash(encApi.hexEnc(txHash.toByteArray()));
		} catch (Exception e) {
			e.printStackTrace();
			oRespCreateTransactionTest.setRetcode(-1);
			oRespCreateTransactionTest.setRetmsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateTransactionTest.build()));
		return;
	}
}
