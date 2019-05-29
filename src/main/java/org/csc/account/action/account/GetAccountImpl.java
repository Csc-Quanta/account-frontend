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
import org.csc.account.api.IStateTrie;
import org.csc.account.gens.Actimpl.*;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Act.*;

@NActorProvider
@Slf4j
@Data
public class GetAccountImpl extends SessionModules<ReqGetAccount> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	IAccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "ETBlock_StateTrie", scope = "global")
	IStateTrie stateTrie;

	@Override
	public String[] getCmds() {
		return new String[] { PACTCommand.GAC.name() };
	}

	@Override
	public String getModule() {
		return "API";
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetAccount pb, final CompleteHandler handler) {
		RespGetAccount.Builder oRespGetAccount = RespGetAccount.newBuilder();

		try {
			Account.Builder oAccount = oAccountHelper
					.getAccount(ByteString.copyFrom(encApi.hexDec(ByteUtil.formatHexAddress(pb.getAddress()))));
			AccountValueImpl.Builder valueImpl = AccountValueImpl.newBuilder();
			valueImpl.setAccountAddress(pb.getAddress());
			if (oAccount != null) {
				AccountValue value = oAccount.getValue();

				valueImpl.setAcceptLimit(value.getAcceptLimit());
				valueImpl.setAcceptMax(String.valueOf(ByteUtil.bytesToBigInteger(value.getAcceptMax().toByteArray())));
				value.getSubAddressList().forEach(a -> valueImpl.addAddress(encApi.hexEnc(a.toByteArray())));
				valueImpl.setAccumulated(String.valueOf(ByteUtil.bytesToBigInteger(value.getAccumulated().toByteArray())));
				valueImpl.setAccumulatedTimestamp(value.getAccumulatedTimestamp());

				ByteString balance = value.getBalance();
				valueImpl.setBalance(String.valueOf(ByteUtil.bytesToBigInteger(balance.toByteArray())));
				// oAccountValueImpl.setCryptos(index, value)
				int size = pb.getS();
				int page = pb.getP();
				if (size == 0) {
					size = 20;
				}
				int start = page * size;
				int end = start + size;

				for (AccountCryptoValue oAccountTokenValue: value.getCryptosList()) {
					AccountCryptoValueImpl.Builder oAccountCryptoValueImpl = AccountCryptoValueImpl.newBuilder();
					oAccountCryptoValueImpl.setSymbol(oAccountTokenValue.getSymbol().toStringUtf8());
					int index = 0;
					for (AccountCryptoToken oAccountCryptoToken: oAccountTokenValue.getTokensList()) {
						if (index < start) {
							index += 1;
						} else if (index >= start && index < end) {
							AccountCryptoTokenImpl.Builder oAccountCryptoTokenImpl = AccountCryptoTokenImpl
									.newBuilder();
							oAccountCryptoTokenImpl.setCode(oAccountCryptoToken.getCode());
							oAccountCryptoTokenImpl.setHash(encApi.hexEnc(oAccountCryptoToken.getHash().toByteArray()));
							oAccountCryptoTokenImpl.setIndex(oAccountCryptoToken.getIndex());
							oAccountCryptoTokenImpl.setName(oAccountCryptoToken.getName());
							oAccountCryptoTokenImpl.setNonce(oAccountCryptoToken.getNonce());
							oAccountCryptoTokenImpl
									.setOwner(encApi.hexEnc(oAccountCryptoToken.getOwner().toByteArray()));
							oAccountCryptoTokenImpl.setOwnertime(oAccountCryptoToken.getOwnertime());
							oAccountCryptoTokenImpl.setTimestamp(oAccountCryptoToken.getTimestamp());
							oAccountCryptoTokenImpl.setTotal(oAccountCryptoToken.getTotal());

							oAccountCryptoValueImpl.addTokens(oAccountCryptoTokenImpl);
							index += 1;
						} else {
							break;
						}
					}
					valueImpl.addCryptos(oAccountCryptoValueImpl);
				}
				valueImpl.setMax(String.valueOf(ByteUtil.bytesToBigInteger(value.getMaxTrans().toByteArray())));
				valueImpl.setNonce(value.getNonce());
				int index = 0;
				for (AccountTokenValue oAccountTokenValue: value.getTokensList()) {
					if (index < start) {
						continue;
					} else if (index >= start && index < end) {
						AccountTokenValueImpl.Builder oAccountTokenValueImpl = AccountTokenValueImpl.newBuilder();
						oAccountTokenValueImpl.setBalance(String
								.valueOf(ByteUtil.bytesToBigInteger(oAccountTokenValue.getBalance().toByteArray())));
						oAccountTokenValueImpl.setToken(oAccountTokenValue.getToken().toStringUtf8());
						oAccountTokenValueImpl.setLocked(String
								.valueOf(ByteUtil.bytesToBigInteger(oAccountTokenValue.getLocked().toByteArray())));
						valueImpl.addTokens(oAccountTokenValueImpl);
					} else {
						break;
					}
				}
				valueImpl.setStorage(encApi.hexEnc(value.getStorage().toByteArray()));
				valueImpl.setCode(encApi.hexEnc(value.getCode().toByteArray()));
				valueImpl.setCodeHash(encApi.hexEnc(value.getCodeHash().toByteArray()));
				valueImpl.setData(value.getData().toStringUtf8());
			} else {
				// log.error("cannot find address::" + pb.getAddress());
			}
			oRespGetAccount.setAddress(pb.getAddress());
			oRespGetAccount.setAccount(valueImpl);
			oRespGetAccount.setRetCode(1);
		} catch (Exception e) {
			log.error("GetAccountImpl error::" + pb.getAddress(), e);
			oRespGetAccount.clear();
			oRespGetAccount.setRetCode(-1);
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetAccount.build()));
	}
}
