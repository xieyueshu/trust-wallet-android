package com.wallet.crypto.trustapp.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class Wallet implements Parcelable {
    public String address;
    public String mnemonic;
    public boolean isMnemonic;

    public Wallet(String address) {
        this.address = address;
    }
    public Wallet(String address,boolean isMnemonic) {
        this.address = address;
        this.isMnemonic=isMnemonic;
    }
    private Wallet(Parcel in) {
        address = in.readString();
        mnemonic = in.readString();
        isMnemonic = in.readByte() != 0;
    }

    public static final Creator<Wallet> CREATOR = new Creator<Wallet>() {
        @Override
        public Wallet createFromParcel(Parcel in) {
            return new Wallet(in);
        }

        @Override
        public Wallet[] newArray(int size) {
            return new Wallet[size];
        }
    };

    public boolean sameAddress(String address) {
        return this.address.equals(address);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(address);
        parcel.writeString(mnemonic);
        parcel.writeByte((byte) (this.isMnemonic ? 1 : 0));
    }
}
