package org.lineageos.settings.hypercharge;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

public class NoHighlightListPreference extends ListPreference {

    public NoHighlightListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public NoHighlightListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NoHighlightListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoHighlightListPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setActivated(false);
        holder.itemView.setBackgroundResource(android.R.color.transparent);
    }
}