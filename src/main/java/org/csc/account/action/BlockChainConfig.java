package org.csc.account.action;

import onight.tfw.outils.conf.PropHelper;

public class BlockChainConfig {
	public static PropHelper props = new PropHelper(null);
	public static String token_record_account_address = props.get("org.csc.account.token.address", null);
	public static String cryptotoken_record_account_address = props.get("org.csc.account.cryptotoken.address", null);
	public static String sidechain_record_account_address = props.get("org.csc.account.sidechain.address", null);

}
