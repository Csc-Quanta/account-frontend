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
import org.csc.account.gens.TxTest.*;
import org.csc.account.gens.Tximpl;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.bcapi.UnitUtil;
import org.csc.evmapi.gens.Tx;

import java.util.ArrayList;
import java.util.List;

@NActorProvider
@Slf4j
@Data
public class TransactionSampleImpl extends SessionModules<ReqCreateTransactionTest> {
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
	boolean isDev = props().get("org.brewchain.man.dev", "true").equals("true");
	
	@Override
	public String[] getCmds() {
		return new String[] { PTSTCommand.STT.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateTransactionTest pb, final CompleteHandler handler) {
		RespCreateTransactionTest.Builder oRespCreateTransactionTest = RespCreateTransactionTest.newBuilder();

		if (!isDev) {
			oRespCreateTransactionTest.setRetcode(-1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateTransactionTest.build()));
			return;
		}
		
		Tx.Transaction.Builder oMultiTransaction = Tx.Transaction.newBuilder();
		Tx.TransactionBody.Builder transactionBody = Tx.TransactionBody.newBuilder();

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
				oMultiTransactionInput4.setToken(ByteString.copyFromUtf8(input.getErc20Symbol()));

				transactionBody.setInput(oMultiTransactionInput4);

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
				//oMultiTransactionOutput1.setSymbol(ByteString.copyFromUtf8(output.getErc721Symbol()));
				transactionBody.addOutputs(oMultiTransactionOutput1);

				// oRespCreateTransactionTest.addTrace("add output address::" +
				// output.getAddress() + " nonce::"
				// +
				// accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(output.getAddress())))
				// + " balance::"
				// +
				// accountHelper.getBalance(ByteString.copyFrom(encApi.hexDec(output.getAddress()))));
			}

			transactionBody.setData(ByteString.copyFrom(encApi.hexDec(pb.getData())));
			oMultiTransaction.clearHash();
			transactionBody.clearSignatures();
			transactionBody.setTimestamp(System.currentTimeMillis());
			// 签名
			ReqTransactionAccount input = pb.getInputList().get(0);
			byte[] sign = encApi.ecSign(input.getPrikey(), transactionBody.build().toByteArray());
			if(log.isDebugEnabled()){
				log.debug("===>priKey: {}, pubKey: {}", input.getPrikey(), input.getPutkey());
				log.debug("===>Body: {}, Sign: {}", encApi.hexEnc(transactionBody.build().toByteArray()), encApi.hexEnc(sign));
			}
			transactionBody.setSignatures(ByteString.copyFrom(sign));
			oMultiTransaction.setBody(transactionBody);

			ByteString txHash = transactionHelper.CreateMultiTransaction(oMultiTransaction).getKey();
			oRespCreateTransactionTest.setTxhash(encApi.hexEnc(txHash.toByteArray()));
		} catch (Exception e) {
			e.printStackTrace();
			oRespCreateTransactionTest.setRetcode(-1);
			oRespCreateTransactionTest.setRetmsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateTransactionTest.build()));
	}
}
