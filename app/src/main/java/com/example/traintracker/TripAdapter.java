package com.example.traintracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class TripAdapter extends ArrayAdapter<Trip> {
    /**
     * Constructs a new {@code TripAdapter}.
     *
     * @param context The current context. The adapter uses this to inflate the layout file.
     * @param trips A list of {@code Trip} objects to display in the list.
     */
    public TripAdapter(Context context, ArrayList<Trip> trips) {
        super(context, 0, trips);
    }

    /**
     * Get a View that displays the data at the specified position in the data set.
     * This method is responsible for creating and populating the view for a single list item.
     * It inflates the layout from `R.layout.item_trip`, retrieves the `Trip` object for the current position,
     * and sets the text for the various TextViews (route, details, kilometers, and duration) based on the `Trip` object's properties.
     * It uses the "convertView" pattern to recycle views for better performance.
     *
     * @param position The position of the item within the adapter's data set of the item whose view we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view is non-null and of an appropriate type before using. If it is not possible to convert this view to display the correct data, this method can create a new view.
     * @param parent The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Trip trip = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_trip, parent, false);
        }
        TextView tvRoute = convertView.findViewById(R.id.tvRoute);
        TextView tvDetails = convertView.findViewById(R.id.tvDetails);
        TextView tvKm = convertView.findViewById(R.id.tvKm);
        TextView tvDuration = convertView.findViewById(R.id.tvDuration);

        tvRoute.setText(trip.route);
        tvDetails.setText(trip.details);
        tvKm.setText(trip.kmFormatted);
        tvDuration.setText(trip.durationText);

        return convertView;
    }
}