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
import org.csc.account.gens.Actimpl.MsgToken;
import org.csc.account.gens.Actimpl.PACTCommand;
import org.csc.account.gens.Actimpl.ReqQueryToken;
import org.csc.account.gens.Actimpl.RespQueryToken;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Act.ERC20TokenValue;

import java.util.List;

@NActorProvider
@Slf4j
@Data
public class GetTokenListImpl extends SessionModules<ReqQueryToken> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	IAccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PACTCommand.QIC.name() };
	}

	@Override
	public String getModule() {
		return "API";// return PACTModule.ACT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqQueryToken pb, final CompleteHandler handler) {
		RespQueryToken.Builder oRespQueryIC = RespQueryToken.newBuilder();

		try {
			List<ERC20TokenValue> tokens = oAccountHelper.getTokens(
					ByteString.copyFrom(encApi.hexDec(pb.getAddress())),ByteString.copyFromUtf8(pb.getToken()));
			for (ERC20TokenValue erc20TokenValue : tokens) {
				MsgToken.Builder oMsgToken = MsgToken.newBuilder();
				oMsgToken.setAmount(String.valueOf(erc20TokenValue.getTotalSupply()));
				oMsgToken.setCreator(erc20TokenValue.getAddress());
				oMsgToken.setTimestamp(String.valueOf(erc20TokenValue.getTimestamp()));
				oMsgToken.setToken(erc20TokenValue.getToken());
				oRespQueryIC.addTokens(oMsgToken.build());
			}
			oRespQueryIC.setRetCode(-1);
		} catch (Exception e) {
			log.warn("ex::", e);
			oRespQueryIC.clear();
			oRespQueryIC.setRetCode(-1);
			oRespQueryIC.setRetMsg("error on query token::" + e);
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespQueryIC.build()));
	}
}
