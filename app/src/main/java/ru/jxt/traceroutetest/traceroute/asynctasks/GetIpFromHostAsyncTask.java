package ru.jxt.traceroutetest.traceroute.asynctasks;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;

/*
* Асинтаск для получения массива ip адресов хоста
* удаления всех IPv6 адресов
* и возврата в onPostExecute первого IPv4 адреса
* InetAddress.getAllByName работает только в отдельном потоке
*
*/
public class GetIpFromHostAsyncTask extends AsyncTask<String, Void, InetAddress> {
    private Context mContext;
    private String error;

    public GetIpFromHostAsyncTask(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    protected InetAddress doInBackground(String... host) {

        //получаем все ip-адреса хоста
        InetAddress[] ipAddressesArray = getAllIpAddresses(host[0]);
        if(ipAddressesArray != null && ipAddressesArray.length != 0) {
            try {
                LinkedList<InetAddress> ipAddresses = new LinkedList<>(Arrays.asList(ipAddressesArray));
                //удаляем все ipv6 адреса
                removeIPv6Addresses(ipAddresses);
                //если оказалось, что массив состоит только из ipv6 адресов - устанавливаем ошибку
                if (ipAddresses.isEmpty())
                    error = "Host doesn't contain ipv4";
                else //иначе возвращаем ip-адрес в виде объекта InetAddress
                    return InetAddress.getByName(ipAddresses.get(0).getHostAddress());
            }
            catch (UnknownHostException e) {
                error = "Unknown host";
            }
        }
        return null;
    }

    private void removeIPv6Addresses(LinkedList<InetAddress> ipAddresses) {
        for (InetAddress ipa : ipAddresses) {
            if (ipa instanceof Inet6Address)
                ipAddresses.remove(ipa);
        }
    }

    private InetAddress[] getAllIpAddresses(@NonNull String hostname) {
        try { //получаем все ip-адреса хоста, getAllByName не работает в UI-потоке
            return InetAddress.getAllByName(hostname);
        } catch (UnknownHostException e) {
            if (isOnline())
                error = "Unknown host"; //если есть подключение к сети, то ошибка в хосте
            else
                error = "Error connection"; //если сети нет, то ошибка в соединении
            return null;
        }
    }

    protected String getError() {
        return error;
    }

    private boolean isOnline() { //проверка наличия подключения к сети
        NetworkInfo netInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return !(netInfo == null || !netInfo.isConnectedOrConnecting());
    }
}
