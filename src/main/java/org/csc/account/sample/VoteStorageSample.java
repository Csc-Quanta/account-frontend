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
import org.csc.account.gens.TxTest.*;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Act.Account;
import org.csc.evmapi.gens.Act.SanctionStorage;
import org.csc.evmapi.gens.Block.BlockEntity;
import org.csc.evmapi.gens.Tx;
import org.csc.evmapi.gens.Tx.SanctionData;

@NActorProvider
@Slf4j
@Data
public class VoteStorageSample extends SessionModules<ReqVoteStorage> {
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
		return new String[] { PTSTCommand.VTT.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqVoteStorage pb, final CompleteHandler handler) {
		RespVoteStorage.Builder oRespVoteStorage = RespVoteStorage.newBuilder();
		
		if (!isDev) {
			oRespVoteStorage.setRetCode(-1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespVoteStorage.build()));
			return;
		}
		
		try {
			Account.Builder oAccount = accountHelper.getAccount(ByteString.copyFrom(encApi.hexDec(pb.getAddress())));

			byte[] v = accountHelper.getStorage(oAccount, encApi.hexDec(pb.getKey()));
			if (v != null) {
				SanctionStorage oSanctionStorage = SanctionStorage.parseFrom(v);
				for (int i = 0; i < oSanctionStorage.getAddressCount(); i++) {
					RespVoteStorageItem.Builder oItem = RespVoteStorageItem.newBuilder();
					ByteString txHash = oSanctionStorage.getTxHash(i);
					Tx.Transaction oMultiTransaction = transactionHelper.GetTransaction(txHash);

					oItem.setAddress(encApi.hexEnc(oSanctionStorage.getAddress(i).toByteArray()));

					BlockEntity oBlockEntity = blockHelper.getBlockByTransaction(encApi.hexDec(txHash.toString()));
					if (oBlockEntity != null) {
						oItem.setBlockHash(encApi.hexEnc(oBlockEntity.getHeader().getHash().toByteArray()));
						oItem.setBlockHeight(oBlockEntity.getHeader().getNumber());
					}
					oItem.setTxHash(encApi.hexEnc(txHash.toByteArray()));
					oItem.setTimestamp(oMultiTransaction.getBody().getTimestamp());
					oItem.setCost(ByteUtil
							.bytesToBigInteger(oMultiTransaction.getBody().getInput().getAmount().toByteArray())
							.toString());

					SanctionData oSanctionData = SanctionData.parseFrom(oMultiTransaction.getBody().getData());
					oItem.setVoteContent(oSanctionData.getContent().toStringUtf8());
					oItem.setVoteResult(oSanctionData.getResult().toStringUtf8());
					oItem.setEndBlockHeight(oSanctionData.getEndBlockNumber());
					oRespVoteStorage.setVoteTxHash(encApi.hexEnc(oSanctionStorage.getVoteTxHash().toByteArray()));
					oRespVoteStorage.addItems(oItem);
				}
				oRespVoteStorage.setRetMsg(oSanctionStorage.toString());

			} else {
				oRespVoteStorage.setRetMsg("");
			}
		} catch (Exception e) {
			e.printStackTrace();
			oRespVoteStorage.setRetCode(-1);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespVoteStorage.build()));
		return;
	}
}
