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
import org.csc.account.gens.Actimpl.*;
import org.csc.account.util.ByteUtil;
import org.csc.bcapi.EncAPI;
import org.csc.evmapi.gens.Act.*;

@NActorProvider
@Slf4j
@Data
public class GetAccountsImpl extends SessionModules<ReqGetAccounts> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	IAccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PACTCommand.GAS.name() };
	}

	@Override
	public String getModule() {
		return PACTModule.ACT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetAccounts pb, final CompleteHandler handler) {
		RespGetAccounts.Builder oRespGetAccount = RespGetAccounts.newBuilder();
		
		try {
			for (String address : pb.getAddressList()) {
				Account.Builder oAccount = oAccountHelper
						.getAccount(ByteString.copyFrom(encApi.hexDec(ByteUtil.formatHexAddress(address))));
				AccountValueImpl.Builder oAccountValueImpl = AccountValueImpl.newBuilder();
				oAccountValueImpl.setAccountAddress(address);
				if (oAccount != null) {
					AccountValue oAccountValue = oAccount.getValue();

					oAccountValueImpl.setAcceptLimit(oAccountValue.getAcceptLimit());
					oAccountValueImpl.setAcceptMax(String.valueOf(ByteUtil.bytesToBigInteger(oAccountValue.getAcceptMax().toByteArray())));
					for (ByteString relAddress : oAccountValue.getSubAddressList()) {
						oAccountValueImpl.addAddress(encApi.hexEnc(relAddress.toByteArray()));
					}
					oAccountValueImpl.setAccumulated(String.valueOf(ByteUtil.bytesToBigInteger(oAccountValue.getAccumulated().toByteArray())));
					oAccountValueImpl.setAccumulatedTimestamp(oAccountValue.getAccumulatedTimestamp());

					oAccountValueImpl.setBalance(String.valueOf(ByteUtil.bytesToBigInteger(oAccountValue.getBalance().toByteArray())));
					// oAccountValueImpl.setCryptos(index, value)
					for (AccountCryptoValue oAccountTokenValue : oAccountValue.getCryptosList()) {
						AccountCryptoValueImpl.Builder oAccountCryptoValueImpl = AccountCryptoValueImpl.newBuilder();
						oAccountCryptoValueImpl.setSymbol(oAccountTokenValue.getSymbol().toStringUtf8());

						for (AccountCryptoToken oAccountCryptoToken : oAccountTokenValue.getTokensList()) {
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
						}
						oAccountValueImpl.addCryptos(oAccountCryptoValueImpl);
					}
					oAccountValueImpl.setMax(String.valueOf(ByteUtil.bytesToBigInteger(oAccountValue.getMaxTrans().toByteArray())));
					oAccountValueImpl.setNonce(oAccountValue.getNonce());
					for (AccountTokenValue oAccountTokenValue : oAccountValue.getTokensList()) {
						AccountTokenValueImpl.Builder oAccountTokenValueImpl = AccountTokenValueImpl.newBuilder();
						oAccountTokenValueImpl.setBalance(String.valueOf(ByteUtil.bytesToBigInteger(oAccountTokenValue.getBalance().toByteArray())));
						oAccountTokenValueImpl.setToken(oAccountTokenValue.getToken().toStringUtf8());
						oAccountTokenValueImpl.setLocked(String.valueOf(ByteUtil.bytesToBigInteger(oAccountTokenValue.getLocked().toByteArray())));
						oAccountValueImpl.addTokens(oAccountTokenValueImpl);
					}
					oAccountValueImpl.setStorage(encApi.hexEnc(oAccountValue.getStorage().toByteArray()));
					oAccountValueImpl.setCode(encApi.hexEnc(oAccountValue.getCode().toByteArray()));
					oAccountValueImpl.setCodeHash(encApi.hexEnc(oAccountValue.getCodeHash().toByteArray()));
					oAccountValueImpl.setData(oAccountValue.getData().toStringUtf8());
				}
				oRespGetAccount.addAddress(address);
				oRespGetAccount.addAccount(oAccountValueImpl);
			}
			oRespGetAccount.setRetCode(1);
		} catch (Exception e) {
			log.error("GetAccountImpl error", e);
			oRespGetAccount.clear();
			oRespGetAccount.setRetCode(-1);
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetAccount.build()));
	}
}
