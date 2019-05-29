package org.csc.account.action.block;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.csc.account.api.*;
import org.csc.account.bean.HashPair;
import org.csc.account.gens.Blockimpl.PBCTCommand;
import org.csc.account.gens.Blockimpl.ReqBlockInfo;
import org.csc.account.gens.Blockimpl.RespBlockInfo;
import org.csc.account.util.OEntityBuilder;
import org.csc.bcapi.EncAPI;

@NActorProvider
@Slf4j
@Data
public class GetBlockInfoImpl extends SessionModules<ReqBlockInfo> {
    @ActorRequire(name = "OEntity_Helper", scope = "global")
    OEntityBuilder oEntityHelper;
    @ActorRequire(name = "bc_encoder", scope = "global")
    EncAPI encApi;
    @ActorRequire(name = "BlockChain_Helper", scope = "global")
    IChainHelper blockChainHelper;
    /**
     * 保存待广播交易
     */
    @ActorRequire(name = "WaitSend_HashMapDB", scope = "global")
    IWaitForSendMap oSendingHashMapDB;
    /**
     * 保存待打包block的交易
     */
    @ActorRequire(name = "WaitBlock_HashMapDB", scope = "global")
    IWaitForBlockMap oPendingHashMapDB;
    /**
     * 保存待打包block的交易
     */
    @ActorRequire(name = "ConfirmTxHashDB", scope = "global")
    IConfirmTxMap oConfirmMapDB;
    @ActorRequire(name = "ETBlock_StateTrie", scope = "global")
    IStateTrie stateTrie;
    @ActorRequire(name = "Transaction_Helper", scope = "global")
    ITransactionHelper transactionHelper;
//	@ActorRequire(name = "BlockStore_UnStable", scope = "global")
//	IBlockUnStableStore unStableStore;

    @ActorRequire(name = "TxPendingQueue", scope = "global")
    IPengingQueue<HashPair> queue;


    @Override
    public String[] getCmds() {
        return new String[]{PBCTCommand.BIO.name()};
    }

    @Override
    public String getModule() {
        return "API";//return PBCTModule.BCT.name();
    }

    @Override
    public void onPBPacket(final FramePacket pack, final ReqBlockInfo pb, final CompleteHandler handler) {
        RespBlockInfo.Builder oRespBlockInfo = RespBlockInfo.newBuilder();

        // if (!isDev) {
        // handler.onFinished(PacketHelper.toPBReturn(pack, oRespBlockInfo.build()));
        // return;
        // }
        try {
            if ("true".equalsIgnoreCase(pack.getExtStrProp("clear"))) {
                transactionHelper.getStats().getAcceptTxCount().set(0);
                transactionHelper.getStats().getBlockTxCount().set(0);
                transactionHelper.getStats().setFirstBlockTxTime(0);
                transactionHelper.getStats().setFirstAcceptTxTime(0);
                transactionHelper.getStats().setLastBlockTxTime(0);
                transactionHelper.getStats().setLastAcceptTxTime(0);
            }
            oRespBlockInfo.setBlockCount(blockChainHelper.getLastStableBlockNumber());
            oRespBlockInfo.setCache("state::" + stateTrie.getCacheSize()
                    // + " pool::"+ stateTrie.getBsPool().size() + " storage::"
                    // + ((stateTrie.getBatchStorage().get() == null ||
                    // stateTrie.getBatchStorage().get().kvs == null)
                    // ? "0"
                    // : stateTrie.getBatchStorage().get().kvs.size())
                    + " unstable:: " + blockChainHelper.getUnStableStorageSize() + " queue:: "
                    + oConfirmMapDB.getQueueSize()
                    // + " trie.rmsize::[p=" +
                    // stateTrie.getRemoveQueue().getCounter().getPtr_pending().get()+",s="
                    // +stateTrie.getRemoveQueue().getCounter().getPtr_sending().get()+",db="
                    // +stateTrie.getRemoveQueue().getCounter().getPtr_saved().get()+"] "
                    + " storage:: " + oConfirmMapDB.size() + " remove:: "
                    + oConfirmMapDB.getRemoveSize() + queue.getStatInfo() + "  bps::"
                    + (transactionHelper.getStats().getBlockTxCount().get() * 1000.0
                    / (transactionHelper.getStats().getLastBlockTxTime() - transactionHelper.getStats().getFirstBlockTxTime())));
            oRespBlockInfo.setNumber(blockChainHelper.getLastBlockNumber());
            // oRespBlockInfo.setCache(blockChainHelper.getBlockCacheDump());
            oRespBlockInfo.setWaitSync(oSendingHashMapDB.size());
            oRespBlockInfo.setWaitBlock(oConfirmMapDB.size());
            oRespBlockInfo.setTxAcceptCount(transactionHelper.getStats().getAcceptTxCount().get());
            oRespBlockInfo.setTxAcceptTps(transactionHelper.getStats().getTxAcceptTps());
            oRespBlockInfo.setTxBlockCount(transactionHelper.getStats().getBlockTxCount().get());
            oRespBlockInfo.setTxBlockTps(transactionHelper.getStats().getTxBlockTps());

            oRespBlockInfo.setMaxBlockTps(transactionHelper.getStats().getMaxBlockTps());
            oRespBlockInfo.setMaxAcceptTps(transactionHelper.getStats().getMaxAcceptTps());

            oRespBlockInfo.setFirstBlockTxTime(transactionHelper.getStats().getFirstBlockTxTime());
            oRespBlockInfo.setLastBlockTxTime(transactionHelper.getStats().getLastBlockTxTime());
            oRespBlockInfo
                    .setBlockTxTimeCostMS(transactionHelper.getStats().getLastBlockTxTime() - transactionHelper.getStats().getFirstBlockTxTime());

            oRespBlockInfo.setFirstAcceptTxTime(transactionHelper.getStats().getFirstAcceptTxTime());
            oRespBlockInfo.setLastAcceptTxTime(transactionHelper.getStats().getLastAcceptTxTime());
            oRespBlockInfo.setAcceptTxTimeCostMS(
                    transactionHelper.getStats().getLastAcceptTxTime() - transactionHelper.getStats().getFirstAcceptTxTime());

            // BigInteger c = BigInteger.ZERO;
            //
            // for (int i = 1; i < blockChainHelper.getLastBlockNumber(); i++) {
            // BlockEntity be = blockChainHelper.getBlockByNumber(i);
            // c = c.add(new BigInteger(String.valueOf(be.getHeader().getTxHashsCount())));
            // }
            //
            // oRespBlockInfo.setRealTxBlockCount(c.longValue());
            oRespBlockInfo.setRollBackBlockCount(transactionHelper.getStats().getRollBackBlockCount().intValue());
            oRespBlockInfo.setRollBackTxCount(transactionHelper.getStats().getRollBackTxCount().intValue());
            oRespBlockInfo.setTxSyncCount(transactionHelper.getStats().getTxSyncCount().intValue());

            // LinkedBlockingDeque<HashPair> lbd = new
            // LinkedBlockingDeque<HashPair>(oConfirmMapDB.getConfirmQueue());
            //
            // int i = 500;
            // for (Iterator<HashPair> it = lbd.iterator(); it.hasNext();) {
            // if (i <= 0) {
            // break;
            // }
            // HashPair item = it.next();
            // WaitBlockItem.Builder oWaitBlockItem = WaitBlockItem.newBuilder();
            // oWaitBlockItem.setC(String.valueOf(item.getBits().bitCount()));
            // oWaitBlockItem.setHash(item.getKey());
            // oWaitBlockItem.setRemove(String.valueOf(item.isRemoved()));
            // oRespBlockInfo.addWaits(oWaitBlockItem);
            // i--;
            // }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            // log.error("GetBlockInfoImpl error", e);
        }
        handler.onFinished(PacketHelper.toPBReturn(pack, oRespBlockInfo.build()));
    }
}
