package com.wallet.crypto.trustapp.service;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.crypto.trustapp.entity.ServiceException;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.widget.MnemonicUtils;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.HDUtils;
import org.ethereum.geth.Accounts;
import org.ethereum.geth.Address;
import org.ethereum.geth.BigInt;
import org.ethereum.geth.Geth;
import org.ethereum.geth.KeyStore;
import org.ethereum.geth.Transaction;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletFile;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import static org.web3j.crypto.Wallet.create;

public class GethKeystoreAccountService implements AccountKeystoreService {
    private static final int PRIVATE_KEY_RADIX = 16;
    /**
     * CPU/Memory cost parameter. Must be larger than 1, a power of 2 and less than 2^(128 * r / 8).
     */
    private static final int N = 1 << 9;
    /**
     * Parallelization parameter. Must be a positive integer less than or equal to Integer.MAX_VALUE / (128 * r * 8).
     */
    private static final int P = 1;

    private final KeyStore keyStore;
    private final File storeDir;

    public GethKeystoreAccountService(File keyStoreFile, File phraseFile) {
        this.storeDir = phraseFile;
        if (!storeDir.exists() && !storeDir.mkdirs()) {
            Crashlytics.logException((Throwable) new IOException("Failed to create key store directory."));
        }
        keyStore = new KeyStore(keyStoreFile.getAbsolutePath(), Geth.LightScryptN, Geth.LightScryptP);
    }


    @Override
    public Single<Wallet> createAccount(String password) {
        return Single.fromCallable(() -> new Wallet(
                keyStore.newAccount(password).getAddress().getHex().toLowerCase()))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Wallet> importKeystore(String store, String password, String newPassword) {
        return Single.fromCallable(() -> {
            org.ethereum.geth.Account account = keyStore
                    .importKey(store.getBytes(Charset.forName("UTF-8")), password, newPassword);
            return new Wallet(account.getAddress().getHex().toLowerCase());
        })
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Wallet> importPrivateKey(String privateKey, String newPassword) {
        return Single.fromCallable(() -> {
            BigInteger key = new BigInteger(privateKey, PRIVATE_KEY_RADIX);
            ECKeyPair keypair = ECKeyPair.create(key);
            WalletFile walletFile = create(newPassword, keypair, N, P);
            return new ObjectMapper().writeValueAsString(walletFile);
        }).compose(upstream -> importKeystore(upstream.blockingGet(), newPassword, newPassword));
    }

    @Override
    public Single<Wallet> importPhraseKey(String phraseKey, String newPassword) {
        Wallet wallet = new Wallet("");
        return Single.fromCallable(() -> phraseKey)
                .map(string -> {
                    wallet.mnemonic = string;
                    // 生成种子
                    byte[] seed = MnemonicUtils.generateSeed(string, null);
                    // 生成主钱包
                    DeterministicKey rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
                    // 由主钱包生成HD钱包管理对象
                    DeterministicHierarchy dh = new DeterministicHierarchy(rootPrivateKey);
                    // 定义钱包路径，符合bip0044协议，以太坊钱包
                    List<ChildNumber> parentPath = HDUtils.parsePath("M/44H/60H/0H/0");
                    // 派生出第一个子私钥 "new ChildNumber(0)" 表示为（m/44'/60'/0'/0/0）目录的私钥
                    DeterministicKey child = dh.deriveChild(parentPath, true, true, new ChildNumber(0));
                    byte[] privateKeyByte = child.getPrivKeyBytes();
                    // 通过私钥生成公私钥对
                    ECKeyPair keyPair = ECKeyPair.create(privateKeyByte);
                    /*
                     * 通过密码和钥匙对生成WalletFile也就是keystore的bean类，newPassword在此处没有具体作用
                     * 以下几行代码主要是通过WalletFile对象获得钱包地址，私钥及keystore并没有进行保存
                     * 后续导出的流程中，如果需要从助记词导出私钥或者Keystore，可以重新生成私钥或keystore即可
                     */
                    WalletFile walletFile = org.web3j.crypto.Wallet.createLight(newPassword, keyPair);
                    if (walletFile != null && walletFile.getAddress() != null && hasAccount(walletFile.getAddress())) {
                        throw new ServiceException("这个地址的钱包已添加");
                    }
                    return walletFile.getAddress();
                }).map(address -> {
                    String value = address;
                    File destination;
                    if (Numeric.containsHexPrefix(value)) {
                        destination = new File(storeDir, getWalletFileName(Numeric.cleanHexPrefix(value)));
                        wallet.address = value;
                    } else {
                        destination = new File(storeDir, getWalletFileName(value));
                        wallet.address = Numeric.prependHexPrefix(value);
                    }
                    /*
                     * 此处只是简单地保存在文件里面，在真实的应用场景，要做加密处理才能够确保助记词的安全。
                     */
                    new ObjectMapper().writeValue(destination, wallet.mnemonic);
                    return wallet;
                });
    }

    private static String getWalletFileName(String address) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("'UTC--'yyyy-MM-dd'T'HH-mm-ss.SSS'--'");
        return dateFormat.format(new Date()) + address + ".json";
    }

    @Override
    public Single<String> exportAccount(Wallet wallet, String password, String newPassword) {
        return Single
                .fromCallable(() -> findAccount(wallet.address))
                .flatMap(account1 -> Single.fromCallable(()
                        -> new String(keyStore.exportKey(account1, password, newPassword))))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<String> exportPhrase(Wallet wallet, String password) {
        /*
         * 由于助记词只是简单地保存在文件里面，所以只需简单读取出来即可。
         * 在真实的应用场景，要做加密和解密导出处理才能够确保助记词的安全。
         */
        return this.getStoreFile(wallet.address)
                .map(file -> new ObjectMapper().readValue(file, String.class));
    }

    @Override
    public Completable deleteAccount(String address, String password) {
        return Single.fromCallable(() -> findAccount(address))
                .flatMapCompletable(account -> Completable.fromAction(
                        () -> keyStore.deleteAccount(account, password)))
                .subscribeOn(Schedulers.io());
    }
    @Override
    public Completable deletePhraseAccount(String address) {
        return Completable.fromAction(() -> {
            File[] arrfile = storeDir.listFiles();
            String object = Numeric.cleanHexPrefix(address.toString());
            for (File file : arrfile) {
                if (!file.getName().contains((CharSequence) object) || file.delete()) continue;
                //  Log.d("KEY_STORE_DS_TAG", (String) "Failed to delete key store file.");
            }
        });
    }
    public Single<File> getStoreFile(final String address) {

        return Single.fromCallable(() -> {
            File[] arrfile = storeDir.listFiles();
            String object = Numeric.cleanHexPrefix(address);
            for (File file : arrfile) {
                if (!file.getName().contains(object)) continue;
                return file;
            }
            return null;
        });
    }

    @Override
    public Single<byte[]> signTransaction(Wallet signer, String signerPassword, String toAddress, BigInteger amount, BigInteger gasPrice, BigInteger gasLimit, long nonce, byte[] data, long chainId) {
        return Single.fromCallable(() -> {
            BigInt value = new BigInt(0);
            value.setString(amount.toString(), 10);

            BigInt gasPriceBI = new BigInt(0);
            gasPriceBI.setString(gasPrice.toString(), 10);

            BigInt gasLimitBI = new BigInt(0);
            gasLimitBI.setString(gasLimit.toString(), 10);

            Transaction tx = new Transaction(
                    nonce,
                    new Address(toAddress),
                    value,
                    gasLimitBI,
                    gasPriceBI,
                    data);

            BigInt chain = new BigInt(chainId); // Chain identifier of the main net
            org.ethereum.geth.Account gethAccount = findAccount(signer.address);
            keyStore.unlock(gethAccount, signerPassword);
            Transaction signed = keyStore.signTx(gethAccount, tx, chain);
            keyStore.lock(gethAccount.getAddress());

            return signed.encodeRLP();
        })
                .subscribeOn(Schedulers.io());
    }

    @Override
    public boolean hasAccount(String address) {
        return keyStore.hasAddress(new Address(address));
    }

    @Override
    public Single<Wallet[]> fetchAccounts() {
        return Single.fromCallable(() -> {
            //通过geth获取的钱包文件只包含keystore导入的或者生成的钱包
            Accounts accounts = keyStore.getAccounts();
            int len = (int) accounts.size();
            //助记词文件列表
            File[] listFiles;
            if ((listFiles = this.storeDir.listFiles()) == null) {
                listFiles = new File[0];
            }
            int phraselen = listFiles.length;


            Wallet[] result = new Wallet[len + phraselen];
            //获取非助记词导入的钱包列表
            for (int i = 0; i < len; i++) {
                org.ethereum.geth.Account gethAccount = accounts.get(i);
                result[i] = new Wallet(gethAccount.getAddress().getHex().toLowerCase());
            }


            //获取助记词导入的钱包列表
            for (int i = 0; i < phraselen; i++) {

                String[] arrstring = listFiles[i].getName().split("--");
                String[] arrstringWallet = arrstring[arrstring.length - 1].split("\\.");
                String address = "";

                if (arrstringWallet.length > 0) {

                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("0x");
                    stringBuilder.append(arrstringWallet[0]);
                    address = stringBuilder.toString();
                } else {

                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("0x");
                    stringBuilder.append(arrstring[arrstring.length - 1]);
                    address = stringBuilder.toString();
                }
                if (com.wallet.crypto.trustapp.entity.Address.isAddress(address)) {
                    result[i + len] = new Wallet(address.toLowerCase(),true);
                }
            }
            return result;
        })
                .subscribeOn(Schedulers.io());
    }

    private org.ethereum.geth.Account findAccount(String address) throws ServiceException {
        Accounts accounts = keyStore.getAccounts();
        int len = (int) accounts.size();
        for (int i = 0; i < len; i++) {
            try {
                android.util.Log.d("ACCOUNT_FIND", "Address: " + accounts.get(i).getAddress().getHex());
                if (accounts.get(i).getAddress().getHex().equalsIgnoreCase(address)) {
                    return accounts.get(i);
                }
            } catch (Exception ex) {
                /* Quietly: interest only result, maybe next is ok. */
            }
        }
        throw new ServiceException("Wallet with address: " + address + " not found");
    }
}
