package org.csc.account.action.account;

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
import org.csc.account.gens.Actimpl.PACTCommand;
import org.csc.account.gens.Actimpl.ReqGetStorage;
import org.csc.account.gens.Actimpl.RespGetStorage;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Act.Account;

@NActorProvider
@Slf4j
@Data
public class GetAccountStorageImpl extends SessionModules<ReqGetStorage> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	IAccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PACTCommand.QAS.name() };
	}

	@Override
	public String getModule() {
		return "API";//return PACTModule.ACT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetStorage pb, final CompleteHandler handler) {
		RespGetStorage.Builder oRespGetStorage = RespGetStorage.newBuilder();

		try {
			Account.Builder oAccount = oAccountHelper
					.getAccount(ByteString.copyFrom(encApi.hexDec(ByteUtil.formatHexAddress(pb.getAddress()))));

			for (int i = 0; i < pb.getKeyCount(); i++) {
				byte[] v = oAccountHelper.getStorage(oAccount, encApi.hexDec(pb.getKey(i)));
				if (v != null) {
					oRespGetStorage.addContent(encApi.hexEnc(v));
				} else {
					oRespGetStorage.addContent("");
				}
			}
			oRespGetStorage.setRetMsg("success");
			oRespGetStorage.setRetCode(1);
		} catch (Exception e) {
			log.error("GetAccountImpl error::" + pb.getAddress(), e);
			oRespGetStorage.clear();
			oRespGetStorage.setRetCode(-1);
			oRespGetStorage.setRetMsg(e.getMessage());
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetStorage.build()));
	}
}
