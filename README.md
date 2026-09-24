# 貯金ガード / Savings Guard

Galaxy S25 / Android 16 向けの個人用Androidアプリの試作。Android Studioでこの `ore` フォルダを開く。
スマートフォンの初期化、DB、サーバー、ログイン、ネットワーク権限は不要。

## 動作

- 日本のマクドナルド (`jp.co.mcdonalds.android`)、ロケットナウ (`com.cpone.customer`)、Uber Eats (`com.ubercab.eats`) のアクティブ画面を検知し、短時間のカバーを表示してホームへ戻す。
- パスワードや通常の解除機能はない。対象アプリを再インストールしても同じパッケージ名なら対象となる。
- 「設定保護を開始する」を押すと、貯金ガード自身のユーザー補助詳細／アプリ情報を判定してホームへ戻す。一般の設定や他アプリの情報画面を一律に禁止しない。
- 設定保護開始のフラグ1個をSharedPreferencesに保存。DBは使わない。画面内容・注文内容・利用履歴は保存／送信しない。
- ユーザー補助サービスは画面のパッケージ名を確認し、設定保護が有効な場合に限りAndroid設定画面の表示テキストを調べる。通知や入力テキストの収集は行わない。

## ビルド

既存のAndroid Studio環境に合わせたAGP 9.4.1 / Gradle 9.6.0。compile SDK 37、target SDK 36（Android 16）、最低Android 12。
Android StudioのGradle JDKに対応JDK（このPCではAndroid Studio1付属JBR 25）を指定する。
`local.properties` のSDKパスはローカル専用でGit対象外。
日本語を含むWindowsパスで動作するよう、パス確認の上書きとGradle JVMの `file.encoding=COMPAT` を設定。Javaソースの読み取りはUTF-8を明示する。

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio1\jbr'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

APKをプロジェクト内へ出力するには ` .\gradlew.bat :app:exportDebugApk` を実行。
配布用APK: `deliverables/savings-guard-debug.apk`

OneDriveによる生成ファイルのロックを避けるため、ビルド中間ファイルとレポートはGradleユーザーホーム下の `savings-guard-builds/<保存先別ID>/app` に置く。ソースはすべてこのプロジェクト内にある。必要なら `-PsavingsBuildRoot=C:/任意の保存先` で変更可能。

## 初回セットアップ（スマホ操作）

1. Android StudioのRun、またはdebug APKを使ってインストールする。
2. 貯金ガードを開き「ユーザー補助を設定する」から貯金ガードをONにする。端末によっては「インストール済みアプリ」にある。
3. Androidの権限説明を読み、自分で許可する。APKから入れた場合の制限付き設定は、必要に応じてAndroidの案内に従う。
4. この画面へ戻り「3アプリを保護中」を確認。各対象アプリを起動して、ホームに戻れることを先に確認する。
5. 「設定保護を開始する」を押す。開始後、アプリ内から解除できない。ユーザー補助のショートカットは設定しない（ショートカットからOFFにできる可能性がある）。
6. 下記の実機確認を実施する。設定保護の文字・クラス判定はGalaxy One UIで未検証のため、必要なら端末に合わせて調整する。

## 実機チェック

- 3アプリそれぞれ：ランチャー、履歴、通知／リンクから開くとホームに戻る。すばやい再起動でも繰り返しブロックする。
- 別アプリのバックグラウンドイベントでホームへ戻らない。
- 設定保護前：ON/OFF設定を通常どおり操作できる。
- 設定保護後：自身のユーザー補助詳細とアプリ情報でホームに戻る。
- Wi-Fi・Bluetooth・他のユーザー補助・他アプリ情報を普通に操作できる。
- 画面回転、画面消灯／復帰、再起動後の動作とサービス状態。
- 分割画面・ポップアップ表示で対象アプリにフォーカスしたときの挙動。
- サービスOFF時は画面に「保護がOFFです」と表示され、保護中と誤表示しない。

## 限界と開発時の復旧

通常のユーザー補助サービスであり、Device OwnerやOSによる強制禁止ではない。
OFF・強制停止・アンインストール・セーフモードなどで回避できる。設定保護は操作を難しくするためのベストエフォートで、画面構成や言語、One UI更新により検知できない場合がある。
対象アプリの起動自体は止めないため一瞬表示され得る。ブラウザー版や別パッケージのアプリは対象外。
サービスはイベントと600ms間隔の確認を使用するため、電池使用量は実機で確認する。

誤検知の修正中に設定へ進めない場合は、開発PCからこのアプリだけを削除して修正版を入れ直せる（スマホの初期化は不要）。データは設定保護フラグのみ。

```powershell
adb uninstall jp.local.savingsguard
```

このコマンドは開発時の復旧手段であり、実行すると制限が解除される。日常用のアプリ内解除ボタンは設けない。
Google Playへの公開・審査対応は今回の範囲外。ユーザー補助の用途を偽装せず `isAccessibilityTool=false` としている。

## 検証結果（2026-09-25）

- `:app:exportDebugApk testDebugUnitTest lintDebug` 成功。
- 判定ロジックのJUnitテスト6件成功（対象パッケージ、初期設定、日英の設定保護、他アプリ／通常の設定の誤検知防止）。
- Lint: エラー0件。Android 16を対象にしているため、最新APIをtargetにしていない旨の警告1件あり。
- Galaxy S25実機へのインストール・画面動作・One UIの設定保護・再起動・電池使用量は未検証。
