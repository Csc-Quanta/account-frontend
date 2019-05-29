package org.csc.account.sample;

import com.google.protobuf.ByteString;

public interface IAccountOperateListener {

	void offerNewAccount(ByteString hexaddress, int nonce);

}