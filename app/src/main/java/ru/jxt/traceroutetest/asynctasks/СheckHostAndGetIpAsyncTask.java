package ru.jxt.traceroutetest.asynctasks;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;

public class СheckHostAndGetIpAsyncTask extends AsyncTask<String, Void, InetAddress> {
    private Context mContext;
    private String error;

    public СheckHostAndGetIpAsyncTask(Context mContext) {
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
                for (InetAddress ipa : ipAddresses) {
                    if (ipa instanceof Inet6Address)
                        ipAddresses.remove(ipa);
                }
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

    private InetAddress[] getAllIpAddresses(String hostname) {
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
