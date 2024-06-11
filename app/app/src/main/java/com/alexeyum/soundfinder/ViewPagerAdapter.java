package com.alexeyum.soundfinder;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final SparseArray<Fragment> registeredFragments = new SparseArray<>();
    private static final int NUM_PAGES = 2;

    public ViewPagerAdapter(FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = null;
        if (position == 0)
            fragment = new ResultsFragment();
        else if (position == 1)
            fragment = new InfoFragment();

        registeredFragments.put(position, fragment);
        return fragment;
    }

    public Fragment getFragment(int position) {
        return registeredFragments.get(position);
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }

}
