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
import org.csc.account.gens.Blockimpl.*;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Block.BlockEntity;

/**
 * 根据块高获取块信息
 *
 * @author lance
 * 5/15/2019 18:04
 */
@NActorProvider
@Slf4j
@Data
public class GetBlockByNumberImpl extends SessionModules<ReqGetBlockByNumber> {
    @ActorRequire(name = "Block_Helper", scope = "global")
    IBlockHelper blockHelper;
    @ActorRequire(name = "bc_encoder", scope = "global")
    EncAPI encApi;
    @ActorRequire(name = "BlockChain_Helper", scope = "global")
    IChainHelper blockChainHelper;

    @Override
    public String[] getCmds() {
        return new String[]{PBCTCommand.GBN.name()};
    }

    @Override
    public String getModule() {
        return "API";
    }

    @Override
    public void onPBPacket(final FramePacket pack, final ReqGetBlockByNumber pb, final CompleteHandler handler) {
        RespGetBlock.Builder oRespGetBlock = RespGetBlock.newBuilder();

        try {
            BlockEntity oBlockEntity = blockChainHelper.getBlockByNumber(pb.getNumber());
            if (oBlockEntity == null) {
                oRespGetBlock.setRetCode(-1);
                oRespGetBlock.setRetMsg("该块高不存在块信息");
                handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetBlock.build()));
                return;
            }

            BlockHeaderImpl.Builder oBlockHeaderImpl = BlockHeaderImpl.newBuilder();
            oBlockHeaderImpl.setBlockHash(encApi.hexEnc(oBlockEntity.getHeader().getHash().toByteArray()));
            oBlockHeaderImpl.setParentHash(encApi.hexEnc(oBlockEntity.getHeader().getPreHash().toByteArray()));
            oBlockHeaderImpl.setNumber(oBlockEntity.getHeader().getNumber());
            oBlockHeaderImpl.setState(encApi.hexEnc(oBlockEntity.getHeader().getStateRoot().toByteArray()));
            oBlockHeaderImpl.setReceipt(encApi.hexEnc(oBlockEntity.getHeader().getReceiptRoot().toByteArray()));
            oBlockHeaderImpl.setTxTrieRoot(encApi.hexEnc(oBlockEntity.getHeader().getTxRoot().toByteArray()));
            oBlockHeaderImpl.setTimestamp(oBlockEntity.getHeader().getTimestamp());
            oBlockHeaderImpl.setExtraData(oBlockEntity.getHeader().getExtData().toStringUtf8());
            oBlockHeaderImpl.setSliceId(oBlockEntity.getHeader().getSliceId());
            oBlockHeaderImpl.addAllSsr(oBlockEntity.getHeader().getSsrList());
            for (ByteString oTxhash : oBlockEntity.getHeader().getTxHashsList()) {
                oBlockHeaderImpl.addTxHashs(encApi.hexEnc(oTxhash.toByteArray()));
            }

            BlockMinerImpl.Builder oBlockMinerImpl = BlockMinerImpl.newBuilder();
            oBlockMinerImpl.setBcuid(oBlockEntity.getMiner().getBcuid());
            oBlockMinerImpl.setAddress(encApi.hexEnc(oBlockEntity.getMiner().getAddress().toByteArray()));
            // oBlockMinerImpl.setNode(oBlockEntity.getMiner().getNode());
            oBlockMinerImpl.setReward(
                    String.valueOf(ByteUtil.bytesToBigInteger(oBlockEntity.getMiner().getReward().toByteArray())));
            for (String part : oBlockEntity.getMiner().getPartsList()) {
                oBlockMinerImpl.addParts(part);
            }
            oBlockMinerImpl.setTermid(oBlockEntity.getMiner().getTermid());
            oBlockMinerImpl.setNodebit(oBlockEntity.getMiner().getBit());

            for (String part : oBlockEntity.getMiner().getPartsList()) {
                oBlockMinerImpl.addParts(part);
            }

            oRespGetBlock.setVersion(String.valueOf(oBlockEntity.getVersion()));
            oRespGetBlock.setHeader(oBlockHeaderImpl);
            oRespGetBlock.setMiner(oBlockMinerImpl);
            oRespGetBlock.setRetCode(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        oRespGetBlock.setRetCode(1);

        handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetBlock.build()));
    }
}
