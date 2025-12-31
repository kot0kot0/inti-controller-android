// トップレベルのビルドファイル
buildscript {
    // 拡張プロパティの定義
    extra.apply {
        set("compose_ui_version", "1.1.1")
    }
}

plugins {
    // 各モジュールで使用するプラグインのバージョンを一括管理
    // apply false は「ここでは定義だけして、実行は各モジュールで行う」という意味です
    id("com.android.application") version "7.3.1" apply false
    id("com.android.library") version "7.3.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}
