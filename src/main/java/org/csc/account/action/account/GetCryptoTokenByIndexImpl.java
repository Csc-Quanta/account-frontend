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
import org.csc.account.gens.Actimpl.PACTModule;
import org.csc.account.gens.Actimpl.ReqGetCryptoTokenByIndex;
import org.csc.account.gens.Actimpl.RespCryptoToken;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Act.Account;
import org.csc.evmapi.gens.Act.AccountCryptoToken;
import org.csc.evmapi.gens.Act.CryptoTokenOrigin;

@NActorProvider
@Slf4j
@Data
public class GetCryptoTokenByIndexImpl extends SessionModules<ReqGetCryptoTokenByIndex> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	IAccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PACTCommand.CTI.name() };
	}

	@Override
	public String getModule() {
		return PACTModule.ACT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetCryptoTokenByIndex pb, final CompleteHandler handler) {
		RespCryptoToken.Builder oRespCryptoToken = RespCryptoToken.newBuilder();

		Account.Builder oCryptoAccount = oAccountHelper
				.getAccount(ByteString.copyFrom(encApi.hexDec(pb.getRecordAddress())));
		AccountCryptoToken oOriginAccountCryptoToken = oAccountHelper.getCryptoTokenByIndex(oCryptoAccount.build(),
				ByteString.copyFrom(encApi.hexDec(pb.getSymbol())), pb.getIndex());

		Account.Builder oCryptoTokenAccount = oAccountHelper
				.getAccount(ByteString.copyFrom(encApi.sha3Encode(encApi.hexDec(pb.getSymbol()))));
		AccountCryptoToken oAccountCryptoToken = oAccountHelper.getCryptoTokenByHash(oCryptoTokenAccount.build(),
				oOriginAccountCryptoToken.getHash().toByteArray());

		CryptoTokenOrigin.Builder oCryptoTokenOrigin = oAccountHelper.getCryptoBySymbol(oCryptoAccount.build(),
				pb.getSymbolBytes().toByteArray());

		if (oAccountCryptoToken != null) {
			oRespCryptoToken.setCode(oAccountCryptoToken.getCode());
			oRespCryptoToken.setCurrent(
					oCryptoTokenOrigin.getOriginValue(oCryptoTokenOrigin.getOriginValueCount() - 1).getEndIndex());
			oRespCryptoToken.setHash(encApi.hexEnc(oOriginAccountCryptoToken.getHash().toByteArray()));
			oRespCryptoToken.setIndex(Integer.parseInt(String.valueOf(oAccountCryptoToken.getIndex())));
			oRespCryptoToken.setName(oAccountCryptoToken.getName());
			oRespCryptoToken.setOwner(encApi.hexEnc(oAccountCryptoToken.getOwner().toByteArray()));
			oRespCryptoToken.setTotal(String.valueOf(oCryptoTokenOrigin.getOriginValue(0).getTotal()));
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCryptoToken.build()));
	}
}
