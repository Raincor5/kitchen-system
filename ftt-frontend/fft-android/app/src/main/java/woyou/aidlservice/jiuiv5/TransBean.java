package woyou.aidlservice.jiuiv5;

import android.os.Parcel;
import android.os.Parcelable;

public class TransBean implements Parcelable {
    public int type;
    public byte[] data;

    public TransBean() {
    }

    protected TransBean(Parcel in) {
        type = in.readInt();
        data = in.createByteArray();
    }

    public static final Creator<TransBean> CREATOR = new Creator<TransBean>() {
        @Override
        public TransBean createFromParcel(Parcel in) {
            return new TransBean(in);
        }

        @Override
        public TransBean[] newArray(int size) {
            return new TransBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeByteArray(data);
    }
} 