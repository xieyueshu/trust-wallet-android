package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Wallet;

import java.math.BigInteger;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public interface WalletRepositoryType {
	Single<Wallet[]> fetchWallets();
	Single<Wallet> findWallet(String address);

	Single<Wallet> createWallet(String password);
	Single<Wallet> importKeystoreToWallet(String store, String password, String newPassword);
    Single<Wallet> importPrivateKeyToWallet(String privateKey, String newPassword);

	/**
	 * 导入助记词实现业务流程层的方法。
	 *
	 * @param phraseKey
	 * @param newPassword
	 * @return
	 */
	Single<Wallet> importPhraseKeyToWallet(String phraseKey, String newPassword);
	Single<String> exportWallet(Wallet wallet, String password, String newPassword);

	/**
	 * 导出助记词实现业务流程层的方法。
	 *
	 * @param wallet
	 * @param password
	 * @return
	 */
	Single<String> exportPhraseWallet(Wallet wallet, String password);
	Completable deleteWallet(String address, String password);
	Completable deletePhraseAccount(String address);

	Completable setDefaultWallet(Wallet wallet);
	Single<Wallet> getDefaultWallet();

	Single<BigInteger> balanceInWei(Wallet wallet);
}
