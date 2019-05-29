package org.csc.account.action.transaction;

import org.csc.account.action.BlockChainConfig;
import org.csc.account.api.IAccountHelper;
import org.csc.account.gens.Actimpl.PACTCommand;
import org.csc.account.gens.Actimpl.ReqQueryToken;
import org.csc.account.gens.Tximpl.PTXTCommand;
import org.csc.account.gens.Tximpl.ReqQuerySideChainSyncInfo;
import org.csc.account.gens.Tximpl.RespQuerySideChainSyncInfo;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Act.Account;
import org.csc.evmapi.gens.Act.SideChainValue;

import com.google.protobuf.ByteString;

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
public class GetSideChainInfoImpl extends SessionModules<ReqQuerySideChainSyncInfo> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	IAccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PTXTCommand.GSC.name() };
	}

	@Override
	public String getModule() {
		return "API";
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqQuerySideChainSyncInfo pb, final CompleteHandler handler) {
		RespQuerySideChainSyncInfo.Builder oRespQuerySideChainSyncInfo = RespQuerySideChainSyncInfo.newBuilder();

		try {
			Account.Builder oRecordAccount = oAccountHelper
					.getAccount(ByteString.copyFrom(encApi.hexDec(BlockChainConfig.sidechain_record_account_address)));

			byte[] recordStorage = oAccountHelper.getStorage(oRecordAccount, encApi.hexDec(pb.getSideChainAddr()));
			if (recordStorage == null) {
				oRespQuerySideChainSyncInfo.clear();
				oRespQuerySideChainSyncInfo.setRetCode(1);
				oRespQuerySideChainSyncInfo.setLastBlockNumber(0);
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespQuerySideChainSyncInfo.build()));
				return;
			}
			SideChainValue.Builder oSideChainValue = SideChainValue.parseFrom(recordStorage).toBuilder();

			oRespQuerySideChainSyncInfo.setRetCode(1);
			if (!oSideChainValue.getLastBlockHash().isEmpty())
				oRespQuerySideChainSyncInfo
						.setLastBlockHash(encApi.hexEnc(oSideChainValue.getLastBlockHash().toByteArray()));

			oRespQuerySideChainSyncInfo.setLastBlockNumber(oSideChainValue.getLastBlockNumber());

			if (!oSideChainValue.getLastSender().isEmpty())
				oRespQuerySideChainSyncInfo.setLastSender(encApi.hexEnc(oSideChainValue.getLastSender().toByteArray()));

			oRespQuerySideChainSyncInfo.setLastTimestamp(oSideChainValue.getLastTimestamp());
			oRespQuerySideChainSyncInfo.setTotalBlocks(oSideChainValue.getTotalBlocks());
		} catch (Exception e) {
			oRespQuerySideChainSyncInfo.setRetCode(-1);
//			oRespQuerySideChainSyncInfo.setRetMsg(e.getMessage());
			log.error("", e);
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespQuerySideChainSyncInfo.build()));
		return;
	}
}
