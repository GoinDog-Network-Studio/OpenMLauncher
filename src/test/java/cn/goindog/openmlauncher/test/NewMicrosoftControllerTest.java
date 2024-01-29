package cn.goindog.openmlauncher.test;

import cn.goindog.OpenMLauncher.account.Microsoft.*;

public class NewMicrosoftControllerTest {
    public static void main(String[] args) {
        MicrosoftOAuthOptions options = new MicrosoftOAuthOptions();
        options.addScope("XboxLive.signin");
        options.addScope("offline_access");
        options.setOAuthType(MicrosoftOAuthType.DEVICE);

        MicrosoftController controller = new MicrosoftController();
        controller.build(options);
    }
}
