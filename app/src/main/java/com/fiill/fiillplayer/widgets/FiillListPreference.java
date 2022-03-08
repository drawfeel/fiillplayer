/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fiill.fiillplayer.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.fiill.fiillplayer.R;

public class FiillListPreference extends ListPreference {
    private CharSequence[] mEntrySummaries;

    public FiillListPreference(Context context) {
        super(context);
        initPreference(context, null);
    }

    public FiillListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPreference(context, attrs);
    }

    public FiillListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPreference(context, attrs);
    }

    public FiillListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPreference(context, attrs);
    }

    private void initPreference(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.FiillListPreference, 0, 0);
        if (a == null)
            return;

        mEntrySummaries = a
                .getTextArray(R.styleable.FiillListPreference_entrySummaries);

        a.recycle();
    }

    @Override
    public void onSetInitialValue(Object defaultValue) {
        super.onSetInitialValue(defaultValue);
        syncSummary();
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        syncSummary();
    }

    @Override
    public void setValueIndex(int index) {
        super.setValueIndex(index);
        syncSummary();
    }

    public int getEntryIndex() {
        CharSequence[] entryValues = getEntryValues();
        CharSequence value = getValue();

        if (entryValues == null || value == null) {
            return -1;
        }

        for (int i = 0; i < entryValues.length; ++i) {
            if (TextUtils.equals(value, entryValues[i])) {
                return i;
            }
        }

        return -1;
    }

    public void setEntrySummaries(CharSequence[] entrySummaries) {
        mEntrySummaries = entrySummaries;
        notifyChanged();
    }

    private void syncSummary() {
        int index = getEntryIndex();
        if (index < 0)
            return;

        if (mEntrySummaries != null && index < mEntrySummaries.length) {
            setSummary(mEntrySummaries[index]);
        } else {
            setSummary(getEntries()[index]);
        }
    }
}
