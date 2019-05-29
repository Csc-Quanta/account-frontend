package org.csc.account.action.account;

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
import org.csc.account.gens.Actimpl.MsgToken;
import org.csc.account.gens.Actimpl.PACTCommand;
import org.csc.account.gens.Actimpl.ReqQueryToken;
import org.csc.account.gens.Actimpl.RespQuerySingleToken;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Act.ERC20TokenValue;

@NActorProvider
@Slf4j
@Data
public class GetTokenInfoImpl extends SessionModules<ReqQueryToken> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	IAccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PACTCommand.QIO.name() };
	}

	@Override
	public String getModule() {
		return "API";// return PACTModule.ACT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqQueryToken pb, final CompleteHandler handler) {
		RespQuerySingleToken.Builder oRespQueryIC = RespQuerySingleToken.newBuilder();

		try {
			if (StringUtils.isBlank(pb.getToken())) {
				oRespQueryIC.setRetCode(-1);
				oRespQueryIC.setRetMsg("token name cannot be empty");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespQueryIC.build()));
				return;
			}

			ERC20TokenValue erc20TokenValue = oAccountHelper.getToken(pb.getToken());
			MsgToken.Builder oMsgToken = MsgToken.newBuilder();
			oMsgToken.setAmount(String.valueOf(ByteUtil.bytesToBigInteger(erc20TokenValue.getTotalSupply().toByteArray())));
			oMsgToken.setCreator(erc20TokenValue.getAddress());
			oMsgToken.setTimestamp(String.valueOf(erc20TokenValue.getTimestamp()));
			oMsgToken.setToken(erc20TokenValue.getToken());
			oRespQueryIC.setTokens(oMsgToken.build());

			oRespQueryIC.setRetCode(1);
		} catch (Exception e) {
			log.warn("ex::", e);
			oRespQueryIC.clear();
			oRespQueryIC.setRetCode(-1);
			oRespQueryIC.setRetMsg("error on query token::" + e);
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespQueryIC.build()));
	}
}
