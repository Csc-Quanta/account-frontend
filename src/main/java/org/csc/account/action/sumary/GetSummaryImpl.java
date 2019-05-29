package org.csc.account.action.sumary;

import org.csc.account.api.IAccountHelper;
import org.csc.account.api.IBlockStore;
import org.csc.account.api.IChainHelper;
import org.csc.account.api.IConfirmTxMap;
import org.csc.account.api.ITransactionHelper;
import org.csc.account.api.IWaitForBlockMap;
import org.csc.account.api.IWaitForSendMap;
import org.csc.account.gens.Sumary.PSYSCommand;
import org.csc.account.gens.Sumary.PSYSModule;
import org.csc.account.gens.Sumary.ReqGetSummary;
import org.csc.account.gens.Sumary.RespGetSummary;
import org.csc.bcapi.EncAPI;

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
public class GetSummaryImpl extends SessionModules<ReqGetSummary> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	IAccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "BlockChain_Helper", scope = "global")
	IChainHelper blockChainHelper;
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	ITransactionHelper transactionHelper;
	@ActorRequire(name = "WaitSend_HashMapDB", scope = "global")
	IWaitForSendMap oSendingHashMapDB; // 保存待广播交易
	@ActorRequire(name = "WaitBlock_HashMapDB", scope = "global")
	IWaitForBlockMap oPendingHashMapDB; // 保存待打包block的交易
	@ActorRequire(name = "ConfirmTxHashDB", scope = "global")
	IConfirmTxMap oConfirmMapDB; // 保存待打包block的交易
	@ActorRequire(name = "BlockStore_Helper", scope = "global")
	IBlockStore blockStore;

	@Override
	public String[] getCmds() {
		return new String[] { PSYSCommand.SUM.name() };
	}

	@Override
	public String getModule() {
		return "API";//return PSYSModule.ACS.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetSummary pb, final CompleteHandler handler) {
		RespGetSummary.Builder oRespGetSummary = RespGetSummary.newBuilder();
		oRespGetSummary.setMaxConnection(String.valueOf(blockStore.getMaxConnectNumber()));
		oRespGetSummary.setMaxStable(String.valueOf(blockStore.getMaxStableNumber()));
		oRespGetSummary.setStable(String.valueOf(blockStore.getCacheStableBlockSize()));
		oRespGetSummary.setUnStable(String.valueOf(blockChainHelper.getUnStableStorageSize()));
		oRespGetSummary.setWaitBlock(String.valueOf(oConfirmMapDB.getQueueSize()));
		oRespGetSummary.setWaitSync(String.valueOf(transactionHelper.getSendingSize()));
		oRespGetSummary.setLastBlockTxTime(transactionHelper.getStats().getLastBlockTxTime());
		oRespGetSummary.setLastAcceptTxTime(transactionHelper.getStats().getLastAcceptTxTime());
		oRespGetSummary.setTxSyncCount(transactionHelper.getStats().getTxSyncCount().intValue());
		oRespGetSummary.setTxAcceptCount(transactionHelper.getStats().getAcceptTxCount().get());

		// for (Iterator<Cell<String, Long, BlockStoreNodeValue>> it =
		// blockStore.getUnStableStore()
		// .getStorage().cellSet().iterator(); it.hasNext();) {
		// Cell<String, Long, BlockStoreNodeValue> item = it.next();
		// UnStableItems.Builder oUnStableItems = UnStableItems.newBuilder();
		// oUnStableItems.setNumber(String.valueOf(item.getValue().getNumber()));
		// oUnStableItems.setHash(item.getValue().getBlockHash());
		// oRespGetSummary.addItems(oUnStableItems.build());
		// }

		// LinkedBlockingDeque<HashPair> lbd = new
		// LinkedBlockingDeque<HashPair>(oConfirmMapDB.getConfirmQueue());

		int i = 500;
		// for (Iterator<HashPair> it = lbd.iterator(); it.hasNext();) {
		// if (i <= 0) {
		// break;
		// }
		// HashPair item = it.next();
		// UnStableItems.Builder oWaitBlockItem = UnStableItems.newBuilder();
		// oWaitBlockItem.setNumber(String.valueOf(item.getBits().bitCount()));
		// oWaitBlockItem.setHash(item.getKey());
		// oWaitBlockItem.setRemove(String.valueOf(item.isRemoved()));
		// oRespGetSummary.addItems(oWaitBlockItem);
		// i--;
		// }

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetSummary.build()));
	}
}
