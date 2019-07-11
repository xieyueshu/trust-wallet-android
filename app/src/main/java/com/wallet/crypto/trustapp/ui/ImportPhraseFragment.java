package com.wallet.crypto.trustapp.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.ui.widget.OnImportPhraseKeyListener;
import com.wallet.crypto.trustapp.ui.widget.OnImportPrivateKeyListener;

public class ImportPhraseFragment extends Fragment implements View.OnClickListener {

    private static final OnImportPhraseKeyListener dummyOnImportPhraseKeyListener = key -> { };

    private EditText phraseKey;
    private OnImportPhraseKeyListener onImportPhraseKeyListener;
    private String testData="kite ramp whale waste airport erase stage dial endorse title arrest fall";
    public static ImportPhraseFragment create() {
        return new ImportPhraseFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getContext())
                .inflate(R.layout.fragment_import_phrase, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        phraseKey = view.findViewById(R.id.phrase_key);
        phraseKey.setText(testData);
        view.findViewById(R.id.import_action).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        String value = phraseKey.getText().toString();
        if (TextUtils.isEmpty(value)) {
            phraseKey.setError(getString(R.string.error_field_required));
        } else {
            onImportPhraseKeyListener.onPhraseKey(phraseKey.getText().toString());
        }
    }

    public void setOnImportPhraseKeyListener(OnImportPhraseKeyListener onImportPhraseKeyListener) {
        this.onImportPhraseKeyListener = onImportPhraseKeyListener == null
                ? dummyOnImportPhraseKeyListener
                : onImportPhraseKeyListener;
    }
}
