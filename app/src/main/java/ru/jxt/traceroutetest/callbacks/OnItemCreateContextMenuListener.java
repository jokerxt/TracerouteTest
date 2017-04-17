package ru.jxt.traceroutetest.callbacks;

import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import ru.jxt.traceroutetest.MainActivity;

public class OnItemCreateContextMenuListener implements View.OnCreateContextMenuListener {
    @Override
    public void onCreateContextMenu(ContextMenu menu, final View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(menu != null) {
            menu.add("Delete").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    ((LinearLayout) v.getParent()).removeView(v);
                    int key = (int) v.getTag();
                    MainActivity.reports.remove(key);
                    return true;
                }
            });
        }
    }
}
