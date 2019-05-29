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
import org.csc.account.api.IChainHelper;
import org.csc.account.gens.Blockimpl.BlockHeaderxImpl;
import org.csc.account.gens.Blockimpl.PBCTCommand;
import org.csc.account.gens.Blockimpl.ReqGetBlockByNumber;
import org.csc.account.gens.Blockimpl.RespGetBlocksByNumber;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Block.BlockEntity;

import java.util.List;

@NActorProvider
@Slf4j
@Data
public class GetBlocksByNumberImpl extends SessionModules<ReqGetBlockByNumber> {
	@ActorRequire(name = "Block_Helper", scope = "global")
	IBlockHelper blockHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "BlockChain_Helper", scope = "global")
	IChainHelper blockChainHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PBCTCommand.GBS.name() };
	}

	@Override
	public String getModule() {
		return "API";//return PBCTModule.BCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetBlockByNumber pb, final CompleteHandler handler) {
		RespGetBlocksByNumber.Builder oRespGetBlock = RespGetBlocksByNumber.newBuilder();

		try {
			List<BlockEntity> list = blockChainHelper.getBlocksByNumber(pb.getNumber());
			for (BlockEntity oBlockEntity : list) {
				
				BlockHeaderxImpl.Builder oBlockHeaderImpl = BlockHeaderxImpl.newBuilder();
				oBlockHeaderImpl.setBlockHash(encApi.hexEnc(oBlockEntity.getHeader().getHash().toByteArray()));
				oBlockHeaderImpl.setNumber(oBlockEntity.getHeader().getNumber());
				oBlockHeaderImpl.setParentHash(encApi.hexEnc(oBlockEntity.getHeader().getPreHash().toByteArray()));
				oBlockHeaderImpl.setTimestamp(oBlockEntity.getHeader().getTimestamp());
				oBlockHeaderImpl.setState(encApi.hexEnc(oBlockEntity.getHeader().getStateRoot().toByteArray()));
				oBlockHeaderImpl.setReceipt(encApi.hexEnc(oBlockEntity.getHeader().getReceiptRoot().toByteArray()));
				oBlockHeaderImpl.setTxTrieRoot(encApi.hexEnc(oBlockEntity.getHeader().getTxRoot().toByteArray()));
				oBlockHeaderImpl.setMiner(encApi.hexEnc(oBlockEntity.getMiner().getAddress().toByteArray()));
				oBlockHeaderImpl.setSliceId(oBlockEntity.getHeader().getSliceId());
				oRespGetBlock.addBlocks(oBlockHeaderImpl);
			}
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetBlock.build()));
	}
}

// getBlocksByNumber