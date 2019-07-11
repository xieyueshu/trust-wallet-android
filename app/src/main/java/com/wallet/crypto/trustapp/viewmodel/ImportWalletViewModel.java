package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.interact.ImportWalletInteract;
import com.wallet.crypto.trustapp.ui.widget.OnImportKeystoreListener;
import com.wallet.crypto.trustapp.ui.widget.OnImportPhraseKeyListener;
import com.wallet.crypto.trustapp.ui.widget.OnImportPrivateKeyListener;

public class ImportWalletViewModel extends BaseViewModel implements OnImportKeystoreListener, OnImportPrivateKeyListener, OnImportPhraseKeyListener {

    private final ImportWalletInteract importWalletInteract;
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();

    ImportWalletViewModel(ImportWalletInteract importWalletInteract) {
        this.importWalletInteract = importWalletInteract;
    }

    @Override
    public void onKeystore(String keystore, String password) {
        progress.postValue(true);
        importWalletInteract
                .importKeystore(keystore, password)
                .subscribe(this::onWallet, this::onError);
    }

    @Override
    public void onPrivateKey(String key) {
        progress.postValue(true);
        importWalletInteract
                .importPrivateKey(key)
                .subscribe(this::onWallet, this::onError);
    }
    @Override
    public void onPhraseKey(String key) {
        progress.postValue(true);
        importWalletInteract
                .importPhraseKey(key)
                .subscribe(this::onWallet, this::onError);
    }

    public LiveData<Wallet> wallet() {
        return wallet;
    }

    private void onWallet(Wallet wallet) {
        progress.postValue(false);
        this.wallet.postValue(wallet);
    }
}
