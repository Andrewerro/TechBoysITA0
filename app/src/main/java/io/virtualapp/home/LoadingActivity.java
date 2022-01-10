package io.virtualapp.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;

import java.util.Locale;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.PackageAppDataStorage;
import io.virtualapp.widgets.EatBeansView;

/**
 * @author Lody
 */

public class LoadingActivity extends VActivity {

    private static final String PKG_NAME_ARGUMENT = "MODEL_ARGUMENT";
    private static final String KEY_INTENT = "KEY_INTENT";
    private static final String KEY_USER = "KEY_USER";
    private PackageAppData appModel;
    private EatBeansView loadingView;

    public static void launch(Context context, String packageName, int userId) {
        try
        {
            Intent intent = VirtualCore.get().getLaunchIntent(packageName, userId);
            if (intent != null)
            {
                Intent loadingPageIntent = new Intent(context, LoadingActivity.class);
                loadingPageIntent.putExtra(PKG_NAME_ARGUMENT, packageName);
                loadingPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                loadingPageIntent.putExtra(KEY_INTENT, intent);
                loadingPageIntent.putExtra(KEY_USER, userId);
                context.startActivity(loadingPageIntent);
            }
        }
        catch(Throwable e)
        {
            // ignored.
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_loading);
            loadingView = (EatBeansView) findViewById(R.id.loading_anim);
            int userId = getIntent().getIntExtra(KEY_USER, -1);
            String pkg = getIntent().getStringExtra(PKG_NAME_ARGUMENT);
            appModel = PackageAppDataStorage.get().acquire(pkg);
            ImageView iconView = (ImageView) findViewById(R.id.app_icon);
            iconView.setImageDrawable(appModel.icon);
            TextView nameView = (TextView) findViewById(R.id.app_name);
            nameView.setText(String.format(Locale.CHINESE, "正在打开 %s...", appModel.name));
            Intent intent = getIntent().getParcelableExtra(KEY_INTENT);
            if (intent == null) {
                Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            VirtualCore.get().setUiCallback(intent, mUiCallback);
            VUiKit.defer().when(() -> {
                if (!appModel.fastOpen) {
                    try {
                        VirtualCore.get().preOpt(appModel.packageName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                VActivityManager.get().startActivity(intent, userId);
            });
        }catch (Throwable e)
        {
            e.printStackTrace();
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private final VirtualCore.UiCallback mUiCallback = new VirtualCore.UiCallback() {

        @Override
        public void onAppOpened(String packageName, int userId) throws RemoteException {
            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        loadingView.startAnim();
    }

    @Override
    protected void onPause() {
        super.onPause();
        loadingView.stopAnim();
    }
}
