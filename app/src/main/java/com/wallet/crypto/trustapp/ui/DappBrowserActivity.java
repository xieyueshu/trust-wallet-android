package com.wallet.crypto.trustapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import trust.core.entity.Address;
import trust.core.entity.Transaction;
import trust.web3.Web3View;

/**
 * DAPP组件功能封装类。
 * 主要提供一个DAPP地址URL，通过Web3view组件来浏览DAPP，以及与DAPP交互。
 * Web3view组件实现功能主要包括：
 * web3view组件提供注入web3.js脚本，捕获DAPP/H5发起的web3的调用；
 * 从而实现发起交易；获取当前账户地址；获取交易结果；等等。
 */
public class DappBrowserActivity extends BaseActivity {
    AlertDialog dialog;

    @Inject
    EthereumNetworkRepositoryType ethereumNetworkRepository;
    @Inject
    FindDefaultWalletInteract findDefaultWalletInteract;

    public static int DAPP_REQUEST_CODE = 100;

    private Button sendButton;
    private EditText urlText;
    private Web3View web3View;
    private boolean confirmationForTokenTransfer = false;
    private Transaction transaction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dapp_browser);
        toolbar();

        // 初始化按钮及URL编辑框
        urlText = findViewById(R.id.dapp_url);
        urlText.setText("https://dice2.win/");

        initWeb3View();

        sendButton = findViewById(R.id.go);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                web3View.loadUrl(urlText.getText().toString());
                web3View.requestFocus();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DAPP_REQUEST_CODE) {
            if (resultCode == DAPP_REQUEST_CODE) {
                if (TextUtils.isEmpty(data.getStringExtra(C.DAPP_RESULT_CODE))) {
                    web3View.onSignCancel(transaction);
                } else
                    web3View.onSignTransactionSuccessful(transaction, data.getStringExtra(C.DAPP_RESULT_CODE));
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    private void initWeb3View(){
        web3View = findViewById(R.id.web3view);
        Log.d("chainId", ethereumNetworkRepository.getDefaultNetwork().chainId + "");
        Log.d("rpcServerUrl", ethereumNetworkRepository.getDefaultNetwork().rpcServerUrl + "");
        //此处设置的区块链ID及节点RPC URL是根据用户在设置里面选择的不同网络来返回的
        web3View.setChainId(ethereumNetworkRepository.getDefaultNetwork().chainId);
        web3View.setRpcUrl(ethereumNetworkRepository.getDefaultNetwork().rpcServerUrl);
        findDefaultWalletInteract
                .find()
                .subscribe(wallet -> {
                    web3View.setWalletAddress(new Address(wallet.address));
                });
        // 拦截发起交易消息，在钱包本地设置参数并跳转到发起交易的界面
        web3View.setOnSignTransactionListener(transaction -> {
            this.transaction = transaction;
            Intent intent = new Intent(this, ConfirmationActivity.class);
            intent.putExtra(C.EXTRA_TO_ADDRESS, transaction.recipient.toString());
            intent.putExtra(C.EXTRA_AMOUNT, transaction.value.toString());
            intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, transaction.contract);
            intent.putExtra(C.EXTRA_DECIMALS, 18);
            intent.putExtra(C.EXTRA_DAPP_DATA, transaction.payload);
            intent.putExtra(C.EXTRA_IS_DAPP, true);
            intent.putExtra(C.EXTRA_SYMBOL, ethereumNetworkRepository.getDefaultNetwork().symbol);
            intent.putExtra(C.EXTRA_SENDING_TOKENS, false);
            startActivityForResult(intent, DAPP_REQUEST_CODE);
        });

    }

}
