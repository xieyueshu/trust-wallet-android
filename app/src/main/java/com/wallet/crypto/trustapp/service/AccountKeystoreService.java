package com.wallet.crypto.trustapp.service;

import com.wallet.crypto.trustapp.entity.Wallet;

import java.math.BigInteger;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface AccountKeystoreService {
	/**
	 * Create account in keystore
	 * @param password account password
	 * @return new {@link Wallet}
	 */
	Single<Wallet> createAccount(String password);

	/**
	 * Include new existing keystore
	 * @param store store to include
	 * @param password store password
	 * @return included {@link Wallet} if success
	 */
	Single<Wallet> importKeystore(String store, String password, String newPassword);
    Single<Wallet> importPrivateKey(String privateKey, String newPassword);
	/**
	 * 助记词导入具体实现方法。
	 *
	 * @param phraseKey
	 * @param newPassword
	 * @return
	 */
	Single<Wallet> importPhraseKey(String phraseKey, String newPassword);

	/**
	 * Export wallet to keystore
	 * @param wallet wallet to export
	 * @param password password from wallet
	 * @param newPassword new password to store
	 * @return store data
	 */
	Single<String> exportAccount(Wallet wallet, String password, String newPassword);

    /**
     * 助记词导出具体实现方法。
     *
     * @param wallet
     * @param password
     * @return
     */
	Single<String> exportPhrase(Wallet wallet, String password);
	/**
	 * Delete account from keystore
	 * @param address account address
	 * @param password account password
	 */
	Completable deleteAccount(String address, String password);
	Completable deletePhraseAccount(String address);
	/**
	 * Sign transaction
	 * @param signer {@link Wallet}
	 * @param signerPassword password from {@link Wallet}
	 * @param toAddress transaction destination address
	 * @param wei
	 * @param nonce
	 * @return sign data
	 */
	Single<byte[]> signTransaction(
			Wallet signer,
			String signerPassword,
			String toAddress,
			BigInteger amount,
			BigInteger gasPrice,
			BigInteger gasLimit,
			long nonce,
			byte[] data,
			long chainId);

	/**
	 * Check if there is an address in the keystore
	 * @param address {@link Wallet} address
	 */
	boolean hasAccount(String address);

	/**
	 * Return all {@link Wallet} from keystore
	 * @return wallets
	 */
	Single<Wallet[]> fetchAccounts();
}
