package com.axelio.recrutamento;

import android.content.ClipData;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Axelio on 9/29/16.
 */

public class EpisodeAdapter extends ArrayAdapter<String> {

    public EpisodeAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public EpisodeAdapter(Context context, int resource, List<String> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null)
        {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.list_row_episode, null);
        }

        String title = getItem(position);

        if (title != null)
        {
            TextView textViewTitle = (TextView) v.findViewById(R.id.textview_episode);
            TextView textViewNumber = (TextView) v.findViewById(R.id.textview_number_episode);

            if (textViewTitle != null)
            {
                textViewTitle.setText(title);
            }

            if (textViewNumber != null) {
                textViewNumber.setText("E"+ (position + 1));
            }

        }

        return v;
    }

}
