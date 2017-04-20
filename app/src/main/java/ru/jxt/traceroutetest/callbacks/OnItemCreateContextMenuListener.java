package ru.jxt.traceroutetest.callbacks;

import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import ru.jxt.traceroutetest.helpers.ReportsHelper;

public class OnItemCreateContextMenuListener implements View.OnCreateContextMenuListener {
    @Override
    public void onCreateContextMenu(ContextMenu menu, final View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(menu != null) {
            //добавим меню "Delete" по долгому тапу
            menu.add("Delete").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    ((LinearLayout) v.getParent()).removeView(v); //удаляем TracedItemView из родительского View
                    int key = (int) v.getTag();  //получаем ключ
                    ReportsHelper.removeReport(key); //и удаляем отчет из списка отчетов
                    return true;
                }
            });
        }
    }
}
