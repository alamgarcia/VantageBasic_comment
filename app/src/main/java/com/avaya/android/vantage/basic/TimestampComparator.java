package com.avaya.android.vantage.basic;

import com.avaya.android.vantage.basic.model.CallData;

import java.util.Comparator;

/**
 * Comparator used for Call logs sorting. Sorting is done using timestamps.
 */
public class TimestampComparator implements Comparator<CallData> {

    @Override
    public int compare(CallData lhs, CallData rhs) {
        try {
            if (rhs.mCallDateTimestamp < lhs.mCallDateTimestamp)
                return -1;
            else if (rhs.mCallDateTimestamp >= lhs.mCallDateTimestamp)
                return 1;
            else
                return 0;
        }catch (Exception e){
            return 0;
        }
    }
}
