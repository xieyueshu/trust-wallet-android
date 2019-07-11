package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.interact.rx.operator.Operators;
import com.wallet.crypto.trustapp.repository.PasswordStore;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ImportWalletInteract {

    private final WalletRepositoryType walletRepository;
    private final PasswordStore passwordStore;

    public ImportWalletInteract(WalletRepositoryType walletRepository, PasswordStore passwordStore) {
        this.walletRepository = walletRepository;
        this.passwordStore = passwordStore;
    }

    public Single<Wallet> importKeystore(String keystore, String password) {
        return passwordStore
                .generatePassword()
                .flatMap(newPassword -> walletRepository
                        .importKeystoreToWallet(keystore, password, newPassword)
                        .compose(Operators.savePassword(passwordStore, walletRepository, newPassword)))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Wallet> importPrivateKey(String privateKey) {
        return passwordStore
                .generatePassword()
                .flatMap(newPassword -> walletRepository
                        .importPrivateKeyToWallet(privateKey, newPassword)
                        .compose(Operators.savePassword(passwordStore, walletRepository, newPassword)))
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 增加导入助记词的控制层方法。
     *
     * @param phraseKey 助记词包含空格及按顺序的字符串
     * @return
     */
    public Single<Wallet> importPhraseKey(String phraseKey) {
        return passwordStore
                .generatePassword()
                .flatMap(newPassword -> walletRepository
                        .importPhraseKeyToWallet(phraseKey, newPassword)
                        .compose(Operators.savePassword(passwordStore, walletRepository, newPassword)))
                .observeOn(AndroidSchedulers.mainThread());
    }
}
