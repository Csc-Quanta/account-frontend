package org.csc.account.action.transaction;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.apache.commons.lang3.StringUtils;
import org.csc.account.api.IAccountHelper;
import org.csc.account.gens.Tximpl.PTXTCommand;
import org.csc.account.gens.Tximpl.ReqSideChainVerify;
import org.csc.account.gens.Tximpl.RespSideChainVerify;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Act.Account;
import org.csc.evmapi.gens.Act.SideChainStorage;
import org.csc.evmapi.gens.Act.SideChainTxStorage;

@NActorProvider
@Slf4j
@Data
public class SideChainTransactionVerifyImpl extends SessionModules<ReqSideChainVerify> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	IAccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PTXTCommand.SCV.name() };
	}

	@Override
	public String getModule() {
		return "API";
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqSideChainVerify pb, final CompleteHandler handler) {
		RespSideChainVerify.Builder oRespSideChainVerify = RespSideChainVerify.newBuilder();
		
		Account.Builder oSideChainRecord = oAccountHelper
				.getAccount(ByteString.copyFrom(encApi.hexDec(pb.getSideChainAddr())));

		if (oSideChainRecord == null) {
			oRespSideChainVerify.setRetCode(-1);
			oRespSideChainVerify.setRetMsg("没有找到子链记录");
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespSideChainVerify.build()));
			return;
		}

		byte[] txStorage = oAccountHelper.getStorage(oSideChainRecord, encApi.hexDec(pb.getBlockHash()));
		if (txStorage == null) {
			oRespSideChainVerify.setRetCode(-1);
			oRespSideChainVerify.setRetMsg("子链交易不存在");
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespSideChainVerify.build()));
			return;
		}

		try {
			SideChainStorage.Builder oSideChainStorage = SideChainStorage.parseFrom(txStorage).toBuilder();
			oRespSideChainVerify.setRetCode(1);
			oRespSideChainVerify.setBlockHash(encApi.hexEnc(oSideChainStorage.getBlockHash().toByteArray()));
			for (SideChainTxStorage oSideChainTxStorage : oSideChainStorage.getTxsList()) {
				if (StringUtils.isBlank(pb.getTxHash())
						|| encApi.hexEnc(oSideChainTxStorage.getTxHash().toByteArray()).equals(pb.getTxHash())) {
					oRespSideChainVerify.setTxHash(encApi.hexEnc(oSideChainTxStorage.getTxHash().toByteArray()));
					oRespSideChainVerify.setStatus(oSideChainTxStorage.getStatus().toStringUtf8());
					oRespSideChainVerify.setResult(oSideChainTxStorage.getResult().toStringUtf8());
				}
			}

			handler.onFinished(PacketHelper.toPBReturn(pack, oRespSideChainVerify.build()));
			return;
		} catch (InvalidProtocolBufferException e) {
			oRespSideChainVerify.setRetCode(-1);
			oRespSideChainVerify.setRetMsg("子链交易数据错误");
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespSideChainVerify.build()));
			return;
		}
	}
}
