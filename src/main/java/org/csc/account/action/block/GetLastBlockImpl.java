package org.csc.account.action.block;

import com.google.protobuf.ByteString;
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
import org.csc.account.gens.Blockimpl.BlockMinerImpl;
import org.csc.account.gens.Blockimpl.PBCTCommand;
import org.csc.account.gens.Blockimpl.ReqBlockInfo;
import org.csc.account.gens.Blockimpl.RespBlockDetail;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Block.BlockEntity;

@NActorProvider
@Slf4j
@Data
public class GetLastBlockImpl extends SessionModules<ReqBlockInfo> {
	@ActorRequire(name = "Block_Helper", scope = "global")
	IBlockHelper blockHelper;
	@ActorRequire(name = "BlockChain_Helper", scope = "global")
	IChainHelper blockChainHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PBCTCommand.GLB.name() };
	}

	@Override
	public String getModule() {
		return "API";//return PBCTModule.BCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqBlockInfo pb, final CompleteHandler handler) {
		RespBlockDetail.Builder oRespBlockDetail = RespBlockDetail.newBuilder();
		try {
			BlockEntity oBlockEntity = blockChainHelper.GetConnectBestBlock();
			if (oBlockEntity == null) {
				oBlockEntity = blockChainHelper.GetStableBestBlock();
			}
			BlockMinerImpl.Builder oBlockMinerImpl = BlockMinerImpl.newBuilder();
			
			oRespBlockDetail.setBlockHash(encApi.hexEnc(oBlockEntity.getHeader().getHash().toByteArray()));
			oRespBlockDetail.setExtraData(oBlockEntity.getHeader().getExtData().toStringUtf8());
			oRespBlockDetail.setNumber(oBlockEntity.getHeader().getNumber());
			oRespBlockDetail.setParentHash(encApi.hexEnc(oBlockEntity.getHeader().getPreHash().toByteArray()));
			oRespBlockDetail.setSliceId(oBlockEntity.getHeader().getSliceId());
			oRespBlockDetail.setTimestamp(oBlockEntity.getHeader().getTimestamp());
			oRespBlockDetail.setStateRoot(encApi.hexEnc(oBlockEntity.getHeader().getStateRoot().toByteArray()));
			//添加ssr
            oRespBlockDetail.addAllSsr(oBlockEntity.getHeader().getSsrList());
			for (ByteString oTxhash : oBlockEntity.getHeader().getTxHashsList()) {
				oRespBlockDetail.addTxHashs(encApi.hexEnc(oTxhash.toByteArray()));
			}
			oBlockMinerImpl.setBcuid(oBlockEntity.getMiner().getBcuid());
			oBlockMinerImpl.setAddress(encApi.hexEnc(oBlockEntity.getMiner().getAddress().toByteArray()));
			//oBlockMinerImpl.setNode(oBlockEntity.getMiner().getNode());
			oBlockMinerImpl.setReward(
					String.valueOf(ByteUtil.bytesToBigInteger(oBlockEntity.getMiner().getReward().toByteArray())));
			for (String part : oBlockEntity.getMiner().getPartsList()) {
				oBlockMinerImpl.addParts(part);
			}
			oBlockMinerImpl.setTermid(oBlockEntity.getMiner().getTermid());
			oBlockMinerImpl.setNodebit(oBlockEntity.getMiner().getBit());
			
			oRespBlockDetail.setMiner(oBlockMinerImpl);
			oRespBlockDetail.setRetCode(1);
		} catch (Exception e) {
			oRespBlockDetail.setRetCode(-1);
			// oRespBlockDetail.setRetMsg(e.);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespBlockDetail.build()));
	}
}
