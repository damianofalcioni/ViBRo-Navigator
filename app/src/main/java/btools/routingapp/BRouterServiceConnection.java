package btools.routingapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BRouterServiceConnection implements ServiceConnection {

    private IBRouterService brouterService;
    private boolean bindingDied;
    private boolean nullBinding;

    public void onServiceConnected(ComponentName className, IBinder boundService) {
        brouterService = IBRouterService.Stub.asInterface(boundService);
        bindingDied = false;
        nullBinding = false;
    }

    public void onServiceDisconnected(ComponentName className) {
        brouterService = null;
    }

    @Override
    public void onBindingDied(@NonNull ComponentName name) {
        brouterService = null;
        bindingDied = true;
    }

    @Override
    public void onNullBinding(@NonNull ComponentName name) {
        brouterService = null;
        nullBinding = true;
    }

    public void disconnect(@NonNull Context ctx) {
        ctx.unbindService(this);
    }

    @Nullable
    public IBRouterService getBrouterService() {
        return brouterService;
    }

    public boolean hasBindingDied() {
        return bindingDied;
    }

    public boolean hasNullBinding() {
        return nullBinding;
    }

    @Nullable
    public static BRouterServiceConnection connect(@NonNull Context ctx) {
        BRouterServiceConnection conn = new BRouterServiceConnection();
        Intent intent = new Intent();
        intent.setClassName("btools.routingapp", "btools.routingapp.BRouterService");
        boolean hasBRouter = ctx.bindService(intent, conn, Context.BIND_AUTO_CREATE);
        if (!hasBRouter) {
            conn = null;
        }
        return conn;
    }
}
