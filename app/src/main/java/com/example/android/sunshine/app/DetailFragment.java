package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

/**
 * Created by jnusca on 3/28/16.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int DETAILS_LOADER = 0;

    private final String LOG_TAG = DetailFragment.class.getSimpleName();
    private ShareActionProvider mShareActionProvider;
    private String mForecastStr;

    TextView mDayView;
    TextView mDateView;
    TextView mHighView;
    TextView mLowView;
    TextView mDescriptionView;
    TextView mHumidityView;
    TextView mWindView;
    TextView mPressureView;
    ImageView mForecastImg;

    private static final String[] DETAIL_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_TABLE_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_WEATHER_HUMIDITY = 5;
    static final int COL_WEATHER_WIND_SPEED = 6;
    static final int COL_WEATHER_DEGREES = 7;
    static final int COL_WEATHER_PRESSURE = 8;
    static final int COL_WEATHER_ID = 9;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mDayView = (TextView)rootView.findViewById(R.id.day_text);
        mDateView = (TextView)rootView.findViewById(R.id.date_text);
        mHighView = (TextView)rootView.findViewById(R.id.high_text);
        mLowView = (TextView)rootView.findViewById(R.id.low_text);
        mDescriptionView = (TextView)rootView.findViewById(R.id.description_text);
        mHumidityView = (TextView)rootView.findViewById(R.id.humidity_text);
        mWindView = (TextView)rootView.findViewById(R.id.wind_text);
        mPressureView = (TextView)rootView.findViewById(R.id.pressure_text);
        mForecastImg = (ImageView)rootView.findViewById(R.id.forecast_icon);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAILS_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//            super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.detailfragment, menu);

        MenuItem shareItem = menu.findItem(R.id.action_share);
        ShareActionProvider shareActionProvider = //(ShareActionProvider)shareItem.getActionProvider();
                (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

        if (shareActionProvider != null && mForecastStr != null)
            shareActionProvider.setShareIntent(createShareForecastIntent());
        else
            Log.d(LOG_TAG, "ShareActionProvider or mForecast is null (?)");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "In onCreateLoader");
        Intent intent = getActivity().getIntent();
        if (intent == null) {
            return null;
        }

        return new CursorLoader(getActivity(),
                intent.getData(),
                DETAIL_COLUMNS,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(LOG_TAG, "In onLoadFinished");
        if (!data.moveToFirst()) {
            return;
        }

        // Read user preference for metric or imperial temperature units
        boolean isMetric = Utility.isMetric(getActivity());

        long date = data.getLong(COL_WEATHER_DATE);
        String dayName = Utility.getDayName(getActivity(), date);
        mDayView.setText(dayName);

        String dateString = Utility.getFormattedMonthDay(getActivity(), date);
        mDateView.setText(dateString);

        mHighView.setText(Utility.formatTemperature(
                getActivity(), data.getFloat(COL_WEATHER_MAX_TEMP), isMetric));
        mLowView.setText(Utility.formatTemperature(
                getActivity(), data.getFloat(COL_WEATHER_MIN_TEMP), isMetric));

        int weatherId = data.getInt(COL_WEATHER_ID);
        int iconResource = Utility.getArtResourceForWeatherCondition(weatherId);
        mForecastImg.setImageResource(iconResource);

        mDescriptionView.setText(data.getString(COL_WEATHER_DESC));

        mHumidityView.setText(getActivity().getString(
                R.string.format_humidity, data.getDouble(COL_WEATHER_HUMIDITY)));

        mWindView.setText(Utility.getFormattedWind(
                getActivity(),
                data.getFloat(COL_WEATHER_WIND_SPEED),
                data.getFloat(COL_WEATHER_DEGREES)
        ));

        mPressureView.setText(getActivity().getString(
                R.string.format_pressure, data.getFloat(COL_WEATHER_PRESSURE)));

        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Don't need to do anything here because the loader isn't holding on to any data
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastStr + " #SunshineApp");
        return shareIntent;
    }
}
