package org.csc.account.action.block;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.csc.account.api.IBlockHelper;
import org.csc.account.api.IBlockStore;
import org.csc.account.api.IChainHelper;
import org.csc.account.bean.BlockStoreNodeValue;
import org.csc.account.gens.Blockimpl.BlocksStore;
import org.csc.account.gens.Blockimpl.PBCTCommand;
import org.csc.account.gens.Blockimpl.ReqGetBlockByNumber;
import org.csc.account.gens.Blockimpl.RespGetBlocksStoreByNumber;
import org.csc.bcapi.EncAPI;

import java.util.Iterator;
import java.util.Map;

@NActorProvider
@Slf4j
@Data
public class GetBlockStoreByNumberImpl extends SessionModules<ReqGetBlockByNumber> {
	@ActorRequire(name = "Block_Helper", scope = "global")
	IBlockHelper blockHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "BlockChain_Helper", scope = "global")
	IChainHelper blockChainHelper;
	@ActorRequire(name = "BlockStore_Helper", scope = "global")
	IBlockStore blockStore;
	
	@Override
	public String[] getCmds() {
		return new String[] { PBCTCommand.GBT.name() };
	}

	@Override
	public String getModule() {
		return "API";//return PBCTModule.BCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetBlockByNumber pb, final CompleteHandler handler) {
		RespGetBlocksStoreByNumber.Builder oRespGetBlock = RespGetBlocksStoreByNumber.newBuilder();

		try {
			for (Iterator<Map.Entry<String, BlockStoreNodeValue>> it = blockStore.getUnStableBlocks(pb.getNumber()); it
					.hasNext();) {
				Map.Entry<String, BlockStoreNodeValue> item = it.next();
				BlocksStore.Builder oBlocksStore = BlocksStore.newBuilder();
				oBlocksStore.setConnect(String.valueOf(item.getValue().isConnect()));
				oBlocksStore.setHash(item.getKey());
				oBlocksStore.setParentHash(item.getValue().getParentHash());
				oBlocksStore.setNumber(String.valueOf(item.getValue().getNumber()));
				oBlocksStore.setMiner(encApi.hexEnc(item.getValue().getBlockEntity().getMiner().getAddress().toByteArray()));

				oRespGetBlock.addBlocks(oBlocksStore);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetBlock.build()));
	}
}

// getBlocksByNumber