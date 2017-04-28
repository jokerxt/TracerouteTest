package ru.jxt.traceroutetest.ui.callbacks;

import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import ru.jxt.traceroutetest.traceroute.helpers.ReportsHelper;

/*
* Слушатель вызывается при долгом удерживании на view с трассируемым адресом
* Он добавляет в контекстное меню пункт "Удалить"
* При нажатии на "Удалить" - удаляется view с трассируемым адресом
* и отчет из списка отчетов при помощи ReportsHelper
*/
public class OnItemCreateContextMenuListener implements View.OnCreateContextMenuListener {
    @Override
    public void onCreateContextMenu(ContextMenu menu, final View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(menu != null) {
            //добавим меню "Delete" по долгому тапу
            menu.add("Удалить").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
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
