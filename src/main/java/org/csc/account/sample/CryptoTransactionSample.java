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
import org.csc.account.gens.TxTest.*;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.bcapi.UnitUtil;
import org.csc.evmapi.gens.Tx;

@NActorProvider
@Slf4j
@Data
public class CryptoTransactionSample extends SessionModules<ReqCreateTransactionTest> {
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

	@Override
	public String[] getCmds() {
		return new String[] { PTSTCommand.TRO.name() };
	}

	boolean isDev = props().get("org.brewchain.man.dev", "true").equals("true");

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateTransactionTest pb, final CompleteHandler handler) {
		RespCreateTransactionTest.Builder oRespCreateTransactionTest = RespCreateTransactionTest.newBuilder();
		Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
		Tx.TransactionBody.Builder oMultiTransactionBody = Tx.TransactionBody.newBuilder();

		if (!isDev) {
			oRespCreateTransactionTest.setRetcode(-1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateTransactionTest.build()));
			return;
		}

		try {
			for (ReqTransactionAccount input : pb.getInputList()) {
				Tx.TransactionInput.Builder oMultiTransactionInput4 = Tx.TransactionInput.newBuilder();
				oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(input.getAddress())));
				oMultiTransactionInput4
						.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(UnitUtil.toWei(input.getAmount()))));

				int nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(input.getAddress())));
				oMultiTransactionInput4.setNonce(nonce);
				oMultiTransactionInput4.addCryptoToken(ByteString.copyFrom(encApi.hexDec(input.getErc721Token())));
				oMultiTransactionInput4.setSymbol(ByteString.copyFromUtf8(input.getErc721Symbol()));
				oMultiTransactionBody.setInput(oMultiTransactionInput4);

				oRespCreateTransactionTest
						.addTrace("add input address::" + input.getAddress() + " nonce::" + nonce + " balance::"
								+ accountHelper.getBalance(ByteString.copyFrom(encApi.hexDec(input.getAddress()))));
			}

			for (ReqTransactionAccount output : pb.getOutputList()) {
				Tx.TransactionOutput.Builder oMultiTransactionOutput1 = Tx.TransactionOutput.newBuilder();
				oMultiTransactionOutput1.setAddress(ByteString.copyFrom(encApi.hexDec(output.getAddress())));
				oMultiTransactionOutput1
						.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(UnitUtil.toWei(output.getAmount()))));
				oMultiTransactionOutput1.addCryptoToken(ByteString.copyFrom(encApi.hexDec(output.getErc721Token())));
				//oMultiTransactionOutput1.setSymbol(output.getErc721Symbol());
				oMultiTransactionBody.addOutputs(oMultiTransactionOutput1);

				// oRespCreateTransactionTest.addTrace("add output address::" +
				// output.getAddress() + " nonce::"
				// +
				// accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(output.getAddress())))
				// + " balance::"
				// +
				// accountHelper.getBalance(ByteString.copyFrom(encApi.hexDec(output.getAddress()))));
			}

			oMultiTransactionBody.setType(TransTypeEnum.TYPE_CryptoTokenTransaction.value());
			oMultiTransactionBody.setData(ByteString.copyFrom(encApi.hexDec(pb.getData())));
			oMultiTransaction.clearHash();
			oMultiTransactionBody.clearSignatures();
			oMultiTransactionBody.setTimestamp(System.currentTimeMillis());
			// 签名
			for (ReqTransactionSignature input : pb.getSignatureList()) {
				oMultiTransactionBody.setSignatures(ByteString.copyFrom(encApi.ecSign(input.getPrivKey(), oMultiTransactionBody.build().toByteArray())));
			}
			oMultiTransaction.setBody(oMultiTransactionBody);

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
